package dbController;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.NonceExceptions;
import tecnico.sec.proto.exceptions.TransactionsExceptions;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class TransactionsTest {

    public static final byte[] CREATEDUSERFROM = {48, -126, 2, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 2, 15, 0, 48, -126, 2, 10, 2, -126, 2, 1, 0, -75, -102, -29, 1, -20, -99, 103, -94, 65, 88, -99, -41, 7, 76, 107, 121, 116, 20, -19, 96, 44, 106, -104, -91, 4, -20, 55, -76, 67, 110, 60, 48, 63, 26, 21, 19, -115, 38, 60, 32, 110, -4, 21, 96, 72, 73, -47, -118, -45, -61, -53, -50, 11, -29, -25, -83, -67, -62, 100, -81, -54, -56, 85, -99, -89, -84, 56, 17, -118, 26, -35, -35, -77, 110, -103, 29, -20, -39, 71, -52, -90, 23, 0, 60, 64, 75, -50, -70, 20, -71, -110, 33, 15, 96, -77, 66, 109, 53, 83, 48, 2, -45, -67, -119, 17, 72, -38, 32, -72, 2, 15, -76, -35, -41, 62, -3, -4, -100, -80, 83, -74, -58, -72, -31, -84, -106, -44, 104, -71, -6, 82, -1, -105, 5, 127, 42, 107, -56, 9, 121, -34, 63, 56, -77, -36, 83, 105, -91, -69, -54, -46, -80, -34, 48, 36, 10, 66, 50, 29, -42, -13, -91, -42, 64, -85, -46, 77, 0, 78, -35, 52, 68, 41, 59, 100, -21, 126, 51, 26, -51, 60, 75, -83, 121, 121, 120, -29, 82, -112, 60, 2, 3, 37, 45, 35, -39, -113, 39, -18, -32, -40, -76, 61, 69, 92, -62, 98, 19, -35, 66, -14, -30, 74, -126, -53, -21, 125, -13, 15, -123, -122, -32, -88, 21, -50, -46, 21, 11, -115, 95, -44, -72, 9, -50, 93, 42, 73, -54, -31, -12, -84, 27, -53, 68, 114, 20, 67, 103, 108, 29, -113, 23, 4, -49, 111, 82, 59, -55, -86, -102, 106, -10, 49, -72, -120, -106, 78, -24, -114, -43, -8, 106, 121, -23, 116, 115, -17, -122, -31, 16, -6, -121, 105, -70, 33, 88, 73, 80, -108, -69, -110, -96, 111, 111, -5, -85, 120, 15, -65, -49, -114, 0, 7, 120, -47, -116, 117, -128, -2, -59, -69, -75, -5, 14, -64, 118, -16, -68, -18, 13, -70, -95, 70, 17, 46, -34, 59, 99, -75, -127, -77, -5, -57, 65, -87, -118, 96, -56, 123, 56, -27, -49, 49, 100, -117, 64, -56, -17, 46, 121, -71, -119, -86, 25, -49, -122, -80, -47, 121, 9, -63, -85, -7, -44, -1, 89, -110, -80, 53, 101, -80, -57, -49, 3, 30, 14, -99, -75, -71, -45, -77, 25, 94, 89, 8, -31, 21, -117, 71, -91, 73, 8, -91, 126, -45, 74, 126, 86, 123, -80, -5, -64, -97, 89, -75, 78, -22, -108, -82, -22, -6, 84, -45, -39, -86, 116, -61, 35, -16, 39, -53, -52, -39, 76, 16, -43, -43, 36, 11, 30, 58, 44, -58, 110, -90, -104, -28, 90, 79, -37, 55, -33, 108, 67, 39, -79, 88, 115, -117, 4, 70, -84, -35, -54, -9, 14, -88, -23, 94, -7, -80, -93, 15, 54, -51, 12, -59, -1, -105, -10, -65, 57, 83, 45, -39, 24, 43, -58, -13, 39, -17, -68, 20, 79, -124, 74, -47, -3, 40, -103, -101, 116, -125, -6, -22, -51, 66, -118, 38, 99, 69, -82, -9, -118, 86, 72, 87, 119, -37, -115, -116, -27, 2, 3, 1, 0, 1};
    public static final byte[] NOTCREATEDUSER = {48, -126, 2, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 2, 15, 0, 48, -126, 2, 10, 2, -126, 2, 1, 0, -75, -102, -29, 1, -20, -99, 103, -94, 65, 88, -99, -41, 7, 76, 107, 121, 116, 20, -19, 96, 44, 106, -104, -91, 4, -20, 55, -76, 67, 110, 60, 48, 63, 26, 21, 19, -115, 38, 60, 32, 110, -4, 21, 96, 72, 73, -47, -118, -45, -61, -53, -50, 11, -29, -25, -83, -67, -62, 100, -81, -54, -56, 85, -99, -89, -84, 56, 17, -118, 26, -35, -35, -77, 110, -103, 29, -20, -39, 71, -52, -90, 23, 0, 60, 64, 75, -50, -70, 20, -71, -110, 33, 15, 96, -77, 66, 109, 53, 83, 48, 2, -45, -67, -119, 17, 72, -38, 32, -72, 2, 15, -76, -35, -41, 62, -3, -4, -100, -80, 83, -74, -58, -72, -31, -84, -106, -44, 104, -71, -6, 82, -1, -105, 5, 127, 42, 107, -56, 9, 121, -34, 63, 56, -77, -36, 83, 105, -92, -69, -54, -46, -80, -34, 48, 36, 10, 66, 50, 29, -42, -13, -91, -42, 64, -85, -46, 77, 0, 78, -35, 52, 68, 41, 59, 100, -21, 126, 51, 26, -51, 60, 75, -83, 121, 121, 120, -29, 82, -112, 60, 2, 3, 37, 45, 35, -39, -113, 39, -18, -32, -40, -76, 61, 69, 92, -62, 98, 19, -35, 66, -14, -30, 74, -126, -53, -21, 125, -13, 15, -123, -122, -32, -88, 21, -50, -46, 21, 11, -115, 95, -44, -72, 9, -50, 93, 42, 73, -54, -31, -12, -84, 27, -53, 68, 114, 20, 67, 103, 108, 29, -113, 23, 4, -49, 111, 82, 59, -55, -86, -102, 106, -10, 49, -72, -120, -106, 78, -24, -114, -43, -8, 106, 121, -23, 116, 115, -17, -122, -31, 16, -6, -121, 105, -70, 33, 88, 73, 80, -108, -69, -110, -96, 111, 111, -5, -85, 120, 15, -65, -49, -114, 0, 7, 120, -47, -116, 117, -128, -2, -59, -69, -75, -5, 14, -64, 118, -16, -68, -18, 13, -70, -95, 70, 17, 46, -34, 59, 99, -75, -127, -77, -5, -57, 65, -87, -118, 96, -56, 123, 56, -27, -49, 49, 100, -117, 64, -56, -17, 46, 121, -71, -119, -86, 25, -49, -122, -80, -47, 121, 9, -63, -85, -7, -44, -1, 89, -110, -80, 53, 101, -80, -57, -49, 3, 30, 14, -99, -75, -71, -45, -77, 25, 94, 89, 8, -31, 21, -117, 71, -91, 73, 8, -91, 126, -45, 74, 126, 86, 123, -80, -5, -64, -97, 89, -75, 78, -22, -108, -82, -22, -6, 84, -45, -39, -86, 116, -61, 35, -16, 39, -53, -52, -39, 76, 16, -43, -43, 36, 11, 30, 58, 44, -58, 110, -90, -104, -28, 90, 79, -37, 55, -33, 108, 67, 39, -79, 88, 115, -117, 4, 70, -84, -35, -54, -9, 14, -88, -23, 94, -7, -80, -93, 15, 54, -51, 12, -59, -1, -105, -10, -65, 57, 83, 45, -39, 24, 43, -58, -13, 39, -17, -68, 20, 79, -124, 74, -47, -3, 40, -103, -101, 116, -125, -6, -22, -51, 66, -118, 38, 99, 69, -82, -9, -118, 86, 72, 87, 119, -37, -115, -116, -27, 2, 3, 1, 0, 1};
    public static final byte[] CREATEDUSERTO = {48, -126, 2, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 2, 15, 0, 48, -126, 2, 10, 2, -126, 2, 1, 0, -75, -102, -29, 1, -20, -99, 103, -94, 65, 88, -99, -41, 7, 76, 107, 121, 116, 20, -19, 96, 44, 106, -104, -91, 4, -20, 55, -76, 67, 110, 60, 48, 63, 26, 21, 19, -115, 38, 60, 32, 110, -4, 21, 96, 72, 73, -47, -118, -45, -61, -53, -50, 11, -29, -25, -83, -67, -62, 100, -81, -54, -56, 85, -99, -89, -84, 56, 17, -118, 26, -35, -35, -77, 110, -103, 29, -20, -39, 71, -52, -90, 23, 0, 60, 64, 75, -50, -70, 20, -71, -110, 33, 15, 96, -77, 66, 109, 53, 83, 48, 2, -45, -67, -119, 17, 72, -38, 32, -72, 2, 15, -76, -35, -41, 62, -3, -4, -100, -80, 83, -74, -58, -72, -31, -84, -106, -44, 104, -71, -6, 82, -1, -105, 5, 127, 42, 107, -56, 9, 121, -34, 63, 56, -77, -36, 83, 105, -91, -69, -54, -46, -80, -34, 48, 36, 11, 66, 50, 29, -42, -13, -91, -42, 64, -85, -46, 77, 0, 78, -35, 52, 68, 41, 59, 100, -21, 126, 51, 26, -51, 60, 75, -83, 121, 121, 120, -29, 82, -112, 60, 2, 3, 37, 45, 35, -39, -113, 39, -18, -32, -40, -76, 61, 69, 92, -62, 98, 19, -35, 66, -14, -30, 74, -126, -53, -21, 125, -13, 15, -123, -122, -32, -88, 21, -50, -46, 21, 11, -115, 95, -44, -72, 9, -50, 93, 42, 73, -54, -31, -12, -84, 27, -53, 68, 114, 20, 67, 103, 108, 29, -113, 23, 4, -49, 111, 82, 59, -55, -86, -102, 106, -10, 49, -72, -120, -106, 78, -24, -114, -43, -8, 106, 121, -23, 116, 115, -17, -122, -31, 16, -6, -121, 105, -70, 33, 88, 73, 80, -108, -69, -110, -96, 111, 111, -5, -85, 120, 15, -65, -49, -114, 0, 7, 120, -47, -116, 117, -128, -2, -59, -69, -75, -5, 14, -64, 118, -16, -68, -18, 13, -70, -95, 70, 17, 46, -34, 59, 99, -75, -127, -77, -5, -57, 65, -87, -118, 96, -56, 123, 56, -27, -49, 49, 100, -117, 64, -56, -17, 46, 121, -71, -119, -86, 25, -49, -122, -80, -47, 121, 9, -63, -85, -7, -44, -1, 89, -110, -80, 53, 101, -80, -57, -49, 3, 30, 14, -99, -75, -71, -45, -77, 25, 94, 89, 8, -31, 21, -117, 71, -91, 73, 8, -91, 126, -45, 74, 126, 86, 123, -80, -5, -64, -97, 89, -75, 78, -22, -108, -82, -22, -6, 84, -45, -39, -86, 116, -61, 35, -16, 39, -53, -52, -39, 76, 16, -43, -43, 36, 11, 30, 58, 44, -58, 110, -90, -104, -28, 90, 79, -37, 55, -33, 108, 67, 39, -79, 88, 115, -117, 4, 70, -84, -35, -54, -9, 14, -88, -23, 94, -7, -80, -93, 15, 54, -51, 12, -59, -1, -105, -10, -65, 57, 83, 45, -39, 24, 43, -58, -13, 39, -17, -68, 20, 79, -124, 74, -47, -3, 40, -103, -101, 116, -125, -6, -22, -51, 66, -118, 38, 99, 69, -82, -9, -118, 86, 72, 87, 119, -37, -115, -116, -27, 2, 3, 1, 0, 1};
    public static final int AMOUNT = 10;

    @BeforeEach
    void initTestDB() throws BalanceExceptions.PublicKeyAlreadyExistException, BalanceExceptions.GeneralMYSQLException {
        DBConnection.testDB();
        Balance.openAccount(CREATEDUSERFROM);
        Balance.openAccount(CREATEDUSERTO);
    }

    @AfterEach
    void cleanTestDB() throws SQLException {
        DBConnection.testDB();
        Connection conn = DBConnection.getConnection();
        String transactionsQuery = "DELETE from TRANSACTIONS";
        Statement transactionsST = conn.createStatement();
        transactionsST.executeUpdate(transactionsQuery);
        String nonceQuery = "DELETE from NONCE";
        Statement nonceST = conn.createStatement();
        nonceST.executeUpdate(nonceQuery);
        String balanceQuery = "DELETE from BALANCE";
        Statement balanceST = conn.createStatement();
        balanceST.executeUpdate(balanceQuery);
    }

    @Test
    void addTransaction() {
        assertDoesNotThrow(() -> Transactions.addTransaction(CREATEDUSERFROM , CREATEDUSERTO , AMOUNT));
    }

    @Test
    void addTransactionWithZeroAmount(){
        assertThrows(TransactionsExceptions.AmountCanNotBeLessThenOneException.class , () -> Transactions.addTransaction(CREATEDUSERFROM , CREATEDUSERTO , 0));
    }

    @Test
    void addTransactionWithNegativeAmount(){
        assertThrows(TransactionsExceptions.AmountCanNotBeLessThenOneException.class , () -> Transactions.addTransaction(CREATEDUSERFROM , CREATEDUSERTO , -AMOUNT));
    }

    @Test
    void addTransactionNotCreatedSender(){
        assertThrows(TransactionsExceptions.SenderPublicKeyNotFoundException.class , () -> Transactions.addTransaction(NOTCREATEDUSER , CREATEDUSERTO , AMOUNT));
    }

    @Test
    void addTransactionNotCreatedReceiver(){
        assertThrows(TransactionsExceptions.ReceiverPublicKeyNotFoundException.class , () -> Transactions.addTransaction(CREATEDUSERFROM , NOTCREATEDUSER , AMOUNT));
    }

    @Test
    void addTransactionSenderAndReceiverAreTheSame(){
        assertThrows(TransactionsExceptions.CanNotSendMoneyToYourselfException.class , () -> Transactions.addTransaction(CREATEDUSERFROM , CREATEDUSERFROM , AMOUNT));
    }

    @Test
    void addTransactionSenderDoesNotHaveBalance(){
        assertThrows(TransactionsExceptions.BalanceNotEnoughException.class , () -> Transactions.addTransaction(CREATEDUSERFROM , CREATEDUSERTO , AMOUNT * 10000));
    }

    @Test
    void changeStatus() throws TransactionsExceptions.FailInsertTransactionException, TransactionsExceptions.BalanceNotEnoughException, TransactionsExceptions.CanNotSendMoneyToYourselfException, NonceExceptions.NonceNotFoundException, TransactionsExceptions.AmountCanNotBeLessThenOneException, BalanceExceptions.PublicKeyNotFoundException, TransactionsExceptions.PublicKeyNotFoundException, TransactionsExceptions.SenderPublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.ReceiverPublicKeyNotFoundException {
        Transactions.addTransaction(CREATEDUSERFROM , CREATEDUSERTO , AMOUNT);
        int id = Integer.parseInt(Transactions.getTransactions(CREATEDUSERTO).get(0).split("]")[0].replace("[" , ""));
        assertDoesNotThrow(() -> Transactions.changeStatus(id , CREATEDUSERTO));
    }

    @Test
    void changeStatusIDNotFound() {
        assertThrows(TransactionsExceptions.TransactionIDNotFoundException.class , () -> Transactions.changeStatus(-1 , CREATEDUSERTO));
    }

    @Test
    void changeStatusIDDoesNotBelongToYou() throws TransactionsExceptions.FailInsertTransactionException, TransactionsExceptions.BalanceNotEnoughException, TransactionsExceptions.CanNotSendMoneyToYourselfException, NonceExceptions.NonceNotFoundException, TransactionsExceptions.AmountCanNotBeLessThenOneException, BalanceExceptions.PublicKeyNotFoundException, TransactionsExceptions.PublicKeyNotFoundException, TransactionsExceptions.SenderPublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.ReceiverPublicKeyNotFoundException {
        Transactions.addTransaction(CREATEDUSERTO , CREATEDUSERFROM , AMOUNT);
        int id = Integer.parseInt(Transactions.getTransactions(CREATEDUSERFROM).get(0).split("]")[0].replace("[" , ""));
        assertThrows(TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException.class , () -> Transactions.changeStatus(id , CREATEDUSERTO));
    }

    @Test
    void changeStatusNotCreatedUser() {
        assertThrows(TransactionsExceptions.PublicKeyNotFoundException.class , () -> Transactions.changeStatus(1 , NOTCREATEDUSER));
    }

    @Test
    void changeStatusReplayAttack() throws TransactionsExceptions.FailInsertTransactionException, TransactionsExceptions.BalanceNotEnoughException, TransactionsExceptions.CanNotSendMoneyToYourselfException, NonceExceptions.NonceNotFoundException, TransactionsExceptions.AmountCanNotBeLessThenOneException, BalanceExceptions.PublicKeyNotFoundException, TransactionsExceptions.PublicKeyNotFoundException, TransactionsExceptions.SenderPublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.ReceiverPublicKeyNotFoundException, TransactionsExceptions.TransactionPublicKeyReceiverDontMatchException, TransactionsExceptions.TransactionAlreadyAcceptedException, TransactionsExceptions.TransactionIDNotFoundException {
        Transactions.addTransaction(CREATEDUSERFROM , CREATEDUSERTO , AMOUNT);
        int id = Integer.parseInt(Transactions.getTransactions(CREATEDUSERTO).get(0).split("]")[0].replace("[" , ""));
        Transactions.changeStatus(id , CREATEDUSERTO);
        assertThrows(TransactionsExceptions.TransactionAlreadyAcceptedException.class ,() -> Transactions.changeStatus(id , CREATEDUSERTO));
    }

    @Test
    void getPendingTransactions() throws TransactionsExceptions.FailInsertTransactionException, TransactionsExceptions.BalanceNotEnoughException, TransactionsExceptions.CanNotSendMoneyToYourselfException, NonceExceptions.NonceNotFoundException, TransactionsExceptions.AmountCanNotBeLessThenOneException, BalanceExceptions.PublicKeyNotFoundException, TransactionsExceptions.PublicKeyNotFoundException, TransactionsExceptions.SenderPublicKeyNotFoundException, BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.ReceiverPublicKeyNotFoundException {
        Transactions.addTransaction(CREATEDUSERFROM , CREATEDUSERTO , AMOUNT);
        assertTrue(!Transactions.getPendingTransactions(CREATEDUSERTO).isEmpty());
    }

    @Test
    void getPendingTransactionsNoTransactions() throws BalanceExceptions.GeneralMYSQLException, TransactionsExceptions.ReceiverPublicKeyNotFoundException, TransactionsExceptions.PublicKeyNotFoundException {
        assertTrue(Transactions.getPendingTransactions(CREATEDUSERTO).isEmpty());
    }

    @Test
    void getPendingTransactionsNotCreatedUser() {
        assertThrows(TransactionsExceptions.PublicKeyNotFoundException.class, () -> Transactions.getPendingTransactions(NOTCREATEDUSER));
    }
}