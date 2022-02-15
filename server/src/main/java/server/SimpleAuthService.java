package server;

import java.sql.*;


public class SimpleAuthService implements AuthService {

    private static Connection connection;
    private static Statement statement;

    public SimpleAuthService() {
        try {
            connect();
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTable() throws SQLException {
        String sql = "create table if not exists DataChat (" + "id integer primary key autoincrement not null,"+
                "login text not null,"+
                "password text not null,"+"" +
                "nickName"+");";
        statement.executeUpdate(sql);
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        String nickName = null;
        try (
                PreparedStatement preparedStatement =
                        connection.prepareStatement(
                                "SELECT nickName FROM DataChat WHERE login=? and DataChat.password=?")) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            nickName = preparedStatement.executeQuery().getString("nickName");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nickName;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {

        try (
                PreparedStatement preparedStatement = connection.prepareStatement
                        (" select login, password, nickName from DataChat ")) {
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                if (resultSet.getString("login").equals(login) ||
                        resultSet.getString("nickName").equals(nickname)) {
                    return false;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        try (
                PreparedStatement preparedStatement =
                        connection.prepareStatement(
                                "INSERT INTO DataChat (login, password, nickName) VALUES (?,?,?)")) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, nickname);
            preparedStatement.addBatch();
            preparedStatement.executeBatch();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public static void changeNick(String nickName,String newNickName){
        try(PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE DataChat SET nickName=? WHERE nickName=?;")){
            preparedStatement.setString(1,newNickName);
            preparedStatement.setString(2,nickName);
            preparedStatement.addBatch();
            preparedStatement.executeBatch();
        }catch (SQLException ex){
            ex.printStackTrace();
        }
    }


    public static void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:datadb.db");
        statement = connection.createStatement();
    }

    public void disconnect() {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
