package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;


public class SimpleAuthService implements AuthService {
    private static final Logger LOGGER = LogManager.getLogger();
    DataBase dataBase;

    SimpleAuthService() {
        dataBase = new DataBase();
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        String nickName = null;
        try {
            DataBase.getNicknameStatement.setString(1, login);
            DataBase.getNicknameStatement.setString(2, password);
            nickName = DataBase.getNicknameStatement.executeQuery().getString("nickName");
        } catch (SQLException e) {
            LOGGER.throwing(e);
        }
        return nickName;
    }


    @Override
    public boolean registration(String login, String password, String nickname) {

        try {
            ResultSet resultSet = DataBase.getLogPassNicknameStatement.executeQuery();
            while (resultSet.next()) {
                if (resultSet.getString("login").equals(login) ||
                        resultSet.getString("nickName").equals(nickname)) {
                    return false;
                }
            }
        } catch (SQLException ex) {
            LOGGER.throwing(ex);
        }
        try {
            DataBase.regInsertStatement.setString(1, login);
            DataBase.regInsertStatement.setString(2, password);
            DataBase.regInsertStatement.setString(3, nickname);
            DataBase.regInsertStatement.addBatch();
            DataBase.regInsertStatement.executeBatch();

        } catch (SQLException ex) {
            LOGGER.throwing(ex);
        }
        return true;
    }

    @Override
    public void changeNick(String nickName, String newNickName) {
        try {
            DataBase.changeNickStatement.setString(1, newNickName);
            DataBase.changeNickStatement.setString(2, nickName);
            DataBase.changeNickStatement.addBatch();
            DataBase.changeNickStatement.executeBatch();
        } catch (SQLException ex) {
            LOGGER.throwing(ex);
        }
    }
}



