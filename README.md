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

Start Databases and Servers through Docker using Bash
```
./runServers.sh -r -b -n NUMBEROFSERVERINSTANCES
```

Start Client
```
mvn compile exec:java -Dexec.mainClass="tecnico.sec.server.Main"
```

## Demo

In order to demonstrate the mechanisms integrated in the project to tackle security and dependability threats,
you will need to follow the following steps.

Run the servers and wait for the DBs to get ready to receive connections.

Run a client and leave it running. Remove the file called clientKeystore.ks and run other client instance.

Once you have two clients running you can test all the endpoints as you want, with invalid inputs with valid inputs.

To test the atomic register algorithm you will need to go on the docker dashboard or get some docker server containers ids and shut them down (shut down a max of N/2 - 1) , then you will need to do some operations both writes and reads, then you turn the server back on and perform the operations as normal and verify that this does not compromise the veracity of the state of the system.

We also had tested some use cases besides the testes referred early:

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

Security:
* Protection against replay attacks in both direction 
* Protection against Spam attacks in almost every endpoint
* Protection against N faults of the system preserving the correctness of the system
* Protection against unauthorized operations

## Additional Information

### Authors

* **Bernardo Várzea** - *ist1102150*
* **Pedro Trincheiras** - *ist1102151*
* **Filipe Barroso** - *ist1102152*

### Acknowledgments

* Professor Miguel Matos
