#!/bin/bash

# If you want to use this script to kill other 
# app, just modify the APPNAME to that app, and
# rename this scprit to a self-explainable name.
APPNAME="DiceTrafficServer"

# An auxiliary function
print_pid_list() {
	N=0
	echo "There is $COUNT instances of $1  running:"
	printf "	PID	PORT\n"
	for i in $(echo $PID_TO_BE_KILLED); do
		let "N=$N+1"
		echo -n "$N	$i	" 
		echo $(ps aux | grep java | grep $i | awk '{printf $17}'| cut -d '=' -f 2)
	done
}


PID_TO_BE_KILLED=$(ps aux | grep java |  grep $APPNAME | awk '{print $2}')
[ -z "$PID_TO_BE_KILLED" ] && echo " No instance of $APPNAME running..." && exit 1
COUNT=$(echo $PID_TO_BE_KILLED | wc -w)

if [  $COUNT > 1 ]; then
	 print_pid_list
	 echo -e "\n"
	 echo "choose which one to  kill, input q to do nothing"	
	 while read input  
	 do
		 if [ "$input" == "q" ]; then
			exit 0
		 elif [ $input -gt $COUNT ] ; then
			echo "No kidding me, dude -.- Out of range! Reinput:"
			continue
		 else
			for i in $(echo $PID_TO_BE_KILLED); do
				let "input=$input-1"
				[ $input == 0 ] && \
				(echo "$APPNAME's pid is $i,now killing it...";
				kill -9 $i) && break;
			done
			exit 0
		fi
	done	
fi

		 
