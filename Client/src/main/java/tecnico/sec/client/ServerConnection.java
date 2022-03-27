package tecnico.sec.client;

import io.grpc.ManagedChannelBuilder;
import tecnico.sec.grpc.ServiceGrpc;

public class ServerConnection {

    private static ServiceGrpc.ServiceBlockingStub stub;


    private static void initConnection(){
        stub = ServiceGrpc.newBlockingStub(ManagedChannelBuilder
                .forAddress("localhost",8080)
                .usePlaintext()
                .build());
    }

    public static ServiceGrpc.ServiceBlockingStub getConnection() {
        if(stub == null){
            initConnection();
        }
        return stub;
    }

}
