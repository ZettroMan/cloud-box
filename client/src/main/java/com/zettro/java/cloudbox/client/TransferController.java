package com.zettro.java.cloudbox.client;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.net.URL;
import java.util.ResourceBundle;

public class TransferController implements Initializable {

    public Label operationDescription;
    public ProgressBar transferProgressBar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    }

    public void pressCancelBtn(ActionEvent actionEvent) {
    }
}
