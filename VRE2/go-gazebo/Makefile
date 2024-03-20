.PHONY: \
	build \
	static-build \
	run \
	tidy \
	tests

build:
	go mod tidy -v
	go build -v

# Great for running in Docker containers.
static-build:
	CGO_ENABLED=0 GOOS=linux go build -a -ldflags '-extldflags "-static"' .

format:
	go fmt .

run:
	go run .

tidy:
	go mod tidy -v

tests:
	go clean -testcache .
	go test -cover -v .
