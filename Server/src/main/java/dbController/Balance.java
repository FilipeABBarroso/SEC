package dbController;

import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Balance {

    public static int getBalance(byte[] publicKey) throws BalanceExceptions.PublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException {
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT * FROM BALANCE WHERE publicKey=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new BalanceExceptions.PublicKeyNotFoundException();
            }
            return rs.getInt("balance");
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    public static void openAccount(byte[] publicKey) throws BalanceExceptions.PublicKeyAlreadyExistException, BalanceExceptions.GeneralMYSQLException {
        try {
            int initialBalance = 1000;
            Connection conn = DBConnection.getConnection();
            String query = "INSERT INTO BALANCE (publicKey,balance) " + "VALUES (?,?);";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setInt(2, initialBalance);
            if (ps.executeUpdate() == 0) {
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals(Constants.DUPLICATED_KEY)) {
                throw new BalanceExceptions.PublicKeyAlreadyExistException();
            } else {
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
    }
}