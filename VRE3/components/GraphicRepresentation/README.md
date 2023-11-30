# Graphic Representation

## Overview
This component is responsible for graphing information produced during and after the execution of the evolutionary algorithm that tries to optimize the value of any of the variables of the irrigation system. Specifically, it takes as an entry the evolution of the different functions of fitness (`fitness_evolution.txt`) and the file with the approach to the front of Pareto produced by the algorithm (`FUN.csv`). The set of plots are stored in an output zip file (`output.zip`).

The entry file `fitness_evolution.txt` must follow the following format, where in each line the evolution of the fitness value of a specific objective is shown:
```txt
0,0,0,0 ...
0,0,0,0 ...
0,0,0,0 ...
```

And the `FUN.csv` file, where each line represents a solution of the approximate Pareto Front:
```csv
obj1,obj2,obj3
0,0,0
0,0,0
...
```

## Usage

Create a virtual environment and install the requirements:

```sh
python -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

Then, run the script with:

```sh
python main.py --help
```

## Tests

Run the tests with:

```sh
python test_main.py
```

## Docker

### Build

Build the image with:

```sh
docker build -t $REGISTRY/smartfood/vre3/graphicrepresentation:1.0.0 .
```

### Run

Run the image with (assuming that the file is in the `data` folder from the current directory):

```sh
docker run --rm $REGISTRY/enbic2lab/misc/graphicrepresentation:1.0.0 --help
```

e.g.

```sh
docker run --rm -v $(pwd)/data:/mnt/shared/ $REGISTRY/enbic2lab/misc/graphicrepresentation:1.0.0 --fitness-evolution-file data/fitness_evolution.txt --fun-file data/FUN.csv --output-folder data
```