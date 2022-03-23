package tecnico.sec.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;

public class Client{

    private final KeyPair keyPair;

    //TODO handle exceptions

    public Client(KeyPair pair) {
        this.keyPair = pair;
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

    public static KeyPair generate_credentials() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        return keyGen.generateKeyPair();
    }

    public static void saveKeyPair(KeyPair clientKeyPair){
        try {
            PrivateKey privateKey = clientKeyPair.getPrivate();
            PublicKey publicKey = clientKeyPair.getPublic();

            // Store Public Key.
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
                    publicKey.getEncoded());
            FileOutputStream fos = new FileOutputStream( "public.key");
            fos.write(x509EncodedKeySpec.getEncoded());
            fos.close();

            // Store Private Key.
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                    privateKey.getEncoded());
            fos = new FileOutputStream("private.key");
            fos.write(pkcs8EncodedKeySpec.getEncoded());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static KeyPair LoadKeyPair() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Read Public Key.
        File filePublicKey = new File("public.key");
        FileInputStream fis = new FileInputStream("public.key");
        byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
        fis.read(encodedPublicKey);
        fis.close();

        // Read Private Key.
        File filePrivateKey = new File("private.key");
        fis = new FileInputStream("private.key");
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

    public static Client initClient() throws NoSuchAlgorithmException {
        Client client;
        try {
            System.out.println("Searching for account...");
            client = new Client(LoadKeyPair());
            System.out.println("Account Loaded!\n");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("No account found! Creating a new account...\n");
            client = new Client(generate_credentials());
            open_account(client.getPublicKey());
            saveKeyPair(client.getKeyPair());
        }
        return client;
    }

    public static void open_account(PublicKey key){
        //TODO
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();
        //AccountServiceGrpc.AccountServiceBlockingStub stub = AccountServiceGrpc.newBlockingStub(channel);
    }

    public static void send_amount(PublicKey source, PublicKey destination, int amount){
        //TODO
    }

    public static int check_account(PublicKey key){
        return 0;
        //TODO
    }

    public static void receive_amount(PublicKey key){
        //TODO
    }

    public static void audit(PublicKey key){
        //TODO
    }

}

