# SEC - BFT Banking

## General Information

chicken nugget?

### Build With

* [Java](https://www.java.com/)
* [Maven](https://maven.apache.org/)
* [Docker](https://www.docker.com/)
* [PostgreSQL](https://www.postgresql.org/)

## Getting Started

In order to get this project up and running... One client per RunTime... keystore file

### Prerequisites

* Java JDK15 or latest version
* Maven 3.8.1 or latest version
* Docker 20.10.13 or latest version

### Setup

Clean and install dependencies
```
mvn clean install
```
Compile source code
```
mvn compile
```
Compile Test source code
```
mvn compile-test
```

### Usage

Start Databases through Docker
```
docker-compose up
```
Start Server
```
mvn compile exec:java -Dexec.mainClass="tecnico.sec.client.Main"
```
Start Client
```
mvn compile exec:java -Dexec.mainClass="tecnico.sec.server.Main"
```

### Demo

In order to demonstrate the mechanisms integrated in the project to tackle security and dependability threats...
beca beca
```
mvn test
```

## Additional Information

### Authors

* **Bernardo Várzea** - *ist1102150*
* **Pedro Trincheiras** - *ist1102151*
* **Filipe Barroso** - *ist1102152*

### Acknowledgments

* Professor Miguel Matos
