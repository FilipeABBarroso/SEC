# SEC - BFT Banking

## General Information

This file describes how to set up the project and test it against attacks.

### Build With

* [Java](https://www.java.com/)
* [Maven](https://maven.apache.org/)
* [Docker](https://www.docker.com/)
* [PostgreSQL](https://www.postgresql.org/)
* [gRPC](https://grpc.io/)

## Getting Started

In order to get this project up and running you will need to follow the next steps.

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

## Usage

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

## Demo

In order to demonstrate the mechanisms integrated in the project to tackle security and dependability threats,
it is possible to run the tests.

In the client the tests were made to test the signature, this is to check
the message integrity.

In the server the tests not only verify the message integrity, but also, for example, to check
in the send_amount request the nonce sent by the client, and to test against attacks, such as, replay attacks.
To test the endpoints it is tested only one, because there are functions difficult to mock the data, such as, send_amount, and the behaviour of the others are similar, but all the functions/components
that are not shared were tested alone.

To run te tests execute the following line:
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
