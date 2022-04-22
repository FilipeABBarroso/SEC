package tecnico.sec.server;

import tecnico.sec.grpc.ServiceGrpc;

import java.security.PublicKey;

public final class Server {
    private final PublicKey publicKey;
    private final ServiceGrpc.ServiceStub connection;

    public Server(PublicKey publicKey, ServiceGrpc.ServiceStub connection) {
        this.publicKey = publicKey;
        this.connection = connection;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public ServiceGrpc.ServiceStub getConnection() {
        return connection;
    }
}
