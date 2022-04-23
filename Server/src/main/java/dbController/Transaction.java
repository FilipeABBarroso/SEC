package dbController;

import com.google.protobuf.ByteString;
import tecnico.sec.KeyStore.singletons.KeyStore;

import java.util.Arrays;

public class Transaction {
    byte[] publicKeySender;
    byte[] getPublicKeyReceiver;
    int amount;
    int id;

    public Transaction(byte[] publicKeySender, byte[] getPublicKeyReceiver, int amount, int id) {
        byte[] dummyArray = {0};
        if (publicKeySender == null) {
            this.publicKeySender = dummyArray;
        } else {
            this.publicKeySender = publicKeySender;
        }
        this.getPublicKeyReceiver = getPublicKeyReceiver;
        this.amount = amount;
        this.id = id;
    }

    public tecnico.sec.grpc.Transaction toTransactionGrpc() {

        return tecnico.sec.grpc.Transaction.newBuilder()
                .setId(id)
                .setAmount(amount)
                .setSender(ByteString.copyFrom(publicKeySender))
                .setReceiver(ByteString.copyFrom(getPublicKeyReceiver)).build();
    }

    @Override
    public String toString() {
        return "[" + id + "]" + " amount: " + amount + "\nFrom: " +
                KeyStore.byteArrayToString(publicKeySender) + "\nTo: " + KeyStore.byteArrayToString(getPublicKeyReceiver) + "\n";
    }
}
