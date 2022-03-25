package tecnico.sec.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import tecnico.sec.grpc.*;

import static tecnico.sec.client.Main.pubKeyToString;

import java.security.*;

public class Client{

    private final ServiceGrpc.ServiceBlockingStub stub;

    //TODO handle exceptions

    public Client(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = ServiceGrpc.newBlockingStub(channel);
    }

    public void open_account(PublicKey key){
        //TODO
        OpenAccountResponse openResponse = stub.openAccount(OpenAccountRequest.newBuilder()
                .setPublicKey(pubKeyToString(key))
                .build());
    }

    public void send_amount(PublicKey source, PublicKey destination, int amount){
        //TODO
        SendAmountResponse sendAmountResponse = stub.sendAmount(SendAmountRequest.newBuilder()
                .setPublicKeySource(pubKeyToString(source))
                .setPublicKeyDestination(pubKeyToString(destination))
                .setAmount(amount)
                .build());
    }

    public void check_account(PublicKey key){
        //TODO
        CheckAccountResponse checkAccountResponse = stub.checkAccount(CheckAccountRequest.newBuilder()
                .setPublicKey(pubKeyToString(key))
                .build());
    }

    public void receive_amount(PublicKey key,int transactionID){
        //TODO
        ReceiveAmountResponse receiveAmountResponse = stub.receiveAmount(ReceiveAmountRequest.newBuilder()
                .setPublicKey(pubKeyToString(key))
                .setTransactionID(transactionID)
                .build());
    }

    public void audit(PublicKey key){
        //TODO
        AuditResponse auditResponse = stub.audit(AuditRequest.newBuilder()
                .setPublicKey(pubKeyToString(key))
                .build());

    }

}

