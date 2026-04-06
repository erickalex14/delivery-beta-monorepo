import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    # Usamos os.environ para aplicar el patron "Fail Fast".
    # Si la variable no existe en el .env, el microservicio no arrancara.
    PROJECT_NAME: str = os.environ["PROJECT_NAME"]
    TESSERACT_CMD_PATH: str = os.environ["TESSERACT_CMD_PATH"]
    APP_PORT: int = int(os.environ.get("APP_PORT", "8083"))

settings = Settings()