package tecnico.sec.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import org.javatuples.Pair;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.grpc.ServiceGrpc;
import tecnico.sec.proto.exceptions.BaseException;
import tecnico.sec.proto.exceptions.KeyExceptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServerConnection {

    private static List<Pair<ServiceGrpc.ServiceStub, PublicKey>> servers;
    private static int count;

    private static void initConnection(){
        servers = new ArrayList<>();
        try {
            File myObj = new File("servers.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String[] data = myReader.nextLine().split(" ");
                try {
                    PublicKey publicKey = KeyStore.stringToPublicKey(data[2]);
                    servers.add(Pair.with(replicaConnection(data[0], Integer.parseInt(data[1])), publicKey));
                } catch (BaseException e) {
                    Status status = Status.fromThrowable(e);
                    System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
                }
            }
            myReader.close();
            count = servers.size();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
    }

    private static ServiceGrpc.ServiceStub replicaConnection(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        return ServiceGrpc.newStub(channel); //todo executors
    }

    public static List<Pair<ServiceGrpc.ServiceStub, PublicKey>> getConnection() {
        if(servers == null){
            initConnection();
        }
        /*
        while( channel.getState(true) != ConnectivityState.READY && channel.getState(true) != ConnectivityState.IDLE){
            System.out.println(channel.getState(true));
            System.out.println("Trying again to connect in 5 seconds...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("Error in wait");
            }
        }*/
        return servers;
    }

    public static int getServerCount(){
        return count;
    }

}
