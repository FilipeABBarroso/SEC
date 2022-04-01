package dbController;

import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;
import tecnico.sec.proto.exceptions.TransactionsExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Transactions {

    public static void addTransaction(byte[] publicKeySender, byte[] publicKeyReceiver, int amount) throws NonceExceptions.NonceNotFoundException, TransactionsExceptions.FailInsertTransactionException, TransactionsExceptions.SenderPublicKeyNotFoundException, BalanceExceptions.PublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.PublicKeyNotFoundException, TransactionsExceptions.BalanceNotEnoughException, TransactionsExceptions.ReceiverPublicKeyNotFoundException, TransactionsExceptions.AmountCanNotBeLessThenOneException, TransactionsExceptions.CanNotSendMoneyToYourselfException {
        if(amount <= 0 ){
            throw new TransactionsExceptions.AmountCanNotBeLessThenOneException();
        }
        if(publicKeySender == publicKeyReceiver){
            throw  new TransactionsExceptions.CanNotSendMoneyToYourselfException();
        }

        try {
            Connection conn = DBConnection.getConnection();

            // Check if sender has enough
            String checkBalanceQuery = "SELECT balance FROM BALANCE WHERE publicKey=?;";
            PreparedStatement checkBalancePS = conn.prepareStatement(checkBalanceQuery);
            checkBalancePS.setBytes(1, publicKeySender);
            ResultSet checkBalanceRS = checkBalancePS.executeQuery();
            if(!checkBalanceRS.next()) {
                throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
            } else {
                if (checkBalanceRS.getInt("balance") < amount){
                    throw new TransactionsExceptions.BalanceNotEnoughException();
                }
            }

            conn.setAutoCommit(false);
            // Add transaction
            String query = "INSERT INTO TRANSACTIONS (publicKeySender,publicKeyReceiver,amount,status) " + "VALUES (?,?,?,?::statusOptions);";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKeySender);
            ps.setBytes(2, publicKeyReceiver);
            ps.setInt(3, amount);
            ps.setString(4, "Pending");
            if(ps.executeUpdate() == 0) {
                throw new TransactionsExceptions.FailInsertTransactionException();
            }

            // update sender balance
            int updatedBalance = Balance.getBalance(publicKeySender) - amount;
            String secondQuery = "UPDATE BALANCE set balance = ? where publicKey=?;";
            PreparedStatement balancePs = conn.prepareStatement(secondQuery);
            balancePs.setInt(1, updatedBalance);
            balancePs.setBytes(2, publicKeySender);
            if (balancePs.executeUpdate() == 0) {
                throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
            }

            // remove sender nonce
            String removeNonceQuery = "DELETE from NONCE where publicKey=?;";
            PreparedStatement removeNoncePs = conn.prepareStatement(removeNonceQuery);
            removeNoncePs.setBytes(1, publicKeySender);
            removeNoncePs.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            if (e.getSQLState().equals(Constants.FOREIGN_KEY_DONT_EXISTS)) {
                if (e.getMessage().contains("receiver")){
                    throw new TransactionsExceptions.ReceiverPublicKeyNotFoundException();
                } else {
                    throw new TransactionsExceptions.SenderPublicKeyNotFoundException();
                }

            } else {
                throw new BalanceExceptions.GeneralMYSQLException();
            }
        }
    }

    public static void changeStatus(int id, byte[] publicKeyReceiver) throws NonceExceptions.NonceNotFoundException, TransactionsExceptions.TransactionIDNotFoundException, TransactionsExceptions.ReceiverPublicKeyNotFoundException, BalanceExceptions.PublicKeyNotFoundException, TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.TransactionAlreadyAcceptedException, TransactionsExceptions.PublicKeyNotFoundException {
        try {
            Balance.getBalance(publicKeyReceiver);
        }catch (BalanceExceptions.PublicKeyNotFoundException e ){
            throw new TransactionsExceptions.PublicKeyNotFoundException();
        }

        try {
            // get receiver public key and amount in the transaction
            Connection conn = DBConnection.getConnection();
            String pkAndAmount = "SELECT publicKeyReceiver, amount, status FROM TRANSACTIONS WHERE id=?;";
            PreparedStatement pkAndAmountPs = conn.prepareStatement(pkAndAmount);
            pkAndAmountPs.setInt(1, id);
            ResultSet pkAndAmountRs = pkAndAmountPs.executeQuery();
            if (!pkAndAmountRs.next()) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }
            if (!Arrays.equals(publicKeyReceiver, pkAndAmountRs.getBytes("publicKeyReceiver"))) {
                throw new TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException();
            }
            if (pkAndAmountRs.getString("status").equals("Completed")) {
                throw new TransactionsExceptions.TransactionAlreadyAcceptedException();
            }

            conn.setAutoCommit(false);

            // change transaction status to completed
            String query = "UPDATE TRANSACTIONS set status = ?::statusOptions where id=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, "Completed");
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) {
                throw new TransactionsExceptions.TransactionIDNotFoundException();
            }

            // update receiver balance
            int updatedBalance = Balance.getBalance(pkAndAmountRs.getBytes("publicKeyReceiver")) + pkAndAmountRs.getInt("amount");
            String secondQuery = "UPDATE BALANCE set balance = ? where publicKey=?;";
            PreparedStatement secondPs = conn.prepareStatement(secondQuery);
            secondPs.setInt(1, updatedBalance);
            secondPs.setBytes(2, pkAndAmountRs.getBytes("publicKeyReceiver"));
            if (secondPs.executeUpdate() == 0) {
                throw new TransactionsExceptions.ReceiverPublicKeyNotFoundException();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new BalanceExceptions.GeneralMYSQLException();
        }
    }

    public static List<String> getPendingTransactions(byte[] publicKey) throws TransactionsExceptions.ReceiverPublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.PublicKeyNotFoundException {
        ArrayList<String> list = new ArrayList<>();
        try {
            Balance.getBalance(publicKey);
        }catch (BalanceExceptions.PublicKeyNotFoundException e ){
            throw new TransactionsExceptions.PublicKeyNotFoundException();
        }
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT publicKeySender, publicKeyReceiver, amount, id FROM TRANSACTIONS WHERE publicKeyReceiver=? AND status=?::statusOptions;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setString(2, "Pending");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction(rs.getBytes("publicKeySender"), rs.getBytes("publicKeySender"),
                        rs.getInt("amount"), rs.getInt("id"));
                list.add(t.toString());
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
        return list;
    }

    public static List<String> getTransactions(byte[] publicKey) throws TransactionsExceptions.ReceiverPublicKeyNotFoundException, TransactionsExceptions.PublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException {
        ArrayList<String> list = new ArrayList<>();
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT * FROM TRANSACTIONS WHERE publicKeyReceiver=? or publicKeySender=?;";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBytes(1, publicKey);
            ps.setBytes(2, publicKey);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction(rs.getBytes("publicKeySender"), rs.getBytes("publicKeySender"),
                        rs.getInt("amount"), rs.getInt("id"));
                list.add(t.toString());
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new BalanceExceptions.GeneralMYSQLException();
        }
        return list;
    }
}