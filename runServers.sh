#!/bin/bash

while getopts 'rbn:' OPTION; do
  case "$OPTION" in
    r)
      echo "Removing..."
      docker stop $(docker ps -a -q)
      docker rm $(docker ps -a -q)
      docker network remove $(docker network ls --filter label=sec-internal-network -q)
      ;;
    b)
      echo "Building..."
      docker network create sec-network
      docker build -t server .
      ;;
    n)
      echo "Initiating..."
      numberOfInstances="$OPTARG"
      basePort=8080
      echo "Generating keyStores..."
      java -jar keyStoreGenerator.jar $((numberOfInstances))
      echo "KeyStores ready!..."
      for instance in $(seq 0 $((numberOfInstances - 1)))
      do
        docker network create --label sec-internal-network -d bridge "sec-server$instance"
        docker run -d --name "db$instance" -e "POSTGRES_USER=postgres" -e "POSTGRES_PASSWORD=postgres" -e  "POSTGRES_DB=SECDatabase" -v "$(pwd)/Server/db/postgres-data-${instance}:/var/lib/postgresql/data" -v "$(pwd)/Server/db/tables.sql:/docker-entrypoint-initdb.d/tables.sql" --restart always postgres:10.5
        port=$((basePort+instance))
        docker run -d --name "server$instance" -e "PASSWORD=admin" -p "$port:8080" --network="sec-network" -v "$(pwd)/GeneratedKeyStores/server${instance}keyStore.jks:/app/serverkeystore.jks" -v "$(pwd)/GeneratedKeyStores/serverInfo.txt:/app/serverInfo.txt" server
        docker network connect --alias "database" "sec-server$instance" "db$instance"
        docker network connect --alias "server" "sec-server$instance" "server$instance"
        echo "Server $instance is running!"
      done
      ;;
    ?)
      echo "script usage: [-r] to remove [-b] to build [-n numberOfInstances] to run n instances" >&2
      exit 1
      ;;
  esac
done