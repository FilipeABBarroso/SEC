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

            byte[] initialSender = {0};
            PreparedStatements.getAddTransaction().setBytes(1, initialSender);
            PreparedStatements.getAddTransaction().setBytes(2, publicKey);
            PreparedStatements.getAddTransaction().setInt(3, initialBalance);
            PreparedStatements.getAddTransaction().setString(4, "Completed");
            PreparedStatements.getAddTransaction().setInt(5, 0);
            PreparedStatements.getAddTransaction().setBytes(6, null);
            if(PreparedStatements.getAddTransaction().executeUpdate() == 0) {
                throw new TransactionsExceptions.FailInsertTransactionException();
            }

            PreparedStatements.getOpenAccountPS().setBytes(1, publicKey);
            PreparedStatements.getOpenAccountPS().setInt(2, initialBalance);
            PreparedStatements.getOpenAccountPS().setInt(3, 0);
            if (PreparedStatements.getOpenAccountPS().executeUpdate() == 0) {
                throw new BalanceExceptions.GeneralMYSQLException();
            }

            /*CallableStatement cs = conn.prepareCall("{ ? = call add_transaction(?, ?, ?, ?::statusOptions, ?, ?) }");
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setBytes(2, initialSender);
            cs.setBytes(3, publicKey);
            cs.setInt(4, initialBalance);
            cs.setString(5, "Completed");
            cs.setInt(6, 0);
            cs.setBytes(7, null);

            cs.executeUpdate();

            int id = cs.getInt(1); */

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