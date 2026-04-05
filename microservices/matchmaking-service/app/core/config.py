import os
from dotenv import load_dotenv

load_dotenv()

class Settings:

    RABBITMQ_url: str = os.getenv("RABBITMQ_url")
    DATABASE_URL: str = os.getenv("DATABASE_URL")

    #logica matchmaking
    SEARCH_RADIUS_KM: float = 2.5 # El radio de búsqueda que definiste en tu informe
    TIMEOUT_SECONDS: int = 15     # El tiempo que la moto tiene para aceptar el viaje

settings = Settings()

