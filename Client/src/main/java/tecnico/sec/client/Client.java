package tecnico.sec.client;

import com.google.protobuf.ByteString;
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
import java.util.List;


public class Client {

    //todo anti spam mechanism

    public static boolean open_account() {
        try {
            PublicKey publicKey = KeyStore.getPublicKey();
            byte[] pubKeyField = publicKey.getEncoded();
            byte[] signature = signMessage(pubKeyField);

            OpenAccountRequest request = OpenAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setSignature(ByteString.copyFrom(signature))
                    .build();

            for (ServerInfo server : ServerConnection.getConnection()) {
                OpenAccountResponse openAccountResponse = server.getStub().openAccount(request);
                try {
                    openAccountCheckResponseSignatures(openAccountResponse, pubKeyField);
                } catch (BaseException ignored) {
                    continue;
                }
                return true;
            }
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void openAccountCheckResponseSignatures(OpenAccountResponse openAccountResponse, byte[] pubKeyField) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException, KeyExceptions.GeneralKeyStoreErrorException {
        for (int i = 0; i < openAccountResponse.getServersList().size(); i++) {
            PublicKey serverPublicKey = KeyStore.stringToPublicKey(openAccountResponse.getServersList().get(i));
            if (ServerConnection.existsPublicKey(serverPublicKey)) {
                openAccountCheckResponse(serverPublicKey.getEncoded(), openAccountResponse.getSignaturesList().get(i).toByteArray(), pubKeyField);
            } else {
                throw new IOExceptions.IOException();
            }
        }
    }

    public static void openAccountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] accountPublicKey) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, accountPublicKey);
    }

    public static boolean send_amount(PublicKey destination, int amount) { //todo write
        try {
            PublicKey source = KeyStore.getPublicKey();
            byte[] sourceField = source.getEncoded();
            byte[] destinationField = destination.getEncoded();

            NonceRequest nonceRequest = NonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(sourceField)).build();

            for (ServerInfo server : ServerConnection.getConnection()) {
                NonceResponse nonceResponse = server.getStub().getNonce(nonceRequest);
                int nonce = nonceResponse.getNonce();

                byte[] signature = signMessage(sourceField, destinationField, amount, nonce);

                SendAmountRequest sendAmountRequest = SendAmountRequest.newBuilder()
                        .setPublicKeySource(ByteString.copyFrom(sourceField))
                        .setPublicKeyDestination(ByteString.copyFrom(destinationField))
                        .setAmount(amount)
                        .setNonce(nonce)
                        .setSignature(ByteString.copyFrom(signature))
                        .build();

                SendAmountResponse sendAmountResponse = server.getStub().sendAmount(sendAmountRequest);

                try {
                    sendAmountCheckResponseSignatures(sendAmountResponse, sourceField, destinationField, amount, nonce + 1);
                } catch (BaseException ignored) {
                    continue;
                }
                return true;
            }
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void sendAmountCheckResponseSignatures(SendAmountResponse sendAmountResponse, byte[] sourceField, byte[] destinationField, int amount, int nonce) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException, KeyExceptions.GeneralKeyStoreErrorException {
        for (int i = 0; i < sendAmountResponse.getServersList().size(); i++) {
            PublicKey serverPublicKey = KeyStore.stringToPublicKey(sendAmountResponse.getServersList().get(i));
            if (ServerConnection.existsPublicKey(serverPublicKey)) {
                sendAmountCheckResponse(serverPublicKey.getEncoded(), sendAmountResponse.getSignaturesList().get(i).toByteArray(), sourceField, destinationField, amount, nonce);
            } else {
                throw new IOExceptions.IOException();
            }
        }
    }

    public static void sendAmountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] source, byte[] destination, int amount, int nonce) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, source, destination, amount, nonce);
    }

    public static boolean receive_amount(int transactionID) { //todo write
        try {
            PublicKey pubKey = KeyStore.getPublicKey();
            byte[] pubKeyField = pubKey.getEncoded();
            byte[] signature = signMessage(pubKeyField, transactionID);

            ReceiveAmountRequest request = ReceiveAmountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setTransactionID(transactionID)
                    .setSignature(ByteString.copyFrom(signature))
                    .build();

            for (ServerInfo server : ServerConnection.getConnection()) {
                ReceiveAmountResponse receiveAmountResponse = server.getStub().receiveAmount(request);

                try {
                    receiveAmountCheckResponseSignatures(receiveAmountResponse, pubKeyField, transactionID);
                } catch (BaseException ignored) {
                    continue;
                }
                return true;
            }
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void receiveAmountCheckResponseSignatures(ReceiveAmountResponse receiveAmountResponse, byte[] accountPublicKey, int transactionID) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException, KeyExceptions.GeneralKeyStoreErrorException {
        for (int i = 0; i < receiveAmountResponse.getServersList().size(); i++) {
            PublicKey serverPublicKey = KeyStore.stringToPublicKey(receiveAmountResponse.getServersList().get(i));
            if (ServerConnection.existsPublicKey(serverPublicKey)) {
                receiveAmountCheckResponse(serverPublicKey.getEncoded(), receiveAmountResponse.getSignaturesList().get(i).toByteArray(), accountPublicKey, transactionID);
            } else {
                throw new IOExceptions.IOException();
            }
        }
    }

    public static void receiveAmountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] accountPublicKey, int transactionID) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, accountPublicKey, transactionID);
    }

    public static Pair<Integer, List<String>> check_account() {
        try {
            PublicKey pubKey = KeyStore.getPublicKey();
            byte[] pubKeyField = pubKey.getEncoded();

            CheckAccountRequest request = CheckAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .build();

            for (ServerInfo server : ServerConnection.getConnection()) {
                CheckAccountResponse checkAccountResponse = server.getStub().checkAccount(request);
                Pair<Integer, List<String>> result = Pair.with(checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList());
                try {
                    checkAccountCheckResponseSignatures(checkAccountResponse);
                } catch (BaseException ignored) {
                    continue;
                }
                return result;
            }
        } catch (BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static void checkAccountCheckResponseSignatures(CheckAccountResponse checkAccountResponse) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException, KeyExceptions.GeneralKeyStoreErrorException {
        for (int i = 0; i < checkAccountResponse.getServersList().size(); i++) {
            PublicKey serverPublicKey = KeyStore.stringToPublicKey(checkAccountResponse.getServersList().get(i));
            if (ServerConnection.existsPublicKey(serverPublicKey)) {
                checkAccountCheckResponse(serverPublicKey.getEncoded(), checkAccountResponse.getSignaturesList().get(i).toByteArray(), checkAccountResponse.getBalance(), checkAccountResponse.getTransactionsList().toArray());
            } else {
                throw new IOExceptions.IOException();
            }
        }
    }

    public static void checkAccountCheckResponse(byte[] serverPublicKey, byte[] signature, int balance, Object[] transactionList) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, balance, transactionList);
    }

    public static List<String> audit(PublicKey key) {
        try {
            AuditRequest request = AuditRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(key.getEncoded()))
                    .build();

            for (ServerInfo server : ServerConnection.getConnection()) {
                AuditResponse auditResponse = server.getStub().audit(request);
                List<String> result = auditResponse.getTransactionsList();
                try {
                    auditCheckResponseSignatures(auditResponse);
                } catch (BaseException ignored) {
                    continue;
                }
                return result;
            }
        } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static void auditCheckResponseSignatures(AuditResponse auditResponse) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException, KeyExceptions.GeneralKeyStoreErrorException {
        for (int i = 0; i < auditResponse.getServersList().size(); i++) {
            PublicKey serverPublicKey = KeyStore.stringToPublicKey(auditResponse.getServersList().get(i));
            if (ServerConnection.existsPublicKey(serverPublicKey)) {
                auditCheckResponse(serverPublicKey.getEncoded(), auditResponse.getSignaturesList().get(i).toByteArray(), auditResponse.getTransactionsList().toArray());
            } else {
                throw new IOExceptions.IOException();
            }
        }
    }

    public static void auditCheckResponse(byte[] serverPublicKey, byte[] signature, Object[] transactionList) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, transactionList);
    }

}

