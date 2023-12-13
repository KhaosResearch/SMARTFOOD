#!/bin/bash

if ! [[ $N_ROBOTS =~ ^[0-9]+$ ]]; then
  echo "Error: N_ROBOTS must be a natural number."
  exit 1
fi

case $WORLD in
  "inspection")
    X0="-1.7"
    Y0="-25.7"
    Z0="1.4"
    ;;
  "orchard")
    X0="-2.1"
    Y0="4.7"
    Z0="0.4"
    ;;
  "agriculture")
    X0="0"
    Y0="0"
    Z0="0"
    ;;
  # Office world has collision problems with the warthog model, technicaly it works but it is not usable
  #"office")
  #  X0="1.3"
  #  Y0="10.67"
  #  Z0="0.3"
  #  ;;
  *)
    echo "Error: Invalid value for the WORLD variable. Valid values are: agriculture, orchard, inspection."
    exit 1
    ;;
esac

LAUNCH_FILE_CONTENT="
<launch>
  <param name=\"${WORLD}_geom\" command=\"\$(find xacro)/xacro --inorder '\$(find cpr_${WORLD}_gazebo)/urdf/${WORLD}_geometry.urdf.xacro'\" />
  <arg name=\"world_name\" default=\"\$(find cpr_${WORLD}_gazebo)/worlds/actually_empty_world.world\" />
  <include file=\"\$(find rosbridge_server)/launch/rosbridge_websocket.launch\">
    <arg name=\"port\" value=\"9090\"/>
  </include>
  <include file=\"\$(find gazebo_ros)/launch/empty_world.launch\">
    <arg name=\"debug\" value=\"0\" />
    <arg name=\"gui\" value=\"false\" />
    <arg name=\"use_sim_time\" value=\"true\" />
    <arg name=\"headless\" value=\"true\" />
    <arg name=\"world_name\" value=\"\$(arg world_name)\" />
  </include>
  <node name=\"${WORLD}_world_spawner\" pkg=\"gazebo_ros\" type=\"spawn_model\" args=\"-urdf -model ${WORLD}_geometry -param ${WORLD}_geom -x 0 -y 0 -z 0 -Y 0\" />"

for ((i=1; i<=$N_ROBOTS; i++)); do
  X_VALUE=$(echo "scale=2; ($i - 1) * -4 + $X0" | bc)
  LAUNCH_FILE_CONTENT+="
  <group ns=\"/warthog_$i\">
    <include file=\"\$(find warthog_gazebo)/launch/spawn_warthog.launch\">
      <arg name=\"x\" value=\"$X_VALUE\" />
      <arg name=\"y\" value=\"$Y0\" />
      <arg name=\"z\" value=\"$Z0\" />
      <arg name=\"robot_name\" value=\"warthog_$i\" />
      <arg name=\"tf_prefix\" value=\"w_$i\" />
    </include>
  </group>"
done

LAUNCH_FILE_CONTENT+="
</launch>"

echo "$LAUNCH_FILE_CONTENT" > /home/catkin_ws/src/cpr_gazebo/cpr_${WORLD}_gazebo/launch/world.launch
