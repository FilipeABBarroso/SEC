package tecnico.sec.server;
import io.grpc.stub.StreamObserver;
import tecnico.sec.grpc.*;
import tecnico.sec.grpc.ServiceGrpc.ServiceImplBase;

public class ServiceImpl extends ServiceImplBase {

    @Override
    public void openAccount(OpenAccountRequest request, StreamObserver<OpenAccountResponse> responseObserver) {
        final int BASE_BALANCE = 1000000;
        String publicKey = request.getPublicKey();
        //todo Check if the publicKey exists in the database and if exists do respondeObserver.onError("ERROR")
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