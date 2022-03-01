package server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static ServerSocket server;
    private static Socket socket;

    private static final int PORT = 8189;
    private List<ClientHandler> clients;
    private SimpleAuthService authService;
    static ExecutorService executorService;
    private static final Logger LOGGER_APP = LogManager.getLogger("root");

    public Server() {
        clients = new CopyOnWriteArrayList<>();
        authService = new SimpleAuthService();
        executorService = Executors.newCachedThreadPool();

        try {
            server = new ServerSocket(PORT);
            LOGGER_APP.info("Server started");

            while (true) {
                socket = server.accept();
                // System.out.println(socket.getLocalSocketAddress());
                LOGGER_APP.info(socket.getLocalSocketAddress());
                //System.out.println("Client connect: " + socket.getRemoteSocketAddress());
                LOGGER_APP.info("Client connect: " + socket.getRemoteSocketAddress());
                new ClientHandler(this, socket);

            }

        } catch (IOException e) {
            LOGGER_APP.throwing(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER_APP.throwing(e);
            }
            try {
                // System.out.println("Server close");
                LOGGER_APP.info("Server close");
                server.close();
                authService.dataBase.disconnect();
                LOGGER_APP.info("db disconnect");
                executorService.shutdown();

            } catch (IOException e) {
                LOGGER_APP.throwing(e);
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("%s : %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void privateMsg(ClientHandler sender, String nickName, String msg) {
        for (ClientHandler c : clients) {
            if (nickName.equals(c.getNickname())) {
                c.sendMsg("Сообщение от " + msg);
                if (sender.equals(c)) {
                    return;
                }
                sender.sendMsg(msg);
                return;
            }
        }
        sender.sendMsg("Полльзователь с ником: " + nickName + " не найден");
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler client : clients) {
            if (client.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientList");
        for (ClientHandler client : clients) {
            sb.append(" ").append(client.getNickname());
        }
        String msg = sb.toString();
        for (ClientHandler client : clients) {
            client.sendMsg(msg);
        }
    }

    public void changeClientNickName(String nickName, String newNickName) {
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(nickName)) client.setNickname(newNickName);
            broadcastClientList();
        }
    }
}
