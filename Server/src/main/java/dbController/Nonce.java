package dbController;

import tecnico.sec.grpc.Challenge;
import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;
import tecnico.sec.proto.exceptions.TransactionsExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Nonce {

    synchronized public static Challenge getNonce (byte[] publicKey) throws NonceExceptions.NonceNotFoundException, BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();

        try {
            PreparedStatements.getNoncePS().setBytes(1, publicKey);
            ResultSet rs = PreparedStatements.getNoncePS().executeQuery();
            if (!rs.next()) {
                throw new NonceExceptions.NonceNotFoundException();
            }
            Challenge challenge = Challenge.newBuilder().setNonce(rs.getLong("nonce")).setZeros(rs.getInt("zeros")).build();
            return challenge;
        } catch (SQLException e) {
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    synchronized public static void createNonce(byte[] publicKey, long nonce , int zeros) throws NonceExceptions.FailInsertNonceException, BalanceExceptions.GeneralMYSQLException, NonceExceptions.PublicKeyNotFoundException, NonceExceptions.NonceAlreadyExistsException {
        Connection conn = DBConnection.getConnection();

        try {
            PreparedStatements.getCreateNoncePS().setBytes(1, publicKey);
            PreparedStatements.getCreateNoncePS().setLong(2, nonce);
            PreparedStatements.getCreateNoncePS().setInt(3, zeros);
            if(PreparedStatements.getCreateNoncePS().executeUpdate() == 0) {
                throw new NonceExceptions.FailInsertNonceException();
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals(Constants.FOREIGN_KEY_DONT_EXISTS)) {
                throw new NonceExceptions.PublicKeyNotFoundException();
            }
            if ( e.getSQLState().equals(Constants.DUPLICATED_KEY)){
                throw new NonceExceptions.NonceAlreadyExistsException();
            }
            throw new BalanceExceptions.GeneralMYSQLException();

        }
    }
}
