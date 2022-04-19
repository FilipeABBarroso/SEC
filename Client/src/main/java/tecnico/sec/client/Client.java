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


public class Client {

    public static PublicKey serverPubKey;

    public static boolean open_account() {
        try {
            PublicKey publicKey = KeyStore.getPublicKey();
            byte[] pubKeyField = publicKey.getEncoded();
            byte[] signature = signMessage(pubKeyField);
            OpenAccountResponse openAccountResponse = ServerConnection.getConnection().openAccount(OpenAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
            openAccountCheckResponse(serverPubKey.getEncoded(), openAccountResponse.getSignature().toByteArray(),pubKeyField);
            return true;
        } catch (StatusRuntimeException | BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void openAccountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] accountPublicKey) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, accountPublicKey);
    }

    public static boolean send_amount(PublicKey destination, int amount) {
        try {
            PublicKey source = KeyStore.getPublicKey();
            byte[] sourceField = source.getEncoded();
            NonceResponse nonceResponse = ServerConnection.getConnection().getNonce(NonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(sourceField)).build());
            int nonce = nonceResponse.getNonce();
            byte[] destinationField = destination.getEncoded();
            byte[] signature = signMessage(sourceField, destinationField, amount, nonce);
            SendAmountResponse sendAmountResponse = ServerConnection.getConnection().sendAmount(SendAmountRequest.newBuilder()
                    .setPublicKeySource(ByteString.copyFrom(sourceField))
                    .setPublicKeyDestination(ByteString.copyFrom(destinationField))
                    .setAmount(amount)
                    .setNonce(nonce)
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
            sendAmountCheckResponse(serverPubKey.getEncoded(), sendAmountResponse.getSignature().toByteArray(), sourceField,destinationField,amount,nonce+1);
            return true;
        } catch (StatusRuntimeException | BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void sendAmountCheckResponse(byte[] serverPublicKey, byte[] signature,byte[] source, byte[] destination,int amount,int nonce) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, source,destination,amount,nonce);
    }

    public static boolean receive_amount(int transactionID) {
        try {
            PublicKey pubKey = KeyStore.getPublicKey();
            byte[] pubKeyField = pubKey.getEncoded();
            byte[] signature = signMessage(pubKeyField, transactionID);
            ReceiveAmountResponse receiveAmountResponse = ServerConnection.getConnection().receiveAmount(ReceiveAmountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setTransactionID(transactionID)
                    .setSignature(ByteString.copyFrom(signature))
                    .build());
            receiveAmountCheckResponse(serverPubKey.getEncoded(), receiveAmountResponse.getSignature().toByteArray(), pubKeyField,transactionID);
            return true;
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void receiveAmountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] accountPublicKey, int transactionID) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, accountPublicKey,transactionID);
    }

    public static Pair<Integer,ProtocolStringList> check_account() {
        try {
            PublicKey pubKey = KeyStore.getPublicKey();
            byte[] pubKeyField = pubKey.getEncoded();
            CheckAccountResponse checkAccountResponse = ServerConnection.getConnection().checkAccount(CheckAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .build());
            checkAccountCheckResponse(serverPubKey.getEncoded(), checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList().toArray());
            return Pair.with(checkAccountResponse.getBalance(),checkAccountResponse.getTransactionsList());
        } catch (StatusRuntimeException | BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static void checkAccountCheckResponse(byte[] serverPublicKey, byte[] signature, int balance, Object[] transactionList) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, balance, transactionList);
    }

    public static ProtocolStringList audit(PublicKey key) {
        try {
            AuditResponse auditResponse = ServerConnection.getConnection().audit(AuditRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(key.getEncoded()))
                    .build());
            auditCheckResponse(serverPubKey.getEncoded(), auditResponse.getSignature().toByteArray(), auditResponse.getTransactionsList().toArray());
            return auditResponse.getTransactionsList();
        } catch (BaseException | StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static void auditCheckResponse(byte[] serverPublicKey, byte[] signature, Object[] transactionList) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, transactionList);
    }

}

