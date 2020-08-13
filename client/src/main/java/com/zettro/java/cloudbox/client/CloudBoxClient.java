package com.zettro.java.cloudbox.client;

import com.sun.javaws.IconUtil;
import com.sun.javaws.Main;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.sound.midi.ControllerEventListener;

public class CloudBoxClient extends Application {
    public final static String clientStoragePath = "C:/_Study/CloudBox/client_storage/";

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        MainController mc = loader.getController();
        primaryStage.setTitle("Simple cloud box client");
        primaryStage.setScene(new Scene(root));
        primaryStage.setOnHidden(e-> mc.shutdown());
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
