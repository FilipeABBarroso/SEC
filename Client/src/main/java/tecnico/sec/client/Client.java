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

    private static final ServiceGrpc.ServiceBlockingStub stub = ServiceGrpc.newBlockingStub(ManagedChannelBuilder
            .forAddress("localhost",8080)
            .usePlaintext()
            .build());
    private static final byte[] serverPubKey = null;

    public static void open_account(PublicKey key) {
        byte[] pubKeyField = key.getEncoded();
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
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return;
        }
        try {
            checkSignature(serverPubKey, openAccountResponse.getSignature().toByteArray(),pubKeyField);
            System.out.println("Account opened!");
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
    }

    public static void send_amount(PublicKey source, PublicKey destination, int amount) {

        int nonce = stub.getNonce(NonceRequest.newBuilder().build()).getNonce();

        byte[] sourceField = source.getEncoded();
        byte[] destinationField = destination.getEncoded();
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
                    .setPublicKeySource(ByteString.copyFrom(sourceField))
                    .setPublicKeyDestination(ByteString.copyFrom(destinationField))
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
            checkSignature(serverPubKey, sendAmountResponse.getSignature().toByteArray(), sourceField,destinationField,amount,nonce+1);
            System.out.println("Amount sent!");
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
    }

    public static void receive_amount(PublicKey key, int transactionID) {
        byte[] pubKeyField = key.getEncoded();
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
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setTransactionID(transactionID)
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return;
        }
        try {
            checkSignature(serverPubKey, receiveAmountResponse.getSignature().toByteArray(), pubKeyField,transactionID);
            System.out.println("Amount received!");
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
    }

    public static void check_account(PublicKey key) {
        CheckAccountResponse checkAccountResponse = null;
        try {
            checkAccountResponse = stub.checkAccount(CheckAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(key.getEncoded()))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return;
        }
        try {
            checkSignature(serverPubKey, checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList());
            System.out.println("Balance: " + checkAccountResponse.getBalance());
            listTransactions(checkAccountResponse.getTransactionsList());
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
    }

    public static void audit(PublicKey key) {
        AuditResponse auditResponse = null;
        try {
            auditResponse = stub.audit(AuditRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(key.getEncoded()))
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

