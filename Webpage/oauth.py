import requests
from flask import url_for
from jwt import PyJWKClient
from requests_oauthlib import OAuth2Session

from config import settings

# Casdoor
SMRT_CONFIG = {
    "well_known_url": "https://auth.khaos.uma.es/.well-known/openid-configuration",
    "client_id": settings.CASDOOR_CLIENT_ID,
    "client_secret": settings.CASDOOR_CLIENT_SECRET,
    "scope": ["profile", "email", "openid"],
}


def get_well_known_metadata():
    response = requests.get(SMRT_CONFIG["well_known_url"])
    response.raise_for_status()
    return response.json()


def get_oauth2_session(**kwargs):
    oauth2_session = OAuth2Session(
        SMRT_CONFIG["client_id"],
        scope=SMRT_CONFIG["scope"],
        redirect_uri=url_for(".callback", _external=True),
        **kwargs,
    )
    return oauth2_session


def get_jwks_client():
    well_known_metadata = get_well_known_metadata()
    jwks_client = PyJWKClient(well_known_metadata["jwks_uri"])
    return jwks_client


jwks_client = get_jwks_client()
