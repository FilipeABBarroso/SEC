package dbController;

import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;
import tecnico.sec.proto.exceptions.TransactionsExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Transactions {

    public static void addTransaction(byte[] publicKeySender, byte[] publicKeyReceiver, int amount) throws NonceExceptions.NonceNotFoundException, TransactionsExceptions.FailInsertTransactionException, TransactionsExceptions.SenderPublicKeyNotFoundException, BalanceExceptions.PublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.PublicKeyNotFoundException {
        try {
            Connection conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Add transaction
            String query = "INSERT INTO TRANSACTIONS (publicKeySender,publicKeyReceiver,amount,completed) " + "VALUES (?,?,?,?);";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKeySender);
            ps.setBytes(2, publicKeyReceiver);
            ps.setInt(3, amount);
            ps.setString(4, "Pending");
            if(ps.executeUpdate() == 0) {
                throw new TransactionsExceptions.FailInsertTransactionException();
            }

            // update sender balance
            int updatedBalance = Balance.getBalance(publicKeySender) + amount;
            String secondQuery = "UPDATE BALANCE set balance = ? where publicKey=?;";
            PreparedStatement balancePs = conn.prepareStatement(secondQuery);
            balancePs.setInt(1, updatedBalance);
            balancePs.setBytes(2, publicKeySender);
            if (balancePs.executeUpdate() == 0) {
                throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
            }

            // remove sender nonce
            String removeNonceQuery = "DELETE from NONCE where publicKey=?;";
            PreparedStatement removeNoncePs = conn.prepareStatement(removeNonceQuery);
            removeNoncePs.setBytes(1, publicKeySender);
            if (removeNoncePs.executeUpdate() == 0) {
                throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
            }

            conn.commit();
        } catch (SQLException e) {
            if (e.getSQLState().equals(Constants.FOREIGN_KEY_DONT_EXISTS)) {
                throw new TransactionsExceptions.PublicKeyNotFoundException();
            } else {
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
    }

    public static void changeStatus(int id, byte[] publicKeyReceiver) throws NonceExceptions.NonceNotFoundException, TransactionsExceptions.TransactionIDNotFoundException, TransactionsExceptions.ReceiverPublicKeyNotFoundException, BalanceExceptions.PublicKeyNotFoundException, TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException, BalanceExceptions.GeneralMYSQLException {
        try {
            // get receiver public key and amount in the transaction
            Connection conn = DBConnection.getConnection();
            String pkAndAmount = "SELECT publicKeyReceiver, amount FROM TRANSACTIONS WHERE id=?;";
            PreparedStatement pkAndAmountPs = conn.prepareStatement(pkAndAmount);
            pkAndAmountPs.setInt(1, id);
            ResultSet pkAndAmountRs = pkAndAmountPs.executeQuery();
            if (!pkAndAmountRs.next()) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }
            if (publicKeyReceiver != pkAndAmountRs.getBytes("publicKeyReceiver")) {
                throw new TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException();
            }

            conn.setAutoCommit(false);

            // change transaction status to completed
            String query = "UPDATE TRANSACTIONS set status = ? where id=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, "Completed");
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }

            // update receiver balance
            int updatedBalance = Balance.getBalance(pkAndAmountRs.getBytes("publicKeyReceiver")) + pkAndAmountRs.getInt("amount");
            String secondQuery = "UPDATE BALANCE set balance = ? where publicKey=?;";
            PreparedStatement secondPs = conn.prepareStatement(secondQuery);
            secondPs.setInt(1, updatedBalance);
            secondPs.setBytes(2, pkAndAmountRs.getBytes("publicKeyReceiver"));
            if (secondPs.executeUpdate() == 0) {
                throw new TransactionsExceptions.ReceiverPublicKeyNotFoundException();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    public static List<String> getPendingTransactions(byte[] publicKey) throws TransactionsExceptions.ReceiverPublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException {
        ArrayList<String> list = new ArrayList<>();
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT * FROM TRANSACTIONS WHERE publicKeyReceiver=? AND status=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setString(2, "Pending");
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new TransactionsExceptions.ReceiverPublicKeyNotFoundException();
            }
            while (!rs.next()) {
                Transaction t = new Transaction(rs.getBytes("publicKeySender"), rs.getBytes("publicKeySender"),
                        rs.getInt("amount"), rs.getInt("id"));
                list.add(t.toString());
            }
        } catch (SQLException e) {
            throw new BalanceExceptions.GeneralMYSQLException();
        }
        return list;
    }

    public static List<String> getTransactions(byte[] publicKey) throws TransactionsExceptions.ReceiverPublicKeyNotFoundException, TransactionsExceptions.PublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException {
        ArrayList<String> list = new ArrayList<>();
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT * FROM TRANSACTIONS WHERE publicKeyReceiver=? or publicKeySender=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setBytes(2, publicKey);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new TransactionsExceptions.PublicKeyNotFoundException();
            }
            while (!rs.next()) {
                Transaction t = new Transaction(rs.getBytes("publicKeySender"), rs.getBytes("publicKeySender"),
                        rs.getInt("amount"), rs.getInt("id"));
                list.add(t.toString());
            }
        } catch (SQLException e) {
            throw new BalanceExceptions.GeneralMYSQLException();
        }
        return list;
    }
}