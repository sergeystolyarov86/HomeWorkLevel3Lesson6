package server;

public interface AuthService {

    String getNicknameByLoginAndPassword(String login,String password);

    boolean registration(String login,String password,String nickname);

      void changeNick(String nickName, String newNickName);

}
