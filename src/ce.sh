#!/bin/sh
echo compiling
ecj -source 9 -target 9 -Xlint:all -deprecation -d ../binOut/ -sourcepath ./ *.java net/freertr/*.java
