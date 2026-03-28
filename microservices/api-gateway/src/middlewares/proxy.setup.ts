import { INestApplication } from '@nestjs/common';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { validateJwtToken } from './auth.middleware';
import { requireRoles } from './roles.middleware';

export function setupProxies(app: INestApplication) {
  // URLs de los microservicios
  const CORE_TRANSACTIONAL_URL =
    process.env.CORE_TRANSACTIONAL_URL || 'http://localhost:8080';
  const IDENTITY_SERVICE_URL =
    process.env.IDENTITY_SERVICE_URL || 'http://localhost:8083';

  // ==========================================
  // RUTAS PÚBLICAS (No requieren Token)
  // ==========================================

  // 0. Redirigir Autenticación (Identity Service)
  app.use(
    '/api/v1/auth',
    createProxyMiddleware({
      target: IDENTITY_SERVICE_URL,
      changeOrigin: true,
      proxyTimeout: 5000, // 5 segundos de timeout
      timeout: 5000,
    }),
  );

  // ==========================================
  // RUTAS PROTEGIDAS Y AUTORIZADAS (JWT + Roles)
  // ==========================================

  // A. Redirigir la logística (Core Transactional)
  app.use(
    '/api/v1/logistic/service-orders',
    validateJwtToken,
    requireRoles(['DRIVER', 'ADMIN']), //Solo choferes y admins
  );
  app.use(
    '/api/v1/logistic/service-orders',
    createProxyMiddleware({
      target: CORE_TRANSACTIONAL_URL,
      changeOrigin: true,
      proxyTimeout: 5000,
      timeout: 5000,
    }),
  );

  // B. Redirigir el historial financiero (Ledger)
  app.use(
    '/api/v1/finance/ledger',
    validateJwtToken,
    requireRoles(['ADMIN']), //Solo admins pueden ver la contabilidad general
  );
  app.use(
    '/api/v1/finance/ledger',
    createProxyMiddleware({
      target: CORE_TRANSACTIONAL_URL,
      changeOrigin: true,
      proxyTimeout: 5000,
      timeout: 5000,
    }),
  );

  // C. Redirigir los cobros y recargas (Wallets)
  app.use(
    '/api/v1/finance/wallets',
    validateJwtToken,
    requireRoles(['CLIENT', 'DRIVER', 'MERCHANT', 'ADMIN']), //Todos los registrados pueden ver su propia billetera
  );
  app.use(
    '/api/v1/finance/wallets',
    createProxyMiddleware({
      target: CORE_TRANSACTIONAL_URL,
      changeOrigin: true,
      proxyTimeout: 5000,
      timeout: 5000,
    }),
  );

  console.log(
    '🛡️ Proxies configurados: Auth JWT + Autorización por Roles + Timeouts.',
  );
}
