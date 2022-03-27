package dbController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Transactions {

    public static void addTransaction(byte[] publicKeySender, byte[] publicKeyReceiver, int amount) {
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
            ps.executeUpdate();
            // update sender balance
            int updatedBalance = Balance.getBalance(publicKeySender) + amount;
            String secondQuery = "UPDATE BALANCE set balance = ? where publicKey=?;";
            PreparedStatement balancePs = conn.prepareStatement(secondQuery);
            balancePs.setInt(1, updatedBalance);
            balancePs.setBytes(2, publicKeySender);
            balancePs.executeUpdate();
            // remove sender nonce
            String removeNonceQuery = "DELETE from NONCE where publicKey=?;";
            PreparedStatement removeNoncePs = conn.prepareStatement(removeNonceQuery);
            removeNoncePs.setBytes(1, publicKeySender);
            removeNoncePs.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void changeStatus(int id) {
        try {
            Connection conn = DBConnection.getConnection();
            String pkAndAmount = "SELECT publicKeyReceiver, amount FROM TRANSACTIONS WHERE id=?;";
            PreparedStatement pkAndAmountPs = conn.prepareStatement(pkAndAmount);
            pkAndAmountPs.setInt(1, id);
            ResultSet pkAndAmountRs = pkAndAmountPs.executeQuery();

            conn.setAutoCommit(false);

            String query = "UPDATE TRANSACTIONS set completed = ? where id=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, "Completed");
            ps.setInt(2, id);
            ResultSet rs = ps.executeQuery();

            int updatedBalance = Balance.getBalance(pkAndAmountRs.getBytes("publicKeyReceiver")) + pkAndAmountRs.getInt("amount");
            String secondQuery = "UPDATE BALANCE set balance = ? where publicKey=?;";
            PreparedStatement secondPs = conn.prepareStatement(secondQuery);
            secondPs.setInt(1, updatedBalance);
            secondPs.setBytes(2, pkAndAmountRs.getBytes("publicKeyReceiver"));
            secondPs.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void getPendingTransactions(byte[] publicKey) {
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT * FROM TRANSACTIONS WHERE publicKeyReceiver=? AND status=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setString(2, "Pending");
            ResultSet rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}