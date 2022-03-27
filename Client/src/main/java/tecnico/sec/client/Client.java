package tecnico.sec.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;

import tecnico.sec.grpc.*;
import tecnico.sec.proto.exceptions.IOExceptions;
import tecnico.sec.proto.exceptions.KeyExceptions;
import tecnico.sec.proto.exceptions.SignatureExceptions;

import static tecnico.sec.KeyStore.singletons.Sign.checkSignature;
import static tecnico.sec.KeyStore.singletons.Sign.signMessage;
import static tecnico.sec.client.Main.pubKeyToString;

import java.security.*;

public class Client {

    private final ServiceGrpc.ServiceBlockingStub stub;
    private final PublicKey serverPubKey;
    private int nonce = 1;

    //TODO handle exceptions, update nounce, get serverpubkey, differentiate exceptions

    public Client(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = ServiceGrpc.newBlockingStub(channel);
        serverPubKey = null; //TODO
    }

    public void open_account(PublicKey key) {
        String pubKeyField = pubKeyToString(key);
        byte[] signature = null;
        try {
            signature = signMessage(pubKeyField);
        } catch (KeyExceptions.InvalidPublicKeyException | SignatureExceptions.CanNotSignException | IOExceptions.IOException e) {
            System.out.println(e.toResponseException().getMessage());
            return;
        }

        try {
            OpenAccountResponse openAccountResponse = stub.openAccount(OpenAccountRequest.newBuilder()
                    .setPublicKey(pubKeyToString(key))
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
            checkSignature(serverPubKey,openAccountResponse.getSignature().toByteArray());
            System.out.println("Account opened!");
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println(status.getCode() + " : " + status.getDescription());
        }
    }

    public void send_amount(PublicKey source, PublicKey destination, int amount) {
        String sourceField = pubKeyToString(source);
        String destinationField = pubKeyToString(destination);
        byte[] signature = null;
        try {
            signature = signMessage(sourceField, destinationField, amount, nonce);
        } catch (KeyExceptions.InvalidPublicKeyException | SignatureExceptions.CanNotSignException | IOExceptions.IOException e) {
            System.out.println(e.toResponseException().getMessage());
            return;
        }

        try {
            SendAmountResponse sendAmountResponse = stub.sendAmount(SendAmountRequest.newBuilder()
                    .setPublicKeySource(sourceField)
                    .setPublicKeyDestination(destinationField)
                    .setAmount(amount)
                    .setNonce(nonce)
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
            checkSignature(serverPubKey, sendAmountResponse.getSignature().toByteArray(),sendAmountResponse.getNonce());
            System.out.println("Amount sent!");
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println(status.getCode() + " : " + status.getDescription());
        }
    }

    public void receive_amount(PublicKey key, int transactionID) {
        String pubKeyField = pubKeyToString(key);
        byte[] signature = null;
        try {
            signature = signMessage(pubKeyField, transactionID);
        } catch (KeyExceptions.InvalidPublicKeyException | SignatureExceptions.CanNotSignException | IOExceptions.IOException e) {
            System.out.println(e.toResponseException().getMessage());
            return;
        }

        try {
            ReceiveAmountResponse receiveAmountResponse = stub.receiveAmount(ReceiveAmountRequest.newBuilder()
                    .setPublicKey(pubKeyToString(key))
                    .setTransactionID(transactionID)
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
            checkSignature(serverPubKey, receiveAmountResponse.getSignature().toByteArray(),receiveAmountResponse.getNonce());
            System.out.println("Amount received!");
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println(status.getCode() + " : " + status.getDescription());
        }
    }

    public void check_account(PublicKey key) {
        //TODO

        String pubKeyField = pubKeyToString(key);
        byte[] signature = null;
        try {
            signature = signMessage(pubKeyField);
        } catch (KeyExceptions.InvalidPublicKeyException | SignatureExceptions.CanNotSignException | IOExceptions.IOException e) {
            System.out.println(e.toResponseException().getMessage());
            return;
        }
        try {
            CheckAccountResponse checkAccountResponse = stub.checkAccount(CheckAccountRequest.newBuilder()
                    .setPublicKey(pubKeyToString(key))
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
            System.out.println("Balance: " + checkAccountResponse.getBalance());
            checkSignature(serverPubKey, checkAccountResponse.getSignature().toByteArray(),checkAccountResponse.getBalance(),checkAccountResponse.getTransactionsList()); //TODO falta nonce
            listTransactions(checkAccountResponse.getTransactionsList());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println(status.getCode() + " : " + status.getDescription());
        }
    }

    public void audit(PublicKey key) {
        //TODO

        try {
            AuditResponse auditResponse = stub.audit(AuditRequest.newBuilder()
                    .setPublicKey(pubKeyToString(key))
                    .build());
            checkSignature(serverPubKey, auditResponse.getSignature().toByteArray(),auditResponse.getTransactionsList());
            listTransactions(auditResponse.getTransactionsList());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println(status.getCode() + " : " + status.getDescription());
        }
    }

    private static void listTransactions(ProtocolStringList transactions){
        transactions.forEach(System.out::println);
    }


}

