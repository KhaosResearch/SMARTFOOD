FROM openjdk:11

LABEL Khaos Research Group <khaos.uma.es>

COPY LICENSE ACKNOWLEDGEMENTS /mnt/shared/

WORKDIR /usr/src/app

RUN apt-get update && apt-get install -y maven
COPY java-project ./java-project
RUN mvn -f java-project/pom.xml clean compile assembly:single

ARG executable
RUN echo "java -cp java-project/target/EA-InpVarOpt-1.0-SNAPSHOT-jar-with-dependencies.jar epanet.$executable \$@" > myScript.sh
ENTRYPOINT ["bash", "myScript.sh"]