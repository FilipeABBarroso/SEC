package dbController;

import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;
import tecnico.sec.proto.exceptions.TransactionsExceptions;


import java.sql.*;

public class Balance {

    synchronized public static int getBalance(byte[] publicKey) throws BalanceExceptions.PublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException {

        try {
            PreparedStatements.getBalancePS().setBytes(1, publicKey);
            ResultSet rs = PreparedStatements.getBalancePS().executeQuery();
            if (!rs.next()) {
                throw new BalanceExceptions.PublicKeyNotFoundException();
            }
            return rs.getInt("balance");
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    synchronized public static void openAccount(byte[] publicKey) throws BalanceExceptions.PublicKeyAlreadyExistException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.FailInsertTransactionException {
        Connection conn = DBConnection.getConnection();

        try {
            int initialBalance = 1000;

            CallableStatement cs = conn.prepareCall("{call addTransaction(?, ?, ?, ?, ?, ?)}");
            cs.setBytes(1, null);
            cs.setBytes(2, publicKey);
            cs.setInt(3, initialBalance);
            cs.setString(4, "Completed");
            cs.setBytes(5, null);
            cs.setBytes(6, null);
            cs.executeQuery();

            int id = cs.getInt(1);

            // conn.setAutoCommit(false);

            /* PreparedStatements.getAddTransactionPS().setBytes(1, null);
            PreparedStatements.getAddTransactionPS().setBytes(2, publicKey);
            PreparedStatements.getAddTransactionPS().setInt(3, initialBalance);
            PreparedStatements.getAddTransactionPS().setString(4, "Completed");
            PreparedStatements.getAddTransactionPS().setBytes(5, null);
            PreparedStatements.getAddTransactionPS().setBytes(6, null);
            if(PreparedStatements.getAddTransactionPS().executeUpdate() == 0) {
                throw new TransactionsExceptions.FailInsertTransactionException();
            }

            PreparedStatements.getSpecificTransactionIdPS().setBytes(1, publicKey);
            PreparedStatements.getSpecificTransactionIdPS().setBytes(2, null);
            PreparedStatements.getSpecificTransactionIdPS().setBytes(3, null);
            ResultSet rs = PreparedStatements.getSpecificTransactionIdPS().executeQuery();
            rs.next();*/

            PreparedStatements.getOpenAccountPS().setBytes(1, publicKey);
            PreparedStatements.getOpenAccountPS().setInt(2, initialBalance);
            PreparedStatements.getOpenAccountPS().setInt(3, id);
            if (PreparedStatements.getOpenAccountPS().executeUpdate() == 0) {
                throw new BalanceExceptions.GeneralMYSQLException();
            }

            // conn.commit();
        } catch (SQLException e) {
            if (e.getSQLState().equals(Constants.DUPLICATED_KEY)) {
                throw new BalanceExceptions.PublicKeyAlreadyExistException();
            } else {
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
    }
}