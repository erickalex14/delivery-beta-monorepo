live-tracking-service/
│
├── cmd/
│   └── api/
│       └── main.go           # Punto de entrada del binario. Solo inicializa dependencias.
│
├── internal/                 # Código privado. Protege tu lógica de negocio.
│   ├── config/
│   │   └── config.go         # Validación estricta del archivo .env (Fail Fast).
│   │
│   ├── redis/
│   │   └── client.go         # Conexión a Redis y ejecución de comandos GEOADD.
│   │
│   ├── rabbitmq/
│   │   └── consumer.go       # Escucha eventos de cierre de viaje para liberar memoria.
│   │
│   └── websocket/
│       ├── client.go         # Maneja la conexión individual (Lectura/Escritura) de un celular.
│       ├── hub.go            # El "Router" que agrupa a los clientes en "Salas" (Rooms).
│       └── handler.go        # Transforma la petición HTTP inicial a un WebSocket persistente.
│
├── .env                      # Credenciales locales
├── go.mod                    # Generado por 'go mod init'
└── go.sum                    # Checksums de seguridad (se genera al instalar paquetes)