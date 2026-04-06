package redis

import (
	"context"
	"log"
	"time"

	"github.com/redis/go-redis/v9"
)

// Cliente encapsula la conexiion  a redis
type Client struct {
	DB *redis.Client
}

// NewClient Inicializa la coneccion a redis y ejecuta ping de verificacion
func NewClient(redisURL string) *Client {
	opts, err := redis.ParseURL(redisURL)
	if err != nil {
		log.Fatalf("Error critico: No se pudo interpretar la URL de Redis: %v", err)
	}

	client := redis.NewClient(opts)

	//Establecemos timeout de 5 segundos para el handshake inicial
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := client.Ping(ctx).Err(); err != nil {
		log.Fatalf("Error critico: Fallo la conexion con el servidor Redis: %v", err)
	}

	log.Println("Conexion a Redis establecida exitosamente.")

	return &Client{
		DB: client,
	}
}

// UpdateDriverLocation inserta o actualiza la coordenada geoespacial del conductor.
// Utiliza el comando nativo GEOADD de Redis.
func (c *Client) UpdateDriverLocation(ctx context.Context, driverID string, lat, lng float64) error {
	// Importante: Redis espera la longitud primero y luego la latitud
	err := c.DB.GeoAdd(ctx, "ubicaciones_activas", &redis.GeoLocation{
		Name:      driverID,
		Longitude: lng,
		Latitude:  lat,
	}).Err()

	if err != nil {
		log.Printf("Fallo al actualizar la ubicacion espacial para el conductor %s: %v", driverID, err)
		return err
	}

	return nil
}

// RemoveDriverLocation elimina el registro espacial del conductor en memoria.
// Se invoca cuando un flete termina o el conductor se desconecta.
func (c *Client) RemoveDriverLocation(ctx context.Context, driverID string) error {
	// GEOADD guarda los datos internamente como un Sorted Set, por lo que usamos ZREM para eliminar
	err := c.DB.ZRem(ctx, "ubicaciones_activas", driverID).Err()
	if err != nil {
		log.Printf("Fallo al purgar la ubicacion del conductor %s: %v", driverID, err)
		return err
	}

	return nil
}
