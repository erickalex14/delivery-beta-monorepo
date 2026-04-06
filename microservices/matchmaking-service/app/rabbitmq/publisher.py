import json
import logging
import aio_pika
from app.core.config import settings

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)


class RabbitMQPublisher:
    def __init__(self):
        self.connection = None
        self.channel = None
        self.exchange = None

    async def connect(self):
        try:
            self.connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
            self.channel = await self.connection.channel()
            self.exchange = self.channel.default_exchange
            logger.info("RabbitMQ Publisher connected successfully.")
        except Exception as e:
            logger.error(f"Failed to initialize RabbitMQ Publisher: {str(e)}")

    async def close(self):
        if self.connection and not self.connection.is_closed:
            await self.connection.close()
            logger.info("RabbitMQ Publisher connection closed.")

    async def publish_event(self, routing_key: str, payload: dict) -> bool:
        if not self.exchange:
            logger.error("Attempted to publish without an active connection.")
            return False

        try:
            message = aio_pika.Message(
                body=json.dumps(payload).encode(),
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT
            )
            await self.exchange.publish(message, routing_key=routing_key)
            logger.debug(f"Event published. Routing Key: {routing_key}")
            return True
        except Exception as e:
            logger.error(f"Failed to publish event '{routing_key}'. Error: {str(e)}")
            return False


publisher = RabbitMQPublisher()