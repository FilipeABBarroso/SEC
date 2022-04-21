package tecnico.sec.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.proto.exceptions.KeyExceptions;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        KeyStore.changeKeyPath("server");
        try {
            System.out.println(KeyStore.publicKeyToString(KeyStore.getCredentials().getPublic()));
        } catch (KeyExceptions.GeneralKeyStoreErrorException e) {
            System.out.println("Error trying to get credentials...");
            return;
        }
        Server server = ServerBuilder
                .forPort(8080)
                .addService(new ServiceImpl())
                .build();
        server.start();
        System.out.println("Running...");
        server.awaitTermination();
    }
}
