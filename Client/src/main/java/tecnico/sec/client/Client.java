package tecnico.sec.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;

import io.grpc.Status;

import io.grpc.StatusRuntimeException;
import org.javatuples.Pair;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.grpc.*;
import tecnico.sec.proto.exceptions.BaseException;

import static tecnico.sec.KeyStore.singletons.Sign.checkSignature;
import static tecnico.sec.KeyStore.singletons.Sign.signMessage;

import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class Client {

    private static final String serverPubKey = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAymYXK6sHUrTgTh6LWHsTSqI1wG6irhWaA7+Ub3mlqmUpLYsssy1+FmSG4TCRZ3N9egxEclJ8eJIBjFpTJYfIVmHZZGE8uwkzpPp5g3ji0DntWTrsanT5Fuhhp7DM4HOLhqqSPazsz8cGvoMVKs4Y8fE0wUN9J2yas5WhRDvBrbGRzlwzvep13Iti3lhyd/Q/ci3PNcQJUHwNiVikPzksHWUPc97Q7GZyGwKXT9EzpAVx1P0OYS+D15oWwBvNO3Q5O2x0ZW/ntjXwJ5ELTHHasdxZT6QvMEjY3k3aP2wXVY7Q+Y8JnZfT6mA98BIO3B6EvGkBUa2XjKdestbb0PFdvxLW1FeqkgblBKmJU+xQyMFbUvB7ghL5NXqI9LRc6TqQqx+kz0B2EzTUqIIQ/gugBP9NdmSYFUG0RQeYu6MIZU+xANqX6geeNei2Vl+RVa9YbfiO3WYAPwY7juFJd00J8SSEPdHnRHOrNT0VeyGZYXsiR3iXDuE6p9yYQ2L3bVZDy6e1VuKotVpwYsJqwgFW29sYZ+M6T6pNKcU2SHUSMCReF/LZ5BghLW4SwOaifL33DPg8+KjYz2wCffN3NIr/W+PRFIIM/hckxPnNlVoXEJiaE9DI0vcq35O5iGIubLlfjOC79R7PdBlQScNXlLyGeV5BCJw9ZYJ1sIJjn5W+rucCAwEAAQ==";

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
        } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return false;
        }
        try {
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), openAccountResponse.getSignature().toByteArray(),pubKeyField);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static boolean send_amount(PublicKey source, PublicKey destination, int amount) {

        byte[] sourceField = source.getEncoded();
        NonceResponse nonceResponse = null;
        try {
            nonceResponse = ServerConnection.getConnection().getNonce(NonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(sourceField)).build());
        } catch (StatusRuntimeException e){
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        int nonce = nonceResponse.getNonce();

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
        } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return false;
        }
        try {
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), sendAmountResponse.getSignature().toByteArray(), sourceField,destinationField,amount,nonce+1);
            return true;
        } catch (BaseException e) {
            System.out.println(e);
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
        } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return false;
        }
        try {
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), receiveAmountResponse.getSignature().toByteArray(), pubKeyField,transactionID);
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
        } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return null;
        }
        try {
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList().toArray());
            return Pair.with(checkAccountResponse.getBalance(),checkAccountResponse.getTransactionsList());
        } catch (BaseException e) {
            e.printStackTrace();
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
        } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("SERVER ERROR : " + status.getCode() + " : " + status.getDescription());
            return null;
        }
        try {
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), auditResponse.getSignature().toByteArray(), auditResponse.getTransactionsList().toArray());
            return auditResponse.getTransactionsList();
        } catch (BaseException e) {
            System.out.println(e);
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

}

