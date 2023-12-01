## NAME

EPANET-SmartFood

## AUTHOR

Adrián Segura Ortiz

## DESCRIPTION

This repository contains a component capable of predicting the values associated with a given variable of the existing connectors in an irrigation system (e.g. robustness, diameter, length, etc.) based on the pressure levels observed at the nodes in the real environment. To do so, it performs an optimization exercise through the JMetal framework focusing its evaluation on the distance found between the real pressure values and those calculated by the well-known simulator called EPANET.

## DOCKER

Build JAR file from the project

```
mvn -f ./EA-InpVarOpt-JMetal/pom.xml clean compile assembly:single
```

### Optimize variable component

```
docker build -t $REGISTRY/smartfood_epanet_optimization -f optimization.dockerfile .
docker run -v $(pwd):/usr/local/src/ $REGISTRY/smartfood_epanet_optimization data/pivot.inp data/pivot_default_pressures.csv 'Roughness' 0.2 'SSE;SEHomogeneity;RoughnessHomogeneity' 10 1000 SMPSO-SyncParallel 0.9 0.1 VAR.csv FUN.csv fitness_evolution.txt
docker push $REGISTRY/smartfood_epanet_optimization
```

### Graphic representation component

```
docker build -t $REGISTRY/smartfood_epanet_graphic-representation -f graphic_representation.dockerfile .
docker run -v $(pwd):/usr/local/src/ $REGISTRY/smartfood_epanet_graphic-representation --fitness-evolution-file fitness_evolution.txt --fun-file FUN.csv --output-zip-file graphics.zip
docker push $REGISTRY/smartfood_epanet_graphic-representation
```

### Simulation component

```
docker build -t $REGISTRY/smartfood_epanet_simulation -f simulation.dockerfile .

# Only INP file (with internal default values)
docker run -v $(pwd):/usr/local/src/ $REGISTRY/smartfood_epanet_simulation data/villar.inp simulated_pressures.csv

# Using a variable file to generate pressures with several different sets of values
docker run -v $(pwd):/usr/local/src/ $REGISTRY/smartfood_epanet_simulation data/villar.inp simulated_pressures.zip VAR.csv 'Roughness'

docker push $REGISTRY/smartfood_epanet_simulation
```

### Parameterization and algorithm comparison component

```
docker build -t $REGISTRY/smartfood_epanet_parameterization -f paretos.dockerfile .
docker run -v $(pwd):/usr/local/src/ $REGISTRY/smartfood_epanet_parameterization data/t0_villar data/t1_villar data/t2_villar data/t3_villar data/t4_villar data/t5_villar data/t6_villar data/t7_villar data/t8_villar data/t9_villar data/t10_villar data/t11_villar data/t12_villar data/t13_villar data/t14_villar data/t15_villar data/t16_villar data/t17_villar data/t18_villar data/t19_villar data/t20_villar data/t21_villar data/t22_villar data/t23_villar
```
