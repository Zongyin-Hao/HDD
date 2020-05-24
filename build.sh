#!/bin/sh  

# Define some constants 
PROJECT_PATH=.
JAR_PATH=$PROJECT_PATH/lib  
BIN_PATH=$PROJECT_PATH/bin  
SRC_PATH=$PROJECT_PATH/src  
  
# First remove the sources.list file if it exists and then create the sources file of the project  
rm -f $SRC_PATH/sources  
find $SRC_PATH -name *.java > $SRC_PATH/sources.list  
  
# First remove the ONSServer directory if it exists and then create the bin directory of ONSServer  
rm -rf $BIN_PATH
mkdir $BIN_PATH 
  
# Compile the project  
javac -d $BIN_PATH -classpath $JAR_PATH/soot.jar @$SRC_PATH/sources.list  
