package main

import (
	"context"
	"flag"
	"github.com/go-redis/redis/v8"
	log "golang.org/x/exp/slog"
	"net"
	"os"
	"os/signal"
)

var (
	serverHost, serverPort, serverApiKey, redisHost, redisPort, redisPassword, containerImage, domain string
	containerTtl, poolSize, maxContainers                                                             int
)

func init() {
	flag.StringVar(&serverHost, "host", "0.0.0.0", "Host address to listen on")
	flag.StringVar(&serverPort, "port", "9010", "Port to listen on")
	flag.StringVar(&serverApiKey, "api-key", "s3cr3t", "Authentication API key for the server")
	flag.StringVar(&redisHost, "redis-host", "localhost", "Host address of the Redis server")
	flag.StringVar(&redisPort, "redis-port", "6379", "Port of the Redis server")
	flag.StringVar(&redisPassword, "redis-password", "", "Password for the Redis server")
	flag.StringVar(&domain, "traefik-http-router-domain", "gazebo.test", "Domain to use for the container")
	// Container options
	flag.StringVar(&containerImage, "container-image", "gzweb:latest", "Docker image to use for the container")
	flag.IntVar(&containerTtl, "container-ttl", 300, "Time to live for the container in seconds")
	flag.IntVar(&poolSize, "pool-size", 1, "Number of warm containers to maintain in the pool.")
	flag.IntVar(&maxContainers, "max-containers", 3, "Maximum number of running containers allowed")
}

func connectToRedis(ctx context.Context) *redis.Client {
	// Define Redis connection options
	options := &redis.Options{
		Addr:     net.JoinHostPort(redisHost, redisPort),
		Password: redisPassword,
		DB:       0,
	}

	client := redis.NewClient(options)

	// Ping Redis to test connection
	if err := client.Ping(ctx).Err(); err != nil {
		log.Error("Failed to connect to Redis", err)
		os.Exit(1)
	}

	// Keyspace events are used to listen for changes in Redis. They are
	// desactivated by default, so we need to enable them explicitly
	_, err := client.Do(ctx, "CONFIG", "SET", "notify-keyspace-events", "KEA").Result()
	if err != nil {
		log.Error("Unable to set keyspace events", err)
		os.Exit(1)
	}

	log.Info("Connected to Redis")
	return client
}

func main() {
	flag.Parse()

	ctx := context.Background()
	ctx, cancel := signal.NotifyContext(ctx, os.Interrupt)
	defer cancel()

	redisClient := connectToRedis(ctx)
	defer redisClient.Close()

	// Create a pool of warm containers ready to serve requests
	warmContainers := make(chan RunningContainer, poolSize)
	go fillPool(ctx, warmContainers, poolSize)

	// Subscribe to Redis keyspace events to listen for key expiration
	pubSub := redisClient.PSubscribe(ctx, "__keyevent@0__:expired")
	defer pubSub.Close()

	ch := pubSub.Channel()

	go func(*redis.PubSub) {
		for {
			select {
			case <-ctx.Done():
				log.Info("shutting down")
				return
			case message, ok := <-ch:
				if !ok {
					log.Info("channel has been closed")
					return
				}
				// If the event is a key expiration, shut down the associated
				// container and decrement the running counter
				if message.Channel == "__keyevent@0__:expired" {
					containerName := message.Payload
					log.Info("removing container", "container", containerName)
					if err := shutDown(ctx, containerName); err != nil {
						log.Error("error shutting down container", err, "container", containerName)
						continue
					}
					redisClient.Decr(ctx, "running")
				}
			}
		}
	}(pubSub)

	log.Info("starting HTTP server")

	httpServer := NewHTTPServer(
		WithHost(serverHost),
		WithPort(serverPort),
		WithApiKey(serverApiKey),
		WithCache(redisClient),
		WithPool(warmContainers),
		WithMaxContainers(maxContainers),
	)
	httpServer.Start(ctx)
}
