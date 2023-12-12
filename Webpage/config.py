from pathlib import Path

from pydantic import BaseSettings


class _Settings(BaseSettings):
    CASDOOR_CLIENT_ID: str = None
    CASDOOR_CLIENT_SECRET: str = None
    APP_SECRET_KEY: str = None
    GC_PROJECT_ID: str = None
    MAX_FILE_SIZE_IN_BYTES: int = 209715200 

    class Config:
        env_file = "credentials/.env"
        file_path = Path(env_file)
        if not file_path.is_file():
            print("⚠️ `.env` not found in current directory")
            print("⚙️ Loading settings from environment")
        else:
            print(f"⚙️ Loading settings from dotenv @ {file_path.absolute()}")


settings = _Settings()
