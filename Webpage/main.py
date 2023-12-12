import json
import os
import threading
from pathlib import Path

import ee
import jwt
from flask import (
    Flask,
    abort,
    flash,
    redirect,
    render_template,
    request,
    session,
    url_for,
)
from google.cloud import storage
from google.oauth2 import service_account
from jwt.exceptions import DecodeError
from werkzeug.exceptions import InternalServerError, Unauthorized

from config import settings
from oauth import SMRT_CONFIG, get_oauth2_session, get_well_known_metadata, jwks_client
from utils import (
    getHash,
    task_with_status,
    unzip_shapefile,
    upload_blob,
    upload_table_to_gee,
)

app = Flask(__name__)

# Set the secret key for the Flask app
app.secret_key = settings.APP_SECRET_KEY
# app.secret_key = settings.CASDOOR_CLIENT_SECRET
app.config.from_object("config.settings")


## Inicialize GEE
with open("credentials/key.json") as source:
    info = json.load(source)

storage_credentials = service_account.Credentials.from_service_account_info(info)
credentials = ee.ServiceAccountCredentials(
    settings.SERCIVE_ACCOUNT, "credentials/gee-cli-key.json"
)
ee.Initialize(credentials)


## Inicialize GCS
storage_client = storage.Client(
    project=settings.GC_PROJECT_ID, credentials=storage_credentials
)
bucket = storage_client.bucket("parcelas")


@app.route("/", methods=["GET", "POST"])
def login_page():
    if request.method == "GET":
        return render_template("login.html")


@app.route("/login")
def login():
    well_known_metadata = get_well_known_metadata()
    oauth2_session = get_oauth2_session()
    authorization_url, state = oauth2_session.authorization_url(
        well_known_metadata["authorization_endpoint"]
    )
    session["oauth_state"] = state
    session["next_url"] = request.referrer
    return redirect(authorization_url)


@app.route("/callback_general")
def callback():
    well_known_metadata = get_well_known_metadata()
    oauth2_session = get_oauth2_session(state=session["oauth_state"])

    session["oauth_token"] = oauth2_session.fetch_token(
        well_known_metadata["token_endpoint"],
        client_secret=SMRT_CONFIG["client_secret"],
        code=request.args["code"],
    )["id_token"]
    token = session["oauth_token"]
    try:
        signing_key = jwks_client.get_signing_key_from_jwt(token)
        header_data = jwt.get_unverified_header(token)
        request.user_data = jwt.decode(
            token,
            signing_key.key,
            algorithms=[header_data["alg"]],
            audience=SMRT_CONFIG["client_id"],
        )
        session["email"] = request.user_data["email"]

    except DecodeError:
        return Unauthorized("Authorization token is invalid")
    except Exception:
        return InternalServerError("Error authenticating client")
    return redirect(session["next_url"])


def login_is_required(function):
    def wrapper(*args, **kwargs):
        if "email" not in session:
            return abort(401)
        else:
            return function()

    return wrapper


@app.route("/services")
def services():
    return render_template("service.html")


@app.route("/VRE1/home_VRE", methods=["GET"])
@login_is_required
def home_VRE():
    """Endpoint for uploading zip files and uploading them to GEE"""

    username = session["email"]
    if request.method == "GET":
        try:
            # List the content of the path given, if it exits
            ee.data.getAsset(
                f"projects/{settings.GC_PROJECT_ID}/assets/{getHash(username)}"
            )

        except ee.ee_exception.EEException:
            parent_asset = ee.data.createAsset(
                {"type": "FOLDER"},
                f"projects/{settings.GC_PROJECT_ID}/assets/{getHash(username)}",
            )
            assetId = parent_asset["id"]
            permissions = {"readers": [], "writers": [], "all_users_can_read": True}
            _ = ee.data.setAssetAcl(assetId, permissions)
            print("New user, folder created")
    return render_template("index.html", hash=getHash(username))


@app.route("/VRE1/upload-shapefile", methods=["GET", "POST"])
def upload_page():
    """Endpoint for zip file upload and upload to GEE"""
    if session:
        username = session["email"]
        if request.method == "GET":
            return render_template("upload.html.j2", hash=getHash(username), size_file=settings.MAX_FILE_SIZE_IN_BYTES)
        elif request.method == "POST":
            zipfile = request.files["zipfile"]
            zipfile.filename = (zipfile.filename).replace(" ", "")
            zipfile.save(zipfile.filename)

            if (zipfile.filename).endswith(".zip"):
                zip_filename = (zipfile.filename)[:-4]
                shapefile_result, error_code = unzip_shapefile(zipfile.filename)
                if error_code == 1:
                    Path(zipfile.filename).unlink(missing_ok=True)
                    flash(shapefile_result)
                    return render_template("upload.html.j2", hash=getHash(username), size_file=settings.MAX_FILE_SIZE_IN_BYTES)

                else:
                    # Upload zip to Google Cloud Storage
                    upload_blob(bucket, username, zipfile.filename)
                    Path(zipfile.filename).unlink(missing_ok=True)

                    upload_thread = threading.Thread(
                        target=upload_table_to_gee,
                        name="Uploader",
                        args=(username, zipfile.filename, zip_filename),
                    )
                    upload_thread.start()

            else:
                Path(zipfile.filename).unlink(missing_ok=True)
                flash("The file is not a zip")

            return redirect(url_for("upload_page"))
    else:
        return redirect(url_for("login_page"))


@app.route("/VRE1/data_management", methods=["GET", "POST"])
def get_status():
    """Endpoint to visualize the status of the task and the files uploaded to GEE"""
    if session:
        username = session["email"]
        if request.method == "GET":
            # Info for the table that contains the file uploaded and its status
            for user in task_with_status:
                if user == getHash(username):
                    for task in task_with_status[user]:
                        if (task["state"] != "SUCCEEDED") or (
                            task["state"] != "FAILED"
                        ):
                            operation_name = task["task_id"]
                            status_state = ee.data.getOperation(operation_name)
                            state = status_state["metadata"]["state"]
                            task["state"] = state

            parent_asset = ee.data.getAsset(
                f"projects/{settings.GC_PROJECT_ID}/assets/{getHash(username)}"
            )
            assetId = parent_asset["name"]
            files_in_gee = ee.data.listAssets({"parent": assetId})["assets"]
            files_per_user = {}
            files_list = []
            # Files uploaded to gee
            for file in files_in_gee:
                file_name = (file["name"]).split(f"/{getHash(username)}/")[1]
                files_list.append(file_name)
            files_per_user[getHash(username)] = files_list

            return render_template(
                "status.html.j2",
                json_file=task_with_status,
                hash=getHash(username),
                files_per_user=files_per_user,
            )
    else:
        return redirect(url_for("login_page"))


@app.route("/deletefile/<file>", methods=["GET", "POST"])
def delete_file(file):
    if session:
        username = session["email"]
        """ Endpoint to delete a specific file from GEE and Google Cloud storage"""
        if request.method == "GET":
            # Delete from GEE
            ee.data.deleteAsset(
                f"projects/{settings.GC_PROJECT_ID}/assets/{getHash(username)}/{file}"
            )
            # Delete from CS
            blob = bucket.blob(f"{getHash(username)}/{file}.zip")
            blob.delete()

            return redirect(url_for("get_status"))
    else:
        return redirect(url_for("login_page"))


@app.route("/VRE1/apps")
def apps():
    if session:
        username = session["email"]
        return render_template("apps.html", hash=getHash(username))
    else:
        return redirect(url_for("login_page"))


@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("login_page"))


if __name__ == "__main__":
    app.run(debug=False, port=8083)
