#!/bin/sh

#prepare 2* 16 MB of our repo for the sg tool.
#will run every day

#amount of files
y=2877

x=0
echo Phase 1

#rm filecode
for i in 1*
do 
  for j in $i/*.java
  do
    echo "$x / $y \r \c"
    x=`expr $x + 1`
    echo _  >> filecode
    echo $j >> filecode
    cat $j >> filecode
  done
done

x=0
echo Phase 2

#rm filecodeup
for i in 1*
do 
  for j in $i/*.java
  do
    echo "$x / $y \r \c"
    x=`expr $x + 1`
    echo _  >> filecodeup
    echo $j >> filecodeup
    tr "[:lower:]" "[:upper:]" < $j >> filecodeup
  done
done

#gzip filecode*
