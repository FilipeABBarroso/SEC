package tecnico.sec.client;


import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import java.util.Scanner;

import tecnico.sec.KeyStore.singletons.KeyStore;

import tecnico.sec.proto.exceptions.KeyExceptions;

public class Main {

    public static void main(String[] args) {
        System.out.println("Welcome to BFTB!\n");
        try {
            KeyStore.getCredentials();
        } catch (KeyExceptions.NoSuchAlgorithmException e) {
            System.out.println("Error trying to get credentials...");
            return;
        }
        while (true) {
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
                case 0 -> Client.open_account(KeyStore.getPublicKey());
                case 1 -> send_amount_request(KeyStore.getPublicKey());
                case 2 -> Client.check_account(KeyStore.getPublicKey());
                case 3 -> receive_amount_request(KeyStore.getPublicKey());
                case 4 -> Client.audit(KeyStore.getPublicKey());
                default -> {
                    System.out.println("Goodbye!");
                    return;
                }
            }
        }
    }

    public static void send_amount_request(PublicKey pubKey) {
        Scanner in = new Scanner(System.in);
        System.out.println("Send to who?");
        String destination = in.nextLine();
        PublicKey destinationPubKey;
        try {
            destinationPubKey = KeyStore.stringToPublicKey(destination);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("Public key not valid!");
            return;
        }
        System.out.println("How much you want to send?");
        int amount = in.nextInt();

        Client.send_amount(pubKey, destinationPubKey, amount);
    }

    public static void receive_amount_request(PublicKey pubKey) {
        Scanner in = new Scanner(System.in);
        System.out.println("What transactionID you want to receive?");
        int transactionID = in.nextInt();
        Client.receive_amount(pubKey, transactionID);
    }
}
