from fastapi import FastAPI
from contextlib import asynccontextmanager
import asyncio
import aio_pika
import json
import logging

from app.core.config import settings
from app.schemas.payload import OrderCreatedPayload
from app.db.session import AsyncSessionLocal
from app.services.dispatcher import dispatcher
from app.rabbitmq.publisher import publisher

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)


async def consume_messages():
    try:
        connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
        channel = await connection.channel()
        queue = await channel.declare_queue("matchmaking_queue", durable=True)

        logger.info("Matchmaking Engine (Python) listening on RabbitMQ...")

        async with queue.iterator() as queue_iter:
            async for message in queue_iter:
                async with message.process():
                    raw_data = json.loads(message.body.decode())

                    try:
                        pattern = raw_data.get("pattern")
                        event_data = raw_data.get("data", raw_data)
                        payload = OrderCreatedPayload(**event_data)

                        if pattern in ["order.ride.created", "order.delivery.ready"]:
                            async with AsyncSessionLocal() as db:
                                await dispatcher.process_dispatch(payload, db)
                        else:
                            logger.debug(f"Event ignored: {pattern}")

                    except Exception as validation_error:
                        logger.error(f"Error processing message: {validation_error}")

    except Exception as e:
        logger.error(f"Error connecting to RabbitMQ Consumer: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1. Start outgoing connection (Publisher)
    await publisher.connect()

    # 2. Start incoming connection (Consumer)
    task = asyncio.create_task(consume_messages())

    yield

    # 3. Graceful shutdown
    task.cancel()
    await publisher.close()


app = FastAPI(title="Matchmaking Dispatch Engine", lifespan=lifespan)


@app.get("/health")
def health_check():
    return {"status": "Matchmaking Engine is running"}