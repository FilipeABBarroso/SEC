package tecnico.sec.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import tecnico.sec.grpc.*;

import static tecnico.sec.client.Main.*;
import static tecnico.sec.client.Main.pubKeyToString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Client{

    private final ServiceGrpc.ServiceBlockingStub stub;

    //TODO handle exceptions

    public Client(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = ServiceGrpc.newBlockingStub(channel);
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

    public static KeyPair initCredentials(String path) throws NoSuchAlgorithmException {
        KeyPair clientKeys;
        try {
            System.out.println("Searching for credentials...");
            clientKeys = LoadKeyPair(path);
            System.out.println("Credentials Loaded!\n");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("Not found! Creating new credentials...\n");
            clientKeys = generateCredentials();
            saveKeyPair(clientKeys,path);
        }
        return clientKeys;
    }

    public void open_account(PublicKey key){
        //TODO
        OpenAccountResponse openResponse = stub.openAccount(OpenAccountRequest.newBuilder()
                .setPublicKey(pubKeyToString(key))
                .setHMAC("HMAC")
                .build());
    }

    public void send_amount(PublicKey source, PublicKey destination, int amount){
        //TODO
        SendAmountResponse sendAmountResponse = stub.sendAmount(SendAmountRequest.newBuilder()
                .setPublicKeySource(pubKeyToString(source))
                .setPublicKeyDestination(pubKeyToString(destination))
                .setAmount(amount)
                .build());
    }

    public void check_account(PublicKey key){
        //TODO
        CheckAccountResponse checkAccountResponse = stub.checkAccount(CheckAccountRequest.newBuilder()
                .setPublicKey(pubKeyToString(key))
                .build());
    }

    public void receive_amount(PublicKey key,int transactionID){
        //TODO
        ReceiveAmountResponse receiveAmountResponse = stub.receiveAmount(ReceiveAmountRequest.newBuilder()
                .setPublicKey(pubKeyToString(key))
                .setTransactionID(transactionID)
                .build());
    }

    public void audit(PublicKey key){
        //TODO
        AuditResponse auditResponse = stub.audit(AuditRequest.newBuilder()
                .setPublicKey(pubKeyToString(key))
                .build());

    }

}

