package tecnico.sec.server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
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

public class ServerInfo {
    private static List<Server> serverList;

    public static List<Server> getServerList(){
        if(serverList == null){
            File myObj = new File("serversInfo.txt");
            try {
            Scanner myReader = new Scanner(myObj);
            List<Server> list = new ArrayList<Server>();
            while (myReader.hasNextLine()) {
                String[] data = myReader.nextLine().split("-");
                    PublicKey publicKey = KeyStore.stringToPublicKey(data[1]);
                    list.add(new Server(publicKey, initConnection("host.docker.internal" , Integer.parseInt(data[0]))));
            }
            myReader.close();
            serverList = list;
            } catch (BaseException e) {
                Status status = Status.fromThrowable(e);
                System.out.println("ERROR : " + status.getCode() + " : " + status.getDescription());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return serverList;
    }

    private static ServiceGrpc.ServiceStub initConnection(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        return ServiceGrpc.newStub(channel);
    }

    public static void serverPublicKeyExists(PublicKey publicKey) throws KeyExceptions.InvalidPublicKeyException {
        getServerList();
        for(Server s : serverList){
            if(publicKey.equals(s.getPublicKey())){
                return;
            }
        }
        throw new KeyExceptions.InvalidPublicKeyException();
    }

}

