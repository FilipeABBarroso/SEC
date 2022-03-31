package tecnico.sec.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;

import io.grpc.Status;

import org.javatuples.Pair;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.grpc.*;
import tecnico.sec.proto.exceptions.BaseException;

import static tecnico.sec.KeyStore.singletons.Sign.checkSignature;
import static tecnico.sec.KeyStore.singletons.Sign.signMessage;

import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class Client {

    private static final String serverPubKey = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAjTu+pLLLj7NKhP9V3KXCV/I6aB19DPspkQpQtSrRO2Akux0Y1aEbShVHmWY/myvHjHSoIeZbMDHMuRQfmMSgqWCenLzHDK1edMLq6Tm/8TvlVRdIE6sDSZZFaG4DmCuR+L8JS93oqdicyKy3EWJRc+Z1yYjq+q/vEKiV8MUHa28LHBxBPFx0U8zC7ajnm3q+rZo14JAzQfzhe2XyAAxG/ewgDurLc5mE1nRP6SXvBG914OR/bKtKG3aklhZDVVodHkvAjJoJK/t42oMHDJGjr+DUlI6Se5J3J6aLgXLHQuJH75vHQ/R793dbR/AKxB/OoURbb1cTclyRE/lKQQgP5TDgtCQ7A/CyYWmPtR7fftVPhH7NkcbuU5rTkMIYSP4s7U27E93WvL1wI954qpCR8t0tStfROIL7WfVbt2zhQT3eqWiav/wXGmqb5ARLmPLYUo8odDJdKKCc5lGwj8NumtPqIKnpH/TtUvWLLSdjFzYR1KBFNsHkeWzh+O713KHu08zH8XcOC5d8JilTnAfSIzUyWJaE0HNuGoeuqyZ2Or3RuZjduVKeMv5NZx7Vvl4vHCjbml5Dcsfzh8vhuPZ06+fTklwNayu+sAkwNmCjTCuUwLnbfAVeHmWjPPXY09Yp03CEKSwKibUlJ4bQBX4rreeHn3cdRBnopm6dn4YCmnMCAwEAAQ==";

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

        int nonce = ServerConnection.getConnection().getNonce(NonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(sourceField)).build()).getNonce();

        byte[] destinationField = destination.getEncoded();
        byte[] signature;

        System.out.println(sourceField);
        System.out.println(destinationField);
        System.out.println(amount);
        System.out.println(nonce);

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
            checkSignature(KeyStore.stringToPublicKey(serverPubKey).getEncoded(), checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList());
            return Pair.with(checkAccountResponse.getBalance(),checkAccountResponse.getTransactionsList());
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

