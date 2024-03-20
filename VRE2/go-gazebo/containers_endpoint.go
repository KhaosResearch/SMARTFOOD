package main

import (
	"context"
	"fmt"
	"net/http"
	"time"
)

func (s *HTTPServer) ContainersEndpoint(resp http.ResponseWriter, req *http.Request) {
	s.logger.Info("Request received", "method", req.Method, "path", req.URL.Path)
	switch req.Method {
	case "GET", "PUT", "POST":
		s.containersCreate(resp, req)
	default:
		resp.WriteHeader(http.StatusMethodNotAllowed)
	}
}

func (s *HTTPServer) containersCreate(resp http.ResponseWriter, _ *http.Request) {
	// Check if the maximum number of containers has been reached, and if so, return an error
	totalContainers, _ := s.config.redisClient.Get(context.Background(), "running").Int()
	if totalContainers >= s.config.maxContainers {
		s.logger.Info("Max number of containers reached", "totalContainers", totalContainers, "maxContainers", s.config.maxContainers)
		resp.WriteHeader(http.StatusServiceUnavailable)
		resp.Write([]byte("Max number of containers reached"))
		return
	}
	container := <-s.config.warmContainers
	s.logger.Info("Container retrieved from pool", "containerName", container.containerName)
	// The container has a TTL, after which it will be stopped
	s.config.redisClient.Set(context.Background(), container.containerName, "", time.Duration(containerTtl)*time.Second)
	s.config.redisClient.Incr(context.Background(), "running")
	resp.WriteHeader(http.StatusOK)
	resp.Write([]byte(fmt.Sprintf("%s.%s", container.containerName, domain)))
}
