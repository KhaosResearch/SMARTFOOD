FROM ros:melodic-ros-base-bionic@sha256:d91ad01fdebb311393ecc3a1e15878f9499823922d43e83ec9907ddf76513658

# install gazebo ros packages
RUN apt-get update --allow-insecure-repositories && apt-get install --allow-unauthenticated -y --no-install-recommends \
    ros-melodic-joint-state-controller=0.17.3-1bionic.20230131.222942 \
    ros-melodic-effort-controllers=0.17.3-1bionic.20230524.154925 \
    ros-melodic-position-controllers=0.17.3-1bionic.20230131.223210 \
    ros-melodic-velocity-controllers=0.17.3-1bionic.20230131.223133 \
    ros-melodic-rostopic=1.14.13-1bionic.20221025.185955 \
    ros-melodic-ros-control=0.18.4-1bionic.20221025.202556 \
    ros-melodic-ros-controllers=0.17.3-1bionic.20230524.160713 \
    ros-melodic-gazebo-ros=2.8.7-1bionic.20221025.195022 \
    ros-melodic-gazebo-ros-pkgs=2.8.7-1bionic.20221025.201835 \
    ros-melodic-gazebo-ros-control=2.8.7-1bionic.20221025.202747 \
    ros-melodic-warthog-simulator=0.2.2-2bionic.20230524.162408 \
    ros-melodic-rosbridge-server=0.11.16-2bionic.20221025.192534 \
    && rm -rf /var/lib/apt/lists/*

# install gzweb dependencies
RUN apt-get update --allow-insecure-repositories && apt-get install --allow-unauthenticated -y --no-install-recommends \
    libjansson-dev \
    libboost-dev \
    imagemagick \
    libtinyxml-dev \
    mercurial \
    cmake \
    build-essential \
    unzip \
    libeigen3-dev \
    curl \
    bc \
    && rm -rf /var/lib/apt/lists/*

# install node and npm
# see: https://gist.github.com/remarkablemark/aacf14c29b3f01d6900d13137b21db3a
RUN mkdir /usr/local/nvm
ENV NVM_DIR /usr/local/nvm
ENV NODE_VERSION 14.18.1
RUN curl https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash \
    && . $NVM_DIR/nvm.sh \
    && nvm install $NODE_VERSION \
    && nvm alias default $NODE_VERSION \
    && nvm use default

ENV NODE_PATH $NVM_DIR/v$NODE_VERSION/lib/node_modules
ENV PATH $NVM_DIR/versions/node/v$NODE_VERSION/bin:$PATH

# clone gzweb
WORKDIR /home/
RUN git clone https://github.com/osrf/gzweb.git \
    && cd gzweb \
    && git checkout 1082133

# copy assets
COPY assets.zip /home/
RUN unzip -o /home/assets.zip -d /home/gzweb/

# compile packages with Catkin
WORKDIR /home/catkin_ws/src
RUN git clone https://github.com/clearpathrobotics/cpr_gazebo.git \
    && cd cpr_gazebo \
    && git checkout 509afbe

WORKDIR /home/catkin_ws/
RUN . /opt/ros/melodic/setup.sh \
    && catkin_make

# copy assets
RUN cp -r /home/catkin_ws/src/cpr_gazebo/* /home/gzweb/http/client/assets/
RUN cp -r /opt/ros/melodic/share/* /home/gzweb/http/client/assets/
RUN cp -r /usr/share/gazebo-9/media/ /home/gzweb/http/client/assets/
RUN cp /home/gzweb/http/client/assets/media/materials/textures/wood.jpg /home/gzweb/http/client/assets/media/materials/textures/wood.png
COPY spawn_warthog.launch /opt/ros/melodic/share/warthog_gazebo/launch/
COPY warthog.gazebo /opt/ros/melodic/share/warthog_description/urdf/

ENV GAZEBO_MODEL_PATH /usr/share/gazebo-9/models:/home/gzweb/http/client/assets/

# Change default camera position
RUN sed -i 's/this.defaultCameraPosition = new THREE.Vector3(0, -5, 5);/this.defaultCameraPosition = new THREE.Vector3(0, 0, 20);/g' /home/gzweb/gz3d/src/gzscene.js

# Build Gzweb after copying assets
WORKDIR /home/gzweb/
RUN . $NVM_DIR/nvm.sh && ./deploy.sh

# Expose the gzweb and gzbridge ports
EXPOSE 8080
EXPOSE 9090

# Set the number of robots to spawn and the world to load
ENV N_ROBOTS 2
ENV WORLD agriculture

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT /entrypoint.sh && \
   npm start & . /opt/ros/melodic/setup.sh && \
  . /home/catkin_ws/devel/setup.sh && \
  roslaunch cpr_${WORLD}_gazebo world.launch -v
