package tecnico.sec.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;

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

import static tecnico.sec.KeyStore.singletons.Sign.checkSignature;
import static tecnico.sec.KeyStore.singletons.Sign.signMessage;

import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


public class Client {

    public static boolean open_account() { //todo read/write
        try {
            PublicKey publicKey = KeyStore.getPublicKey();
            byte[] pubKeyField = publicKey.getEncoded();
            byte[] signature = signMessage(pubKeyField);

            OpenAccountRequest request = OpenAccountRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(pubKeyField))
                    .setSignature(ByteString.copyFrom(signature))
                    .build();

            List<Boolean> replies = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            List<Boolean> result = new ArrayList<>();

            for (Pair<ServiceGrpc.ServiceStub, PublicKey> server : ServerConnection.getConnection()) {
                server.getValue0().openAccount(request, new StreamObserver<OpenAccountResponse>() {
                    @Override
                    public void onNext(OpenAccountResponse response) {
                        try {
                            openAccountCheckResponse(server.getValue1().getEncoded(), response.getSignature().toByteArray(), pubKeyField);
                            resultCompute(true);
                        } catch (BaseException e) {
                            Status status = Status.fromThrowable(e);
                            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error occurred " + t.getMessage());
                        resultCompute(false);
                    }

                    public void resultCompute(boolean value) {
                        synchronized (latch) {
                            if (latch.getCount() != 0) {
                                replies.add(value);
                                try {
                                    Boolean readResult = readListResult(replies);
                                    result.add(readResult);
                                    latch.countDown();
                                } catch (RuntimeException ignored){}
                            }
                        }
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
            latch.await();
            if(!result.isEmpty()){
                return result.get(0);
            }
        } catch (InterruptedException | BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
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

            List<Boolean> replies = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            List<Boolean> result = new ArrayList<>();

            for (Pair<ServiceGrpc.ServiceStub, PublicKey> server : ServerConnection.getConnection()) {
                server.getValue0().getNonce(nonceRequest, new StreamObserver<>() {
                    @Override
                    public void onNext(NonceResponse response) {
                        int nonce = response.getNonce();
                        byte[] signature = new byte[0];
                        try {
                            signature = signMessage(sourceField, destinationField, amount, nonce);
                        } catch (BaseException e) {
                            Status status = Status.fromThrowable(e);
                            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
                        }

                        SendAmountRequest sendAmountRequest = SendAmountRequest.newBuilder()
                                .setPublicKeySource(ByteString.copyFrom(sourceField))
                                .setPublicKeyDestination(ByteString.copyFrom(destinationField))
                                .setAmount(amount)
                                .setNonce(nonce)
                                .setSignature(ByteString.copyFrom(signature))
                                .build();

                        server.getValue0().sendAmount(sendAmountRequest, new StreamObserver<SendAmountResponse>() {
                            @Override
                            public void onNext(SendAmountResponse response) {
                                try {
                                    sendAmountCheckResponse(server.getValue1().getEncoded(), response.getSignature().toByteArray(), sourceField, destinationField, amount, nonce + 1);
                                    resultCompute(true);
                                } catch (BaseException e) {
                                    Status status = Status.fromThrowable(e);
                                    System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
                                }
                            }

                            @Override
                            public void onError(Throwable t) {
                                System.out.println("Error occurred " + t.getMessage());
                                resultCompute(false);
                            }

                            public void resultCompute(boolean value) {
                                synchronized (latch) {
                                    if (latch.getCount() != 0) {
                                        replies.add(value);
                                        try {
                                            Boolean readResult = readListResult(replies);
                                            result.add(readResult);
                                            latch.countDown();
                                        } catch (RuntimeException ignored){}
                                    }
                                }
                            }

                            @Override
                            public void onCompleted() {
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable t) {
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
            latch.await();
            if(!result.isEmpty()){
                return result.get(0);
            }
        } catch (InterruptedException | BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
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

            List<Boolean> replies = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            List<Boolean> result = new ArrayList<>();

            for (Pair<ServiceGrpc.ServiceStub, PublicKey> server : ServerConnection.getConnection()) {
                server.getValue0().receiveAmount(request,new StreamObserver<ReceiveAmountResponse>() {

                    @Override
                    public void onNext(ReceiveAmountResponse response) {
                        try {
                            receiveAmountCheckResponse(server.getValue1().getEncoded(), response.getSignature().toByteArray(), pubKeyField, transactionID);
                            resultCompute(true);
                        } catch (BaseException e) {
                            Status status = Status.fromThrowable(e);
                            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error occurred " + t.getMessage());
                        resultCompute(false);
                    }

                    public void resultCompute(boolean value) {
                        synchronized (latch) {
                            if (latch.getCount() != 0) {
                                replies.add(value);
                                try {
                                    Boolean readResult = readListResult(replies);
                                    result.add(readResult);
                                    latch.countDown();
                                } catch (RuntimeException ignored){}
                            }
                        }
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
            latch.await();
            if(!result.isEmpty()){
                return result.get(0);
            }
        } catch (InterruptedException | BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return false;
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

            List<Pair<Integer, List<String>>> replies = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            List<Pair<Integer, List<String>>> result = new ArrayList<>();

            for (Pair<ServiceGrpc.ServiceStub, PublicKey> server : ServerConnection.getConnection()) {
                server.getValue0().checkAccount(request, new StreamObserver<CheckAccountResponse>() {
                    @Override
                    public void onNext(CheckAccountResponse response) {
                        try {
                            checkAccountCheckResponse(server.getValue1().getEncoded(), response.getSignature().toByteArray(), response.getBalance(), response.getTransactionsList().toArray());
                            resultCompute(Pair.with(response.getBalance(),response.getTransactionsList()));
                        } catch (BaseException e) {
                            Status status = Status.fromThrowable(e);
                            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error occurred " + t.getMessage());
                    }

                    public void resultCompute(Pair<Integer, List<String>> value) {
                        synchronized (latch) {
                            if (latch.getCount() != 0) {
                                replies.add(value);
                                try {
                                    Pair<Integer, List<String>> readResult = readListResult(replies);
                                    result.add(readResult);
                                    latch.countDown();
                                } catch (RuntimeException ignored){}
                            }
                        }
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
            latch.await();
            if(!result.isEmpty()){
                return result.get(0);
            }
        } catch (InterruptedException | BaseException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static void checkAccountCheckResponse(byte[] serverPublicKey, byte[] signature, int balance, Object[] transactionList) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, balance, transactionList);
    }

    public static List<String> audit(PublicKey key) {
        try {
            AuditRequest request = AuditRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(key.getEncoded()))
                    .build();

            List<List<String>> replies = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            List<List<String>> result = new ArrayList<>();

            for (Pair<ServiceGrpc.ServiceStub, PublicKey> server : ServerConnection.getConnection()) {
                server.getValue0().audit(request, new StreamObserver<AuditResponse>() {
                    @Override
                    public void onNext(AuditResponse response) {
                        try {
                            auditCheckResponse(server.getValue1().getEncoded(), response.getSignature().toByteArray(), response.getTransactionsList().toArray());
                            resultCompute(response.getTransactionsList());
                        } catch (BaseException e) {
                            Status status = Status.fromThrowable(e);
                            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error occurred " + t.getMessage());
                    }

                    public void resultCompute(List<String> value) {
                        synchronized (latch) {
                            if (latch.getCount() != 0) {
                                replies.add(value);
                                try {
                                    List<String> readResult = readListResult(replies);
                                    result.add(readResult);
                                    latch.countDown();
                                } catch (RuntimeException ignored){}
                            }
                        }
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
            latch.await();
            if(!result.isEmpty()){
                return result.get(0);
            }
        } catch (InterruptedException e) {
            Status status = Status.fromThrowable(e);
            System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
        }
        return null;
    }

    public static void auditCheckResponse(byte[] serverPublicKey, byte[] signature, Object[] transactionList) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, SignatureExceptions.SignatureDoNotMatchException, IOExceptions.IOException, KeyExceptions.NoSuchAlgorithmException {
        checkSignature(serverPublicKey, signature, transactionList);
    }

    public static <T> T readListResult(List<T> list) {
        Map<T, Integer> map = new HashMap<>();

        for (T t : list) {
            Integer val = map.get(t);
            map.put(t, val == null ? 1 : val + 1);
        }

        int count = (ServerConnection.getServerCount() / 2) + 1;
        for(Map.Entry<T, Integer> e : map.entrySet()){
            if(e.getValue() > count){
                return e.getKey();
            }
        }

        throw new RuntimeException();
    }

    //todo async - watch nonces - anti spam mechanism - dont wait for all?

}

