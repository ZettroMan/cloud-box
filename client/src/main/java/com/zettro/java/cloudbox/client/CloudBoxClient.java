package com.zettro.java.cloudbox.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

public class CloudBoxClient extends Application {
    public final static String clientStoragePath = "C:\\_Study\\CloudBox\\client_storage";
    public final static Logger stdLogger = Logger.getRootLogger();
    public final static Stage transferStage = new Stage();
    public static TransferController transferController = null;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        MainController mc = loader.getController();
        primaryStage.setTitle("Simple cloud box client");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(new Scene(root));
        primaryStage.setOnHidden(e-> mc.shutdown());

        loader = new FXMLLoader(getClass().getResource("/transfer.fxml"));
        root = loader.load();
        transferStage.setScene(new Scene(root));
        transferStage.setAlwaysOnTop(true);
        transferStage.setResizable(false);
        transferStage.setTitle("Идет передача файла");
        transferController = loader.getController();
        //transferStage.initModality(APPLICATION_MODAL);

        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
