package tecnico.sec.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;

import tecnico.sec.grpc.*;
import tecnico.sec.proto.exceptions.BaseException;

import static tecnico.sec.KeyStore.singletons.Sign.checkSignature;
import static tecnico.sec.KeyStore.singletons.Sign.signMessage;
import static tecnico.sec.client.Main.pubKeyToString;

import java.security.*;

public class Client {

    private final ServiceGrpc.ServiceBlockingStub stub;
    private final PublicKey serverPubKey;
    private int nonce = 1;

    //TODO handle exceptions, update nounce, get serverpubkey

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
        } catch (BaseException e) {
            System.out.println(e.toResponseException().getMessage());
            return;
        }
        OpenAccountResponse openAccountResponse = null;
        try {
            openAccountResponse = stub.openAccount(OpenAccountRequest.newBuilder()
                    .setPublicKey(pubKeyToString(key))
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return;
        }
        try {
            checkSignature(serverPubKey, openAccountResponse.getSignature().toByteArray());
            System.out.println("Account opened!");
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
    }

    public void send_amount(PublicKey source, PublicKey destination, int amount) {
        String sourceField = pubKeyToString(source);
        String destinationField = pubKeyToString(destination);
        byte[] signature = null;
        try {
            signature = signMessage(sourceField, destinationField, amount, nonce);
        } catch (BaseException e) {
            System.out.println(e.toResponseException().getMessage());
            return;
        }

        SendAmountResponse sendAmountResponse = null;
        try {
            sendAmountResponse = stub.sendAmount(SendAmountRequest.newBuilder()
                    .setPublicKeySource(sourceField)
                    .setPublicKeyDestination(destinationField)
                    .setAmount(amount)
                    .setNonce(nonce)
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return;
        }
        try {
            checkSignature(serverPubKey, sendAmountResponse.getSignature().toByteArray(), sendAmountResponse.getNonce());
            System.out.println("Amount sent!");
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
    }

    public void receive_amount(PublicKey key, int transactionID) {
        String pubKeyField = pubKeyToString(key);
        byte[] signature = null;
        try {
            signature = signMessage(pubKeyField, transactionID);
        } catch (BaseException e) {
            System.out.println(e.toResponseException().getMessage());
            return;
        }

        ReceiveAmountResponse receiveAmountResponse = null;
        try {
            receiveAmountResponse = stub.receiveAmount(ReceiveAmountRequest.newBuilder()
                    .setPublicKey(pubKeyToString(key))
                    .setTransactionID(transactionID)
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return;
        }
        try {
            checkSignature(serverPubKey, receiveAmountResponse.getSignature().toByteArray(), receiveAmountResponse.getNonce());
            System.out.println("Amount received!");
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
    }

    public void check_account(PublicKey key) {
        String pubKeyField = pubKeyToString(key);
        byte[] signature = null;
        try {
            signature = signMessage(pubKeyField);
        } catch (BaseException e) {
            System.out.println(e.toResponseException().getMessage());
            return;
        }

        CheckAccountResponse checkAccountResponse = null;
        try {
            checkAccountResponse = stub.checkAccount(CheckAccountRequest.newBuilder()
                    .setPublicKey(pubKeyToString(key))
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
            System.out.println("Balance: " + checkAccountResponse.getBalance());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return;
        }
        try {
            checkSignature(serverPubKey, checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList()); //TODO falta nonce
            listTransactions(checkAccountResponse.getTransactionsList());
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
    }

    public void audit(PublicKey key) {
        AuditResponse auditResponse = null;
        try {
            auditResponse = stub.audit(AuditRequest.newBuilder()
                    .setPublicKey(pubKeyToString(key))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return;
        }
        try {
            checkSignature(serverPubKey, auditResponse.getSignature().toByteArray(), auditResponse.getTransactionsList());
            listTransactions(auditResponse.getTransactionsList());
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
    }

    private static void listTransactions(ProtocolStringList transactions) {
        transactions.forEach(System.out::println);
    }


}

