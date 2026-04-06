package websocket

import (
	"context"
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

// LocationPayload mapea el JSON enviado por la aplicacion movil.
type LocationPayload struct {
	DriverID string  `json:"driver_id"`
	Lat      float64 `json:"lat"`
	Lng      float64 `json:"lng"`
}

type Client struct {
	Hub    *Hub
	Conn   *websocket.Conn
	Send   chan []byte
	RoomID string
	Role   string
}

func (c *Client) WritePump() {
	defer func() {
		c.Conn.Close()
	}()
	for {
		select {
		case message, ok := <-c.Send:
			if !ok {
				c.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := c.Conn.WriteMessage(websocket.TextMessage, message); err != nil {
				return
			}
		}
	}
}

func (c *Client) ReadPump() {
	defer func() {
		c.Hub.Unregister <- c
		c.Conn.Close()
	}()
	for {
		_, message, err := c.Conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				log.Printf("Interrupcion inesperada de lectura en WebSocket: %v", err)
			}
			break
		}

		if c.Role == "DRIVER" {
			// 1. Retransmitir al cliente de la sala
			c.Hub.Broadcast <- Message{
				RoomID:  c.RoomID,
				Payload: message,
			}

			// 2. Extraer datos del JSON
			var loc LocationPayload
			if err := json.Unmarshal(message, &loc); err != nil {
				log.Printf("Error de validacion: No se pudo decodificar el payload JSON: %v", err)
				continue
			}

			// 3. Ejecutar GEOADD en Redis
			ctx := context.Background()
			err = c.Hub.Redis.UpdateDriverLocation(ctx, loc.DriverID, loc.Lat, loc.Lng)
			if err != nil {
				log.Printf("Error al sincronizar coordenadas espaciales en memoria: %v", err)
			} else {
				// Este log lo puedes borrar en produccion para no saturar la consola
				log.Printf("Coordenada de %s actualizada en Redis a %f, %f", loc.DriverID, loc.Lat, loc.Lng)
			}
		}
	}
}

func ServeWS(hub *Hub, w http.ResponseWriter, r *http.Request) {
	roomID := r.URL.Query().Get("room_id")
	role := r.URL.Query().Get("role")

	if roomID == "" || role == "" {
		http.Error(w, "Error de validacion: Los parametros 'room_id' y 'role' son obligatorios.", http.StatusBadRequest)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("Fallo critico al realizar upgrade a protocolo WebSocket: %v", err)
		return
	}

	client := &Client{
		Hub:    hub,
		Conn:   conn,
		Send:   make(chan []byte, 256),
		RoomID: roomID,
		Role:   role,
	}

	client.Hub.Register <- client

	go client.WritePump()
	go client.ReadPump()
}
