# Smart-food Gazebo

This repo hosts Dockerfiles for robotic simulation scenarios with Gazebo and ROS.

## Deployment

Use the `compose.yml` file to deploy the application:

```bash
docker compose up -d
```

## Usage

The container is configured to launch a Warthog's model in an outdoor simulation environment.

Open a browser and navigate to `http://localhost:8080` to see the [Gzweb](https://github.com/osrf/gzweb) interface.

You can [drive the model](https://www.clearpathrobotics.com/assets/guides/noetic/warthog/Driving.html) using the client application at `http://localhost:8081`.

## License

All rights reserved (c) Khaos Research. Do not distribute.
