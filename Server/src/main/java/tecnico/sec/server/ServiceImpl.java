package tecnico.sec.server;
import com.google.protobuf.ByteString;
import dbController.Balance;
import dbController.Nonce;
import dbController.Transactions;
import io.grpc.stub.StreamObserver;
import tecnico.sec.KeyStore.singletons.Sign;
import tecnico.sec.grpc.*;
import tecnico.sec.grpc.ServiceGrpc.ServiceImplBase;
import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.BaseException;
import tecnico.sec.proto.exceptions.NonceExceptions;

import java.security.SecureRandom;
import java.util.List;

public class ServiceImpl extends ServiceImplBase {

    @Override
    public void getNonce(NonceRequest request, StreamObserver<NonceResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();

        try {
            int nonce = Nonce.getNonce(publicKey);
            responseObserver.onNext(NonceResponse.newBuilder().setNonce(nonce).build());
            responseObserver.onCompleted();
        } catch (NonceExceptions.NonceNotFoundException | BalanceExceptions.GeneralMYSQLException e) {
            SecureRandom random = new SecureRandom();
            int nonce = random.nextInt();
            responseObserver.onNext(NonceResponse.newBuilder().setNonce(nonce).build());
            responseObserver.onCompleted();
            try {
                Nonce.createNonce(publicKey , nonce);
            } catch (BaseException ex) {
                responseObserver.onError(ex);
            }
        }
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
            System.out.println(e);
            responseObserver.onError(e.toResponseException());
        }
    }

    @Override
    public void sendAmount(SendAmountRequest request, StreamObserver<SendAmountResponse> responseObserver) {
        byte[] publicKeySource = request.getPublicKeySource().toByteArray();
        byte[] publicKeyDestination = request.getPublicKeyDestination().toByteArray();
        int amount = request.getAmount();
        int nonce = request.getNonce();
        byte[] signature = request.getSignature().toByteArray();

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
            builder.addAllTransactions(transactions);
            builder.setBalance(balance);
            byte[] signedFields = Sign.signMessage(balance , builder.getTransactionsList().toArray());
            builder.setSignature(ByteString.copyFrom(signedFields));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }

    @Override
    public void audit(AuditRequest request, StreamObserver<AuditResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();

        try {
            List<String> transactions = Transactions.getTransactions(publicKey);
            AuditResponse.Builder builder = AuditResponse.newBuilder();
            builder.addAllTransactions(transactions);
            byte[] signedFields = Sign.signMessage(builder.getTransactionsList().toArray());
            builder.setSignature(ByteString.copyFrom(signedFields));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }
}