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

            conn.setAutoCommit(false);

            PreparedStatements.getOpenAccountPS().setBytes(1, publicKey);
            PreparedStatements.getOpenAccountPS().setInt(2, initialBalance);
            PreparedStatements.getOpenAccountPS().setInt(3, 0);
            if (PreparedStatements.getOpenAccountPS().executeUpdate() == 0) {
                throw new BalanceExceptions.GeneralMYSQLException();
            }

            PreparedStatements.getAddTransaction().setBytes(1, null);
            PreparedStatements.getAddTransaction().setBytes(2, publicKey);
            PreparedStatements.getAddTransaction().setInt(3, initialBalance);
            PreparedStatements.getAddTransaction().setString(4, "Completed");
            PreparedStatements.getAddTransaction().setLong(5, 0);
            PreparedStatements.getAddTransaction().setBytes(6, null);
            PreparedStatements.getAddTransaction().setInt(7, 0);
            PreparedStatements.getAddTransaction().setInt(8, 0);
            if(PreparedStatements.getAddTransaction().executeUpdate() == 0) {
                throw new TransactionsExceptions.FailInsertTransactionException();
            }

            conn.commit();

            // return id;
        } catch (SQLException e) {
            System.out.println(e);
            if (e.getSQLState().equals(Constants.DUPLICATED_KEY) || e.getSQLState().equals(Constants.DUPLICATED_DUPLICATED_KEY)) {
                try {
                    conn.commit();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                throw new BalanceExceptions.PublicKeyAlreadyExistException();
            } else {
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
    }
}