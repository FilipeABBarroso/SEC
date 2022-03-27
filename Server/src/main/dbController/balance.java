public class Balance {

    public static int getBalance(byte[] publicKey) {
        try {
            Connection conn = PostgreSQLJDBC.getConnection();
            String query = "SELECT * FROM BALANCE WHERE publicKey=?;";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setByte(1, publicKey);
            ResultSet rs = ps.executeQuery();
            int balance = 0;
            if (rs.next()) {
                balance = rs.getString('balance');
            }
            return balance;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void creatUser(byte[] publicKey) {
        try {
            int initialBalance = 1000;
            Connection conn = PostgreSQLJDBC.getConnection();
            String query = "INSERT INTO BALANCE (publicKey,balance) " + "VALUES (?,?);";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setByte(1, publicKey);
            ps.setInt(2, initialBalance);
            ResultSet rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateBalance(int amount, byte[] publicKey) {
        try {
            int updatedBalance = getBalance() + amount;
            String query = "UPDATE BALANCE set balance = ? where publicKey=?;";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setByte(1, updatedBalance);
            ps.setInt(2, publicKey);
            ResultSet rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}