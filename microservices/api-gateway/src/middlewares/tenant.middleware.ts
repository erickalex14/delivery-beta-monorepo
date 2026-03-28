import { Request, Response, NextFunction } from 'express';

export function tenantMiddleware(
  req: Request,
  res: Response,
  next: NextFunction,
) {
  //Intentar leer el header directo (Pa las apps de react native)
  const headerTenant = req.headers['x-tenant-id'] as string;

  //Intentar leer el subdominio pa el portal web de next
  const host = req.headers.host || '';
  const subdomain = host.split('.')[0]; // Asumiendo que el formato es subdominio.dominio.com

  let finalTenantId = 'DEFAUL_SYSTEM_TENANT'; //Pa peticiones internas

  if (headerTenant) {
    //Si la app movil manda el id exacto se le hace caso a la app
    finalTenantId = headerTenant;
  } else if (
    subdomain &&
    !['www', 'api', 'localhost:3000', '127'].includes(subdomain)
  ) {
    // Si viene de la web (ej: santaana.delivery.com)
    //En produccion esto se consultara a redis, por ahora va mapeado directo
    if (subdomain === 'santaana') finalTenantId = 'tenant-santa-ana-uuid';
    if (subdomain === 'portoviejo') finalTenantId = 'tenant-portoviejo-uuid';
  }

  //Inyectar el TennantID  en los headers pa que el identity service y el core en java lo puedan leer
  req.headers['x-tenant-id'] = finalTenantId;

  next();
}
