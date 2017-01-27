#!/bin/bash
netsize=(20 40 60 80 100)
speed=(15 25 35)
for i in ${speed[@]};
do
	#change speed in config file
	sed -i "16s/.*/protocol.positionprotocol.maxSpeed $i/" config.txt 

	for j in ${netsize[@]};
	do
		#change network in config file
		sed -i "5s/.*/network.size $j/" config.txt 
		#run test
		make run
	done
done 
