package tecnico.sec.client;


import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import java.util.Scanner;

import com.google.protobuf.ProtocolStringList;
import org.javatuples.Pair;
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
                case 0 -> open_account_request();
                case 1 -> send_amount_request();
                case 2 -> check_account_request();
                case 3 -> receive_amount_request();
                case 4 -> audit_request();
                default -> {
                    System.out.println("Goodbye!");
                    return;
                }
            }
        }
    }

    public static void open_account_request(){
        if(Client.open_account(KeyStore.getPublicKey())){
            System.out.println("Account opened!");
        } else {
            System.out.println("Failed to open account!");
        }
    }

    public static void send_amount_request() {
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

        if(Client.send_amount(KeyStore.getPublicKey(), destinationPubKey, amount)){
            System.out.println("Amount sent!");
        } else {
            System.out.println("Failed to send amount!");
        }
    }

    public static void receive_amount_request() {
        Scanner in = new Scanner(System.in);
        System.out.println("What transactionID you want to receive?");
        int transactionID = in.nextInt();

        if(Client.receive_amount(KeyStore.getPublicKey(), transactionID)) {
            System.out.println("Amount received!");
        } else {
            System.out.println("Failed to receive amount!");
        }
    }

    public static void check_account_request() {
        Pair<Integer,ProtocolStringList> res = Client.check_account(KeyStore.getPublicKey());
        if (res!=null) {
            System.out.println("Balance : " + res.getValue0());
            listTransactions(res.getValue1());
        } else {
            System.out.println("Failed to check account!");
        }
    }

    public static void audit_request() {
        ProtocolStringList res = Client.audit(KeyStore.getPublicKey());
        if(res != null){
            listTransactions(res);
        } else {
            System.out.println("Failed to audit account!");
        }
    }

    private static void listTransactions(ProtocolStringList transactions) {
        transactions.forEach(System.out::println);
    }
}
