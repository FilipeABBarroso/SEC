package tecnico.sec.client;

import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import static tecnico.sec.client.Client.*;


public class Main {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println("Welcome to BFTB!\n");
        Client client = initClient();
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
                    System.out.println("How much you want to send?");
                    int amount = in.nextInt();
                    // TODO get account to send
                    send_amount(client.getPublicKey(), client.getPublicKey(), amount);
                }
                case 2 -> {
                    int balance = check_account(client.getPublicKey());
                    System.out.println("Your balance is " + balance + "Gs\n");
                    //TODO display incoming transfers
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
}
