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

    async def _run_cascade_algorithm(self, payload: OrderCreatedPayload, drivers: list, db: AsyncSession):
        logger.info(f"Initiating cascade algorithm with {len(drivers)} potential drivers.")

        for driver_id in drivers:
            logger.info(f"Pinging driver ID: {driver_id} for Order ID: {payload.order_id}")

            event_payload = {
                "pattern": "driver.pinged",
                "data": {
                    "order_id": payload.order_id,
                    "driver_id": driver_id,
                    "origin": {"lat": payload.origin.lat, "lng": payload.origin.lng}
                }
            }

            await publisher.publish_event("notifications_queue", event_payload)

            # TODO: Idealmente, usar un lock distribuido (Redis) en lugar de un sleep pasivo.
            # Por el momento, esperamos el timeout estándar de la configuración.
            await asyncio.sleep(15)

            # Simulamos verificación de estado. Si el viaje fue aceptado por este u otro evento, abortar la cascada.
            # order_status = await self._check_order_status(payload.order_id, db)
            # if order_status != 'CREATED':
            #     logger.info(f"Order {payload.order_id} is no longer pending. Stopping cascade.")
            #     break

            logger.info(f"Timeout reached for driver ID: {driver_id}. Proceeding to next candidate.")

        logger.info(f"Cascade algorithm completed for Order ID: {payload.order_id}.")


dispatcher = DispatcherService()