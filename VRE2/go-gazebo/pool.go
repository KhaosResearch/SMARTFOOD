package main

import (
	"context"
	"time"

	log "golang.org/x/exp/slog"
)

func fillPool(ctx context.Context, pool chan RunningContainer, poolSize int) {
	ticker := time.NewTicker(time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			log.Info("Shutting down pool")
			cleanupPool(pool)
			return
		case <-ticker.C:
			if len(pool) < poolSize {
				container, err := createContainer(ctx)
				if err != nil {
					log.Error("Failed to create container", err)
					time.Sleep(time.Second)
					continue
				}
				log.Info("New container created and started", "container", container.containerID)
				// TODO: Check if the container is healthy before adding it to the pool
				pool <- *container
				log.Info("Container added to pool", "container", container.containerID)
			}
		}
	}
}

func cleanupPool(pool chan RunningContainer) {
	close(pool) // Close the channel before draining it
	for container := range pool {
		if err := shutDown(context.Background(), container.containerID); err != nil {
			log.Error("Failed to shut down container", err, "container", container.containerID)
		}
	}
}
