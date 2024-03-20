package main

import (
	"context"
	"net"
	"net/http"
	"os"
	"sync"

	log "golang.org/x/exp/slog"
)

type HTTPServer struct {
	config Options
	mux    *http.ServeMux
	logger *log.Logger
}

func NewHTTPServer(opts ...Option) *HTTPServer {
	s := &HTTPServer{
		config: newOptions(opts...),
		mux:    http.NewServeMux(),
		logger: log.New(log.NewJSONHandler(os.Stdout)),
	}
	s.addRoutes(true)
	return s
}

// Start starts the HTTP server and blocks until the server is stopped.
func (s *HTTPServer) Start(ctx context.Context) error {
	httpServer := http.Server{
		Addr:    net.JoinHostPort(s.config.host, s.config.port),
		Handler: s.mux,
	}
	go func() {
		s.logger.Info("listening on " + s.config.host)
		if err := httpServer.ListenAndServe(); err != http.ErrServerClosed {
			s.logger.Error("error listening and serving", err)
		}
	}()
	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		defer wg.Done()
		<-ctx.Done()
		if err := httpServer.Shutdown(context.Background()); err != nil {
			s.logger.Error("error shutting down server", err)
		}
	}()
	wg.Wait()
	return nil
}

type handler func(http.ResponseWriter, *http.Request)

func protected(apiKey string, next handler) handler {
	return func(w http.ResponseWriter, r *http.Request) {
		token := r.Header.Get("X-Api-Key")
		if token != apiKey {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		next(w, r)
	}
}

// addRoutes attaches handlers to the mux.
func (s *HTTPServer) addRoutes(enableDebug bool) {
	s.mux.HandleFunc("/containers", protected(s.config.apiKey, s.ContainersEndpoint))
	if enableDebug {
		s.mux.HandleFunc(
			"/healthz", func(resp http.ResponseWriter, req *http.Request) {
				resp.WriteHeader(http.StatusOK)
			},
		)
	}
}
