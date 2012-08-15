#!/bin/bash

cd ../Common_Java &&
mvn clean install

cd ../Common_Java_Game
mvn clean install 

cd ../DiceTrafficServer
mvn clean install
