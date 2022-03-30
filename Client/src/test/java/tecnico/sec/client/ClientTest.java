package tecnico.sec.client;

import org.junit.jupiter.api.Test;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.proto.exceptions.KeyExceptions;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    private final KeyPair keys;

    ClientTest() throws KeyExceptions.NoSuchAlgorithmException {
        keys = KeyStore.getCredentials();
    }

    @Test
    void open_account() {
        assertTrue(Client.open_account(keys.getPublic()));
    }

    @Test
    void send_amount() {

    }

    @Test
    void receive_amount() {
    }

    @Test
    void check_account() {
    }

    @Test
    void audit() {
    }

}