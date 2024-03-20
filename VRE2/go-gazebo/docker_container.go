package main

import (
	"context"
	"fmt"

	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/network"
	log "github.com/sirupsen/logrus"
)

type ContainerOpts struct {
	ContainerConfig *container.Config
	HostConfig      *container.HostConfig
	NetworkConfig   *network.NetworkingConfig
	ContainerName   string
}

type RunningContainer struct {
	containerCtx    context.Context
	containerCancel context.CancelFunc
	containerID     string
	containerName   string
}

// createContainerConfig returns container configuration with defaults.
func createContainerConfig() ContainerOpts {
	containerName := GetRandomName(1)
	www := fmt.Sprintf("%s-www", containerName)
	ws := fmt.Sprintf("%s-ws", containerName)
	// Define the container configuration
	hostConfig := &container.HostConfig{
		RestartPolicy: container.RestartPolicy{
			Name: "always",
		},
	}
	config := &container.Config{
		Image: containerImage,
		// The container exposes two services: a web server and a WebSocket server,
		// listening on ports 8080 and 9090, respectively. We use labels to
		// configure Traefik to route traffic to the correct service
		Labels: map[string]string{
			fmt.Sprintf("traefik.http.routers.%s-router.rule", www): fmt.Sprintf(
				"Host(`%s.%s`)", containerName, domain,
			),
			fmt.Sprintf("traefik.http.routers.%s-router.service", www): fmt.Sprintf(
				"%s-service", www,
			),
			fmt.Sprintf("traefik.http.services.%s-service.loadbalancer.server.port", www): "8080",
			fmt.Sprintf(
				"traefik.http.routers.%s-router.rule", ws,
			): fmt.Sprintf(
				"Host(`api.%s.%s`)", containerName, domain,
			),
			fmt.Sprintf("traefik.http.routers.%s-router.service", ws): fmt.Sprintf(
				"%s-service", ws,
			),
			fmt.Sprintf("traefik.http.services.%s-service.loadbalancer.server.port", ws): "9090",
			"traefik.docker.network": "traefik",
		},
	}
	// The network must be the same as the one used by Traefik
	networkConfig := &network.NetworkingConfig{
		EndpointsConfig: map[string]*network.EndpointSettings{"traefik-net": {NetworkID: "traefik-net"}},
	}
	fmt.Printf("Running container: %s", containerName)
	return ContainerOpts{
		ContainerConfig: config,
		HostConfig:      hostConfig,
		NetworkConfig:   networkConfig,
		ContainerName:   containerName,
	}
}

// createContainer starts a new Gazebo container on host.
func createContainer(ctx context.Context) (*RunningContainer, error) {
	containerOpts := createContainerConfig()
	createResponse, err := client.ContainerCreate(
		ctx,
		containerOpts.ContainerConfig,
		containerOpts.HostConfig,
		containerOpts.NetworkConfig,
		nil,
		containerOpts.ContainerName,
	)
	if err != nil {
		log.WithError(err).Error("Failed to setup container")
		return nil, err
	}

	log.WithField("containerID", createResponse.ID).Info("Starting container")
	if err := client.ContainerStart(ctx, createResponse.ID, container.StartOptions{}); err != nil {
		log.WithField("containerID", createResponse.ID).WithError(err).Error("Failed to start container")
		return nil, err
	}

	containerCtx, containerCancel := context.WithCancel(ctx)

	return &RunningContainer{
		containerCtx:    containerCtx,
		containerCancel: containerCancel,
		containerID:     createResponse.ID,
		containerName:   containerOpts.ContainerName,
	}, nil
}

// shutDown stops and remove running container from machine.
func shutDown(ctx context.Context, containerName string) error {
	removeOptions := container.RemoveOptions{Force: true}
	return client.ContainerRemove(ctx, containerName, removeOptions)
}
