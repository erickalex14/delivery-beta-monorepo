from fastapi import FastAPI
from contextlib import asynccontextmanager
import asyncio
import aio_pika
import json

from app.core.config import settings
from app.schemas.payload import OrderCreatedPayload
from app.db.session import AsyncSessionLocal
from app.services.dispatcher import dispatcher


async def consume_messages():
    try:
        connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
        channel = await connection.channel()
        queue = await channel.declare_queue("matchmaking_queue", durable=True)

        print("Motor de Matchmaking (Python) escuchando en RabbitMQ...")

        async with queue.iterator() as queue_iter:
            async for message in queue_iter:
                async with message.process():
                    raw_data = json.loads(message.body.decode())

                    try:
                        # Extraemos la data y el patrón del mensaje de NestJS/Java
                        pattern = raw_data.get("pattern")
                        event_data = raw_data.get("data", raw_data)
                        payload = OrderCreatedPayload(**event_data)

                        # Filtramos qué eventos nos interesan para despachar
                        if pattern in ["order.ride.created", "order.delivery.ready"]:

                            # 🗄Abrimos una sesión de base de datos corta y segura
                            async with AsyncSessionLocal() as db:
                                await dispatcher.process_dispatch(payload, db)
                        else:
                            print(f"Evento ignorado: {pattern}")

                    except Exception as validation_error:
                        print(f"Error procesando el mensaje: {validation_error}")

    except Exception as e:
        print(f"Error conectando a RabbitMQ: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    task = asyncio.create_task(consume_messages())
    yield
    task.cancel()


app = FastAPI(title="Matchmaking Dispatch Engine", lifespan=lifespan)


@app.get("/health")
def health_check():
    return {"status": "El cerebro de Python está vivo "}