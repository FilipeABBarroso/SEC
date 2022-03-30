package dbController;

import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Balance {

    public static int getBalance(byte[] publicKey) throws NonceExceptions.NonceNotFoundException, BalanceExceptions.PublicKeyNotFoundException {
        int balance = 0;
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT * FROM BALANCE WHERE publicKey=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new BalanceExceptions.PublicKeyNotFoundException();
            }
            balance = rs.getInt("balance");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balance;
    }

    public static void creatUser(byte[] publicKey) throws BalanceExceptions.PublicKeyAlreadyExistException {
        try {
            int initialBalance = 1000;
            Connection conn = DBConnection.getConnection();
            String query = "INSERT INTO BALANCE (publicKey,balance) " + "VALUES (?,?);";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setInt(2, initialBalance);
            if (ps.executeUpdate() == 0) {
                throw new BalanceExceptions.PublicKeyAlreadyExistException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateBalance(int amount, byte[] publicKey) throws BalanceExceptions.PublicKeyNotFoundException, NonceExceptions.NonceNotFoundException {
        try {
            int updatedBalance = getBalance(publicKey) + amount;
            Connection conn = DBConnection.getConnection();
            String query = "UPDATE BALANCE set balance = ? where publicKey=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, updatedBalance);
            ps.setBytes(2, publicKey);
            if (ps.executeUpdate() == 0) {
                throw new BalanceExceptions.PublicKeyNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}