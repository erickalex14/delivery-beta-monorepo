import { Request, Response, NextFunction } from 'express';
import * as crypto from 'crypto';

export function globalLogger(req: Request, res: Response, next: NextFunction) {
  const timestamp = new Date().toISOString();
  const ip = req.ip || req.socket.remoteAddress;
  const endpoint = req.originalUrl;
  const method = req.method;

  // Generamos un ID único para rastrear la petición a través de todos los microservicios
  const correlationId = req.headers['x-correlation-id'] || crypto.randomUUID();
  req.headers['x-correlation-id'] = correlationId;

  // Interceptamos el final de la respuesta para saber qué status code devolvió
  res.on('finish', () => {
    const statusCode = res.statusCode;
    // Si el usuario ya pasó por el auth.middleware, tendrá su ID aquí
    const userId = req.headers['X-User-Id'] || 'Anónimo';

    console.log(
      `[${timestamp}] [ReqID: ${correlationId}] IP: ${ip} | User: ${userId} | ${method} ${endpoint} | Status: ${statusCode}`,
    );
  });

  next();
}
