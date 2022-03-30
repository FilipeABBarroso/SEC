package tecnico.sec.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;

import io.grpc.Status;

import org.javatuples.Pair;
import tecnico.sec.grpc.*;
import tecnico.sec.proto.exceptions.BaseException;

import static tecnico.sec.KeyStore.singletons.Sign.checkSignature;
import static tecnico.sec.KeyStore.singletons.Sign.signMessage;

import java.security.*;

public class Client {

    private static final byte[] serverPubKey = null;

    public static boolean open_account(PublicKey key) {
        byte[] pubKeyField = key.getEncoded();
        byte[] signature;
        try {
            signature = signMessage(pubKeyField);
        } catch (BaseException e) {
            System.out.println(e.toResponseException().getMessage());
            return false;
        }
        OpenAccountResponse openAccountResponse;
        try {
            openAccountResponse = ServerConnection.getConnection().openAccount(OpenAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return false;
        }
        try {
            checkSignature(serverPubKey, openAccountResponse.getSignature().toByteArray(),pubKeyField);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static boolean send_amount(PublicKey source, PublicKey destination, int amount) {

        int nonce = ServerConnection.getConnection().getNonce(NonceRequest.newBuilder().build()).getNonce();

        byte[] sourceField = source.getEncoded();
        byte[] destinationField = destination.getEncoded();
        byte[] signature;
        try {
            signature = signMessage(sourceField, destinationField, amount, nonce);
        } catch (BaseException e) {
            System.out.println(e.toResponseException().getMessage());
            return false;
        }

        SendAmountResponse sendAmountResponse;
        try {
            sendAmountResponse = ServerConnection.getConnection().sendAmount(SendAmountRequest.newBuilder()
                    .setPublicKeySource(ByteString.copyFrom(sourceField))
                    .setPublicKeyDestination(ByteString.copyFrom(destinationField))
                    .setAmount(amount)
                    .setNonce(nonce)
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return false;
        }
        try {
            checkSignature(serverPubKey, sendAmountResponse.getSignature().toByteArray(), sourceField,destinationField,amount,nonce+1);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static boolean receive_amount(PublicKey key, int transactionID) {
        byte[] pubKeyField = key.getEncoded();
        byte[] signature;
        try {
            signature = signMessage(pubKeyField, transactionID);
        } catch (BaseException e) {
            System.out.println(e.toResponseException().getMessage());
            return false;
        }

        ReceiveAmountResponse receiveAmountResponse;
        try {
            receiveAmountResponse = ServerConnection.getConnection().receiveAmount(ReceiveAmountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setTransactionID(transactionID)
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return false;
        }
        try {
            checkSignature(serverPubKey, receiveAmountResponse.getSignature().toByteArray(), pubKeyField,transactionID);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static Pair<Integer,ProtocolStringList> check_account(PublicKey key) {
        CheckAccountResponse checkAccountResponse;
        try {
            checkAccountResponse = ServerConnection.getConnection().checkAccount(CheckAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(key.getEncoded()))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return null;
        }
        try {
            checkSignature(serverPubKey, checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList());
            return Pair.with(checkAccountResponse.getBalance(),checkAccountResponse.getTransactionsList());
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static ProtocolStringList audit(PublicKey key) {
        AuditResponse auditResponse;
        try {
            auditResponse = ServerConnection.getConnection().audit(AuditRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(key.getEncoded()))
                    .build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return null;
        }
        try {
            checkSignature(serverPubKey, auditResponse.getSignature().toByteArray(), auditResponse.getTransactionsList());
            return auditResponse.getTransactionsList();
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }


}

