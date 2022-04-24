package tecnico.sec.server;
import com.google.protobuf.ByteString;
import dbController.Balance;
import dbController.Nonce;
import dbController.Transactions;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.KeyStore.singletons.Sign;
import tecnico.sec.client.ServerConnection;
import tecnico.sec.client.WriteResponse;
import tecnico.sec.grpc.*;
import tecnico.sec.grpc.Error;
import tecnico.sec.grpc.ServiceGrpc.ServiceImplBase;
import tecnico.sec.proto.exceptions.*;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ServiceImpl extends ServiceImplBase {

    public void validateChallenge(byte[] publicKeySource , ChallengeCompleted solve , long nonce) throws NonceExceptions.NonceNotFoundException, BalanceExceptions.GeneralMYSQLException, KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        Challenge sentChallenge = Nonce.getNonce(publicKeySource);
        if(nonce == sentChallenge.getNonce() + 1 && Sign.toHex(solve.getHash().toByteArray()).startsWith("0".repeat(sentChallenge.getZeros()))){
            Sign.checkSignature(publicKeySource , solve.getHash().toByteArray() , nonce , solve.getPadding());
        }
    }

    @Override
    public void getChallenge(ChallengeRequest request, StreamObserver<ChallengeResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();
        long clientNonce = request.getNonce();
        try {
            Challenge challenge = Nonce.getNonce(publicKey);
            byte[] sign = Sign.signMessage(challenge.getNonce() , challenge.getZeros() , clientNonce + 1);
            responseObserver.onNext(ChallengeResponse.newBuilder().setChallenge(challenge).setSignature(ByteString.copyFrom(sign)).build());
            responseObserver.onCompleted();
        } catch (NonceExceptions.NonceNotFoundException | BalanceExceptions.GeneralMYSQLException e) {
            try {
                SecureRandom random = new SecureRandom();
                long nonce = random.nextLong();
                int zeros = (int) Math.round( Math.random());
                Nonce.createNonce(publicKey , nonce , zeros);
                Challenge challenge = Challenge.newBuilder().setNonce(nonce).setZeros(zeros).build();
                byte[] sign = Sign.signMessage(challenge.getNonce() , challenge.getZeros() , clientNonce + 1);
                responseObserver.onNext(ChallengeResponse.newBuilder().setChallenge(challenge).setSignature(ByteString.copyFrom(sign)).build());
                responseObserver.onCompleted();
            } catch (BaseException ex) {
                try {
                    String errorMessage = ex.toResponseException().getMessage();
                    byte[] signedPublicKey = Sign.signMessage(publicKey,errorMessage);
                    Error error = Error.newBuilder().setMessage(errorMessage).setSignature(ByteString.copyFrom(signedPublicKey)).build();
                    responseObserver.onNext(ChallengeResponse.newBuilder().setError(error).build());
                    responseObserver.onCompleted();
                } catch (BaseException ignored){
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    @Override
    public void updateTransactions(UpdateTransactionsRequest request, StreamObserver<Ack> responseObserver) {
        responseObserver.onNext(Ack.newBuilder().build());
        responseObserver.onCompleted();

        try {
            List<Transaction> transactions = request.getTransactionsList();
            List<PublicKey> servers = new ArrayList<>();
            for (int i = 0; i < request.getQuorum().getServerMessagesCount(); i++) {
                PublicKey serverPublicKey = KeyStore.toPublicKey(request.getQuorum().getServerMessages(i).getPublicKey().toByteArray());
                ServerInfo.serverPublicKeyExists(serverPublicKey);
                servers.add(serverPublicKey);
                Sign.checkSignature(serverPublicKey.getEncoded(), request.getQuorum().getServerMessages(i).getSignature().toByteArray(), transactions);
            }

            List<WriteResponse> replies = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(ServerConnection.getConnection().size() / 2 + 1);
            ServerUpdateRequest req = ServerUpdateRequest.newBuilder().setLatestID(Transactions.getLastTransactionId()).build();

            for (Server server : ServerInfo.getServerList()) {
                if (servers.contains(server.getPublicKey())) {
                    server.getConnection().getMissingTransactions(req, new StreamObserver<ServerUpdateReply>() {
                        @Override
                        public void onNext(ServerUpdateReply response) {
                            try {
                                Sign.checkSignature(server.getPublicKey().getEncoded(), response.getSignature().toByteArray(), response.getTransactionsList());
                                synchronized (replies) {
                                    replies.add(new WriteResponse(response.getTransactionsList(), false, ""));
                                }
                                latch.countDown();

                            } catch (BaseException ignored) {
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            System.out.println("Error occurred " + t.getMessage());
                        }

                        @Override
                        public void onCompleted() {
                        }
                    });}
                }

                latch.await();
                WriteResponse response = WriteResponse.getResult(replies);
                Transactions.addMissingTransactions((List<Transaction>) response.getResponse());
            } catch(InterruptedException | BaseException e){
                Status status = Status.fromThrowable(e);
                System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
            }
    }

    @Override
    public void sendAmount(SendAmountRequest request, StreamObserver<SendAmountResponse> responseObserver) {
        byte[] publicKeySource = request.getPublicKeySource().toByteArray();
        byte[] publicKeyDestination = request.getPublicKeyDestination().toByteArray();
        int amount = request.getAmount();
        long nonce = request.getNonce();
        try {
            validateChallenge(publicKeySource , request.getChallenge() , request.getNonce());
            byte[] signature = request.getSignature().toByteArray();
            Sign.checkSignature(publicKeySource, signature, publicKeySource , publicKeyDestination , amount , nonce , request.getChallenge());
            Transactions.addTransaction(publicKeySource , publicKeyDestination , amount , nonce , signature);
            byte[] signedIncrementedNonce = Sign.signMessage(publicKeySource , publicKeyDestination , amount , nonce + 1);
            SendAmount sendAmount = SendAmount.newBuilder().setSignature(ByteString.copyFrom(signedIncrementedNonce)).build();
            responseObserver.onNext(SendAmountResponse.newBuilder().setSendAmount(sendAmount).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            try {
                String errorMessage = e.toResponseException().getMessage();
                byte[] signedPublicKey = Sign.signMessage(publicKeySource , publicKeyDestination , amount , nonce + 1 , errorMessage);
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
            validateChallenge(publicKey , request.getChallenge() , request.getNonce());
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
            validateChallenge(publicKey , request.getChallenge() , request.getNonce());
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
            try {
                String errorMessage = e.toResponseException().getMessage();
                byte[] signedPublicKey = Sign.signMessage(publicKey , errorMessage);
                Error error = Error.newBuilder().setMessage(errorMessage).setSignature(ByteString.copyFrom(signedPublicKey)).build();
                responseObserver.onNext(CheckAccountResponse.newBuilder().setError(error).build());
                responseObserver.onCompleted();
            } catch (BaseException ignored){
            }
        }
    }

    @Override
    public void audit(AuditRequest request, StreamObserver<AuditResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();
        byte[] selfPublicKey = request.getSelfPublicKey().toByteArray();

        try {
            validateChallenge(selfPublicKey , request.getChallenge() , request.getNonce());
            List<Transaction> transactions = Transactions.getTransactions(publicKey);
            Audit.Builder builder = Audit.newBuilder();
            builder.addAllTransactions(transactions);
            byte[] signedFields = Sign.signMessage(builder.getTransactionsList().toArray());
            builder.setSignature(ByteString.copyFrom(signedFields));
            responseObserver.onNext(AuditResponse.newBuilder().setAudit(builder.build()).build());
            responseObserver.onCompleted();
        } catch (BaseException e) {
            try {
                String errorMessage = e.toResponseException().getMessage();
                byte[] signedPublicKey = Sign.signMessage(publicKey ,  selfPublicKey, errorMessage);
                Error error = Error.newBuilder().setMessage(errorMessage).setSignature(ByteString.copyFrom(signedPublicKey)).build();
                responseObserver.onNext(AuditResponse.newBuilder().setError(error).build());
                responseObserver.onCompleted();
            } catch (BaseException ignored){
            }
        }
    }

    @Override
    public void getMissingTransactions(ServerUpdateRequest request , StreamObserver<ServerUpdateReply> responseObserver){
        try {
            List<Transaction> list =  Transactions.getMissingTransactions(request.getLatestID());
            responseObserver.onNext(ServerUpdateReply.newBuilder().addAllTransactions(list).setSignature(ByteString.copyFrom(Sign.signMessage(list))).build());
            responseObserver.onCompleted();
        } catch (BalanceExceptions.GeneralMYSQLException | KeyExceptions.InvalidPublicKeyException | SignatureExceptions.CanNotSignException | IOExceptions.IOException | KeyExceptions.NoSuchAlgorithmException | KeyExceptions.GeneralKeyStoreErrorException e) {
        }
    }
}