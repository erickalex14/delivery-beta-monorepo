## Nebula Delivery / Ride-Hailing

---

## Descripción General

El **API Gateway** es el punto de entrada único al sistema. Su responsabilidad es:

- Autenticar usuarios
- Autorizar acciones
- Validar datos
- Proteger contra ataques
- Enrutar solicitudes a microservicios

> **Importante:**  
> El Gateway **NO contiene lógica de negocio**, solo seguridad y orquestación.

---

## 1. Autenticación (JWT + Refresh Tokens)

### Requisitos
- Access Token corto (≈ 15 min)
- Refresh Token seguro
- Rotación de tokens

### flujo
Login → Access + Refresh  
Expira Access → usar Refresh → nuevo Access  

### Buenas practicas
- Guardar refresh tokens en Redis/DB
- Invalidar tokens en logout
- Rotación automática (refresh token cambia en cada uso)

---

## 2. Rate Limiting

- Evita spam, bots y DDoS
- OTP → 3 intentos / 5 min

### ### Buenas practicas
- Rate limit por IP + usuario
- Diferentes límites por endpoint
- Integrar con Redis (distribuido)

---

## 3. CORS Estricto

```ts
app.enableCors({
  origin: ['https://tuapp.com'],
});
```

### ### Buenas practicas
- Lista blanca dinámica por entorno
- Bloquear origins desconocidos en producción
---

## 4. Validación Global

```ts
app.useGlobalPipes(new ValidationPipe({
  whitelist: true,
  forbidNonWhitelisted: true,
}));
```

### ### Buenas practicas
- Sanitizar inputs (trim, escape)
- Validar DTOs estrictos

---

## 5. Autorización (Roles)

- CLIENT
- DRIVER
- MERCHANT
- ADMIN (Osea nosotros que vamos a mantener el sistema y quisiéramos auditar o generar reportes etc)

### ### Buenas practicas
- RBAC dinámico desde DB
- Permisos por acción (no solo rol)

---

## 6. Multi-Tenant

```ts
if (user.tenant_id !== request.tenant_id) {
  throw new ForbiddenException();
}
```

### ### Buenas practicas
- Middleware global que inyecte tenant automáticamente
- Subdominios por tenant (ej: santaana.app.com)

---

## 7. Logging

Registrar:
- user_id
- endpoint
- IP
- timestamp

### ### Buenas practicas
- Centralizar logs (ELK, Datadog)
- Correlation ID por request

---

## 8. Anti-Fraude

- detectar abuso de OTP
- múltiples cuentas por IP

### ### Buenas practicas
- Device fingerprinting
- Score de riesgo por usuario

---

## 9. Seguridad de Tokens

- no usar localStorage
- HttpOnly cookies / secure storage

### ### Buenas practicas
- Token binding (device/IP)
- Detección de uso sospechoso

---

## 10. Routing

- enrutar a microservicios correctos
- balanceo de carga

### ### Buenas practicas
- Circuit breaker
- Retry automático
- Timeout por servicio

---

## 11. Sanitización

- prevenir SQLi y XSS

### ### Buenas practicas
- Librerías de sanitización
- Escape de output en frontend

---

## 12. Headers de Seguridad

```ts
app.use(helmet());
```

### ### Buenas practicas
- CSP (Content Security Policy)
- HSTS activado

---

## 13. Requests Sospechosos

- bloquear payloads grandes
- bloquear JSON inválido

### ### Buenas practicas
- WAF (Web Application Firewall)
- listas negras dinámicas

---

## 14. OTP Seguro

- un solo uso
- expiración corta

### ### Buenas practicas
- OTP por hash
- invalidación inmediata tras uso

---

## 15. Blacklist

- bloquear user_id
- bloquear IP
- bloquear device_id

### ### Buenas practicas
- listas negras distribuidas (Redis)
- bloqueo automático por comportamiento

---

## Arquitectura

CLIENT → API GATEWAY → MICROSERVICIOS

---

## Conclusión

- El API Gateway es la primera línea de defensa.
- Implementar seguridad robusta es crucial para proteger datos y usuarios.

> Si pasa el Gateway, el sistema confía.

---

## ### Buenas practicas(Opcional, Pero en lo posible lo implementaremos)


- WAF avanzado (Cloudflare, AWS Shield)
- Machine Learning antifraude
- Geo-blocking
- Rate limiting inteligente
