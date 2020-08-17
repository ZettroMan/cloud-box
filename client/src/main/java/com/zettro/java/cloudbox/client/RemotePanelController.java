package com.zettro.java.cloudbox.client;

import com.zettro.java.cloudbox.common.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class RemotePanelController implements Initializable {

    @FXML
    TableView<FileInfo> filesTable;
    @FXML
    TextField tfRemotePath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Имя");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        fileNameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) text = "[ DIR ]";
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(120);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(150);


        filesTable.getColumns().addAll(fileNameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileSizeColumn);
    }


    public void refreshRemoteFilesList(FilesListMessage flm) {
        filesTable.getItems().clear();
        flm.getFiles().forEach(filesTable.getItems()::add);
        filesTable.sort();
    }


    public void pressOnRemoteUpBtn(ActionEvent actionEvent) {
        Network.sendMsg(new TraverseToFolderMessage(".."));
    }

    public void mouseClickAction(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            if(filesTable.getSelectionModel().getSelectedItem().getSize() != -1L) return;
            Network.sendMsg(new TraverseToFolderMessage(filesTable.getSelectionModel().getSelectedItem().getFilename()));
        }
    }


    public void pressOnRemoteRefreshBtn(ActionEvent actionEvent) {
        Network.sendMsg(new TraverseToFolderMessage("."));
    }

 }
