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
    //returns the id of the item
    public int addItem(String name, int amount, String description, String producer, int price, int groupID) throws SQLException {
        Statement st = connection.createStatement();
        st.executeUpdate("insert into items (name, amount,  description, producer, price, groupID) " +
                "values ('" + name + "', " + amount + ", '" + description + "', '" + producer + "', " + price + ", " + groupID + ")");
        st = connection.createStatement();
        ResultSet resultSet = st.executeQuery("select * from items where name='" + name + "';");
        resultSet.next();
        return resultSet.getInt("id");
    }

    //add a group
    //returns the id of the group
    public int addGroup(String name, String description) throws SQLException {
        Statement st = connection.createStatement();
        st.executeUpdate("insert into item_groups (name,description) values ('" + name + "', '" + description + "')");
        st = connection.createStatement();
        ResultSet resultSet = st.executeQuery("select * from item_groups where name='" + name + "';");
        resultSet.next();
        return resultSet.getInt("id");
    }


    public ResultSet selectAllFromItems() throws SQLException {
        Statement test = connection.createStatement();
        return test.executeQuery("SELECT * FROM items");
    }

    public ResultSet itemByGroupID(int groupID) throws SQLException {
        Statement test = connection.createStatement();
        return test.executeQuery("SELECT * FROM items where groupID=" + groupID + ");");
    }

    public ResultSet selectAllFromGroups() throws SQLException {
        Statement test = connection.createStatement();
        return test.executeQuery("SELECT * FROM item_groups");
    }

    public ResultSet selectAllFromUsers() throws SQLException {
        Statement test = connection.createStatement();
        return test.executeQuery("SELECT * FROM users");
    }

    public ResultSet getItemByID(int id) throws SQLException {
        Statement st = connection.createStatement();
        return st.executeQuery("select * from items where id=" + id + ";");
    }
    public ResultSet getGroupByID(int id) throws SQLException {
        Statement st = connection.createStatement();
        return st.executeQuery("select * from item_groups where id=" + id + ";");
    }


    public void updateAmountByID(int id, int updatedAmount) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("update items set amount=" + updatedAmount + " where id=" + id + ";");
    }

    public void updateItemByID(int id, String name, int amount, String description, String producer, int price, int groupID) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("update items set " +
                "amount=" + amount + ", name='" + name +"', desciption='" + description + "'" +
                ", producer='" + producer + "',  groupID='" + groupID + "',  price=" + price +
                " where id=" + id +  ";");
     }
    public void updateGroupByID(int id, String name,String description) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("update item_groups set " +
                "description='" + description + "', name='" + name +"' where id=" + id + ";");

    }

    public void updateAmountByName(String name, int updatedAmount) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("update items set amount=" + updatedAmount + " where name='" + name + "';");
    }

    public void deleteItemByID(int id) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("delete from items where id=" + id + ";");
    }

    public void deleteItemByName(String name) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("delete from items where name='" + name + "';");
    }

    public void deleteGroupbyID(String name) throws SQLException {
        Statement test = connection.createStatement();
        test.executeUpdate("delete from items where name='" + name + "';");
    }

    public ResultSet listItemByName() throws SQLException {
        Statement test = connection.createStatement();
        return test.executeQuery("select * from items order by name");
    }

    public ResultSet listItemByID() throws SQLException {
        Statement test = connection.createStatement();
        return test.executeQuery("select * from items order by id");
    }

    public ResultSet listItemByAmount() throws SQLException {
        Statement test = connection.createStatement();
        return test.executeQuery("select * from items order by amount");
    }

}
