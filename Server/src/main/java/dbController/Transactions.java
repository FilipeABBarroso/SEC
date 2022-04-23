package dbController;

import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;
import tecnico.sec.proto.exceptions.TransactionsExceptions;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
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

            // TODO: adicionar senderTransactionId
            //PreparedStatements.getAddTransaction().setInt(1, senderTransactionId);
            PreparedStatements.getAddTransaction().setBytes(1, publicKeySender);
            PreparedStatements.getAddTransaction().setBytes(2, publicKeyReceiver);
            PreparedStatements.getAddTransaction().setInt(3, amount);
            PreparedStatements.getAddTransaction().setString(4, "Pending");
            PreparedStatements.getAddTransaction().setLong(5, nonce);
            PreparedStatements.getAddTransaction().setBytes(6, signature);
            if(PreparedStatements.getAddTransaction().executeUpdate() == 0) {
                throw new TransactionsExceptions.FailInsertTransactionException();
            }

            // FIXME: valor martelado q vai ser igual ao senderTransactionId
            int id = 1;

            // update sender balance
            int updatedBalance = Balance.getBalance(publicKeySender) - amount;
            PreparedStatements.getUpdateBalancePS().setInt(1, updatedBalance);
            // PreparedStatements.getUpdateBalancePS().setInt(2, id);
            PreparedStatements.getUpdateBalancePS().setBytes(2, publicKeySender);
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

            // change transaction status to completed
            PreparedStatements.getUpdateTransactionPS().setString(1, "Completed");
            PreparedStatements.getUpdateTransactionPS().setInt(2, id);
            if (PreparedStatements.getUpdateTransactionPS().executeUpdate() == 0) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }

            // update receiver balance
            int updatedBalance = Balance.getBalance(pkAndAmountRs.getBytes("publicKeyReceiver")) + pkAndAmountRs.getInt("amount");
            // TODO: adicionar lastTransactionId
            // int lastTransactionId = Balance.getBalance(pkAndAmountRs.getBytes("publicKeyReceiver")) + pkAndAmountRs.getInt("amount");
            PreparedStatements.getUpdateBalancePS().setInt(1, updatedBalance);
            // PreparedStatements.getUpdateBalancePS().setInt(2, lastTransactionId);
            PreparedStatements.getUpdateBalancePS().setBytes(2, pkAndAmountRs.getBytes("publicKeyReceiver"));
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

        Connection conn = DBConnection.getConnection();

        try {
            PreparedStatements.getTransactionPS().setBytes(1, publicKey);
            PreparedStatements.getTransactionPS().setString(2, "Pending");
            ResultSet rs = PreparedStatements.getTransactionPS().executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction(rs.getBytes("publicKeySender"), rs.getBytes("publicKeySender"),
                        rs.getInt("amount"), rs.getInt("id"));
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

        Connection conn = DBConnection.getConnection();

        try {
            PreparedStatements.getAllTransactionPS().setBytes(1, publicKey);
            PreparedStatements.getAllTransactionPS().setBytes(2, publicKey);
            ResultSet rs = PreparedStatements.getAllTransactionPS().executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction(rs.getBytes("publicKeySender"), rs.getBytes("publicKeySender"),
                        rs.getInt("amount"), rs.getInt("id"));
                list.add(t.toTransactionGrpc());
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
        return list;
    }
}