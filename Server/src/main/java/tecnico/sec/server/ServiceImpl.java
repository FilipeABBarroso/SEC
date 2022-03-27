package tecnico.sec.server;
import io.grpc.stub.StreamObserver;
import tecnico.sec.KeyStore.singletons.Sign;
import tecnico.sec.grpc.*;
import tecnico.sec.grpc.ServiceGrpc.ServiceImplBase;
import tecnico.sec.proto.exceptions.BaseException;

public class ServiceImpl extends ServiceImplBase {

    @Override
    public void openAccount(OpenAccountRequest request, StreamObserver<OpenAccountResponse> responseObserver) {
        String publicKey = request.getPublicKey();
        byte[] signature = request.getSignature().toByteArray();

        try {

            Sign.checkSignature(publicKey, signature, publicKey);


        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }


    }

    @Override
    public void sendAmount(SendAmountRequest request, StreamObserver<SendAmountResponse> responseObserver) {
        String publicKeySource = request.getPublicKeySource();
        String publicKeyDestination = request.getPublicKeyDestination();
        int amount = request.getAmount();
        int nonce = request.getNonce();
        byte[] signature = request.getSignature().toByteArray();

        try {

            Sign.checkSignature(publicKeySource, signature, publicKeySource);


        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }

    }

    @Override
    public void receiveAmount(ReceiveAmountRequest request, StreamObserver<ReceiveAmountResponse> responseObserver) {
        String publicKey = request.getPublicKey();
        int transactionID = request.getTransactionID();
        byte[] signature = request.getSignature().toByteArray();

        try {

            Sign.checkSignature(publicKey, signature, publicKey);


        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }

    }

    @Override
    public void checkAccount(CheckAccountRequest request, StreamObserver<CheckAccountResponse> responseObserver) {
        String publicKey = request.getPublicKey();
        byte[] signature = request.getSignature().toByteArray();

        try {

            Sign.checkSignature(publicKey, signature, publicKey);


        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }

    @Override
    public void audit(AuditRequest request, StreamObserver<AuditResponse> responseObserver) {
        String publicKey = request.getPublicKey();
        byte[] signature = request.getSignature().toByteArray();

        try {

            Sign.checkSignature(publicKey, signature, publicKey);


        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
    }
}