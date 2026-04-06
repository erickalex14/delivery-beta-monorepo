package rabbitmq

import (
	"encoding/json"
	"log"

	"github.com/deliveryapp/delivery-superapp/live-tracking/internal/websocket"
	amqp "github.com/rabbitmq/amqp091-go"
)

// Consumer maneja la conexion a RabbitMQ y la ingesta de eventos asincronos.
type Consumer struct {
	conn    *amqp.Connection
	channel *amqp.Channel
	hub     *websocket.Hub
}

// EventPayload define la estructura esperada de los mensajes del Core Transaccional.
type EventPayload struct {
	Pattern string `json:"pattern"`
	Data    struct {
		OrderID string `json:"order_id"`
	} `json:"data"`
}

// NewConsumer establece la conexion con el broker de mensajeria.
func NewConsumer(amqpURL string, hub *websocket.Hub) (*Consumer, error) {
	conn, err := amqp.Dial(amqpURL)
	if err != nil {
		return nil, err
	}

	ch, err := conn.Channel()
	if err != nil {
		return nil, err
	}

	// Declaramos la cola especifica para este microservicio
	_, err = ch.QueueDeclare(
		"tracking_events_queue",
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		return nil, err
	}

	return &Consumer{
		conn:    conn,
		channel: ch,
		hub:     hub,
	}, nil
}

// Start inicia la escucha continua de mensajes en una goroutine independiente.
func (c *Consumer) Start() {
	msgs, err := c.channel.Consume(
		"tracking_events_queue",
		"",
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Fatalf("Fallo critico al registrar el consumidor de RabbitMQ: %v", err)
	}

	log.Println("Consumidor de RabbitMQ inicializado. Escuchando eventos de cierre de viajes.")

	go func() {
		for d := range msgs {
			var payload EventPayload
			if err := json.Unmarshal(d.Body, &payload); err != nil {
				log.Printf("Error de validacion: No se pudo decodificar el evento de RabbitMQ: %v", err)
				continue
			}

			// Verificamos si el evento indica que un flete o delivery finalizo
			if payload.Pattern == "order.ride.completed" || payload.Pattern == "order.delivery.completed" {
				orderID := payload.Data.OrderID
				if orderID != "" {
					log.Printf("Evento de finalizacion recibido para la orden: %s. Iniciando proceso de purga.", orderID)
					// Inyectamos el ID de la orden en el canal de cierre del Hub
					c.hub.CloseRoom <- orderID
				}
			}
		}
	}()
}

// Close libera los recursos de red de forma segura.
func (c *Consumer) Close() {
	if c.channel != nil {
		c.channel.Close()
	}
	if c.conn != nil {
		c.conn.Close()
	}
	log.Println("Conexion de RabbitMQ cerrada correctamente.")
}
