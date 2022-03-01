package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;
    private static final Logger LOGGER = LogManager.getLogger();



    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
           Server.executorService.execute(() -> {
                try {
                    socket.setSoTimeout(120000);
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            out.writeUTF("/end");

                            LOGGER.info("Клиент  решил отключиться");
                            throw new RuntimeException("Клиент решил отключиться");

                        }
                        // Auth
                        if (str.startsWith("/auth")) {
                            String[] token = str.split("\\s+");
                            if (token.length < 3) {
                                continue;
                            }


                            String newNick = server
                                    .getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            if (newNick != null) {
                                login = token[1];
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg("/auth_ok " + nickname);
                                    server.subscribe(this);
                                    LOGGER.info("Client authenticated.\n" +
                                            "Nick: " + nickname + "\n" +
                                            "Address: " + socket.getLocalSocketAddress());
//                                    System.out.println("Client authenticated.\n" +
//                                            "Nick: " + nickname + "\n" +
//                                            "Address: " + socket.getLocalSocketAddress());
                                    socket.setSoTimeout(0);
                                    break;
                                } else {
                                    sendMsg("Пользователь с таким логином уже авторизовался");
                                }
                            } else {
                                sendMsg("Неверный логин/пароль");
                            }

                        }
                        // Reg
                        if (str.startsWith("/reg ")) {
                            String[] token = str.split("\\s+", 4);
                            if (token.length < 4) {
                                continue;
                            }
                            boolean b = server.getAuthService().registration(token[1], token[2], token[3]);

                            if (b) {
                                sendMsg("/reg_ok");
                                LOGGER.info(String.format
                                        ("Зарегистрирован новый пользователь c  ником %s",token[3]));
                            } else {
                                sendMsg("/reg_no");
                            }
                        }
                    }
                    // Work
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            out.writeUTF("/end");

                            break;
                        }else if(str.startsWith("/newNick")){
                            String name = this.nickname;
                            String[] token = str.split("\\s+");
                            server.changeClientNickName(this.nickname,token[1]);
                            out.writeUTF("/newNick"+" "+token[1]);
                            server.getAuthService().changeNick(this.nickname,token[1]);
                            LOGGER.info(String.format
                                    ("Пользователь %s сменил ник на %s",name,token[1]));
                        }
                        else if (str.startsWith("/w")) {
                            String[] token = str.split("\\s+", 3);
                            server.privateMsg(this, token[1], this.nickname + ": " + token[2]);
                        } else server.broadcastMsg(this, str);
                    }
                } catch (SocketTimeoutException e) {
                    try {
                        out.writeUTF("/end)");
                    } catch (IOException ex) {
                        LOGGER.throwing(ex);
                    }
                } catch (RuntimeException e) {
                    LOGGER.throwing(e);
                } catch (IOException e) {
                    LOGGER.throwing(e);
                } finally {
                    server.unsubscribe(this);
                    LOGGER.info("Client: " + socket.getLocalSocketAddress() + " disconnect ");
                    try {
                        socket.close();

                    } catch (IOException e) {
                        LOGGER.throwing(e);
                    }
                }
            });

        } catch (IOException e) {
            LOGGER.throwing(e);
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            LOGGER.throwing(e);
        }
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getLogin() {
        return login;
    }
}

