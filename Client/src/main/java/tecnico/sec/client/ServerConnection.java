package tecnico.sec.client;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import tecnico.sec.grpc.ServiceGrpc;

public class ServerConnection {

    private static ServiceGrpc.ServiceBlockingStub stub;
    private static ManagedChannel channel;


    private static void initConnection(){
        channel = ManagedChannelBuilder
                .forAddress("localhost",8080)
                .usePlaintext()
                .build();
        stub = ServiceGrpc.newBlockingStub(channel);
    }

    public static ServiceGrpc.ServiceBlockingStub getConnection() {
        if(stub == null){
            initConnection();
        }
        return stub;
    }

}
