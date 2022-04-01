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
To test the endpoints we only create testes for one endpoint, because there are functions that are difficult to mock, such as, send_amount due to the existence of randomly generated nonces. So we tested the endpoint open_account for the porpouse of testing the signature operations and the GRPC mechanisms of retriving errors and responses and, in the parts that are diferent between the endpoints we tested it by creating unit tests for every function that we call except the signature ones that we explain early.

To run te tests execute the following line:
```
mvn test
```

Client tests : 

* open_account_normal
* open_account_key_signature_mismatch
* send_amount_normal
* send_amount_signature_mismatch
* send_amount_nonce_mismatch
* receive_amount_normal
* receive_amount_signature_mismatch
* check_account_normal
* check_account_signature_mismatch
* audit_normal
* audit_signature_mismatch

Server tests : 
* creatUserNormalUse
* creatUserThatAlreadyIsCreated
* creatUserReplayAttack
* getBalanceNormalUse
* getBalancePublicKeyNotFound

* openAccount
* openAccountUserAlreadyExists
* openAccountSignatureDoNotMatch
* openAccountNoParams

* getNonce
* getNonceThatDoesNotExist
* getNonceReplayAttack
* creatNonce
* creatNonceThatAlreadyExist
* creatNonceWithAUnregisteredAccount

* addTransaction
* addTransactionWithZeroAmount
* addTransactionWithNegativeAmount
* addTransactionNotCreatedSender
* addTransactionNotCreatedReceiver
* addTransactionSenderAndReceiverAreTheSame
* addTransactionSenderDoesNotHaveBalance
* changeStatus
* changeStatusIDNotFound
* changeStatusIDDoesNotBelongToYou
* changeStatusNotCreatedUser
* changeStatusReplayAttack
* getPendingTransactions
* getPendingTransactionsNoTransactions
* getPendingTransactionsNotCreatedUser

## Additional Information

### Authors

* **Bernardo Várzea** - *ist1102150*
* **Pedro Trincheiras** - *ist1102151*
* **Filipe Barroso** - *ist1102152*

### Acknowledgments

* Professor Miguel Matos
