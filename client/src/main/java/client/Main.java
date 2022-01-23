package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.InputStream;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/client.fxml"));
        primaryStage.setTitle("ChatGB");
        primaryStage.setScene(new Scene(root, 550, 450));
        InputStream iconStream =
                getClass().getResourceAsStream("/image/icon.png");

        Image image = new Image(iconStream);
        primaryStage.getIcons().add(image);
        primaryStage.show();

    }


    public static void main(String[] args) {
        launch(args);
    }
}
