FROM openjdk:11

LABEL Khaos Research Group <khaos.uma.es>

COPY components/Parameterization/LICENSE components/Parameterization/ACKNOWLEDGEMENTS /mnt/shared/

RUN apt-get update && apt-get install -y maven
RUN mvn -f java-project/pom.xml clean compile assembly:single
RUN ls
COPY java-project/target/EA-InpVarOpt-1.0-SNAPSHOT-jar-with-dependencies.jar /mnt/shared/main.jar

WORKDIR /usr/src/app

ENTRYPOINT ["java",  "-cp",  "main.jar", "epanet.RunnerParameterization"]