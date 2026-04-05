import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    # Si estas variables no están en el .env, el sistema se cae intencionalmente (Fail Fast)
    RABBITMQ_URL: str = os.environ["RABBITMQ_URL"]
    DATABASE_URL: str = os.environ["DATABASE_URL"]

    # Estas sí pueden tener un fallback porque son lógica de negocio (no secretos)
    SEARCH_RADIUS_KM: float = float(os.getenv("SEARCH_RADIUS_KM", "2.5"))
    TIMEOUT_SECONDS: int = int(os.getenv("TIMEOUT_SECONDS", "15"))


settings = Settings()