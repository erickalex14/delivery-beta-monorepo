import logging
from fastapi import FastAPI
from app.core.config import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.PROJECT_NAME)

@app.on_event("startup")
async def startup_event():
    logger.info("Inicializando motor de seguridad KYC automatizado.")

@app.get("/health")
def health_check():
    return {"status": "Servicio de validacion KYC operativo"}