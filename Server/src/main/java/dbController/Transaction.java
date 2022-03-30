package dbController;

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

    public byte[] getPublicKeySender() {
        return publicKeySender;
    }

    public byte[] getGetPublicKeyReceiver() {
        return getPublicKeyReceiver;
    }

    public int getAmount() {
        return amount;
    }

    public int getId() {
        return id;
    }
}
