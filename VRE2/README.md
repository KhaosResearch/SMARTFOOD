# Smart-food Gazebo

This VRE allows to control several robots in a Gazebo robotics simulator instance using the mouse in a joystick. Both the Gazebosim and the joystick client are deployed in web interfaces. Also, it is possible to record robot's movements and saving them in a JSON. Then, it is possible playing them back in any moment and robot.

## Build

First, you have to build both Docker images (`warthog_joystick` and `gzweb`) using the following commands:

```bash
docker build -t gzweb .
```

```bash
cd warthog_joystick
docker build -t warthog_joystick:latest .
```

## Quick deployment
*The quick deployment is optional since it is already integrated with the webpage*

You can modify the environment variables in the `docker-compose.yml` file to change the simulation scenario (`WORLD`) and the number of robots deployed (`N_ROBOTS`).
The `N_ROBOTS` variable has to be a positive integer greater than 0.
The `WORLD` variable can be one of the following:
- agriculture
- orchard
- inspection

Then, you can deploy the simulation scenario with:
```bash
docker compose up
```

You can also specify both environment variables using a *.env* file with the `--env-file` flag:

```bash
docker compose --env-file .env up
```

## Quick usage (optional)
*The quick usage is optional since it is already integrated with the webpage*

The container is configured to launch a Warthog's model in an outdoor simulation environment.

Open a browser and navigate to `http://localhost:8080` to see the [Gzweb](https://github.com/osrf/gzweb) interface.

You can [drive any deployed warthog](https://www.clearpathrobotics.com/assets/guides/noetic/warthog/Driving.html) using the client application at `http://localhost:8081`.


# Integrating the deployment with the Webpage

To automate the deployment of the VRE by just pressing a button on the webpage, we have developed a provisioner. The Gazebo provisioner is a simple HTTP server to run robotic simulations with Gazebo and ROS on demand. It listens for requests to start new robotic simulation environments and serve them with [Traefik proxy](https://traefik.io/traefik/).

## Pre-requisites

Make sure you have built the `gzweb` image as explained in [build section](#build).

## Deployment

Run `go-gazebo/docker-compose.yml` containing Redis server, Traefik and go-gazebo. 
- Redis is used to store the state of the containers.
- Traefik is used as a reverse proxy to serve the containers.
- go-gazebo is the provisioner.

```bash
cd go-gazebo
docker compose up -d
```

## Go-gazebo parameters

When the docker compose is launched, the go-gazebo server is executed with the following parameters:

```bash
./go-gazebo -api-key s3cr3t \ 
    -container-image gzweb:latest \
    -container-ttl 120 \
    -host 0.0.0.0 \
    -max-containers 3 \
    -pool-size 1 \
    -port 9010 \
    -redis-host redis \
    -redis-port 6379 \
    -traefik-http-router-domain gazebo.test
```

This should be changed in the `go-gazebo/docker-compose.yml` according to your needs. The default values and the description of each parameter are:

```
  -api-key string
        Authentication API key for the server (default "s3cr3t")
  -container-image string
        Docker image to use for the container (default "gzweb:latest")
  -container-ttl int
        Time to live for the container in seconds (default 300)
  -host string
        Host address to listen on (default "0.0.0.0")
  -max-containers int
        Maximum number of running containers allowed (default 3)
  -pool-size int
        Number of warm containers to maintain in the pool. (default 1)
  -port string
        Port to listen on (default "9010")
  -redis-host string
        Host address of the Redis server (default "localhost")
  -redis-password string
        Password for the Redis server
  -redis-port string
        Port of the Redis server (default "6379")
  -traefik-http-router-domain string
        Domain to use for the container (default "gazebo.test")
```

An example of usage where the server listens on `localhost:9010`, the container time to live is 120 seconds, 2 containers are created initially, and the maximum number of containers running at the same time is 4:

```bash
$ ./go-gazebo -host localhost -port 9010 -container-ttl 120 -pool-size 2 -max-containers 4
```

To start a new container, you can send a POST request to the `/containers` endpoint with the `X-Api-Key` header set to `s3cr3t`:

```bash
$ curl -H "X-Api-Key: s3cr3t" -X POST 'http://localhost:9010/containers'
```

This will return a string with the next format `<container-name>.<traefik-http-router-domain>`. For example, `serene_burnell.gazebo.test`.

However, the provisioner is designed to be used with the Webpage. The Webpage will send a POST request to the provisioner to start a new container.

### Configuration

In the example above, Traefik is running on port `80`. The containers are served with the following URLs:

> http://container-name.gazebo.test (for the web interface)

and

> http://api.container-name.gazebo.test (for the websocket API)

where `container-name` is the name of the container, e.g., [http://serene_burnell.gazebo.test](http://serene_burnell.gazebo.test).

When accesing the containers from another machine, you need to resolve the `.test` top-level domain to the IP address of the host machine. You can do this with [dnsmasq](https://thekelleys.org.uk/dnsmasq/doc.html) (replace `<host-ip>` with the IP address of the host machine):

```bash
$ echo 'address=/.test/<host-ip>' >> /etc/dnsmasq.conf
```