import { Request, Response, NextFunction } from 'express';

//Esta funcion recibe un arreglo con los roles permitidos para una ruta
export function requireRoles(allowedRoles: string[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    // Leemos el rol que el auth middleware inyecto previamente
    // Express lo convierte automaticamente a los headers a minusculas
    const userRole = req.headers['x-user-role'] as string;

    if (!userRole) {
      return res.status(403).json({
        status: 403,
        error: 'Forbiden',
        message: 'Acceso denegado: No se proporcionó un rol de usuario',
      });
    }

    //Verificar si el rol esta en la lista de acceso
    if (!allowedRoles.includes(userRole)) {
      return res.status(403).json({
        status: 403,
        error: 'Forbiden',
        message: `Acceso denegado. Se requiere uno de los siguientes roles: ${allowedRoles.join(', ')}. Tu rol actual es: ${userRole}`,
      });
    }
    // Si tiene rol correcto se deja entrar al microservice
    next();
  };
}
