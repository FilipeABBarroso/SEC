package tecnico.sec.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.proto.exceptions.KeyExceptions;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, KeyExceptions.GeneralKeyStoreErrorException {
        KeyStore.changeKeyPath("server/");
        KeyStore.getCredentials();
        Server server = ServerBuilder
                .forPort(8080)
                .addService(new ServiceImpl())
                .build();
        server.start();
        server.awaitTermination();
    }
}
