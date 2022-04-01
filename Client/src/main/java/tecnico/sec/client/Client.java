package tecnico.sec.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;

import io.grpc.Status;

import io.grpc.StatusRuntimeException;
import org.javatuples.Pair;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.grpc.*;
import tecnico.sec.proto.exceptions.BaseException;
import tecnico.sec.proto.exceptions.IOExceptions;
import tecnico.sec.proto.exceptions.KeyExceptions;
import tecnico.sec.proto.exceptions.SignatureExceptions;

import static tecnico.sec.KeyStore.singletons.Sign.checkSignature;
import static tecnico.sec.KeyStore.singletons.Sign.signMessage;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class Client {

    private static final String serverPubKey = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAxuJj/x4gUFuHguIRySzf9NY2OI4mPQgMaJ9/A7YPd/XTeLn0bxxReM7iC9ymkZgijZlEN1BPqVfRrLK0waYqmAzwNcr+4GZd2PnAdhnuf7cRAkILsoESJZHxyyNUYY8aOVaAxUOFiIc5nqEsdQ50Kia06vLb3Mg1h4zvmp3pIsxJqsjasOl8fSRe4LRF1SVtdlCsMW9DgyuPZVeS8KASbnzbkpxrk7fZk+ddU7eoIh2nXeqzGTVKC3TXq1yWYCaSDB4/F06ibShSQfAoMPQ5ncu9t3ox5e1jX7uEqCjYtskPr8k8zudgyndgQq6iHLLh6SYMJhhgGHMKDT0hB6mRpjA1pk7v4UR8v5sIQIPVB0VZDU05LlPtrl+rz1aauTfHCoiZyCJlO8KmX8g2zwCB1eUgAjeLnwLfPrPwwCjJkCQuYsDUsqvBSon8sPILsoWOJkyNK52S8ObR3iNdu7LLF39kWtsNaVgwmX+Qwul4JR33pL8e4cUiGMzLS1kmXJ5kIgQXN5d5PInDibyHlvg7SUFQqVZFcjiaNyCHDwYpz5P6o9R/5t+M8NMUMCskqMXJvElNoabnzB02szpB3Gp/Gl2A+41xSE+81c8h7dYz1Egl5tU2FEKDm2IQCfZ0TlgnYv1TY59aAt3YZym6xfyQ7Vt8sSMEhQClJg2Wu7Q3RS8CAwEAAQ==";

    public static boolean open_account(PublicKey key) {
        byte[] pubKeyField = key.getEncoded();
        System.out.println(Arrays.toString(pubKeyField));
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
            //checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), openAccountResponse.getSignature().toByteArray(),pubKeyField);
            openAccountCheckResponse(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), openAccountResponse.getSignature().toByteArray(),pubKeyField);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void openAccountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] accountPublicKey) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, accountPublicKey);
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
            //checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), sendAmountResponse.getSignature().toByteArray(), sourceField,destinationField,amount,nonce+1);
            sendAmountCheckResponse(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), sendAmountResponse.getSignature().toByteArray(), sourceField,destinationField,amount,nonce+1);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void sendAmountCheckResponse(byte[] serverPublicKey, byte[] signature,byte[] source, byte[] destination,int amount,int nonce) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, source,destination,amount,nonce);
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
            //checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), receiveAmountResponse.getSignature().toByteArray(), pubKeyField,transactionID);
            receiveAmountCheckResponse(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), receiveAmountResponse.getSignature().toByteArray(), pubKeyField,transactionID);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void receiveAmountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] accountPublicKey, int transactionID) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, accountPublicKey,transactionID);
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
            //checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList().toArray());
            checkAccountCheckResponse(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList().toArray());
            return Pair.with(checkAccountResponse.getBalance(),checkAccountResponse.getTransactionsList());
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static void checkAccountCheckResponse(byte[] serverPublicKey, byte[] signature, int balance, Object[] transactionList) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, balance, transactionList);
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
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

}

