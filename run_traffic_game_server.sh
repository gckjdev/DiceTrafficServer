nohup java -Xms512m -Dconfig.lang=1 -Dmongodb.address=127.0.0.1 -Dconfig.robot=1 -Dserver.port=9000 -jar GameTrafficServer-1.0-SNAPSHOT.jar > ./traffic_server.log &
