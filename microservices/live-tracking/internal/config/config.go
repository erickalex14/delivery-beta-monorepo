package config

import (
	"log"
	"os"

	"github.com/joho/godotenv"
)

// Config almacena las variables de entorno necesarias para la operacion del microservicio.
type Config struct {
	RedisURL    string
	RabbitMQURL string
	ServerPort  string
}

// Load lee el archivo .env, inicializa las variables y valida que las credenciales criticas existan.
func Load() *Config {
	err := godotenv.Load()
	if err != nil {
		log.Println("Advertencia: No se encontro el archivo .env. Utilizando variables de entorno del sistema.")
	}

	redisURL := os.Getenv("REDIS_URL")
	if redisURL == "" {
		log.Fatal("Error Critico: La variable de entorno REDIS_URL no esta definida.")
	}

	rabbitMQURL := os.Getenv("RABBITMQ_URL")
	if rabbitMQURL == "" {
		log.Fatal("Error Critico: La variable de entorno RABBITMQ_URL no esta definida.")
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080" // Puerto por defecto si no se especifica uno
	}

	return &Config{
		RedisURL:    redisURL,
		RabbitMQURL: rabbitMQURL,
		ServerPort:  port,
	}
}
