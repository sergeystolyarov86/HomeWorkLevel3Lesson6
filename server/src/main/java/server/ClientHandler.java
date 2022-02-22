package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;
    private static ExecutorService executorService = Executors.newCachedThreadPool();


    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            executorService.execute(() -> {
                try {
                    socket.setSoTimeout(120000);
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            out.writeUTF("/end");

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
                                    System.out.println("Client authenticated.\n" +
                                            "Nick: " + nickname + "\n" +
                                            "Address: " + socket.getLocalSocketAddress());
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
                            String[] token = str.split("\\s+");
                            server.getAuthService().changeNick(this.nickname,token[1]);
                            server.changeClientNickName(this.nickname,token[1]);
                            out.writeUTF("/newNick"+" "+token[1]);

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
                        ex.printStackTrace();
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("Client: " + socket.getLocalSocketAddress() + " disconnect ");
                    try {
                        socket.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
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

