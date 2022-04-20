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

    public static boolean open_account() {
        try {
            PublicKey publicKey = KeyStore.getPublicKey();
            byte[] pubKeyField = publicKey.getEncoded();
            byte[] signature = signMessage(pubKeyField);
            for(Pair<ServiceGrpc.ServiceBlockingStub, PublicKey> server : ServerConnection.getConnection()) {
                OpenAccountResponse openAccountResponse = server.getValue0().openAccount(OpenAccountRequest.newBuilder()
                        .setPublicKey(ByteString.copyFrom(pubKeyField))
                        .setSignature(ByteString.copyFrom(signature))
                        .build());
                openAccountCheckResponse(server.getValue1().getEncoded(), openAccountResponse.getSignature().toByteArray(), pubKeyField);
            }
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
            for(Pair<ServiceGrpc.ServiceBlockingStub, PublicKey> server : ServerConnection.getConnection()) {
                NonceResponse nonceResponse = server.getValue0().getNonce(NonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(sourceField)).build());
                int nonce = nonceResponse.getNonce();
                byte[] destinationField = destination.getEncoded();
                byte[] signature = signMessage(sourceField, destinationField, amount, nonce);
                SendAmountResponse sendAmountResponse = server.getValue0().sendAmount(SendAmountRequest.newBuilder()
                        .setPublicKeySource(ByteString.copyFrom(sourceField))
                        .setPublicKeyDestination(ByteString.copyFrom(destinationField))
                        .setAmount(amount)
                        .setNonce(nonce)
                        .setSignature(ByteString.copyFrom(signature))
                        .build());
                sendAmountCheckResponse(server.getValue1().getEncoded(), sendAmountResponse.getSignature().toByteArray(), sourceField, destinationField, amount, nonce + 1);
            }
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
            for(Pair<ServiceGrpc.ServiceBlockingStub, PublicKey> server : ServerConnection.getConnection()) {
                ReceiveAmountResponse receiveAmountResponse = server.getValue0().receiveAmount(ReceiveAmountRequest.newBuilder()
                        .setPublicKey(ByteString.copyFrom(pubKeyField))
                        .setTransactionID(transactionID)
                        .setSignature(ByteString.copyFrom(signature))
                        .build());
                receiveAmountCheckResponse(server.getValue1().getEncoded(), receiveAmountResponse.getSignature().toByteArray(), pubKeyField, transactionID);
            }
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
            CheckAccountResponse checkAccountResponse = null;
            for (Pair<ServiceGrpc.ServiceBlockingStub, PublicKey> server : ServerConnection.getConnection()) {
                checkAccountResponse = server.getValue0().checkAccount(CheckAccountRequest.newBuilder()
                        .setPublicKey(ByteString.copyFrom(pubKeyField))
                        .build());
                checkAccountCheckResponse(server.getValue1().getEncoded(), checkAccountResponse.getSignature().toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList().toArray());
            }
            return Pair.with(checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList());
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
            ProtocolStringList list = null;
            for (Pair<ServiceGrpc.ServiceBlockingStub, PublicKey> server : ServerConnection.getConnection()) {
                AuditResponse auditResponse = server.getValue0().audit(AuditRequest.newBuilder()
                        .setPublicKey(ByteString.copyFrom(key.getEncoded()))
                        .build());
                auditCheckResponse(server.getValue1().getEncoded(), auditResponse.getSignature().toByteArray(), auditResponse.getTransactionsList().toArray());
                auditResponse.getTransactionsList().forEach( (x) -> {if(!list.contains(x)) list.add(x);});
            }
            return list;
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

