public class Transactions {

    public static void addTransaction(byte[] publicSender, byte[] publicReceiver, int amount) {
        // nonce??
        try {
            Connection conn = PostgreSQLJDBC.getConnection();
            String query = "INSERT INTO TRANSACTIONS (publicsender,publicreceiver,amount,completed) " + "VALUES (?,?,?,?);";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setByte(1, publicSender);
            ps.setByte(2, publicReceiver);
            ps.setInt(3, amount);
            ps.setString(4, 'Pending');
            ResultSet rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void changeStatus(int id) {
        try {
            Connection conn = PostgreSQLJDBC.getConnection();
            String query = "UPDATE TRANSACTIONS set completed = ? where id=?;";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, 'Completed');
            ps.setInt(2, id);
            ResultSet rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}