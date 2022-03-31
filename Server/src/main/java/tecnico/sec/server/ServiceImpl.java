package tecnico.sec.server;
import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import dbController.Balance;
import dbController.Nonce;
import dbController.Transactions;
import io.grpc.stub.StreamObserver;
import tecnico.sec.KeyStore.singletons.Sign;
import tecnico.sec.grpc.*;
import tecnico.sec.grpc.ServiceGrpc.ServiceImplBase;
import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.BaseException;
import tecnico.sec.proto.exceptions.DataBaseExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

public class ServiceImpl extends ServiceImplBase {

    @Override
    public void getNonce(NonceRequest request, StreamObserver<NonceResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();
        int nonce;
        try {
            nonce = Nonce.getNonce(publicKey);
        } catch (NonceExceptions.NonceNotFoundException | BalanceExceptions.GeneralMYSQLException e) {
            SecureRandom random = new SecureRandom();
            nonce = random.nextInt();
            try {
                Nonce.createNonce(publicKey , nonce);
            } catch (NonceExceptions.FailInsertNonceException | BalanceExceptions.GeneralMYSQLException | NonceExceptions.PublicKeyNotFoundException ex) {
                responseObserver.onError(ex);
            }
        }
        responseObserver.onNext(NonceResponse.newBuilder().setNonce(nonce).build());
        responseObserver.onCompleted();
    }

    @Override
    public void openAccount(OpenAccountRequest request, StreamObserver<OpenAccountResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();
        byte[] signature = request.getSignature().toByteArray();
        try {
            Sign.checkSignature(publicKey, signature, publicKey);
            Balance.openAccount(publicKey);
            byte[] signedPublicKey = Sign.signMessage(publicKey);
            responseObserver.onNext(OpenAccountResponse.newBuilder().setSignature(ByteString.copyFrom(signedPublicKey)).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public static String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return "0x" + hexStringBuffer;
    }

    @Override
    public void sendAmount(SendAmountRequest request, StreamObserver<SendAmountResponse> responseObserver) {
        byte[] publicKeySource = request.getPublicKeySource().toByteArray();
        byte[] publicKeyDestination = request.getPublicKeyDestination().toByteArray();
        int amount = request.getAmount();
        int nonce = request.getNonce();
        byte[] signature = request.getSignature().toByteArray();
        System.out.println(encodeHexString(publicKeySource));
        System.out.println(encodeHexString(publicKeyDestination));
        System.out.println(Arrays.toString(publicKeySource));
        System.out.println(Arrays.toString(publicKeyDestination));
        System.out.println(Arrays.equals(publicKeySource ,  publicKeyDestination));
        try {
            Sign.checkSignature(publicKeySource, signature, publicKeySource , publicKeyDestination , amount , nonce);
            Transactions.addTransaction(publicKeySource , publicKeyDestination , amount);
            byte[] signedIncrementedNonce = Sign.signMessage(publicKeySource , publicKeyDestination , amount , nonce + 1);
            responseObserver.onNext(SendAmountResponse.newBuilder().setSignature(ByteString.copyFrom(signedIncrementedNonce)).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }

    @Override
    public void receiveAmount(ReceiveAmountRequest request, StreamObserver<ReceiveAmountResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();
        int transactionID = request.getTransactionID();
        byte[] signature = request.getSignature().toByteArray();

        try {
            Sign.checkSignature(publicKey, signature, publicKey , transactionID);
            Transactions.changeStatus(transactionID , publicKey);
            byte[] signedFields = Sign.signMessage(publicKey , transactionID);
            responseObserver.onNext(ReceiveAmountResponse.newBuilder().setSignature(ByteString.copyFrom(signedFields)).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }

    @Override
    public void checkAccount(CheckAccountRequest request, StreamObserver<CheckAccountResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();

        try {
            int balance = Balance.getBalance(publicKey);
            List<String> transactions = Transactions.getPendingTransactions(publicKey);

            CheckAccountResponse.Builder builder = CheckAccountResponse.newBuilder();
            int count = 0;
            for(String t : transactions){
                builder.setTransactions(count , t);
                count++;
            }
            builder.setBalance(balance);
            byte[] signedFields = Sign.signMessage(balance , builder.getTransactionsList().toArray());
            builder.setSignature(ByteString.copyFrom(signedFields));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            System.out.println(e);
            responseObserver.onError(e.toResponseException());
        }
    }

    @Override
    public void audit(AuditRequest request, StreamObserver<AuditResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();

        try {
            int balance = Balance.getBalance(publicKey);
            List<String> transactions = Transactions.getTransactions(publicKey);

            byte[] signedFields = Sign.signMessage(balance , transactions);
            AuditResponse.Builder builder = AuditResponse.newBuilder();
            builder.setSignature(ByteString.copyFrom(signedFields));
            int count = 0;
            for(String t : transactions){
                builder.setTransactions(count , t);
                count++;
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }
}