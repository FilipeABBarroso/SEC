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
        while( channel.getState(true) != ConnectivityState.READY && channel.getState(true) != ConnectivityState.IDLE){
            System.out.println(channel.getState(true));
            System.out.println("Trying again to connect in 10 seconds...");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("Error in wait");
            }
        }
        return stub;
    }

}
