package dbController;

import com.google.protobuf.ByteString;
import tecnico.sec.KeyStore.singletons.KeyStore;

import java.util.Arrays;

public class Transaction {
    byte[] publicKeySender;
    int amount;
    byte[] publicKeyReceiver;
    long nonce;
    byte[] signature;
    String status;
    int senderTransactionId;
    int receiverTransactionId;

    public Transaction(byte[] publicKeySender, byte[] publicKeyReceiver, int amount, int senderTransactionId) {
        byte[] dummyArray = {0};
        if (publicKeySender == null) {
            this.publicKeySender = dummyArray;
        } else {
            this.publicKeySender = publicKeySender;
        }
        this.publicKeyReceiver = publicKeyReceiver;
        this.amount = amount;
        this.senderTransactionId = senderTransactionId;
    }

    public Transaction(byte[] publicKeySender, byte[] publicKeyReceiver, int amount, long nonce, byte[] signature, String status, int senderTransactionId, int receiverTransactionId) {
        byte[] dummyArray = {0};
        if (publicKeySender == null) {
            this.publicKeySender = dummyArray;
        } else {
            this.publicKeySender = publicKeySender;
        }
        if (signature == null) {
            this.signature = dummyArray;
        } else {
            this.signature = signature;
        }
        this.publicKeyReceiver = publicKeyReceiver;
        this.amount = amount;
        this.senderTransactionId = senderTransactionId;
        this.nonce = nonce;
        this.status = status;
        this.receiverTransactionId = receiverTransactionId;
    }

    public byte[] getPublicKeySender() {
        return publicKeySender;
    }

    public int getAmount() {
        return amount;
    }

    public byte[] getPublicKeyReceiver() {
        return publicKeyReceiver;
    }

    public long getNonce() {
        return nonce;
    }

    public byte[] getSignature() {
        return signature;
    }

    public String getStatus() {
        return status;
    }

    public int getSenderTransactionId() {
        return senderTransactionId;
    }

    public int getReceiverTransactionId() {
        return receiverTransactionId;
    }

    public tecnico.sec.grpc.Transaction toTransactionGrpc() {

        return tecnico.sec.grpc.Transaction.newBuilder()
                .setIdSender(senderTransactionId)
                .setAmount(amount)
                .setSender(ByteString.copyFrom(publicKeySender))
                .setReceiver(ByteString.copyFrom(publicKeyReceiver)).build();
    }

    public tecnico.sec.grpc.Transaction toDetailedTransactionGrpc() {

        return tecnico.sec.grpc.Transaction.newBuilder()
                .setAccepted(status == "Completed" ? true : false)
                .setIdSender(senderTransactionId)
                .setIdReceiver(receiverTransactionId)
                .setAmount(amount)
                .setSignature(ByteString.copyFrom(signature))
                .setNonce(nonce)
                .setSender(ByteString.copyFrom(publicKeySender))
                .setReceiver(ByteString.copyFrom(publicKeyReceiver)).build();
    }

    @Override
    public String toString() {
        return "[" + senderTransactionId + "]" + " amount: " + amount + "\nFrom: " +
                KeyStore.byteArrayToString(publicKeySender) + "\nTo: " + KeyStore.byteArrayToString(publicKeyReceiver) + "\n";
    }
}
