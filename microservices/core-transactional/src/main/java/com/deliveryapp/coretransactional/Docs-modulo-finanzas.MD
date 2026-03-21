# Módulo de Finanzas - Core Transactional (v1.2.0)

## 1. Descripción General
Este módulo constituye el núcleo económico de la **SuperApp Multimodal**. Proporciona una infraestructura de **Billetera Virtual (Wallet)** diseñada específicamente para conductores y comercios en el mercado de **Santa Ana, Manabí**.

El sistema abandona el manejo de saldos simples en favor de una arquitectura de **Libro Mayor (Ledger)**, garantizando que cada movimiento de dinero sea inmutable, rastreable y auditable al 100%.

---

## 2. Arquitectura de Datos (Esquema `finance`)
Siguiendo el diseño del ERD, la base de datos se divide en tres entidades críticas para asegurar la integridad financiera:

* **`wallets`**: Almacena el balance actual del usuario. Funciona como un "caché de lectura rápida", pero no es la fuente de verdad absoluta.
* **`ledger_entries`**: Registro de "Cabecera" que explica el motivo de la transacción (ej: Recarga, Comisión de Flete, Pago de Pedido).
* **`ledger_postings`**: El detalle contable. Registra los movimientos de entrada (`CREDIT`) y salida (`DEBIT`). La suma de estos registros define el saldo real del usuario.

---

## 3. Especificaciones Técnicas
* **Framework:** Spring Boot 4.0.4.
* **Lenguaje:** Java 17 (Uso estricto de `BigDecimal` para precisión monetaria).
* **Base de Datos:** PostgreSQL (Puerto 5433 en entorno Docker).
* **Gestión de Transacciones:** `@Transactional` para asegurar atomicidad (si el Ledger falla, el saldo no se altera).

---

## 4. API Reference
Todos los endpoints están disponibles bajo la ruta base `/api/v1/wallets`.

### Gestión de Billetera
| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `POST` | `/` | Crea una nueva billetera (inicializada en $0.00). |
| `POST` | `/{driverId}/top-up` | Realiza una recarga de saldo (`CREDIT`). |
| `POST` | `/{driverId}/debit` | Realiza un cobro de comisión o pago (`DEBIT`). |
| `GET` | `/{driverId}/history` | Obtiene el historial detallado de movimientos. |

---

## 5. Reglas de Negocio Implementadas
1.  **Validación de Fondos:** No se permiten débitos si el balance de la billetera es menor al monto solicitado.
2.  **Transparencia de Auditoría:** Cada `LedgerPosting` debe estar vinculado obligatoriamente a una `LedgerEntry` que describa el motivo.
3.  **Ordenamiento:** El historial de movimientos se entrega siempre en orden cronológico descendente para facilitar la lectura del usuario.

