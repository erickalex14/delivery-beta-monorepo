from fastapi import FastAPI
from contextlib import asynccontextmanager
import asyncio
import aio_pika
import json
from app.core.config import settings
from app.schemas.payload import OrderCreatedPayload


async def consume_messages():
    try:
        connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)
        channel = await connection.channel()
        queue = await channel.declare_queue("matchmaking_queue", durable=True)

        print("Motor de Matchmaking (Python) escuchando en RabbitMQ...")

        async with queue.iterator() as queue_iter:
            async for message in queue_iter:
                async with message.process():
                    # 1. Decodificamos el JSON
                    raw_data = json.loads(message.body.decode())

                    # 2. Magia de Pydantic: Validamos que el JSON tenga la estructura correcta
                    try:
                        # Asumimos que Nest/Java manda algo como { pattern: "order.created", data: {...} }
                        event_data = raw_data.get("data", raw_data)
                        payload = OrderCreatedPayload(**event_data)
                        print(f"Nuevo viaje validado: ID {payload.order_id} Tipo: {payload.order_type}")
                        print(f"Origen: Lat {payload.origin.lat}, Lng {payload.origin.lng}")

                        # ¡Aquí llamaremos a geo_engine.py para buscar motos!

                    except Exception as validation_error:
                        print(f"Error de validación, el JSON está mal formado: {validation_error}")

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