package tecnico.sec.server;
import com.google.protobuf.ByteString;
import dbController.Balance;
import dbController.Nonce;
import dbController.Transactions;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.javatuples.Pair;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.KeyStore.singletons.Sign;
import tecnico.sec.grpc.*;
import tecnico.sec.grpc.Error;
import tecnico.sec.grpc.ServiceGrpc.ServiceImplBase;
import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.BaseException;
import tecnico.sec.proto.exceptions.NonceExceptions;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class ServiceImpl extends ServiceImplBase {

    @Override
    public void getNonce(NonceRequest request, StreamObserver<NonceResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();

        try {
            int nonce = Nonce.getNonce(publicKey);
            tecnico.sec.grpc.Nonce nonceField = tecnico.sec.grpc.Nonce.newBuilder().setNonce(nonce).build();
            responseObserver.onNext(NonceResponse.newBuilder().setNonce(nonceField).build());
            responseObserver.onCompleted();
        } catch (NonceExceptions.NonceNotFoundException | BalanceExceptions.GeneralMYSQLException e) {
            SecureRandom random = new SecureRandom();
            int nonce = random.nextInt();
            tecnico.sec.grpc.Nonce nonceField = tecnico.sec.grpc.Nonce.newBuilder().setNonce(nonce).build();
            try {
                Nonce.createNonce(publicKey , nonce);
                responseObserver.onNext(NonceResponse.newBuilder().setNonce(nonceField).build());
                responseObserver.onCompleted();
            } catch (BaseException ex) {
                try {
                    //responseObserver.onError(ex);
                    String errorMessage = ex.getMessage();
                    byte[] signedPublicKey = Sign.signMessage(publicKey,errorMessage);
                    Error error = Error.newBuilder().setMessage(ex.getMessage()).setSignature(ByteString.copyFrom(signedPublicKey)).build();
                    responseObserver.onNext(NonceResponse.newBuilder().setError(error).build());
                    responseObserver.onCompleted();
                } catch (BaseException ignored){
                }
            }
        }
    }

    @Override
    public void openAccount(OpenAccountRequest request, StreamObserver<OpenAccountResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();
        byte[] signature = request.getSignature().toByteArray();

        try {
            Sign.checkSignature(publicKey, signature, publicKey);
            System.out.println(1);
            Balance.openAccount(publicKey);
            System.out.println(2);
            byte[] signedPublicKey = Sign.signMessage(publicKey);
            OpenAccount openAccount = OpenAccount.newBuilder().setSignature(ByteString.copyFrom(signedPublicKey)).build();
            responseObserver.onNext(OpenAccountResponse.newBuilder().setOpenAccount(openAccount).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            try {
                String errorMessage = e.toResponseException().getMessage();
                byte[] signedPublicKey = Sign.signMessage(publicKey,errorMessage);
                Error error = Error.newBuilder().setMessage(errorMessage).setSignature(ByteString.copyFrom(signedPublicKey)).build();
                responseObserver.onNext(OpenAccountResponse.newBuilder().setError(error).build());
                responseObserver.onCompleted();
            } catch (BaseException ignored){
            }
        }
    }

    /*
    @Override
    public void updateTransactions(UpdateTransactionsRequest request, StreamObserver<Ack> responseObserver) {
        //todo update -- transactions mal assinadas(done) -- check signatures(done)
        responseObserver.onNext(Ack.newBuilder().build());
        responseObserver.onCompleted();

        try {
            List<Transaction> transactions = request.getTransactionsList();
            for (int i = 0; i < request.getServersList().size(); i++) {
                PublicKey serverPublicKey = KeyStore.stringToPublicKey(request.getServersList().get(i));
                ServerInfo.serverPublicKeyExists(serverPublicKey);
                Sign.checkSignature(serverPublicKey.getEncoded(),request.getSignaturesList().get(i).toByteArray(),transactions);
            }
            //todo update transactions
        } catch(BaseException e){
            System.out.println("Problems Updating");
        }
    }*/

    @Override
    public void sendAmount(SendAmountRequest request, StreamObserver<SendAmountResponse> responseObserver) {
        byte[] publicKeySource = request.getPublicKeySource().toByteArray();
        byte[] publicKeyDestination = request.getPublicKeyDestination().toByteArray();
        int amount = request.getAmount();
        int nonce = request.getNonce();
        byte[] signature = request.getSignature().toByteArray();

        try {
            Sign.checkSignature(publicKeySource, signature, publicKeySource , publicKeyDestination , amount , nonce);
            Transactions.addTransaction(publicKeySource , publicKeyDestination , amount , nonce , signature);
            byte[] signedIncrementedNonce = Sign.signMessage(publicKeySource , publicKeyDestination , amount , nonce + 1);
            SendAmount sendAmount = SendAmount.newBuilder().setSignature(ByteString.copyFrom(signedIncrementedNonce)).build();
            responseObserver.onNext(SendAmountResponse.newBuilder().setSendAmount(sendAmount).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            try {
                //responseObserver.onError(ex);
                String errorMessage = e.toResponseException().getMessage();
                byte[] signedPublicKey = Sign.signMessage(publicKeySource , publicKeyDestination , amount , nonce + 1,errorMessage);
                Error error = Error.newBuilder().setMessage(errorMessage).setSignature(ByteString.copyFrom(signedPublicKey)).build();
                responseObserver.onNext(SendAmountResponse.newBuilder().setError(error).build());
                responseObserver.onCompleted();
            } catch (BaseException ignored){
            }
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
            ReceiveAmount receiveAmount = ReceiveAmount.newBuilder().setSignature(ByteString.copyFrom(signedFields)).build();
            responseObserver.onNext(ReceiveAmountResponse.newBuilder().setReceiveAmount(receiveAmount).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            try {
                //responseObserver.onError(ex);
                String errorMessage = e.toResponseException().getMessage();
                byte[] signedPublicKey = Sign.signMessage(publicKey , transactionID, errorMessage);
                Error error = Error.newBuilder().setMessage(errorMessage).setSignature(ByteString.copyFrom(signedPublicKey)).build();
                responseObserver.onNext(ReceiveAmountResponse.newBuilder().setError(error).build());
                responseObserver.onCompleted();
            } catch (BaseException ignored){
            }
        }
    }

    @Override
    public void checkAccount(CheckAccountRequest request, StreamObserver<CheckAccountResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();

        try {
            int balance = Balance.getBalance(publicKey);
            List<Transaction> transactions = Transactions.getPendingTransactions(publicKey);
            CheckAccount.Builder builder = CheckAccount.newBuilder();
            builder.addAllTransactions(transactions);
            builder.setBalance(balance);
            byte[] signedFields = Sign.signMessage(balance , builder.getTransactionsList().toArray());
            builder.setSignature(ByteString.copyFrom(signedFields));
            responseObserver.onNext(CheckAccountResponse.newBuilder().setCheckAccount(builder.build()).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }

    @Override
    public void audit(AuditRequest request, StreamObserver<AuditResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();

        try {
            List<Transaction> transactions = Transactions.getTransactions(publicKey);
            Audit.Builder builder = Audit.newBuilder();
            builder.addAllTransactions(transactions);
            byte[] signedFields = Sign.signMessage(builder.getTransactionsList().toArray());
            builder.setSignature(ByteString.copyFrom(signedFields));
            responseObserver.onNext(AuditResponse.newBuilder().setAudit(builder.build()).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }
}