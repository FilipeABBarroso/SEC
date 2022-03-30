package tecnico.sec.server;
import com.google.protobuf.ByteString;
import dbController.Nonce;
import io.grpc.stub.StreamObserver;
import tecnico.sec.KeyStore.singletons.Sign;
import tecnico.sec.grpc.*;
import tecnico.sec.grpc.ServiceGrpc.ServiceImplBase;
import tecnico.sec.proto.exceptions.BaseException;

import java.security.SecureRandom;

public class ServiceImpl extends ServiceImplBase {

    @Override
    public void getNonce(NonceRequest request, StreamObserver<NonceResponse> responseObserver) {
        SecureRandom random = new SecureRandom();
        int nonce = random.nextInt();

        Nonce.creatNonce(request.getPublicKey().toByteArray() , nonce);

        responseObserver.onNext(NonceResponse.newBuilder().setNonce(nonce).build());
        responseObserver.onCompleted();
    }

    @Override
    public void openAccount(OpenAccountRequest request, StreamObserver<OpenAccountResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();
        byte[] signature = request.getSignature().toByteArray();

        try {

            Sign.checkSignature(publicKey, signature, publicKey);


        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void sendAmount(SendAmountRequest request, StreamObserver<SendAmountResponse> responseObserver) {
        byte[] publicKeySource = request.getPublicKeySource().toByteArray();
        byte[] publicKeyDestination = request.getPublicKeyDestination().toByteArray();
        int amount = request.getAmount();
        int nonce = request.getNonce();
        byte[] signature = request.getSignature().toByteArray();

        try {

            Sign.checkSignature(publicKeySource, signature, publicKeySource);
            byte[] signedIncrementedNonce = Sign.signMessage(nonce + 1);
            responseObserver.onNext(SendAmountResponse.newBuilder().setSignature(ByteString.copyFrom(signedIncrementedNonce)).build());
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void receiveAmount(ReceiveAmountRequest request, StreamObserver<ReceiveAmountResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();
        int transactionID = request.getTransactionID();
        byte[] signature = request.getSignature().toByteArray();

        try {

            Sign.checkSignature(publicKey, signature, publicKey);

        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void checkAccount(CheckAccountRequest request, StreamObserver<CheckAccountResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();

        responseObserver.onCompleted();
    }

    @Override
    public void audit(AuditRequest request, StreamObserver<AuditResponse> responseObserver) {
        byte[] publicKey = request.getPublicKey().toByteArray();
        responseObserver.onCompleted();
    }
}