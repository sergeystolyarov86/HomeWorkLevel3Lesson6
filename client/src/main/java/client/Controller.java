package client;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {


    @FXML
    public TextArea textArea;
    @FXML
    public TextField textField;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public HBox authPanel;
    @FXML
    public HBox msgPanel;
    @FXML
    public ListView<String> clientList;


    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;

    private boolean authenticated;
    private String nickname;

    private Stage stage;
    private Stage regStage;
    private RegController regcontroller;


    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setManaged(authenticated);
        msgPanel.setVisible(authenticated);
        clientList.setManaged(authenticated);
        clientList.setVisible(authenticated);

        if (!authenticated) {
            nickname = "";
        }
        setTitle(nickname);
        textArea.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textArea.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (socket != null && !socket.isClosed()) {
                    try {
                        out.writeUTF("/end");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/auth_ok")) {
                                nickname = str.split("\\s+")[1];
                                setAuthenticated(true);
                                createFileOfHistory(nickname);
                                getHistoryMsg(nickname);
                                break;
                            }
                            if (str.startsWith("/reg_ok")) {
                                regcontroller.showResult("/reg_ok");
                            }
                            if (str.startsWith("/reg_no")) {
                                regcontroller.showResult("/reg_no");
                            }
                        } else {
                            textArea.appendText(str + "\n");

                        }
                    }
                    while (authenticated) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/clientList ")) {
                                String[] token = str.split("\\s+");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });

                            } else if (str.startsWith("/newNick")) {

                                String[] token = str.split("\\s");
                                changeNick(token[1], this.nickname);
                            }

                        } else {
                            textArea.appendText(str + "\n");
                            saveMsg(textArea.getText(), nickname);

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("disconnect");
                    setAuthenticated(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        String msg = String.format("/auth %s %s", loginField.getText().trim(), passwordField.getText().trim());
        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nickname) {
        Platform.runLater(() -> {
            if (nickname.equals("")) {
                stage.setTitle("ChatGB");
            } else stage.setTitle(String.format("ChatGB: Добро пожаловать %s", nickname));
        });
    }

    @FXML
    public void clickClientList() {
        String receiver = clientList.getSelectionModel().getSelectedItem();
        if (receiver.equals(nickname)) {
            textField.setText("/newNick ");
        } else textField.setText("/w " + receiver + " ");
    }

    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("ChatGB registration");
            regStage.setScene(new Scene(root, 400, 320));

            regcontroller = fxmlLoader.getController();
            regcontroller.setController(this);

            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.initStyle(StageStyle.UTILITY);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToReg() {
        if (regStage == null) {
            createRegWindow();
        }
        Platform.runLater(() -> regStage.show());
    }

    public void registration(String login, String password, String nickname) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        String msg = String.format("/reg %s %s %s", login, password, nickname);
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createFileOfHistory(String nickname) {
        String dirName = "history";
        File dir = new File("client/src/main/resources", dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String fileName = nickname + ".txt";
        File file = new File(dir, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveMsg(String msg, String nickname) {
        String fileName = "client/src/main/resources/history/" + nickname + ".txt";

        try (
                FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(msg + "\t");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getHistoryMsg(String nickname) {
        String fileName = "client/src/main/resources/history/" + nickname + ".txt";
        try (
                FileReader fileReader = new FileReader(fileName)) {
            BufferedReader reader = new BufferedReader(fileReader);
            StringBuilder story = new StringBuilder();
            while (reader.ready()) {
                story.append(reader.readLine()).append("\t");
            }
            String str = story.toString();

            String[] arrStr = str.split("\t");
            List<String> list = new ArrayList<>(Arrays.asList(arrStr));
            if (list.size() >= 100) {
                for (int i = list.size() - 100; i < list.size(); i++) {
                    textArea.appendText(list.get(i) + "\n");
                }
            } else {
                for (String s : list) {
                    textArea.appendText(s + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeNick(String newNick, String nickname) {
        File file = new File("client/src/main/resources/history/" + nickname + ".txt");
        File newNickFile = new File("client/src/main/resources/history/" + newNick + ".txt");
        file.renameTo(newNickFile);
    }
}



