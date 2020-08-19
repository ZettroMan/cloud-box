package com.zettro.java.cloudbox.client;

import com.zettro.java.cloudbox.common.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.*;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class LocalPanelController implements Initializable {

    @FXML
    ComboBox<String> disksBox;
    @FXML
    TextField tfLocalPath;
    @FXML
    TableView<FileInfo> filesTable;

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

        disksBox.getItems().clear();
        for (Path path : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(path.toString());
        }
        disksBox.getSelectionModel().select(0);

        refreshFilesList(Paths.get(CloudBoxClient.clientStoragePath));
    }

    public void update() {
        refreshFilesList(Paths.get(tfLocalPath.getText()));
    }

    public void refreshFilesList(Path path) {
        Platform.runLater(()->{
            try {
                filesTable.getItems().clear();
                tfLocalPath.setText(path.normalize().toAbsolutePath().toString());
                filesTable.getItems().addAll(Files.list(path).filter(path1 -> {
                    try {
                        return (!Files.readAttributes(path1, DosFileAttributes.class).isHidden());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }).map(FileInfo::new).collect(Collectors.toList()));
                filesTable.sort();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось обновить локальный список файлов", ButtonType.OK);
                alert.showAndWait();
            }
        });
    }

    public void pressOnLocalUpBtn(ActionEvent actionEvent) {
        Path upperPath = Paths.get(tfLocalPath.getText()).getParent();
        if (upperPath != null) {
            refreshFilesList(upperPath);
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        refreshFilesList(Paths.get(disksBox.getSelectionModel().getSelectedItem()));
    }

    public void mouseClickAction(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            if(filesTable.getSelectionModel().getSelectedItem() == null) return;
            if(filesTable.getSelectionModel().getSelectedItem().getSize() != -1L) return;
            Path path = Paths.get(tfLocalPath.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
            refreshFilesList(path);
        }
    }
}
