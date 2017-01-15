VER=1.0.4

.PHONY: all clean doc release

all:
	javac -classpath src:jep-2.3.0.jar:djep-1.0.0.jar:peersim-1.0.5.jar:peersim-docletjar `find src -name "*.java"`
clean:
	rm -f `find -name "*.class"`

run: all
	java -classpath src:jep-2.3.0.jar:djep-1.0.0.jar:peersim-1.0.5.jar:peersim-docletjar peersim.Simulator config.txt
