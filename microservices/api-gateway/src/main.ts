import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import helmet from 'helmet';
import { ValidationPipe } from '@nestjs/common';
import * as express from 'express';
import { setupProxies } from './middlewares/proxy.setup';
import { globalLogger } from './middlewares/logger.middleware';
import { tenantMiddleware } from './middlewares/tenant.middleware';
import { stripInternalHeaders } from './middlewares/strip-headers.middleware';

const xssClean = require('xss-clean');

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // 1.(Bloquea inyecciones de headers)

  app.use(stripInternalHeaders);

  // 2. CAPAS DE SEGURIDAD Y LOGGING

  app.use(helmet());
  app.use(xssClean());
  app.use(tenantMiddleware);
  app.use(globalLogger);

  // 3. CORS Estricto
  app.enableCors({
    origin: [
      'https://tuapp.com',
      'http://localhost:8081',
      'exp://192.168.x.x:8081',
    ],
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE',
    credentials: true,
  });

  // 4. EL PROXY / ENRUTAMIENTO (¡CLAVE!)

  setupProxies(app);

  // 5. CONSUMO DE BODY Y VALIDACIÓN LOCAL
  // Esto solo afectará a las rutas que el proxy NO atrapó
  // (por ejemplo, si en el futuro haces un AppController local en NestJS)
  app.use(express.json({ limit: '500kb' }));
  app.use(express.urlencoded({ extended: true, limit: '500kb' }));

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  await app.listen(3000);
  console.log('API Gateway blindado y escuchando en http://localhost:3000');
}
bootstrap();
