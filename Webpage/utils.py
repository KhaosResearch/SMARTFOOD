import hashlib
import time
import zipfile
from tempfile import TemporaryDirectory
from zipfile import BadZipfile

import ee
import fiona

from config import settings

## Json with task info
task_with_status = {}


def getHash(username: str):
    hexdigest = hashlib.blake2b(username.encode(), digest_size=8).hexdigest()
    hash = "-".join(hexdigest[i : i + 4] for i in range(0, len(hexdigest), 4))
    return hash


def unzip_shapefile(file):
    """Return the error code and the result of the function

    * error_code: 0 if everything goes well. 1 if there is some error
    * result: if error_code is 0, the geometry. If the error_code is 1, the reason of the error


    """
    try:
        with TemporaryDirectory() as tmpdirname:
            with zipfile.ZipFile(f"{file}", "r") as zip_ref:
                zip_ref.extractall(tmpdirname)

            try:
                with fiona.open(tmpdirname) as shp:
                    result = shp.meta["schema"]["geometry"]
                    error_code = 0

            except Exception:
                result = "The file does not have a shapefile format"
                error_code = 1
    except BadZipfile:
        result = "The zip is empty"
        error_code = 1

    return result, error_code


def upload_blob(bucket, username, destination_blob_name):
    """Upload a file to the bucket in Google Cloud Storage"""

    blob = bucket.blob(f"{getHash(username)}/{destination_blob_name}")

    blob.upload_from_filename(destination_blob_name)

    print("File uploaded to Google Cloud Storage")


def upload_table_to_gee(username, zip_filename_with_extension, zip_filename):
    """Upload the file from the bucket in Google Cloud Storage to GEE"""
    updated_table = ee.data.startTableIngestion(
        request_id=None,
        params={
            "id": f"projects/{settings.GC_PROJECT_ID}/assets/{getHash(username)}/{zip_filename}",
            "sources": [
                {
                    "primaryPath": f"gs://parcelas/{getHash(username)}/{zip_filename_with_extension}"
                }
            ],
        },
        allow_overwrite=True,
    )

    # Saves useful task information for future viewing
    state = ""
    task = {}
    task["file_name"] = zip_filename_with_extension
    task["state"] = state
    task["task_id"] = updated_table["name"]

    if getHash(username) in task_with_status:
        (task_with_status[getHash(username)]).append(task)
    else:
        task_with_status[getHash(username)] = []
        (task_with_status[getHash(username)]).append(task)

    while state != "SUCCEEDED" and state != "FAILED":
        status_state = ee.data.getOperation(updated_table["name"])
        state = status_state["metadata"]["state"]
        time.sleep(1)
        print("state:", state)

    if state == "SUCCEEDED":
        # Set public permission to the uploaded file
        table_permissions = {"readers": [], "writers": [], "all_users_can_read": True}
        ee.data.setAssetAcl(
            f"projects/{settings.GC_PROJECT_ID}/assets/{getHash(username)}/{zip_filename}",
            table_permissions,
        )
        print("Public permissions set")
