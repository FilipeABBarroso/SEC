package tecnico.sec.client;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

import static tecnico.sec.client.Client.*;

public class Main {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println("Welcome to BFTB!\n");
        Client client = new Client("localhost",8080);
        KeyPair clientKeys = initCredentials("");
        while(true){
            Scanner in = new Scanner(System.in);
            System.out.println("""
                    Select what you want to do:
                    0.Open Account
                    1.Send Money
                    2.Check Balance
                    3.Receive Money
                    4.Audit Account
                    5.Leave""");
            int selected = in.nextInt();
            switch (selected) {
                case 0 -> {
                    client.open_account(clientKeys.getPublic());
                }
                case 1 -> {
                    send_amount_request(client, clientKeys);
                }
                case 2 -> {
                    client.check_account(clientKeys.getPublic());
                }
                case 3 -> {
                    receive_amount_request(client, clientKeys);
                }
                case 4 -> {
                    client.audit(clientKeys.getPublic());
                }
                default -> {
                    System.out.println("Goodbye!");
                    return;
                }
            }
        }
    }

    public static PublicKey stringToPubKey(String pubKeyString) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicBytes = Base64.getDecoder().decode(pubKeyString);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static String pubKeyToString(PublicKey pubKey) {
        return Base64.getEncoder().encodeToString(pubKey.getEncoded());
    }

    public static void send_amount_request(Client client, KeyPair clientKeys){
        Scanner in = new Scanner(System.in);
        System.out.println("Send to who?");
        String destination = in.nextLine();
        PublicKey destinationPubKey = null;
        try {
            destinationPubKey = stringToPubKey(destination);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("Public key not valid!");
            return;
        }
        System.out.println("How much you want to send?");
        int amount = in.nextInt();

        client.send_amount(clientKeys.getPublic(), destinationPubKey, amount);
    }

    public static void receive_amount_request(Client client, KeyPair clientKeys) {
        Scanner in = new Scanner(System.in);
        System.out.println("What transactionID you want to receive?");
        int transactionID = in.nextInt();
        client.receive_amount(clientKeys.getPublic(), transactionID);
    }
}
