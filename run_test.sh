#!/bin/bash
files=("no_leader.dat" "election_rate.dat" "election_time.dat")
density_file="density.dat"
netsize=(20 40 60 70 80 100)
speed=(15 25 35)
WIDTH=2000
HEIGHT=2000
make clean

#First tests (varying network size and node speed)
sed -i "42s/.*/control.endcontroler.testType 0/" config.txt 
sed -i "25s/.*/protocol.emitter.scope 170/" config.txt 
for i in ${netsize[@]};
do
	#change network size in config file
	sed -i "5s/.*/network.size $i/" config.txt 

	#Append network size in data files for gnuplot
	for k in ${files[@]};
	do
		echo -n "$i " >> "data/$k"
	done

	for j in ${speed[@]};
	do
		#change speed in config file
		sed -i "16s/.*/protocol.positionprotocol.maxSpeed $j/" config.txt 
		#run test
		make run
	done

	for k in ${files[@]};
	do
		echo "" >> "data/$k"
	done

done 

#Second tests (varying node density and node range)
range=(100 130 150 160 170)

sed -i "42s/.*/control.endcontroler.testType 1/" config.txt 
sed -i "16s/.*/protocol.positionprotocol.maxSpeed 15/" config.txt 
for i in ${netsize[@]};
do
	#change network size in config file
	sed -i "5s/.*/network.size $i/" config.txt 
	density=`echo "$i / ($WIDTH*$HEIGHT)" | bc -l`
	echo -n "$density " >> "data/$density_file"

	for j in ${range[@]};
	do
		#change scope in config file
		sed -i "25s/.*/protocol.emitter.scope $j/" config.txt 
		#run test
		make run
	done

	#Append return in data files for gnuplot
	echo "" >> "data/$density_file"

done 


cd data
gnuplot -p -e 'set title "Time without leader graph"; set xlabel "network size"; set ylabel "time without leader"; plot "no_leader.dat" using 1:2 title "Speed 15" with lines, "no_leader.dat" using 1:3 title "Speed 25" with lines, "no_leader.dat" using 1:4 title "Speed 35" with lines'
gnuplot -p -e 'set title "Election rate graph"; set xlabel "network size"; set ylabel "election rate"; plot "election_rate.dat" using 1:2 title "Speed 15" with lines, "election_rate.dat" using 1:3 title "Speed 25" with lines, "election_rate.dat" using 1:4 title "Speed 35" with lines'
gnuplot -p -e 'set title "Election time"; set xlabel "network size"; set ylabel "election time"; plot "election_time.dat" using 1:2 title "Speed 15" with lines, "election_time.dat" using 1:3 title "Speed 25" with lines, "election_time.dat" using 1:4 title "Speed 35" with lines'
gnuplot -p -e 'set title "Average number of leader"; set xlabel "density"; set ylabel "number of leader"; plot "density.dat" using 1:2 title "range 100" with lines, "density.dat" using 1:3 title "range 150" with lines, "density.dat" using 1:4 title "range 200" with lines, "density.dat" using 1:5 title "range 250" with lines'
