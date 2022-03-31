package dbController;

import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;
import tecnico.sec.proto.exceptions.TransactionsExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Nonce {

    public static int getNonce (byte[] publicKey) throws NonceExceptions.NonceNotFoundException, BalanceExceptions.GeneralMYSQLException {
        int out = 0;
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT nonce FROM NONCE WHERE publicKey=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new NonceExceptions.NonceNotFoundException();
            }
            out = rs.getInt("nonce");
        } catch (SQLException e) {
            throw new BalanceExceptions.GeneralMYSQLException();
        }
        return out;
    }

    public static void createNonce(byte[] publicKey, int nonce) throws NonceExceptions.FailInsertNonceException, BalanceExceptions.GeneralMYSQLException, NonceExceptions.PublicKeyNotFoundException {
        try {
            Connection conn = DBConnection.getConnection();
            String query = "INSERT INTO NONCE (publicKey,nonce) " + "VALUES (?,?);";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setInt(2, nonce);
            if(ps.executeUpdate() == 0) {
                throw new NonceExceptions.FailInsertNonceException();
            }
        } catch (SQLException e) {
            System.out.println(e);
            if (e.getSQLState().equals(Constants.DUPLICATED_KEY) || e.getSQLState().equals(Constants.FOREIGN_KEY_DONT_EXISTS)) {
                throw new NonceExceptions.PublicKeyNotFoundException();
            } else {
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
    }
}
