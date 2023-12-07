# Optimization

## Overview

This component contains a Java project that takes advantage of the EPANET simulator. Since its functional diversity, it has been divided into three subcomponents:

* **Variable Optimization**: Component capable of predicting the values associated with a given variable of the existing connectors in an irrigation system (roughness or monirloss) based on the pressure levels observed at the nodes in the real environment. To do so, it performs an optimization exercise through the JMetal framework focusing its evaluation on the distance found between the real pressure values and those calculated by the well-known simulator called EPANET. It takes as input an .inp file with the irrigation scenario (`input.inp`) and a .csv with the set of true system pressures (`input.csv`). It returns as output the individuals of the approximate pareto front found (`VAR.csv`), the values of these individuals for each of the fitness functions (`FUN.csv`) and a txt file with the progress of these values along the different iterations (`fitness_evolution.txt`).

* **Simulation**: Component responsible for simulating the pressures of an irrigation system given an input scenario. Optionally, a VAR.csv file resulting from the Variable Optimization component can be provided to iteratively replace the values of the variables preset in the input .inp with those present in the Pareto front approximation. It takes as input an .inp file with the irrigation scenario (`input.inp`) and optionally the csv with the values of the variables to be replaced (`VAR.csv`). Output is a zip file (`output.zip`) with a csv for each row of the VAR.csv file, or in case of only simulating with the .inp values, a single csv.

* **Parameterization**: Component responsible for comparing different evolutionary algorithm configurations designed for the Optimization Variable component. It takes as input a zip file (`input.zip`) with the different .inp (irrigation systems) and .csv (real pressures) files. The result is another zip file (`output.zip`) with the results of the comparisons.

## Usage

Build the Maven project and generate the .Jar file with the dependencies

```sh
mvn -f java-project/pom.xml clean compile assembly:single
```

Then run the .Jar as follows:

```sh
# Variable Optimization
java -cp java-project/target/EA-InpVarOpt-1.0-SNAPSHOT-jar-with-dependencies.jar epanet.RunnerVariableOptimization --help

# Simulation
java -cp java-project/target/EA-InpVarOpt-1.0-SNAPSHOT-jar-with-dependencies.jar epanet.RunnerSimulation --help

# Parameterization
java -cp java-project/target/EA-InpVarOpt-1.0-SNAPSHOT-jar-with-dependencies.jar epanet.RunnerParameterization --help
```

## Tests

Run the tests with:

```sh
mvn -f java-project/pom.xml clean test
```

## Docker

### Build

Build the image with:

```sh
# Variable Optimization
docker build -t $REGISTRY/smartfood/vre3/variableoptimization:1.0.0 --build-arg executable=RunnerVariableOptimization .

# Simulation
docker build -t $REGISTRY/smartfood/vre3/simulation:1.0.0 --build-arg executable=RunnerSimulation .

# Parameterization
docker build -t $REGISTRY/smartfood/vre3/parameterization:1.0.0 --build-arg executable=RunnerParameterization .
```

### Run

Run the image with (assuming that the file is in the `data` folder from the current directory):

```sh
# Variable Optimization
docker run --rm $REGISTRY/smartfood/vre3/variableoptimization:1.0.0 --help

# Simulation
docker run --rm $REGISTRY/smartfood/vre3/simulation:1.0.0 --help

# Parameterization
docker run --rm $REGISTRY/smartfood/vre3/parameterization:1.0.0 --help
```

e.g.

```sh
# Variable Optimization
docker run --rm -v $(pwd)/data:/mnt/shared/ $REGISTRY/smartfood/vre3/variableoptimization:1.0.0 --inp-file /mnt/shared/pivot.inp --pressure-file /mnt/shared/pivot_default_pressures.csv

# Simulation
docker run --rm -v $(pwd)/data:/mnt/shared/ $REGISTRY/smartfood/vre3/simulation:1.0.0 --inp-file /mnt/shared/pivot.inp --var-file /mnt/shared/VAR.csv --str-variables Roughness

# Parameterization
docker run --rm -v $(pwd)/data:/mnt/shared/ $REGISTRY/smartfood/vre3/parameterization:1.0.0 --input-zip /mnt/shared/input.zip
```
