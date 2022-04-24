package tecnico.sec.client;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.javatuples.Pair;

import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.KeyStore.singletons.Sign;
import tecnico.sec.grpc.*;
import tecnico.sec.proto.exceptions.BaseException;
import tecnico.sec.proto.exceptions.IOExceptions;
import tecnico.sec.proto.exceptions.KeyExceptions;
import tecnico.sec.proto.exceptions.SignatureExceptions;

import static tecnico.sec.KeyStore.singletons.Sign.checkSignature;
import static tecnico.sec.KeyStore.singletons.Sign.signMessage;

import java.security.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


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
            int quorum = ServerConnection.getConnection().size() / 2 + 1;
            CountDownLatch latch = new CountDownLatch(1);

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
                                        if (WriteResponse.quorumExists(replies,quorum)) {
                                            latch.countDown();
                                        }
                                    }
                                }
                                case ERROR -> {
                                    errorCheckSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(),pubKeyField,response.getError().getMessage());
                                    synchronized (replies) {
                                        replies.add(new WriteResponse(response, true, response.getError().getMessage()));
                                        if (WriteResponse.quorumExists(replies,quorum)) {
                                            latch.countDown();
                                        }
                                    }
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
            if(latch.await(15, TimeUnit.SECONDS)) { //waits for 15 seconds for a quorum result
                WriteResponse response = WriteResponse.getResult(replies);
                System.out.println(response.getMessage());
                return !response.isError();
            } else { //result is not achieved
                System.out.println("OPERATION UNAVAILABLE!");
            }
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

            List<WriteResponse> replies = Collections.synchronizedList(new ArrayList<>());
            int quorum = ServerConnection.getConnection().size() / 2 + 1;
            CountDownLatch latch = new CountDownLatch(1);

            for (ServerInfo server : ServerConnection.getConnection()) {
                SecureRandom random = new SecureRandom();
                long clientNonce = random.nextLong();
                ChallengeRequest challengeRequest = ChallengeRequest.newBuilder().setPublicKey(ByteString.copyFrom(sourceField)).setNonce(clientNonce).build();
                server.getStub().getChallenge(challengeRequest,  new StreamObserver<ChallengeResponse>() {
                    @Override
                    public void onNext(ChallengeResponse response) {
                        try {
                            switch (response.getResponseCase()) {
                                case CHALLENGE -> {
                                    Sign.checkSignature(server.getPublicKey().getEncoded() , response.getSignature().toByteArray() , response.getChallenge().getNonce() , response.getChallenge().getZeros() , clientNonce + 1 );
                                    ChallengeCompleted result = Sign.solveChallenge(response.getChallenge().getZeros() , response.getChallenge().getNonce());
                                    long nonce = response.getChallenge().getNonce();
                                    byte[] signature = signMessage(sourceField, destinationField, amount, nonce + 1 , result);
                                    SendAmountRequest sendAmountRequest = SendAmountRequest.newBuilder()
                                            .setPublicKeySource(ByteString.copyFrom(sourceField))
                                            .setPublicKeyDestination(ByteString.copyFrom(destinationField))
                                            .setAmount(amount)
                                            .setNonce(nonce + 1)
                                            .setSignature(ByteString.copyFrom(signature))
                                            .setChallenge(result)
                                            .build();
                                    server.getStub().sendAmount(sendAmountRequest, new StreamObserver<SendAmountResponse>() {
                                        @Override
                                        public void onNext(SendAmountResponse response) {
                                            try {
                                                switch (response.getResponseCase()) {
                                                    case SENDAMOUNT -> {
                                                        sendAmountCheckResponse(server.getPublicKey().getEncoded(), response.getSendAmount().getSignature().toByteArray(), sourceField, destinationField, amount, nonce + 2);
                                                        synchronized (replies) {
                                                            replies.add(new WriteResponse(response, false, ""));
                                                            if (WriteResponse.quorumExists(replies,quorum)) {
                                                                latch.countDown();
                                                            }
                                                        }
                                                    }
                                                    case ERROR -> {
                                                        checkSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(),sourceField,destinationField,amount,nonce + 2,response.getError().getMessage());
                                                        synchronized (replies) {
                                                            replies.add(new WriteResponse(response, true, response.getError().getMessage()));
                                                            if (WriteResponse.quorumExists(replies,quorum)) {
                                                                latch.countDown();
                                                            }
                                                        }
                                                    }
                                                    case RESPONSE_NOT_SET -> {
                                                    }
                                                }
                                            } catch(BaseException ignored){
                                                latch.countDown(); //todo qué isto??
                                            }
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
                        } catch(BaseException ignored){
                        }
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
            if(latch.await(15, TimeUnit.SECONDS)) { //waits for 15 seconds for a quorum result
                WriteResponse response = WriteResponse.getResult(replies);
                System.out.println(response.getMessage());
                return !response.isError();
            } else { //result is not achieved
                System.out.println("OPERATION UNAVAILABLE!");
            }
        } catch (BaseException | InterruptedException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
    }

    public static void sendAmountCheckResponse(byte[] serverPublicKey, byte[] signature, byte[] source, byte[] destination, int amount, long nonce) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, source, destination, amount, nonce);
    }

    public static boolean receive_amount(int transactionID) {
        try {
            PublicKey pubKey = KeyStore.getPublicKey();
            byte[] pubKeyField = pubKey.getEncoded();
            byte[] signature = signMessage(pubKeyField, transactionID);

            List<WriteResponse> replies = Collections.synchronizedList(new ArrayList<>());
            int quorum = ServerConnection.getConnection().size() / 2 + 1;
            CountDownLatch latch = new CountDownLatch(1);

            for (ServerInfo server : ServerConnection.getConnection()) {
                SecureRandom random = new SecureRandom();
                long clientNonce = random.nextLong();
                ChallengeRequest challengeRequest = ChallengeRequest.newBuilder().setPublicKey(ByteString.copyFrom(pubKeyField)).setNonce(clientNonce).build();
                server.getStub().getChallenge(challengeRequest,  new StreamObserver<ChallengeResponse>() {
                    @Override
                    public void onNext(ChallengeResponse response) {
                        switch (response.getResponseCase()) {
                            case CHALLENGE -> {
                                ChallengeCompleted result = null;
                                try {
                                    result = Sign.solveChallenge(response.getChallenge().getZeros() , response.getChallenge().getNonce());
                                } catch (Exception e) {
                                }
                                long nonce = response.getChallenge().getNonce();
                                ReceiveAmountRequest request = ReceiveAmountRequest.newBuilder()
                                        .setPublicKey(ByteString.copyFrom(pubKeyField))
                                        .setTransactionID(transactionID)
                                        .setSignature(ByteString.copyFrom(signature))
                                        .setNonce(nonce + 1)
                                        .setChallenge(result)
                                        .build();
                                server.getStub().receiveAmount(request, new StreamObserver<ReceiveAmountResponse>() {
                                    @Override
                                    public void onNext(ReceiveAmountResponse response) {
                                        try {
                                            switch (response.getResponseCase()) {
                                                case RECEIVEAMOUNT -> {
                                                    receiveAmountCheckResponse(server.getPublicKey().getEncoded(), response.getReceiveAmount().getSignature().toByteArray(), pubKeyField, transactionID);
                                                    synchronized (replies) {
                                                        replies.add(new WriteResponse(response, false, ""));
                                                        if (WriteResponse.quorumExists(replies,quorum)) {
                                                            latch.countDown();
                                                        }
                                                    }
                                                }
                                                case ERROR -> {
                                                    checkSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(), pubKeyField, transactionID, response.getError().getMessage());
                                                    synchronized (replies) {
                                                        replies.add(new WriteResponse(response, true, response.getError().getMessage()));
                                                        if (WriteResponse.quorumExists(replies,quorum)) {
                                                            latch.countDown();
                                                        }
                                                    }
                                                }
                                                case RESPONSE_NOT_SET -> {
                                                }
                                            }
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
                                    });
                                }
                                case ERROR, RESPONSE_NOT_SET -> {
                                }
                            }
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
            if(latch.await(15, TimeUnit.SECONDS)) { //waits for 15 seconds for a quorum result
                WriteResponse response = WriteResponse.getResult(replies);
                System.out.println(response.getMessage());
                return !response.isError();
            } else { //result is not achieved
                System.out.println("OPERATION UNAVAILABLE!");
            }
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

            List<ReadResponse> replies = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(ServerConnection.getConnection().size());

            for (ServerInfo server : ServerConnection.getConnection()) {
                SecureRandom random = new SecureRandom();
                long clientNonce = random.nextLong();
                ChallengeRequest challengeRequest = ChallengeRequest.newBuilder().setPublicKey(ByteString.copyFrom(pubKeyField)).setNonce(clientNonce).build();
                server.getStub().getChallenge(challengeRequest,  new StreamObserver<ChallengeResponse>() {
                    @Override
                    public void onNext(ChallengeResponse response) {
                        switch (response.getResponseCase()) {
                            case CHALLENGE -> {
                                ChallengeCompleted result = null;
                                try {
                                    result = Sign.solveChallenge(response.getChallenge().getZeros() , response.getChallenge().getNonce());
                                } catch (Exception e) {
                                }
                                long nonce = response.getChallenge().getNonce();
                                CheckAccountRequest request = CheckAccountRequest.newBuilder()
                                        .setPublicKey(ByteString.copyFrom(pubKeyField))
                                        .setNonce(nonce+1)
                                        .setChallenge(result)
                                        .build();
                    server.getStub().checkAccount(request,  new StreamObserver<CheckAccountResponse>() {
                        @Override
                        public void onNext(CheckAccountResponse response) {
                            try {
                                switch (response.getResponseCase()) {
                                    case CHECKACCOUNT -> {
                                        checkAccountCheckResponse(server.getPublicKey().getEncoded(), response.getCheckAccount().getSignature().toByteArray(), response.getCheckAccount().getBalance(), response.getCheckAccount().getTransactionsList().toArray());
                                        synchronized (replies) {
                                            replies.add(new ReadResponse(server, response, false, "", response.getCheckAccount().getTransactionsList(),response.getCheckAccount().getBalance()));
                                            latch.countDown();
                                        }
                                    }
                                    case ERROR -> {
                                        errorCheckSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(),pubKeyField,response.getError().getMessage());
                                        synchronized (replies) {
                                            replies.add(new ReadResponse(server, response, true, response.getError().getMessage(), new ArrayList<>(),0));
                                            latch.countDown();
                                        }
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
                        public void onCompleted() {}
                        }
                    );
                            }
                            case ERROR, RESPONSE_NOT_SET -> {}
                            }
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
            if(latch.await(15, TimeUnit.SECONDS)) { //waits for 15 seconds for a quorum result
                ReadResponse response = ReadResponse.getResult(replies);
                System.out.println(response.getMessage());
                if (!response.isError()) {
                    updateServers(replies,response);
                    return Pair.with(response.getBalance(), response.getTransactions());
                }
            } else {
                System.out.println("OPERATION UNAVAILABLE!");
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

            List<ReadResponse> replies = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(ServerConnection.getConnection().size());

            for (ServerInfo server : ServerConnection.getConnection()) {
                SecureRandom random = new SecureRandom();
                long clientNonce = random.nextLong();
                ChallengeRequest challengeRequest = ChallengeRequest.newBuilder().setPublicKey(ByteString.copyFrom(pubKeyField)).setNonce(clientNonce).build();
                server.getStub().getChallenge(challengeRequest,  new StreamObserver<ChallengeResponse>() {
                    @Override
                    public void onNext(ChallengeResponse response) {
                        switch (response.getResponseCase()) {
                            case CHALLENGE -> {
                                ChallengeCompleted result = null;
                                try {
                                    result = Sign.solveChallenge(response.getChallenge().getZeros() , response.getChallenge().getNonce());
                                } catch (Exception e) {
                                }
                                long nonce = response.getChallenge().getNonce();
                                AuditRequest request = null;
                                try {
                                    request = AuditRequest.newBuilder()
                                            .setPublicKey(ByteString.copyFrom(pubKeyField))
                                            .setSelfPublicKey(ByteString.copyFrom(KeyStore.getPublicKey().getEncoded()))
                                            .setNonce(nonce)
                                            .setChallenge(result)
                                            .build();
                                } catch (KeyExceptions.GeneralKeyStoreErrorException e) {
                                    e.printStackTrace();
                                }
                                server.getStub().audit(request, new StreamObserver<AuditResponse>() {
                                    @Override
                                    public void onNext(AuditResponse response) {
                                        try {
                                            switch (response.getResponseCase()) {
                                                case AUDIT -> {
                                                    auditCheckResponse(server.getPublicKey().getEncoded(), response.getAudit().getSignature().toByteArray(), response.getAudit().getTransactionsList().toArray());
                                                    synchronized (replies) {
                                                        replies.add(new ReadResponse(server, response, false, "", response.getAudit().getTransactionsList(),0));
                                                        latch.countDown();
                                                    }
                                                }
                                                case ERROR -> {
                                                    checkSignature(server.getPublicKey().getEncoded(), response.getError().getSignature().toByteArray(),pubKeyField, KeyStore.getPublicKey().getEncoded() ,response.getError().getMessage());
                                                    synchronized (replies) {
                                                        replies.add(new ReadResponse(server, response, true, response.getError().getMessage(), new ArrayList<>(),0));
                                                        latch.countDown();
                                                    }
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
                            case ERROR, RESPONSE_NOT_SET -> {}
                        }
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
            if(latch.await(15, TimeUnit.SECONDS)) { //waits for 15 seconds for a quorum result
                ReadResponse response = ReadResponse.getResult(replies);
                System.out.println(response.getMessage());
                if (!response.isError()) {
                    updateServers(replies,response);
                    return response.getTransactions();
                }
            } else {
                System.out.println("OPERATION UNAVAILABLE!");
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

    public static void updateServers(List<ReadResponse> responses, ReadResponse result) {
        List<ReadResponse> responsesQuorum = ReadResponse.getResponseQuorum(responses, result);
        for (ReadResponse r : responses) {
            if(!r.equals(result)){
                //todo call endpoint
            }
        }
    }


}

