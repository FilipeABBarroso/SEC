package tecnico.sec.client;


import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import java.util.Base64;
import java.util.Scanner;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import org.javatuples.Pair;
import tecnico.sec.KeyStore.singletons.KeyStore;

import tecnico.sec.proto.exceptions.KeyExceptions;

public class Main {

    public static void main(String[] args) {
        KeyStore.changeKeyPath("client/");
        System.out.println("Welcome to BFTB!\n");
        try {
            System.out.println(KeyStore.publicKeyToString(KeyStore.getCredentials().getPublic()));
        } catch (KeyExceptions.GeneralKeyStoreErrorException e) {
            System.out.println("Error trying to get credentials...");
            return;
        }
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.print("""
                    1.Open Account
                    2.Send Amount
                    3.Check Balance
                    4.Receive Amount
                    5.Audit Account
                    0.Leave
                    Select what you want to do:""");
            try {
                String selected = in.nextLine();
                switch (Integer.parseInt(selected)) {
                    case 0 ->  {
                        System.out.println("Goodbye!");
                        return;
                    }
                    case 1 -> open_account_request();
                    case 2 -> send_amount_request();
                    case 3 -> check_account_request();
                    case 4 -> receive_amount_request();
                    case 5 -> audit_request();
                    default -> {
                        System.out.println("\nBad Input!\n");
                    }
                }
            } catch (Exception e){
                System.out.println("\nBad Input!\n");
            }

        }
    }

    public static void open_account_request(){
        try {
            if(Client.open_account(KeyStore.getPublicKey())){
                System.out.println("Account opened!");
            } else {
                System.out.println("Failed to open account!");
            }
        } catch (KeyExceptions.GeneralKeyStoreErrorException e) {
            e.printStackTrace();
        }
    }

    public static void send_amount_request() {
        Scanner in = new Scanner(System.in);
        System.out.println("Send to who?");
        String destination = in.nextLine();
        PublicKey destinationPubKey;
        try {
            destinationPubKey = KeyStore.stringToPublicKey(destination);
        } catch (KeyExceptions.GeneralKeyStoreErrorException e) {
            System.out.println("Public key not valid!");
            return;
        }
        System.out.println("How much you want to send?");
        int amount = in.nextInt();

        try {
            if(Client.send_amount(KeyStore.getPublicKey(), destinationPubKey, amount)){
                System.out.println("Amount sent!");
            } else {
                System.out.println("Failed to send amount!");
            }
        } catch (KeyExceptions.GeneralKeyStoreErrorException e) {
            e.printStackTrace();
        }
    }

    public static void receive_amount_request() {
        Scanner in = new Scanner(System.in);
        System.out.println("What transactionID you want to receive?");
        int transactionID = in.nextInt();

        try {
            if(Client.receive_amount(KeyStore.getPublicKey(), transactionID)) {
                System.out.println("Amount received!");
            } else {
                System.out.println("Failed to receive amount!");
            }
        } catch (KeyExceptions.GeneralKeyStoreErrorException e) {
            e.printStackTrace();
        }
    }

    public static void check_account_request() {
        Pair<Integer,ProtocolStringList> res = null;
        try {
            res = Client.check_account(KeyStore.getPublicKey());
        } catch (KeyExceptions.GeneralKeyStoreErrorException e) {
            e.printStackTrace();
        }
        if (res!=null) {
            System.out.println("Balance : " + res.getValue0());
            listTransactions(res.getValue1());
        } else {
            System.out.println("Failed to check account!");
        }
    }

    public static void audit_request() {
        ProtocolStringList res = null;
        try {
            res = Client.audit(KeyStore.getPublicKey());
        }catch (KeyExceptions.GeneralKeyStoreErrorException e) {
            e.printStackTrace();
        }
        if(res != null){
            listTransactions(res);
        } else {
            System.out.println("Failed to audit account!");
        }
    }

    private static void listTransactions(ProtocolStringList transactions) {
        if(transactions.isEmpty()){
            System.out.println("No transactions!");
            return;
        }
        transactions.forEach(System.out::println);
    }
}
