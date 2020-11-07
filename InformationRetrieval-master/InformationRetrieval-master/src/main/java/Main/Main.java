package Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        URL url = getClass().getClassLoader().getResource("Main.fxml");
        Parent root = FXMLLoader.load(url);
//        URL url = getClass().getResource("/Main.fxml");
//        Parent root = FXMLLoader.load(url);

//        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("Main.fxml")));
        primaryStage.setTitle("ENGINE 4 YOU");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
