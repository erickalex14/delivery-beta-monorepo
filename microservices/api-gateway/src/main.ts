import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import helmet from 'helmet';
import { ValidationPipe } from '@nestjs/common';
import * as express from 'express';
import { setupProxies } from './middlewares/proxy.setup'; // Importamos la función de configuración de proxies

async function bootstrap() {
  const app = await NestFactory.create(AppModule);


  // CAPAS DE SEGURIDAD (Reglas 3, 4, 12 y 13)

  app.use(helmet());

  app.use(express.json({ limit: '500kb' }));
  app.use(express.urlencoded({ extended: true, limit: '500kb' }));

  app.enableCors({
    origin: [
      'https://tuapp.com',
      'http://localhost:8081',
      'exp://192.168.x.x:8081',
    ],
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE',
    credentials: true,
  });

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  // ==========================================
  // 🌉 REGLAS DE ENRUTAMIENTO
  // ==========================================

  // ¡Mira qué limpio queda esto ahora!
  setupProxies(app);

  await app.listen(3000);
  console.log(
    '🛡️ API Gateway blindado, ordenado y escuchando en http://localhost:3000',
  );
}
bootstrap();
