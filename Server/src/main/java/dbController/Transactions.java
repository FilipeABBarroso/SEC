package dbController;

import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;
import tecnico.sec.proto.exceptions.TransactionsExceptions;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Transactions {

    synchronized public static void addTransaction(byte[] publicKeySender, byte[] publicKeyReceiver, int amount, long nonce, byte[] signature) throws NonceExceptions.NonceNotFoundException, TransactionsExceptions.FailInsertTransactionException, TransactionsExceptions.SenderPublicKeyNotFoundException, BalanceExceptions.PublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.PublicKeyNotFoundException, TransactionsExceptions.BalanceNotEnoughException, TransactionsExceptions.ReceiverPublicKeyNotFoundException, TransactionsExceptions.AmountCanNotBeLessThenOneException, TransactionsExceptions.CanNotSendMoneyToYourselfException {
        if(amount <= 0 ){
            throw new TransactionsExceptions.AmountCanNotBeLessThenOneException();
        }
        if(Arrays.equals(publicKeySender,publicKeyReceiver)){
            throw new TransactionsExceptions.CanNotSendMoneyToYourselfException();
        }

        Connection conn = DBConnection.getConnection();

        try {


            // Check if sender has enough
            PreparedStatements.getCheckBalancePS().setBytes(1, publicKeySender);
            ResultSet checkBalanceRS = PreparedStatements.getCheckBalancePS().executeQuery();
            if(!checkBalanceRS.next()) {
                throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
            } else {
                if (checkBalanceRS.getInt("balance") < amount){
                    throw new TransactionsExceptions.BalanceNotEnoughException();
                }
            }

            conn.setAutoCommit(false);

            int senderTransactionId = getLastTransactionId() + 1;
            //PreparedStatements.getAddTransaction().setInt(1, senderTransactionId);
            PreparedStatements.getAddTransaction().setBytes(1, publicKeySender);
            PreparedStatements.getAddTransaction().setBytes(2, publicKeyReceiver);
            PreparedStatements.getAddTransaction().setInt(3, amount);
            PreparedStatements.getAddTransaction().setString(4, "Pending");
            PreparedStatements.getAddTransaction().setLong(5, nonce);
            PreparedStatements.getAddTransaction().setBytes(6, signature);
            PreparedStatements.getAddTransaction().setInt(7, 0);
            PreparedStatements.getAddTransaction().setInt(8, senderTransactionId);
            if(PreparedStatements.getAddTransaction().executeUpdate() == 0) {
                throw new TransactionsExceptions.FailInsertTransactionException();
            }

            // update sender balance
            int updatedBalance = Balance.getBalance(publicKeySender) - amount;
            PreparedStatements.getUpdateBalancePS().setInt(1, updatedBalance);
            PreparedStatements.getUpdateBalancePS().setInt(2, senderTransactionId);
            PreparedStatements.getUpdateBalancePS().setBytes(3, publicKeySender);
            if (PreparedStatements.getUpdateBalancePS().executeUpdate() == 0) {
                throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
            }

            // remove sender nonce
            PreparedStatements.getRemoveNoncePS().setBytes(1, publicKeySender);
            PreparedStatements.getRemoveNoncePS().executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            System.out.println(e);
            if (e.getSQLState().equals(Constants.FOREIGN_KEY_DONT_EXISTS)) {
                if (e.getMessage().contains("receiver")){
                    throw new TransactionsExceptions.ReceiverPublicKeyNotFoundException();
                } else {
                    throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
                }

            } else {
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
    }

    synchronized public static void changeStatus(int id, byte[] publicKeyReceiver) throws NonceExceptions.NonceNotFoundException, TransactionsExceptions.TransactionIDNotFoundException, TransactionsExceptions.ReceiverPublicKeyNotFoundException, BalanceExceptions.PublicKeyNotFoundException, TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.TransactionAlreadyAcceptedException, TransactionsExceptions.PublicKeyNotFoundException {
        try {
            Balance.getBalance(publicKeyReceiver);
        }catch (BalanceExceptions.PublicKeyNotFoundException e ){
            throw new TransactionsExceptions.PublicKeyNotFoundException();
        }

        Connection conn = DBConnection.getConnection();

        try {
            // get receiver public key and amount in the transaction
            PreparedStatements.getReceiverPKAndAmountPS().setInt(1, id);
            ResultSet pkAndAmountRs = PreparedStatements.getReceiverPKAndAmountPS().executeQuery();
            if (!pkAndAmountRs.next()) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }
            if (!Arrays.equals(publicKeyReceiver, pkAndAmountRs.getBytes("publicKeyReceiver"))) {
                throw new TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException();
            }
            if (pkAndAmountRs.getString("status").equals("Completed")) {
                throw new TransactionsExceptions.TransactionAlreadyAcceptedException();
            }

            conn.setAutoCommit(false);

            int receiverTransactionId = getLastTransactionId() + 1;

            // change transaction status to completed
            PreparedStatements.getUpdateTransactionPS().setString(1, "Completed");
            PreparedStatements.getUpdateTransactionPS().setInt(2, receiverTransactionId);
            PreparedStatements.getUpdateTransactionPS().setInt(3, id);
            if (PreparedStatements.getUpdateTransactionPS().executeUpdate() == 0) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }

            // update receiver balance
            int updatedBalance = Balance.getBalance(pkAndAmountRs.getBytes("publicKeyReceiver")) + pkAndAmountRs.getInt("amount");
            PreparedStatements.getUpdateBalancePS().setInt(1, updatedBalance);
            PreparedStatements.getUpdateBalancePS().setInt(2, receiverTransactionId);
            PreparedStatements.getUpdateBalancePS().setBytes(3, pkAndAmountRs.getBytes("publicKeyReceiver"));
            if (PreparedStatements.getUpdateBalancePS().executeUpdate() == 0) {
                throw new TransactionsExceptions.ReceiverPublicKeyNotFoundException();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    synchronized public static List<tecnico.sec.grpc.Transaction> getPendingTransactions(byte[] publicKey) throws TransactionsExceptions.ReceiverPublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.PublicKeyNotFoundException {
        List<tecnico.sec.grpc.Transaction> list = new ArrayList<>();
        try {
            Balance.getBalance(publicKey);
        }catch (BalanceExceptions.PublicKeyNotFoundException e ){
            throw new TransactionsExceptions.PublicKeyNotFoundException();
        }

        try {
            PreparedStatements.getPendingTransactionPS().setBytes(1, publicKey);
            PreparedStatements.getPendingTransactionPS().setString(2, "Pending");
            ResultSet rs = PreparedStatements.getPendingTransactionPS().executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction(rs.getBytes("publicKeySender"), rs.getBytes("publicKeySender"),
                        rs.getInt("amount"), rs.getInt("senderTransactionId"));
                list.add(t.toTransactionGrpc());
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
        return list;
    }

    synchronized public static List<tecnico.sec.grpc.Transaction> getTransactions(byte[] publicKey) throws TransactionsExceptions.ReceiverPublicKeyNotFoundException, TransactionsExceptions.PublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException {
        ArrayList<tecnico.sec.grpc.Transaction> list = new ArrayList<>();

        try {
            PreparedStatements.getAllTransactionPS().setBytes(1, publicKey);
            PreparedStatements.getAllTransactionPS().setBytes(2, publicKey);
            ResultSet rs = PreparedStatements.getAllTransactionPS().executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction(rs.getBytes("publicKeySender"), rs.getBytes("publicKeyReceiver"),
                        rs.getInt("amount"), rs.getInt("senderTransactionId"));
                list.add(t.toTransactionGrpc());
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
        return list;
    }

    synchronized public static void updateReceiverTransactionId(int id, int receiverTransactionId) throws BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.ReceiverPublicKeyNotFoundException {
        try {
            PreparedStatements.getUpdateReceiverTransactionIdPS().setInt(1, receiverTransactionId);
            PreparedStatements.getUpdateReceiverTransactionIdPS().setInt(2, id);
            if (PreparedStatements.getUpdateReceiverTransactionIdPS().executeUpdate() == 0) {
                throw new TransactionsExceptions.ReceiverPublicKeyNotFoundException();
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    synchronized public static void updateSenderTransactionId(int id, int receiverTransactionId) throws BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.TransactionIDNotFoundException {
        try {
            PreparedStatements.getUpdateSenderTransactionIdPS().setInt(1, receiverTransactionId);
            PreparedStatements.getUpdateSenderTransactionIdPS().setInt(2, id);
            if (PreparedStatements.getUpdateSenderTransactionIdPS().executeUpdate() == 0) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    synchronized public static void changeMissingStatus(int id, byte[] publicKeyReceiver, int receiverTransactionId) throws NonceExceptions.NonceNotFoundException, TransactionsExceptions.TransactionIDNotFoundException, TransactionsExceptions.ReceiverPublicKeyNotFoundException, BalanceExceptions.PublicKeyNotFoundException, TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.TransactionAlreadyAcceptedException, TransactionsExceptions.PublicKeyNotFoundException {
        try {
            Balance.getBalance(publicKeyReceiver);
        }catch (BalanceExceptions.PublicKeyNotFoundException e ){
            throw new TransactionsExceptions.PublicKeyNotFoundException();
        }

        Connection conn = DBConnection.getConnection();

        try {
            // get receiver public key and amount in the transaction
            PreparedStatements.getReceiverPKAndAmountPS().setInt(1, id);
            ResultSet pkAndAmountRs = PreparedStatements.getReceiverPKAndAmountPS().executeQuery();
            if (!pkAndAmountRs.next()) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }
            if (!Arrays.equals(publicKeyReceiver, pkAndAmountRs.getBytes("publicKeyReceiver"))) {
                throw new TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException();
            }
            if (pkAndAmountRs.getString("status").equals("Completed")) {
                throw new TransactionsExceptions.TransactionAlreadyAcceptedException();
            }

            conn.setAutoCommit(false);

            // change transaction status to completed
            PreparedStatements.getUpdateTransactionPS().setString(1, "Completed");
            PreparedStatements.getUpdateTransactionPS().setInt(2, receiverTransactionId);
            PreparedStatements.getUpdateTransactionPS().setInt(3, id);
            if (PreparedStatements.getUpdateTransactionPS().executeUpdate() == 0) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }

            // update receiver balance
            int updatedBalance = Balance.getBalance(pkAndAmountRs.getBytes("publicKeyReceiver")) + pkAndAmountRs.getInt("amount");
            PreparedStatements.getUpdateBalancePS().setInt(1, updatedBalance);
            PreparedStatements.getUpdateBalancePS().setInt(2, receiverTransactionId);
            PreparedStatements.getUpdateBalancePS().setBytes(3, pkAndAmountRs.getBytes("publicKeyReceiver"));
            if (PreparedStatements.getUpdateBalancePS().executeUpdate() == 0) {
                throw new TransactionsExceptions.ReceiverPublicKeyNotFoundException();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    synchronized public static int getLastTransactionId() throws BalanceExceptions.GeneralMYSQLException {
        int max;

        try{
            ResultSet rs = PreparedStatements.getTransactionStampsIdPS().executeQuery();
            if (!rs.next()) {
                max = 1;
            } else {
                max = rs.getInt("senderTransactionId");
                int receiverTransactionId = rs.getInt("receiverTransactionId");
                if (max < receiverTransactionId) {
                    max = receiverTransactionId;
                }
            }
            return max;
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    synchronized public static void addMissingTransactions(ArrayList<Transaction> transactions) throws BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.FailInsertTransactionException, TransactionsExceptions.SenderPublicKeyNotFoundException, BalanceExceptions.PublicKeyNotFoundException, TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException, NonceExceptions.NonceNotFoundException, TransactionsExceptions.PublicKeyNotFoundException, TransactionsExceptions.ReceiverPublicKeyNotFoundException, TransactionsExceptions.TransactionAlreadyAcceptedException, TransactionsExceptions.TransactionIDNotFoundException {
        Connection conn = DBConnection.getConnection();

        transactions.sort(Comparator.comparing(Transaction::getSenderTransactionId));
        for (Transaction t: transactions) {
            try{
                conn.setAutoCommit(false);

                PreparedStatements.getTransactionPS().setInt(1, t.getSenderTransactionId());
                ResultSet transaction = PreparedStatements.getTransactionPS().executeQuery();
                if (!transaction.next()) {
                    // add transaction
                    PreparedStatements.getAddTransaction().setBytes(1, t.getPublicKeySender());
                    PreparedStatements.getAddTransaction().setBytes(2, t.getPublicKeyReceiver());
                    PreparedStatements.getAddTransaction().setInt(3, t.getAmount());
                    PreparedStatements.getAddTransaction().setString(4, t.getStatus());
                    PreparedStatements.getAddTransaction().setLong(5, t.getNonce());
                    PreparedStatements.getAddTransaction().setBytes(6, t.getSignature());
                    PreparedStatements.getAddTransaction().setInt(7, t.getReceiverTransactionId());
                    PreparedStatements.getAddTransaction().setInt(8, t.getSenderTransactionId());
                    if(PreparedStatements.getAddTransaction().executeUpdate() == 0) {
                        throw new TransactionsExceptions.FailInsertTransactionException();
                    }

                    // update sender balance
                    PreparedStatements.getBalancePS().setBytes(1, t.getPublicKeySender());
                    ResultSet senderBalanceRS = PreparedStatements.getBalancePS().executeQuery();
                    if (!senderBalanceRS.next()) {
                        throw new BalanceExceptions.PublicKeyNotFoundException();
                    }
                    int updatedSenderBalance = senderBalanceRS.getInt("balance") - t.getAmount();
                    int currentSenderId = senderBalanceRS.getInt("lastTransactionId");
                    PreparedStatements.getUpdateBalancePS().setInt(1, updatedSenderBalance);
                    PreparedStatements.getUpdateBalancePS().setInt(2, currentSenderId > t.getSenderTransactionId() ? currentSenderId : t.getSenderTransactionId());
                    PreparedStatements.getUpdateBalancePS().setBytes(3, t.getPublicKeySender());
                    if (PreparedStatements.getUpdateBalancePS().executeUpdate() == 0) {
                        throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
                    }

                    if (t.getStatus().equals("Completed")) {
                        // update receiver balance
                        PreparedStatements.getBalancePS().setBytes(1, t.getPublicKeySender());
                        ResultSet ReceiverBalanceRS = PreparedStatements.getBalancePS().executeQuery();
                        if (!ReceiverBalanceRS.next()) {
                            throw new BalanceExceptions.PublicKeyNotFoundException();
                        }
                        int updatedReceiverBalance = ReceiverBalanceRS.getInt("balance") + t.getAmount();
                        int currentReceiverId = ReceiverBalanceRS.getInt("lastTransactionId");
                        PreparedStatements.getUpdateBalancePS().setInt(1, updatedReceiverBalance);
                        PreparedStatements.getUpdateBalancePS().setInt(2, currentReceiverId > t.getReceiverTransactionId() ? currentReceiverId : t.getReceiverTransactionId());
                        PreparedStatements.getUpdateBalancePS().setBytes(3, t.getPublicKeySender());
                        if (PreparedStatements.getUpdateBalancePS().executeUpdate() == 0) {
                            throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
                        }
                    }

                } else {
                    changeMissingStatus(t.getSenderTransactionId(), t.getPublicKeyReceiver(), t.getReceiverTransactionId());
                }

                conn.commit();

            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        };
    }

    synchronized public static ArrayList<Transaction> getMissingTransactions(int senderTransactionId) throws BalanceExceptions.GeneralMYSQLException {
        ArrayList<Transaction> list = new ArrayList<>();

        try {
            PreparedStatements.getTransactionsPS().setInt(1, senderTransactionId);
            PreparedStatements.getTransactionsPS().setInt(2, senderTransactionId);
            ResultSet rs = PreparedStatements.getAllTransactionPS().executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction(rs.getBytes("publicKeySender"), rs.getBytes("publicKeyReceiver"),
                        rs.getInt("amount"), rs.getLong("nonce"), rs.getBytes("signature"),
                        rs.getString("status"), rs.getInt("senderTransactionId"), rs.getInt("receiverTransactionId"));
                list.add(t);
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
        return list;
    }
}