import { INestApplication } from '@nestjs/common';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { validateJwtToken } from './auth.middleware';

export function setupProxies(app: INestApplication) {
  // URLs de los microservicios
  const CORE_TRANSACTIONAL_URL =
    process.env.CORE_TRANSACTIONAL_URL || 'http://localhost:8080';
  const IDENTITY_SERVICE_URL =
    process.env.IDENTITY_SERVICE_URL || 'http://localhost:8083';

  // RUTAS PÚBLICAS (No requieren Token)

  // 0. Redirigir Autenticación (Identity Service)
  app.use(
    '/api/v1/auth',
    createProxyMiddleware({
      target: IDENTITY_SERVICE_URL,
      changeOrigin: true,
    }),
  );

  // RUTAS PROTEGIDAS (Requieren Token JWT)

  // A. Redirigir la logística (Core Transactional)
  app.use('/api/v1/logistic/service-orders', validateJwtToken);
  app.use(
    '/api/v1/logistic/service-orders',
    createProxyMiddleware({
      target: CORE_TRANSACTIONAL_URL,
      changeOrigin: true,
    }),
  );

  // B. Redirigir el historial financiero (Ledger)
  app.use('/api/v1/finance/ledger', validateJwtToken);
  app.use(
    '/api/v1/finance/ledger',
    createProxyMiddleware({
      target: CORE_TRANSACTIONAL_URL,
      changeOrigin: true,
    }),
  );

  // C. Redirigir los cobros y recargas (Wallets)
  app.use('/api/v1/finance/wallets', validateJwtToken);
  app.use(
    '/api/v1/finance/wallets',
    createProxyMiddleware({
      target: CORE_TRANSACTIONAL_URL,
      changeOrigin: true,
    }),
  );

  console.log(
    'Proxies configurados: Autenticación pública y rutas protegidas con JWT.',
  );
}
