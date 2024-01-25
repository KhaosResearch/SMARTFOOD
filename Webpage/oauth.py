import requests
from flask import url_for
from jwt import PyJWKClient
from requests_oauthlib import OAuth2Session

from config import settings

# Keycloak
KEYCLOAK_CONFIG = {
    "well_known_url": "https://auth.lifewatch.eu/realms/lifewatcheric/.well-known/openid-configuration",
    "client_id": settings.KEYCLOAK_CLIENT_ID,
    "client_secret": settings.KEYCLOAK_CLIENT_SECRET,
}

def get_well_known_metadata():
    response = requests.get(KEYCLOAK_CONFIG["well_known_url"])
    response.raise_for_status()
    return response.json()

def get_oauth2_session(**kwargs):
    oauth2_session = OAuth2Session(
        KEYCLOAK_CONFIG["client_id"],
        scope=["openid", "profile", "email", "attributes"],
        redirect_uri=url_for(".callback", _external=True, _scheme="https"),
        **kwargs,
    )
    return oauth2_session

def get_jwks_client():
    well_known_metadata = get_well_known_metadata()
    jwks_client = PyJWKClient(well_known_metadata["jwks_uri"])
    return jwks_client

jwks_client = get_jwks_client()
