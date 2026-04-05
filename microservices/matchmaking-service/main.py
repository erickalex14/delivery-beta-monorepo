from fastapi import FastAPI
from contextlib import asynccontextmanager
import asyncio
import aio_pika
import os
from dotenv import load_dotenv
from uvicorn.main import print_version

load_dotenv()
#Worker Consumidor de RabbitMQ
async def consume_messages():
    rabbitmq_url = os.getenv("RABBITMQ_URL")
    if not rabbitmq_url:
        print("RABBITMQ_URL not set")
        return

    try:
        #Hacemos que conecte automaticamente si rabbitmq se reincia
        connection = await aio_pika.connect_robust(rabbitmq_url)
        channel = await connection.channel()

        #Deecalarar la cola exclusiva pa este microservicio
        queue = await channel.declare_queue("matchmaking_queue", durable=True)
        print("Motor de Matchmaking escuchando en rabbitmq")

        #Escuchar mensajes infinitamente
        async with queue.iterator() as queue_iter:
            async for message in queue_iter:
                async with message.process(): # .process() hace el ACK automático si no hay errores
                    print("Python Matchmaking recibió mensaje:", {message.body.decode()})
                    # Aquí iría la lógica de postgis y el algoritmo cascada despues
    except Exception as e:
        print("Error en el consumidor de RabbitMQ:", {e})

#Ciclo de vida de la app
@asynccontextmanager
async def lifespan(app: FastAPI):
    #Cuando el server arranca
    task = asyncio.create_task(consume_messages())
    yield
    #cuando se apaga
    task.cancel()


#Instanciar FastApi
app = FastAPI(title="Matchmaking Dispatch Engine", lifespan=lifespan)


#Endpoint de check de salud
@app.get("/health")
def health_check():
    return {"status": "El cerebro de Python está vivo"}

