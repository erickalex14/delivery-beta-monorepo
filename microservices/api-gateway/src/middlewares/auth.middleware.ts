import { Request, Response, NextFunction } from 'express';
import * as jwt from 'jsonwebtoken';

export function validateJwtToken(
  req: Request,
  res: Response,
  next: NextFunction,
) {
  // 1. Extraemos el token de la cabecera 'Authorization: Bearer <token>'
  const authHeader = req.headers['authorization'];

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      status: 401,
      error: 'Unauthorized',
      message: 'Token de acceso faltante o formato inválido.',
    });
  }

  const token = authHeader.split(' ')[1];

  try {
    // 2. Verificamos la firma matemática del token (Asegúrate de que coincida con el de Java)
    const secret = process.env.JWT_SECRET || 'FIRMA_SECRETA_NEBULA_2026';
    const decoded = jwt.verify(token, secret) as any;

    // 3. 🛡️ INYECCIÓN DE HEADERS (MAGIA NIVEL GOD)
    // El API Gateway muta la petición y le incrusta los datos del usuario.
    // Así, el Core Transactional en Java solo tiene que leer estos headers.
    req.headers['X-User-Id'] = decoded.userId || decoded.sub; // Depende de cómo lo llames en Java
    req.headers['X-User-Role'] = decoded.role;

    // 4. Dejamos que la petición continúe hacia el Proxy (hacia el microservicio de destino)
    next();
  } catch (error) {
    // Si el token expiró o fue alterado, el Gateway lo bloquea como un muro de contención
    return res.status(401).json({
      status: 401,
      error: 'Unauthorized',
      message: 'El token ha expirado o es inválido.',
    });
  }
}
