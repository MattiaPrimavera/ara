# Leader election

## Usage

```bash
make run
```
Starts simulation with default parameters


```bash
make test
```
Starts test suit, prints stats to standard output and generates image charts based on statistical data (might take a few minutes for huge simulations)

## Test architecture
Multiple simulations are run varying different parameters. This is achieved by the mean of the bash script (run_test) that automatically changes the parameters in the configuration file. Each simulation results are eventually stored into a data file (in repertory ./data) which gnuplot tool exploits in order to generate image charts.
