package websocket

import (
	"log"
	"sync"

	"github.com/deliveryapp/delivery-superapp/live-tracking/internal/redis"
)

type Message struct {
	RoomID  string
	Payload []byte
}

type Hub struct {
	clients    map[string]map[*Client]bool
	Register   chan *Client
	Unregister chan *Client
	Broadcast  chan Message
	CloseRoom  chan string // Nuevo canal para recibir ordenes de cierre desde RabbitMQ
	mutex      sync.RWMutex
	Redis      *redis.Client
}

func NewHub(redisClient *redis.Client) *Hub {
	return &Hub{
		clients:    make(map[string]map[*Client]bool),
		Register:   make(chan *Client),
		Unregister: make(chan *Client),
		Broadcast:  make(chan Message),
		CloseRoom:  make(chan string),
		Redis:      redisClient,
	}
}

func (h *Hub) Run() {
	log.Println("Hub de WebSockets inicializado y a la espera de eventos.")
	for {
		select {
		case client := <-h.Register:
			h.mutex.Lock()
			if _, ok := h.clients[client.RoomID]; !ok {
				h.clients[client.RoomID] = make(map[*Client]bool)
			}
			h.clients[client.RoomID][client] = true
			h.mutex.Unlock()
			log.Printf("Cliente registrado exitosamente en la sala: %s", client.RoomID)

		case client := <-h.Unregister:
			h.mutex.Lock()
			if room, ok := h.clients[client.RoomID]; ok {
				if _, exists := room[client]; exists {
					delete(room, client)
					close(client.Send)
					if len(room) == 0 {
						delete(h.clients, client.RoomID)
						log.Printf("Sala %s eliminada de memoria por inactividad.", client.RoomID)
					}
				}
			}
			h.mutex.Unlock()

		case message := <-h.Broadcast:
			h.mutex.RLock()
			if room, ok := h.clients[message.RoomID]; ok {
				for client := range room {
					select {
					case client.Send <- message.Payload:
					default:
						close(client.Send)
						delete(room, client)
					}
				}
			}
			h.mutex.RUnlock()

		// Nueva logica: Cierre forzoso de la sala orquestado por eventos externos
		case roomID := <-h.CloseRoom:
			h.mutex.Lock()
			if room, ok := h.clients[roomID]; ok {
				for client := range room {
					close(client.Send)
					delete(room, client)
				}
				delete(h.clients, roomID)
				log.Printf("Sala %s purgada de la memoria tras finalizacion del viaje.", roomID)
			}
			h.mutex.Unlock()
		}
	}
}
