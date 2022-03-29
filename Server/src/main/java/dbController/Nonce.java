package dbController;

import tecnico.sec.proto.exceptions.NonceExceptions;

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

        }
        return out;
    }

    public static void creatNonce(byte[] publicKey, int nonce) {
        try {
            Connection conn = DBConnection.getConnection();
            String query = "INSERT INTO NONCE (publicKey,nonce) " + "VALUES (?,?);";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setInt(2, nonce);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
