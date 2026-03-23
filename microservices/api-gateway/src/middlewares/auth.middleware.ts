import { Request, Response, NextFunction } from 'express';
import * as jwt from 'jsonwebtoken';

export function validateJwtToken(
  req: Request,
  res: Response,
  next: NextFunction,
) {
  //Buscamos el token en el header de la petición
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      error: 'No autorizado',
      message: 'Token no proporcionado o formato incorrecto',
    });
  }

  //Extraemos el token del header
  const token = authHeader.split(' ')[1];

  try {
    const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_default';
    //Verificamos el token
    const decoded = jwt.verify(token, JWT_SECRET);
    //Si todo va bien se inyectya la info del usuarioa la peticion
    //para que los microservicios sepan quien hace la llamada
    req['user'] = decoded;
    //Damos paso libre
    next();
  } catch (error) {
    if (error instanceof Error && error.name === 'TokenExpiredError') {
      return res.status(401).json({
        error: 'Token Expirado',
        message: 'Usa tu refresh_token para obtener uno nuevo',
      });
    }
    return res.status(401).json({
      error: 'No Autorizado',
      message: 'El token es inválido o ha sido modificado',
    });
  }
}
