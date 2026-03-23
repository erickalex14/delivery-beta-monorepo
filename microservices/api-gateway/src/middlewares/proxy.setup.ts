import { INestApplication } from '@nestjs/common';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { validateJwtToken } from './auth.middleware';

export function setupProxies(app: INestApplication) {
  const CORE_TRANSACTIONAL_URL =
    process.env.CORE_TRANSACTIONAL_URL || 'http://localhost:8080';
  // A. Redirigir la logística
  app.use('/api/v1/logistic/service-orders', validateJwtToken); // 🛡 1. Pasa por autenticación JWT
  app.use(
    '/api/v1/logistic/service-orders',
    createProxyMiddleware({
      // 2. Si pasa, entra al proxy
      target: CORE_TRANSACTIONAL_URL,
      changeOrigin: true,
    }),
  );

  // B. Redirigir el historial financiero (Ledger)
  app.use('/api/v1/finance/ledger', validateJwtToken); //  1. Pasa por autenticación JWT
  app.use(
    '/api/v1/finance/ledger',
    createProxyMiddleware({
      //2. Si pasa, entra al proxy
      target: CORE_TRANSACTIONAL_URL,
      changeOrigin: true,
    }),
  );

  // C. Redirigir los cobros y recargas (Wallets)

  app.use('/api/v1/finance/wallets', validateJwtToken); //  1. Pasa por autenticación JWT
  app.use(
    '/api/v1/finance/wallets',
    createProxyMiddleware({
      // 2. Si pasa, entra al proxy
      target: CORE_TRANSACTIONAL_URL,
      changeOrigin: true,
    }),
  );

  console.log('Proxies configurados con JWT Authentication.');
}
