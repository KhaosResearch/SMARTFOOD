# Smart-food Gazebo

This VRE allows to control several robots in a Gazebo robotics simulator instance using the mouse in a joystick. Both the Gazebosim and the joystick client are deployed in web interfaces. Also, it is possible to record robot's movements and saving them in a JSON. Then, it is possible playing them back in any moment and robot.

## Build

First, you have to build both Docker images (`controller` and `gzweb`) using the following commands:

```bash
docker build -t gzweb .
```

```bash
cd warthog_joystick
docker build -t warthog_joystick:latest .
```

## Deployment

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

You can also specify both environment variables using a *.env* file wiht the `--env-file` flag:

```bash
docker compose --env-file .env up
```

## Usage

The container is configured to launch a Warthog's model in an outdoor simulation environment.

Open a browser and navigate to `http://localhost:8080` to see the [Gzweb](https://github.com/osrf/gzweb) interface.

You can [drive any deployed warthog](https://www.clearpathrobotics.com/assets/guides/noetic/warthog/Driving.html) using the client application at `http://localhost:8081`.

## License

All rights reserved (c) Khaos Research. Do not distribute.
