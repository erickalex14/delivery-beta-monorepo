package main

import (
	"log"
	"net/http"

	"github.com/deliveryapp/delivery-superapp/live-tracking/internal/config"
	"github.com/deliveryapp/delivery-superapp/live-tracking/internal/rabbitmq"
	"github.com/deliveryapp/delivery-superapp/live-tracking/internal/redis"
	"github.com/deliveryapp/delivery-superapp/live-tracking/internal/websocket"
)

func healthCheckHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status": "El motor de Live Tracking se encuentra operativo"}`))
}

func main() {
	cfg := config.Load()

	// 1. Inicializamos Redis
	redisClient := redis.NewClient(cfg.RedisURL)
	defer redisClient.DB.Close()

	// 2. Inicializamos el Hub de WebSockets
	hub := websocket.NewHub(redisClient)
	go hub.Run()

	// 3. Inicializamos el Consumidor de RabbitMQ para purga de memoria
	rmqConsumer, err := rabbitmq.NewConsumer(cfg.RabbitMQURL, hub)
	if err != nil {
		log.Fatalf("Fallo en la conexion a RabbitMQ: %v", err)
	}
	defer rmqConsumer.Close()
	rmqConsumer.Start()

	// 4. Configuracion de rutas HTTP
	mux := http.NewServeMux()
	mux.HandleFunc("/health", healthCheckHandler)
	mux.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		websocket.ServeWS(hub, w, r)
	})

	log.Printf("Iniciando servidor de Live Tracking en el puerto %s", cfg.ServerPort)

	server := &http.Server{
		Addr:    ":" + cfg.ServerPort,
		Handler: mux,
	}

	if err := server.ListenAndServe(); err != nil {
		log.Fatalf("Fallo en la inicializacion del servidor: %v", err)
	}
}
