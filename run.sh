#!/bin/sh  

path=$1
main=$2

# Define some constants
PROJECT_PATH=. 
JAR_PATH=$PROJECT_PATH/lib  
BIN_PATH=$PROJECT_PATH/bin  
  
# Run the project as a background process  
java -classpath $BIN_PATH:$JAR_PATH/soot.jar hybriddetector.Main $path $main
