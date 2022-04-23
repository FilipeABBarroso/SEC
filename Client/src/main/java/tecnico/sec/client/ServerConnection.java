package tecnico.sec.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import org.javatuples.Pair;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.grpc.ServiceGrpc;
import tecnico.sec.proto.exceptions.BaseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class ServerInfo{

    private final ServiceGrpc.ServiceStub stub;
    private final String host;
    private final int port;
    private final PublicKey publicKey;

    public ServerInfo(String host, int port, PublicKey publicKey) {
        this.host = host;
        this.port = port;
        this.publicKey = publicKey;
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = ServiceGrpc.newStub(channel);
    }


    public ServiceGrpc.ServiceStub getStub() {
        return stub;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}

public class ServerConnection {

    private static List<ServerInfo> servers;
    private static int count;

    private static void initConnection(){
        servers = new ArrayList<>();
        try {
            File myObj = new File("GeneratedKeyStores/serversInfo.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String[] data = myReader.nextLine().split("-");
                try {
                    PublicKey publicKey = KeyStore.stringToPublicKey(data[1]);
                    servers.add(new ServerInfo("localhost",Integer.parseInt(data[0]),publicKey));
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

    public static List<ServerInfo> getConnection() {
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

    public static boolean existsPublicKey(PublicKey publicKey) {
        for(ServerInfo s : servers) {
            if(publicKey.equals(s.getPublicKey())) {
                return true;
            }
        }
        return false;
    }

}
