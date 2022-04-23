param(
    [Switch] $rm,
    [Switch] $build
)

$num_repls=$args[0]

if( $rm )
{
    echo "Removing..."
    docker stop $(docker ps -a -q)
    docker rm $(docker ps -a -q)
    docker network remove $(docker network ls --filter label=sec-internal-network -q)
}

if( $build )
{
    echo "Building..."
    docker network create sec-network
    docker build -t server .
}

$base_port=8080
echo "Generating keyStores..."
java -jar keyStoreGenerator.jar $(($num_repls))
echo "KeyStores ready!..."
for ($i = 0 ; $i -lt $num_repls ; $i++)
{
    docker network create --label sec-internal-network -d bridge "sec-server$i"
    docker run -d --name "db$i" -e "POSTGRES_USER=postgres" -e "POSTGRES_PASSWORD=postgres" -e  "POSTGRES_DB=SECDatabase" -v "$(pwd)/Server/db/postgres-data-${i}:/var/lib/postgresql/data" -v "$(pwd)/Server/db/tables.sql:/docker-entrypoint-initdb.d/tables.sql" --restart always postgres:10.5
    $port=$(($base_port+$i))
    docker run -d --name "server$i" -e "PASSWORD=admin" -p "${port}:8080" --network="sec-network" -v "$(pwd)/GeneratedKeyStores/server${i}keystore.jks:/app/serverkeystore.jks" -v "$(pwd)/GeneratedKeyStores/serversInfo.txt:/app/serversInfo.txt" server
    docker network connect --alias "database" "sec-server$i" "db$i"
    docker network connect --alias "server" "sec-server$i" "server$i"
    echo "Server $i is running!"
}

