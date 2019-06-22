import java.sql.*;

public class JDBCservice {
    private Connection connection;

    public JDBCservice() throws SQLException {
        String user = "root";
        String password = "root";
        String connectionURL = "jdbc:mysql://localhost:3306/ClientServer";
        connection = DriverManager.getConnection(connectionURL, user, password);

    }

    //add an item
    public int Create(String name, int amount) throws SQLException {
        Statement st = connection.createStatement();
        st.executeUpdate("insert into items (name, amount) values ('" + name + "', " + amount + ")");
        st = connection.createStatement();
        ResultSet resultSet = st.executeQuery("select * from items where name='"+name+"';");
        resultSet.next();
       return resultSet.getInt("id");
    }


    public ResultSet Read() throws SQLException {
        Statement test = connection.createStatement();
        return test.executeQuery("SELECT * FROM items");
    }

    public ResultSet GetById(int id) throws SQLException {
        Statement st = connection.createStatement();
        return st.executeQuery("select * from items where id="+id+";");
    }

    public void UpdateAmountByID(int id, int updatedAmount) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("update items set amount=" + updatedAmount + " where id=" + id + ";");
    }
    public void UpdateByID(int id, String updatedName, int updatedAmount) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("update items set amount=" + updatedAmount + " where id=" + id + ";");
        test.executeUpdate("update items set name='" + updatedName + "' where id=" + id + ";");
    }


    public void UpdateAmountByName(String name, int updatedAmount) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("update items set amount=" + updatedAmount + " where name='" + name + "';");
    }

    public void DeleteByID(int id) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("delete from items where id=" + id + ";");
    }

    public void DeleteByName(String name) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("delete from items where name='" + name + "';");
    }

    public ResultSet ListByName() throws SQLException{
        Statement test = connection.createStatement();
       return test.executeQuery("select * from items order by name");
    }
    public ResultSet ListByID() throws SQLException{
        Statement test = connection.createStatement();
        return test.executeQuery("select * from items order by id");
    }
    public ResultSet ListByAmount() throws SQLException{
        Statement test = connection.createStatement();
        return test.executeQuery("select * from items order by amount");
    }


}
