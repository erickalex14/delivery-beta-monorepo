import * as dotenv from 'dotenv';
dotenv.config();
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';

async function bootstrap() {
  // Validamos de forma estricta que la variable exista para que no falle en producción
  if (!process.env.RABBITMQ_URL) {
    throw new Error(
      'ERROR CRÍTICO: La variable RABBITMQ_URL no está definida en el .env',
    );
  }

  const app = await NestFactory.createMicroservice<MicroserviceOptions>(
    AppModule,
    {
      transport: Transport.RMQ,
      options: {
        // ¡Magia! Ahora la URL viene blindada y oculta desde el .env
        urls: [process.env.RABBITMQ_URL],
        queue: 'notifications_queue',
        queueOptions: {
          durable: true,
        },
      },
    },
  );

  await app.listen();
  console.log('Notification Service escuchando eventos en RabbitMQ...');
}
bootstrap();
