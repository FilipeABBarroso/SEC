package dbController;

import tecnico.sec.KeyStore.singletons.KeyStore;

import java.util.Arrays;

public class Transaction {
    byte[] publicKeySender;
    byte[] getPublicKeyReceiver;
    int amount;
    int id;

    public Transaction(byte[] publicKeySender, byte[] getPublicKeyReceiver, int amount, int id) {
        this.publicKeySender = publicKeySender;
        this.getPublicKeyReceiver = getPublicKeyReceiver;
        this.amount = amount;
        this.id = id;
    }

    @Override
    public String toString() {
        return "[" + id + "]" + " amount: " + amount + "\nFrom: " +
                KeyStore.byteArrayToString(publicKeySender) + "\nTo: " + KeyStore.byteArrayToString(getPublicKeyReceiver) + "\n";
    }
}
