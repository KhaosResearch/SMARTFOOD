package main

import "github.com/go-redis/redis/v8"

type Options struct {
	host           string
	port           string
	apiKey         string
	redisClient    *redis.Client
	warmContainers <-chan RunningContainer
	maxContainers  int
}

type Option func(*Options)

func WithHost(host string) Option {
	return func(o *Options) {
		o.host = host
	}
}

func WithPort(port string) Option {
	return func(o *Options) {
		o.port = port
	}
}

func WithApiKey(apiKey string) Option {
	return func(o *Options) {
		o.apiKey = apiKey
	}
}

func WithCache(redisClient *redis.Client) Option {
	return func(o *Options) {
		o.redisClient = redisClient
	}
}

func WithPool(warmContainers <-chan RunningContainer) Option {
	return func(o *Options) {
		o.warmContainers = warmContainers
	}
}

func WithMaxContainers(maxContainers int) Option {
	return func(o *Options) {
		o.maxContainers = maxContainers
	}
}

func newOptions(opts ...Option) Options {
	defaultOpts := Options{
		host:           "localhost",
		port:           "9010",
		apiKey:         "",
		redisClient:    nil,
		warmContainers: nil,
		maxContainers:  1,
	}
	for _, opt := range opts {
		opt(&defaultOpts)
	}
	return defaultOpts
}
