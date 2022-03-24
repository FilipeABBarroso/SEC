package tecnico.sec.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import tecnico.sec.grpc.*;

import static tecnico.sec.client.Main.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

public class Client{

    private final KeyPair keyPair;
    private final ServiceGrpc.ServiceBlockingStub stub;


    //TODO handle exceptions

    public Client(KeyPair pair) {
        this.keyPair = pair;
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();
        this.stub = ServiceGrpc.newBlockingStub(channel);
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public static void saveKeyPair(KeyPair clientKeyPair, String path){
        try {
            PrivateKey privateKey = clientKeyPair.getPrivate();
            PublicKey publicKey = clientKeyPair.getPublic();

            // Store Public Key.
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
                    publicKey.getEncoded());
            FileOutputStream fos = new FileOutputStream( path + "public.key");
            fos.write(x509EncodedKeySpec.getEncoded());
            fos.close();

            // Store Private Key.
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                    privateKey.getEncoded());
            fos = new FileOutputStream(path + "private.key");
            fos.write(pkcs8EncodedKeySpec.getEncoded());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static KeyPair LoadKeyPair(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Read Public Key.
        File filePublicKey = new File(path + "public.key");
        FileInputStream fis = new FileInputStream(path + "public.key");
        byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
        fis.read(encodedPublicKey);
        fis.close();

        // Read Private Key.
        File filePrivateKey = new File(path + "private.key");
        fis = new FileInputStream(path + "private.key");
        byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
        fis.read(encodedPrivateKey);
        fis.close();

        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    public static KeyPair generateCredentials() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        return keyGen.generateKeyPair();
    }

    public static Client initClient(String path) throws NoSuchAlgorithmException {
        Client client;
        try {
            System.out.println("Searching for account...");
            client = new Client(LoadKeyPair(path));
            System.out.println("Account Loaded!\n");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("No account found! Creating a new account...\n");
            client = new Client(generateCredentials());
            client.open_account(client.getPublicKey());
            saveKeyPair(client.getKeyPair(),path);
        }
        return client;
    }

    public void open_account(PublicKey key){
        //TODO channel to attribute

        OpenAccountResponse openResponse = stub.openAccount(OpenAccountRequest.newBuilder()
                .setPublicKey(pubKeyToString(key))
                .setHMAC("HMAC")
                .build());
    }

    public void send_amount(PublicKey source, PublicKey destination, int amount){
        //TODO
    }

    public int check_account(PublicKey key){
        return 0;
        //TODO
    }

    public void receive_amount(PublicKey key){
        //TODO
    }

    public void audit(PublicKey key){
        //TODO
    }

}

