import asyncio
import logging
from sqlalchemy.ext.asyncio import AsyncSession
from app.schemas.payload import OrderCreatedPayload
from app.services.geo_engine import geo_engine
from app.rabbitmq.publisher import publisher

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)


class DispatcherService:

    async def process_dispatch(self, payload: OrderCreatedPayload, db: AsyncSession):
        logger.info(f"Iniciando proceso de dispatch para la orden: {payload.order_id}, Type: {payload.order_type}")

        available_drivers = await geo_engine.find_nearby_available_drivers(
            session=db,
            lat=payload.origin.lat,
            lng=payload.origin.lng
        )

        if not available_drivers:
            logger.warning(f"No hay conductores disponibles para la orden: {payload.order_id}.")
            return

        await self._run_cascade_algorithm(payload, available_drivers)

    async def _run_cascade_algorithm(self, payload: OrderCreatedPayload, drivers: list):
        logger.info(f"Inicializando algoritmo cascada con {len(drivers)} ptenciales conductores.")

        for driver_id in drivers:
            logger.info(f"Pinging driver ID: {driver_id} for Order ID: {payload.order_id}")

            # Construimos el payload para notificar al conductor
            event_payload = {
                "pattern": "driver.pinged",
                "data": {
                    "order_id": payload.order_id,
                    "driver_id": driver_id,
                    "origin": {"lat": payload.origin.lat, "lng": payload.origin.lng}
                }
            }

            # Publicamos el evento en RabbitMQ
            await publisher.publish_event("notifications_queue", event_payload)

            # Simulamos el tiempo de espera por la respuesta del conductor
            await asyncio.sleep(2)

            logger.info(f"Tiempo de espera excedido para: {driver_id}. Procediendo al siguiente candidato.")

        logger.info(f"Algoritmo cascada completado para la orden: {payload.order_id}. No acceptance received.")


dispatcher = DispatcherService()