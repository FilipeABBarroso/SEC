package tecnico.sec.server;
import io.grpc.stub.StreamObserver;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.KeyStore.singletons.Sign;
import tecnico.sec.grpc.*;
import tecnico.sec.grpc.ServiceGrpc.ServiceImplBase;
import tecnico.sec.proto.exceptions.BaseException;
import tecnico.sec.proto.exceptions.IOExceptions;

import java.security.PublicKey;

public class ServiceImpl extends ServiceImplBase {

    @Override
    public void openAccount(OpenAccountRequest request, StreamObserver<OpenAccountResponse> responseObserver) {
        final int BASE_BALANCE = 1000000;
        try {
            PublicKey publicKey = KeyStore.stringToPubKey(request.getPublicKey());
            byte[] signature = request.getSignature().toByteArray();
            Sign.checkSignature(publicKey, signature, request.getPublicKey());
        } catch (IOExceptions.IOException e){
            System.out.println(e.toResponseException().getMessage());
        } catch (BaseException e) {
            responseObserver.onError(e.toResponseException());
        }
        //todo Check if the publicKey exists in the database and if exists do responseObserver.onError("ERROR")
        //todo Create a entry in the BALANCE database with the PK public key and the balance as BASE_BALANCE
    }

    @Override
    public void sendAmount(SendAmountRequest request, StreamObserver<SendAmountResponse> responseObserver) {
        super.sendAmount(request, responseObserver);
    }

    @Override
    public void receiveAmount(ReceiveAmountRequest request, StreamObserver<ReceiveAmountResponse> responseObserver) {
        super.receiveAmount(request, responseObserver);
    }

    @Override
    public void checkAccount(CheckAccountRequest request, StreamObserver<CheckAccountResponse> responseObserver) {
        super.checkAccount(request, responseObserver);
    }

    @Override
    public void audit(AuditRequest request, StreamObserver<AuditResponse> responseObserver) {
        super.audit(request, responseObserver);
    }
}