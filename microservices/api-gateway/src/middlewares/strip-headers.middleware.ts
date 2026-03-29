import { Request, Response, NextFunction } from 'express';

export function stripInternalHeaders(
  req: Request,
  res: Response,
  next: NextFunction,
) {
  // Eliminamos cualquier cabecera interna que el cliente intente inyectar maliciosamente
  delete req.headers['x-user-id'];
  delete req.headers['x-user-role'];
  // Nota: x-tenant-id SÍ lo dejamos pasar porque tu app móvil lo envía legítimamente.
  next();
}
