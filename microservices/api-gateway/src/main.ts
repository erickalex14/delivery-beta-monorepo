import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import helmet from 'helmet';
import { ValidationPipe } from '@nestjs/common';
import * as express from 'express';
import { setupProxies } from './middlewares/proxy.setup';
import { globalLogger } from './middlewares/logger.middleware';
import { tenantMiddleware } from './middlewares/tenant.middleware';
import { stripInternalHeaders } from './middlewares/strip-headers.middleware';

import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // 1. EL GUARDIÁN SILENCIOSO
  app.use(stripInternalHeaders);

  // 2. CAPAS DE SEGURIDAD Y LOGGING
  app.use(helmet());
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

  // 4. EL PROXY / ENRUTAMIENTO
  setupProxies(app);

  // 5. CONSUMO DE BODY Y VALIDACIÓN LOCAL
  app.use(express.json({ limit: '500kb' }));
  app.use(express.urlencoded({ extended: true, limit: '500kb' }));

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  // ==========================================
  // CONFIGURACIÓN DE SWAGGER
  // ==========================================
  const config = new DocumentBuilder()
    .setTitle('API Gateway - Nebula')
    .setDescription(
      'Documentación de los endpoints locales del Gateway (Proxy Auth y Orquestación)',
    )
    .setVersion('1.0')
    .addBearerAuth() // Le pone el candadito para enviar JWT
    .build();

  const document = SwaggerModule.createDocument(app, config);
  // La interfaz gráfica vivirá en http://localhost:3000/api-docs
  SwaggerModule.setup('api-docs', app, document);

  await app.listen(3000);
  console.log('API Gateway blindado y escuchando en http://localhost:3000');
  console.log('Swagger Gateway corriendo en http://localhost:3000/api-docs');
}
bootstrap();
