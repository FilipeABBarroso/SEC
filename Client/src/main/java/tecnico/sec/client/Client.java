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

    private static final String serverPubKey = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAvRrRa/d8h7SjT+VVMxKe2Tg3dhakRkMTKWYlJGYYvgTVLftM4POHs31lG9rSP/HZFN+86OHRZGqbzSjjrFjmzxIFmdftIVqF8szrH3HSdgR2ib6g8nPW8Uc1lOsl53q1MGjyVofYcU3ysD9S3wfHRdIOyrOAEXRU+/b6DELU8lY2ECuXOf3JPfvcDzs4nv+X84tpn1sHRh/H761mT5PrjjWxNbzKIot4/IBllu4wGkss5jOYNc5n3ethXMNoxLR2xVSf12p6hCd+WHN5x1nPLG5yMz0J3GIeD1cex85BVeRdYhD2P1Vd7hifnwLO2KZS8nbPXlTZkNCHXheMpOrstmW7aylDQJLbvvrfg/mnqRmLo/97EG7whuABHoeJD3wlmjOv2GbFBrj3MbFZKYOMbbouMajU2Il3M+5UPfrqn66mnFP0VW3qS1H3hZglJs/HOBs+/cYFXtj+oRIJ93FNWm5pLdhoBtxAkdF3Nd2xNipFVLZTpItAf3jWoiUOKdWHK5r3tXFlu0VJJ4ViCSqNOvLkCvQSuQJkkPihoHp/ZSnH6JRhkoIdihru2RRL1S7AGe6Ub7+766rUBuHRQ8UWSn/dp1AVVhuyTAHJR4JLWAZaeoMAiWlz3rtKL0h1y89Los6R6t9WicQLU3qi+1vXVC0bcLzFAOOHyAU+2aGEaecCAwEAAQ==";

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
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), openAccountResponse.getSignature().toByteArray(),pubKeyField);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
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
        } catch (Exception e) {
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
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
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
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), receiveAmountResponse.getSignature().toByteArray(), pubKeyField,transactionID);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
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
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList().toArray());
            return Pair.with(checkAccountResponse.getBalance(),checkAccountResponse.getTransactionsList());
        } catch (BaseException e) {
            e.printStackTrace();
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
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
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), auditResponse.getSignature().toByteArray(), auditResponse.getTransactionsList());
            return auditResponse.getTransactionsList();
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("CLIENT ERROR : " + status.getCode() + " : " + status.getDescription());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }


}

