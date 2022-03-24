package tecnico.sec.client;

import java.security.KeyFactory;
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
        Client client = initClient("");
        while(true){
            Scanner in = new Scanner(System.in);
            System.out.println("""
                    Select what you want to do:
                    1.Send Money
                    2.Check Balance
                    3.Receive Money
                    4.Audit Account
                    0.Leave""");
            int selected = in.nextInt();
            switch (selected) {
                case 1 -> {
                    send_amount_request(client);
                }
                case 2 -> {
                    check_account_request(client);
                }
                case 3 -> {
                    receive_amount(client.getPublicKey());
                }
                case 4 -> {
                    audit(client.getPublicKey());
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

    public static void send_amount_request(Client client){
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

        send_amount(client.getPublicKey(), destinationPubKey, amount);
    }

    public static void check_account_request(Client client) {
        int balance = check_account(client.getPublicKey());
        System.out.println("Your balance is " + balance + "Gs\n");
        //TODO display incoming transfers
    }
}
