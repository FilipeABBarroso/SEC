package tecnico.sec.server;
import com.google.protobuf.ByteString;
import dbController.Balance;
import dbController.Nonce;
import dbController.Transactions;
import io.grpc.stub.StreamObserver;
import org.javatuples.Pair;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.KeyStore.singletons.Sign;
import tecnico.sec.grpc.*;
import tecnico.sec.grpc.ServiceGrpc.ServiceImplBase;
import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.BaseException;
import tecnico.sec.proto.exceptions.NonceExceptions;

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
            responseObserver.onError(e.toResponseException());
        }
    }

    @Override
    public void broadCastOpenAccount(OpenAccountRequest request , StreamObserver<OpenAccountResponse> responseObserver){
        List<Pair<String,Object>> responses = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(ServerInfo.getServerList().size() / 2 + 1);
        for(Server server : ServerInfo.getServerList()){
            server.getConnection().openAccount(request, new StreamObserver<>() {
                @Override
                public void onNext(OpenAccountResponse response) {
                    synchronized (latch) {
                        if (latch.getCount() != 0) {
                            try {
                                Sign.checkSignature(server.getPublicKey().getEncoded() , response.getSignature().toByteArray() , request.getPublicKey().toByteArray());
                                responses.add(Pair.with(KeyStore.publicKeyToString(server.getPublicKey()) ,response));
                            }catch (Exception e) {
                                System.out.println(e);
                            }
                            latch.countDown();
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    synchronized (latch) {
                        if (latch.getCount() != 0) {
                            responses.add(Pair.with(KeyStore.publicKeyToString(server.getPublicKey()) ,t));
                            latch.countDown();
                        }
                    }
                }

                @Override
                public void onCompleted() {
                }
            });
        }
        try {
            latch.await();

            Map<Object, Long> count = responses.stream().collect(groupingBy(p -> p.getValue1().getClass(), counting()));
            System.out.println(count);
            Map.Entry<Object , Long> max = Collections.max(count.entrySet(), Comparator.comparing(Map.Entry::getValue));
            System.out.println(max.getKey());
            System.out.println(OpenAccountResponse.class);
            System.out.println(max.getKey() instanceof OpenAccountResponse);
            if (max.getKey() instanceof OpenAccountResponse) {
                System.out.println("Entrou");
                responseObserver.onNext(null);
                responseObserver.onCompleted();
            }
            else{
                responseObserver.onError(null);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
    public void broadCastCheckAccount(CheckAccountRequest request, StreamObserver<CheckAccountResponse> responseObserver) {
        List<CheckAccountResponse> responses = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(ServerInfo.getServerList().size() / 2 + 1);
        for(Server server : ServerInfo.getServerList()){
            server.getConnection().checkAccount(request, new StreamObserver<>() {
                @Override
                public void onNext(CheckAccountResponse response) {
                    synchronized (latch) {
                        if (latch.getCount() != 0) {
                            try {
                                Sign.checkSignature(server.getPublicKey().getEncoded() , response.getSignature().toByteArray() , request.getPublicKey().toByteArray());
                                responses.add(response);
                            }catch (Exception e) {
                                System.out.println(e);
                            }
                            latch.countDown();
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    synchronized (latch) {
                        if (latch.getCount() != 0) {
                            System.out.println(t.getMessage());
                            responses.add(null);
                            latch.countDown();
                        }
                    }
                }

                @Override
                public void onCompleted() {
                }
            });
        }
        try {
            latch.await();
            int good = 0;
            int bad = 0;
            for(CheckAccountResponse response:responses){
                if(response == null) bad++;
                else good++;
            }
            System.out.println(good);
            System.out.println(bad);
            responseObserver.onNext(responses.get(0));
            responseObserver.onCompleted();
        } catch (InterruptedException e) {
            e.printStackTrace();
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