package dbController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Transactions {

    public static void addTransaction(byte[] publicSender, byte[] publicReceiver, int amount) {
        // nonce??
        try {
            Connection conn = DBConnection.getConnection();
            String query = "INSERT INTO TRANSACTIONS (publicsender,publicreceiver,amount,completed) " + "VALUES (?,?,?,?);";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicSender);
            ps.setBytes(2, publicReceiver);
            ps.setInt(3, amount);
            ps.setString(4, "Pending");
            ResultSet rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void changeStatus(int id) {
        try {
            Connection conn = DBConnection.getConnection();
            String query = "UPDATE TRANSACTIONS set completed = ? where id=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, "Completed");
            ps.setInt(2, id);
            ResultSet rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}