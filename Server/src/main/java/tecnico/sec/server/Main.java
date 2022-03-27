package tecnico.sec.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder
                .forPort(53467)
                .addService(new ServiceImpl())
                .build();
        server.start();
        server.awaitTermination();
    }
}
