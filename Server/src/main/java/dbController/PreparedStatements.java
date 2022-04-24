package dbController;

import tecnico.sec.proto.exceptions.BalanceExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreparedStatements {

    private static PreparedStatement getBalance;
    private static PreparedStatement openAccount;
    private static PreparedStatement getNonce;
    private static PreparedStatement createNonce;
    private static PreparedStatement addTransaction;
    private static PreparedStatement checkBalance;
    private static PreparedStatement updateBalance;
    private static PreparedStatement removeNonce;
    private static PreparedStatement updateTransaction;
    private static PreparedStatement receiverPKAndAmount;
    private static PreparedStatement getPendingTransaction;
    private static PreparedStatement getAllTransaction;
    private static PreparedStatement getTransaction;
    private static PreparedStatement transactionStamps;
    private static PreparedStatement updateReceiverTransaction;
    private static PreparedStatement updateSenderTransaction;

    private static PreparedStatement getFirstTransactionId;
    private static PreparedStatement getTransactions;

    public static PreparedStatement getBalancePS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (getBalance == null){
            try {
                String query = "SELECT * FROM BALANCE WHERE publicKey=?;";
                getBalance = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return getBalance;
    }

    public static PreparedStatement getOpenAccountPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (openAccount == null){
            try {
                String query = "INSERT INTO BALANCE (publicKey,balance,lastTransactionId) " + "VALUES (?,?,?);";
                openAccount = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return openAccount;
    }

    public static PreparedStatement getNoncePS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (getNonce == null){
            try {
                String query = "SELECT nonce, zeros FROM NONCE WHERE publicKey=?;";
                getNonce = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return getNonce;
    }

    public static PreparedStatement getCreateNoncePS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (createNonce == null){
            try {
                String query = "INSERT INTO NONCE (publicKey,nonce,zeros) " + "VALUES (?,?,?);";
                createNonce = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return createNonce;
    }

    public static PreparedStatement getAddTransaction() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (addTransaction == null){
            try {
                String query = "INSERT INTO Transactions (publicKeySender, publicKeyReceiver, amount, status, nonce, signature, receiverTransactionId, senderTransactionId) " + "VALUES (?,?,?,?::statusOptions,?,?,?,?);";
                addTransaction = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return addTransaction;
    }

    public static PreparedStatement getCheckBalancePS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (checkBalance == null){
            try {
                String query = "SELECT balance FROM BALANCE WHERE publicKey=?;";
                checkBalance = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return checkBalance;
    }

    public static PreparedStatement getUpdateBalancePS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (updateBalance == null){
            try {
                String query = "UPDATE BALANCE set balance = ?, lastTransactionId = ? where publicKey=?;";
                updateBalance = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return updateBalance;
    }

    public static PreparedStatement getRemoveNoncePS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (removeNonce == null){
            try {
                String query = "DELETE from NONCE where publicKey=?;";
                removeNonce = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return removeNonce;
    }

    public static PreparedStatement getUpdateTransactionPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (updateTransaction == null){
            try {
                String query = "UPDATE TRANSACTIONS set status = ?::statusOptions, receiverTransactionId = ? where senderTransactionId=?;";
                updateTransaction = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return updateTransaction;
    }

    public static PreparedStatement getUpdateReceiverTransactionIdPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (updateReceiverTransaction == null){
            try {
                String query = "UPDATE TRANSACTIONS set receiverTransactionId = ? where id=?;";
                updateReceiverTransaction = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return updateReceiverTransaction;
    }

    public static PreparedStatement getUpdateSenderTransactionIdPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (updateSenderTransaction == null){
            try {
                String query = "UPDATE TRANSACTIONS set senderTransactionId = ? where senderTransactionId=?;";
                updateSenderTransaction = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return updateSenderTransaction;
    }

    public static PreparedStatement getReceiverPKAndAmountPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (receiverPKAndAmount == null){
            try {
                String query = "SELECT publicKeyReceiver, amount, status FROM TRANSACTIONS WHERE senderTransactionId=?;";
                receiverPKAndAmount = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return receiverPKAndAmount;
    }

    public static PreparedStatement getPendingTransactionPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (getPendingTransaction == null){
            try {
                String query = "SELECT publicKeySender, publicKeyReceiver, amount, id FROM TRANSACTIONS WHERE publicKeyReceiver=? AND status=?::statusOptions;";
                getPendingTransaction = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return getPendingTransaction;
    }

    public static PreparedStatement getTransactionPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (getTransaction == null){
            try {
                String query = "SELECT id FROM TRANSACTIONS WHERE senderTransactionId = ?;";
                getTransaction = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return getTransaction;
    }

    public static PreparedStatement getAllTransactionPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (getAllTransaction == null){
            try {
                String query = "SELECT * FROM TRANSACTIONS WHERE publicKeyReceiver=? or publicKeySender=?;";
                getAllTransaction = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return getAllTransaction;
    }

    public static PreparedStatement getSpecificTransactionIdPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (getFirstTransactionId == null){
            try {
                String query = "SELECT id FROM TRANSACTIONS WHERE publicKeyReceiver=? and publicKeySender=? and nonce=? and ;";
                getFirstTransactionId = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return getFirstTransactionId;
    }

    public static PreparedStatement getTransactionStampsIdPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (transactionStamps == null){
            try {
                String query = "SELECT MAX(senderTransactionId), MAX(senderTransactionId) FROM Transactions";
                transactionStamps = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return transactionStamps;
    }

    public static PreparedStatement getTransactionsPS() throws BalanceExceptions.GeneralMYSQLException {
        Connection conn = DBConnection.getConnection();
        if (getTransactions == null){
            try {
                String query = "SELECT * FROM Transactions WHERE senderTransactionId > ? OR receiverTransactionId > ?";
                getTransactions = conn.prepareStatement(query);
            } catch (SQLException e) {
                System.out.println(e);
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
        return getTransactions;
    }
}
