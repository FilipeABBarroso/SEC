package dbController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Balance {

    public static int getBalance(byte[] publicKey) {
        int balance = 0;
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT * FROM BALANCE WHERE publicKey=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                balance = rs.getInt("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balance;
    }

    public static void creatUser(byte[] publicKey) {
        try {
            int initialBalance = 1000;
            Connection conn = DBConnection.getConnection();
            String query = "INSERT INTO BALANCE (publicKey,balance) " + "VALUES (?,?);";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setInt(2, initialBalance);
            ResultSet rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateBalance(int amount, byte[] publicKey) {
        try {
            int updatedBalance = getBalance(publicKey) + amount;
            Connection conn = DBConnection.getConnection();
            String query = "UPDATE BALANCE set balance = ? where publicKey=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, updatedBalance);
            ps.setBytes(2, publicKey);
            ResultSet rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}