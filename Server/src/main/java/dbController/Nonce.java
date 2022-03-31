package dbController;

import tecnico.sec.proto.exceptions.DataBaseExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;
import tecnico.sec.proto.exceptions.TransactionsExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Nonce {

    public static int getNonce (byte[] publicKey) throws NonceExceptions.NonceNotFoundException {
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
            e.printStackTrace();
        }
        return out;
    }

    public static void createNonce(byte[] publicKey, int nonce) throws NonceExceptions.FailInsertNonceException, DataBaseExceptions.GeneralDatabaseError {
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
            throw new DataBaseExceptions.GeneralDatabaseError();
        }
    }
}
