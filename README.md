# Leader election

## Ressources
src/ - contains java sources
data/ - folder where statistical datas are saved
data_sample/ - folder with a sample of statistical data
Makefile - run simple simulation or test suit
run_test.sh - script used to run test suit
plot_graph.sh - uses gnuplot to generate image charts

## NECESSARY LIBRARIES TO RUN
This program needs several peersim libraries to run :
djep-1.0.0.jar
jep-2.3.0.jar
peersim-1.0.5.jar
peersim-doclet.jar

This libraries can be found at peersim.sourceforge.et/#download and should be located in the same directory than the Makefile.

## Usage

```bash
make default
```
Starts simulation with default parameters
WARNING : Files in data folder are written at the end of every simulation so running with "make run" might corrupt previous statistical data. Dont forget to save them else where if necessary.


```bash
make test
```
Starts test suit, prints stats to standard output and generates image charts based on statistical data (might take a few minutes for huge simulations)

## Test architecture
Multiple simulations are run varying different parameters. This is achieved by the mean of the bash script (run_test) that automatically changes the parameters in the configuration file. Each simulation results are eventually stored into a data file (in repertory ./data) which gnuplot tool exploits in order to generate image charts.
