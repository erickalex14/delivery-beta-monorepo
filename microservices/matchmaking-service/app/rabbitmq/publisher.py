import json
import logging
import aio_pika
from aio_pika import message
from app.core.config import settings

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

class RabbitMQPublisher:

    @staticmethod
    async def publlish_event(routing_key: str, payload: dict) -> bool:
        """
        publica un evento a la exchange por defecto de rabbitmq
        """
        try:
            connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
            async with connection:
                channel = await connection.channel()

                message = aio_pika.Message(
                    body = json.dumps(payload).encode(),
                    delivery_mode=aio_pika.DeliveryMode.PERSISTENT
                )

                await channel.default_exchange.publish(
                    message,
                    routing_key=routing_key,
                )

                logger.info(f"Evento publicado correctamente. Routing key: {routing_key}")
                return True

        except Exception as e:
            logger.error(f"Error al publicar evento '{routing_key}. Error: {str(e)}")
            return False
publisher = RabbitMQPublisher()