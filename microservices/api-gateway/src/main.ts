import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import helmet from 'helmet';
import { ValidationPipe } from '@nestjs/common';
import * as express from 'express';
import { setupProxies } from './middlewares/proxy.setup';
import { globalLogger } from './middlewares/logger.middleware';
import { tenantMiddleware } from './middlewares/tenant.middleware';
import { stripInternalHeaders } from './middlewares/strip-headers.middleware';

// Importamos el sanitizador (usamos require para evitar problemas de tipado en TS)
const xssClean = require('xss-clean');

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // ==========================================
  // CAPAS DE SEGURIDAD Y MIDDLEWARES GLOBALES
  // ==========================================

  // 12. Headers de Seguridad (Helmet)
  app.use(helmet());

  // 7. Logging Global (Registra cada petición)
  app.use(globalLogger);

  // 6. Multi-Tenant (Inyecta la ciudad/franquicia)
  app.use(tenantMiddleware);

  // 11. Sanitización Anti-XSS (Limpia scripts maliciosos de los inputs)
  app.use(xssClean());

  app.use(stripInternalHeaders);

  // 13. Límite de Payload (Anti-Spam de archivos gigantes)
  app.use(express.json({ limit: '500kb' }));
  app.use(express.urlencoded({ extended: true, limit: '500kb' }));

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

  // 4. Validación Global y Limpieza de DTOs
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  // ==========================================
  // REGLAS DE ENRUTAMIENTO
  // ==========================================

  setupProxies(app);

  await app.listen(3000);
  console.log(
    'API Gateway blindado, sanitizado y escuchando en http://localhost:3000',
  );
}
bootstrap();
