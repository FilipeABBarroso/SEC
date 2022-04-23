package tecnico.sec.client;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.javatuples.Pair;

import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.grpc.*;
import tecnico.sec.proto.exceptions.BaseException;
import tecnico.sec.proto.exceptions.IOExceptions;
import tecnico.sec.proto.exceptions.KeyExceptions;
import tecnico.sec.proto.exceptions.SignatureExceptions;

import static java.util.stream.Collectors.groupingBy;
import static tecnico.sec.KeyStore.singletons.Sign.checkSignature;
import static tecnico.sec.KeyStore.singletons.Sign.signMessage;

import java.security.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;


public class Client {

    //todo anti spam mechanism - error signature

    public static boolean open_account() {
        try {
            PublicKey publicKey = KeyStore.getPublicKey();
            byte[] pubKeyField = publicKey.getEncoded();
            byte[] signature = signMessage(pubKeyField);

            OpenAccountRequest request = OpenAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setSignature(ByteString.copyFrom(signature))
                    .build();

            List<WriteResponse> replies = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(ServerConnection.getConnection().size() / 2 + 1);

            for (ServerInfo server : ServerConnection.getConnection()) {
                server.getStub().openAccount(request, new StreamObserver<OpenAccountResponse>() {
                    @Override
                    public void onNext(OpenAccountResponse response) {
                        try {
                            switch (response.getResponseCase()) {
                                case OPENACCOUNT -> {
                                    openAccountCheckResponse(server.getPublicKey().getEncoded(), response.getOpenAccount().getSignature().toByteArray(), pubKeyField);
                                    synchronized (replies) {
                                        replies.add(new WriteResponse(response, false, ""));
                                    }
                                    latch.countDown();
                                }
                                case ERROR -> {
                                    errorCheckSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(),pubKeyField,response.getError().getMessage());
                                    synchronized (replies) {
                                        replies.add(new WriteResponse(response, true, response.getError().getMessage()));
                                    }
                                    latch.countDown();
                                }
                                case RESPONSE_NOT_SET -> {
                                }
                            }
                        } catch(BaseException ignored){}
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error occurred " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
            latch.await();
            WriteResponse response = WriteResponse.getResult(replies);
            System.out.println(response.getMessage());
            return !response.isError();
        } catch (InterruptedException | BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void errorCheckSignature(byte[] serverPublicKey, byte[] signature, byte[] accountPublicKey, String errorMessage) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey,signature,accountPublicKey,errorMessage);
    }

    public static void openAccountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] accountPublicKey) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, accountPublicKey);
    }

    public static boolean send_amount(PublicKey destination, int amount) {
        try {
            PublicKey source = KeyStore.getPublicKey();
            byte[] sourceField = source.getEncoded();
            byte[] destinationField = destination.getEncoded();

            NonceRequest nonceRequest = NonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(sourceField)).build();

            List<WriteResponse> replies = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(ServerConnection.getServerCount() / 2 + 1);

            for (ServerInfo server : ServerConnection.getConnection()) {
                server.getStub().getNonce(nonceRequest,  new StreamObserver<NonceResponse>() {
                    @Override
                    public void onNext(NonceResponse response) {
                        try {
                            switch (response.getResponseCase()) {
                                case NONCE -> {
                                    int nonce = response.getNonce().getNonce();

                                    byte[] signature = signMessage(sourceField, destinationField, amount, nonce);

                                    SendAmountRequest sendAmountRequest = SendAmountRequest.newBuilder()
                                            .setPublicKeySource(ByteString.copyFrom(sourceField))
                                            .setPublicKeyDestination(ByteString.copyFrom(destinationField))
                                            .setAmount(amount)
                                            .setNonce(nonce)
                                            .setSignature(ByteString.copyFrom(signature))
                                            .build();

                                    server.getStub().sendAmount(sendAmountRequest, new StreamObserver<SendAmountResponse>() {
                                        @Override
                                        public void onNext(SendAmountResponse response) {
                                            try {
                                                switch (response.getResponseCase()) {
                                                    case SENDAMOUNT -> {
                                                        sendAmountCheckResponse(server.getPublicKey().getEncoded(), response.getSendAmount().getSignature().toByteArray(), sourceField, destinationField, amount, nonce + 1);
                                                        synchronized (replies) {
                                                            replies.add(new WriteResponse(response, false, ""));
                                                        }
                                                        latch.countDown();
                                                    }
                                                    case ERROR -> {
                                                        checkSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(),sourceField,destinationField,amount,nonce + 1,response.getError().getMessage());
                                                        synchronized (replies) {
                                                            replies.add(new WriteResponse(response, true, response.getError().getMessage()));
                                                        }
                                                        latch.countDown();
                                                    }
                                                    case RESPONSE_NOT_SET -> {
                                                    }
                                                }
                                            } catch(BaseException ignored){}
                                        }

                                        @Override
                                        public void onError(Throwable t) {
                                            System.out.println("Error occurred " + t.getMessage());
                                        }

                                        @Override
                                        public void onCompleted() {
                                        }
                                    });
                                }
                                case ERROR, RESPONSE_NOT_SET -> {
                                }
                            }
                        } catch(BaseException ignored){}
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error occurred " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                    }
                });

            }
            latch.await();
            WriteResponse response = WriteResponse.getResult(replies);
            System.out.println(response.getMessage());
            return response.isError();
        } catch (BaseException | InterruptedException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void sendAmountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] source, byte[] destination, int amount, int nonce) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, source, destination, amount, nonce);
    }

    public static boolean receive_amount(int transactionID) {
        try {
            PublicKey pubKey = KeyStore.getPublicKey();
            byte[] pubKeyField = pubKey.getEncoded();
            byte[] signature = signMessage(pubKeyField, transactionID);

            ReceiveAmountRequest request = ReceiveAmountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setTransactionID(transactionID)
                    .setSignature(ByteString.copyFrom(signature))
                    .build();

            List<WriteResponse> replies = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(ServerConnection.getServerCount() / 2 + 1);

            for (ServerInfo server : ServerConnection.getConnection()) {
                server.getStub().receiveAmount(request, new StreamObserver<ReceiveAmountResponse>() {
                    @Override
                    public void onNext(ReceiveAmountResponse response) {
                        try {
                            switch (response.getResponseCase()) {
                                case RECEIVEAMOUNT -> {
                                    receiveAmountCheckResponse(server.getPublicKey().getEncoded(), response.getReceiveAmount().getSignature().toByteArray(), pubKeyField, transactionID);
                                    synchronized (replies) {
                                        replies.add(new WriteResponse(response, false, ""));
                                    }
                                    latch.countDown();
                                }
                                case ERROR -> {
                                    checkSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(),pubKeyField,transactionID,response.getError().getMessage());
                                    synchronized (replies) {
                                        replies.add(new WriteResponse(response, true, response.getError().getMessage()));
                                    }
                                    latch.countDown();
                                }
                                case RESPONSE_NOT_SET -> {
                                }
                            }
                        } catch(BaseException ignored){}
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error occurred " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
            latch.await();
            WriteResponse response = WriteResponse.getResult(replies);
            System.out.println(response.getMessage());
            return response.isError();
        } catch (BaseException | InterruptedException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void receiveAmountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] accountPublicKey, int transactionID) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, accountPublicKey, transactionID);
    }

    public static Pair<Integer, List<Transaction>> check_account() {
        try {
            PublicKey pubKey = KeyStore.getPublicKey();
            byte[] pubKeyField = pubKey.getEncoded();

            CheckAccountRequest request = CheckAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .build();

            List<ReadResponse> replies = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(ServerConnection.getServerCount() / 2 + 1);

            for (ServerInfo server : ServerConnection.getConnection()) {
                server.getStub().checkAccount(request,  new StreamObserver<CheckAccountResponse>() {
                    @Override
                    public void onNext(CheckAccountResponse response) {
                        try {
                            switch (response.getResponseCase()) {
                                case CHECKACCOUNT -> {
                                    checkAccountCheckResponse(server.getPublicKey().getEncoded(), response.getCheckAccount().getSignature().toByteArray(), response.getCheckAccount().getBalance(), response.getCheckAccount().getTransactionsList().toArray());
                                    synchronized (replies) {
                                        replies.add(new ReadResponse(server, response, false, "", response.getCheckAccount().getTransactionsList(),response.getCheckAccount().getBalance()));
                                    }
                                    latch.countDown();
                                }
                                case ERROR -> {
                                    errorCheckSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(),pubKeyField,response.getError().getMessage());
                                    synchronized (replies) {
                                        replies.add(new ReadResponse(server, response, true, response.getError().getMessage(), new ArrayList<>(),0));
                                    }
                                    latch.countDown();
                                }
                                case RESPONSE_NOT_SET -> {
                                }
                            }
                        } catch(BaseException ignored){}
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error occurred " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
            latch.await();
            ReadResponse response = ReadResponse.getResult(replies);
            System.out.println(response.getMessage());
            if(!response.isError()){
                //todo return to user and update servers?
                return Pair.with(response.getBalance(),response.getTransactions());
            }
        } catch (BaseException | InterruptedException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static void checkAccountCheckResponse(byte[] serverPublicKey, byte[] signature, int balance, Object[] transactionList) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, balance, transactionList);
    }

    public static List<Transaction> audit(PublicKey key) {
        try {
            byte[] pubKeyField = key.getEncoded();
            AuditRequest request = AuditRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .build();

            List<ReadResponse> replies = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(ServerConnection.getServerCount() / 2 + 1);

            for (ServerInfo server : ServerConnection.getConnection()) {
                server.getStub().audit(request, new StreamObserver<AuditResponse>() {
                    @Override
                    public void onNext(AuditResponse response) {
                        try {
                            switch (response.getResponseCase()) {
                                case AUDIT -> {
                                    auditCheckResponse(server.getPublicKey().getEncoded(), response.getAudit().getSignature().toByteArray(), response.getAudit().getTransactionsList().toArray());
                                    synchronized (replies) {
                                        replies.add(new ReadResponse(server, response, false, "", response.getAudit().getTransactionsList(),0));
                                    }
                                    latch.countDown();
                                }
                                case ERROR -> {
                                    errorCheckSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(),pubKeyField,response.getError().getMessage());
                                    synchronized (replies) {
                                        replies.add(new ReadResponse(server, response, true, response.getError().getMessage(), new ArrayList<>(),0));
                                    }
                                    latch.countDown();
                                }
                                case RESPONSE_NOT_SET -> {
                                }
                            }
                        } catch(BaseException ignored){}
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error occurred " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
            latch.await();
            ReadResponse response = ReadResponse.getResult(replies);
            System.out.println(response.getMessage());
            if(!response.isError()){
                //todo update servers?
                return response.getTransactions();
            }
        } catch (StatusRuntimeException | InterruptedException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static void auditCheckResponse(byte[] serverPublicKey, byte[] signature, Object[] transactionList) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, transactionList);
    }

    public static void updateServers(List<ReadResponse> responses, ReadResponse result,List<ServerInfo> serverToUpdate) {
        List<ReadResponse> responsesQuorum = ReadResponse.getResponseQuorum(responses, result);
        for (ServerInfo s : serverToUpdate) {
            //todo call endpoint --
        }
    }


}

