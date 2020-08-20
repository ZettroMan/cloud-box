package com.zettro.java.cloudbox.client;

import com.zettro.java.cloudbox.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class MainController implements Initializable {

    @FXML
    public TextField tfUsername;
    @FXML
    public PasswordField tfPassword;
    @FXML
    public TextField tfServerAddress;
    @FXML
    public TextField tfServerPort;
    @FXML
    public Button btnConnect;
    @FXML
    public CheckBox cbNewUser;
    @FXML
    VBox localPanel, remotePanel;

    LocalPanelController lpc;
    RemotePanelController rpc;
    private boolean authPassed = false;
    private FileOutputStream fos = null;
    private int chunkCounter = 0;
    ConfirmationMessage confirmationMessage = new ConfirmationMessage();
    FilesListRequest filesListRequest = new FilesListRequest();
    private boolean transferMode = false;
    Logger stdLogger = CloudBoxClient.stdLogger;
    String clientStoragePath = CloudBoxClient.clientStoragePath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        restoreSettings();
        lpc = (LocalPanelController) localPanel.getProperties().get("ctrl");
        rpc = (RemotePanelController) remotePanel.getProperties().get("ctrl");
    }

    private void restoreSettings() {
        try (FileInputStream fis = new FileInputStream("Settings.cbx");
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            ProgramSettings programSettings = (ProgramSettings) ois.readObject();
            tfUsername.textProperty().setValue(programSettings.getUsername());
            tfPassword.textProperty().setValue(programSettings.getPassword());
            tfServerAddress.textProperty().setValue(programSettings.getServerAddress());
            tfServerPort.textProperty().setValue(programSettings.getServerPort());
            stdLogger.info("Настройки программы успешно загружены!");

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveSettings() {
        try (FileOutputStream fos = new FileOutputStream("Settings.cbx");
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            ProgramSettings programSettings = new ProgramSettings(
                    tfUsername.textProperty().getValue(),
                    tfPassword.textProperty().getValue(),
                    tfServerAddress.textProperty().getValue(),
                    tfServerPort.textProperty().getValue());
            oos.writeObject(programSettings);
            stdLogger.info("Настройки программы сохранены!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile() throws IOException {
        if (lpc.filesTable.getSelectionModel().getSelectedItem() != null &&
                (lpc.filesTable.getSelectionModel().getSelectedItem().getSize() != -1L)) {
            ChunkedFileMessage cfm = new ChunkedFileMessage(Paths.get(lpc.tfLocalPath.getText(),
                    lpc.filesTable.getSelectionModel().getSelectedItem().getFilename()), 1024 * 1024);
            Network.sendMsg(cfm);
            while (cfm.readNextChunk() != -1) {
                Network.sendMsg(cfm);
            }
            Network.sendMsg(cfm);
            cfm.close();
            Network.sendMsg(filesListRequest);
        }
    }

    public void downloadFile() {
        if (rpc.filesTable.getSelectionModel().getSelectedItem() != null &&
                (rpc.filesTable.getSelectionModel().getSelectedItem().getSize() != -1L)) {
            Network.sendMsg(new FileRequest(rpc.filesTable.getSelectionModel().getSelectedItem().getFilename(),
                    FileRequest.ActionType.DOWNLOAD));
        }
    }

    public void pressOnConnectBtn(ActionEvent actionEvent) {
        if (!Network.isStarted()) {
            try {
                Network.start(tfServerAddress.getText(), Integer.parseInt(tfServerPort.getText()));
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось соединиться с сервером", ButtonType.OK);
                alert.show();
                return;
            }
            // запускаем поток, обрабатывающий входящие сообщения
            Thread t = new Thread(() -> {
                try {
                    lpc.update();
                    while (true) {
                        AbstractMessage am = Network.readObject();
                        if (am instanceof AuthAnswer) {
                            stdLogger.debug("AuthAnswer received");
                            AuthAnswer authAnswer = (AuthAnswer) am;
                            if (authAnswer.getAuthResult() == AuthAnswer.AuthResult.PASSED) {
                                authPassed = true;
                                stdLogger.info(authAnswer.getMessage());
                                Platform.runLater(() -> {
                                    btnConnect.setText("Отключиться");
                                    cbNewUser.setSelected(false);
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION, authAnswer.getMessage(), ButtonType.OK);
                                    alert.showAndWait();
                                });
                                Network.sendMsg(filesListRequest);
                                lpc.update();
                                continue;
                            }
                            authPassed = false;
                            stdLogger.info(authAnswer.getMessage());
                            if (cbNewUser.isSelected()) {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION, authAnswer.getMessage(), ButtonType.OK);
                                    alert.showAndWait();
                                });
                            } else {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION, authAnswer.getMessage(), ButtonType.OK);
                                    alert.showAndWait();
                                });
                            }
                            btnConnect.setText("Подключиться");
                            rpc.filesTable.getItems().clear();
                            lpc.update();
                            Network.stop(); // отключаем соединение полностью
                            break;
                        }
                        // пока не пройдена аутентификация - игнорируем сообщения от сервера, отличные от AuthAnswer
                        if (!authPassed) continue;

                        if (am instanceof ChunkedFileMessage) {
                            transferMode = true;
                            Network.sendMsg(confirmationMessage);  // шлем подтверждение получения блока

                            ChunkedFileMessage cfm = (ChunkedFileMessage) am;
                            if (cfm.getBytesRead() != -1) {
                                if (cfm.getBytesRead() == 0) {
                                    fos = new FileOutputStream(lpc.tfLocalPath.getText() + "\\" + cfm.getFileName());
                                    chunkCounter = 1;
                                } else {
                                    fos.write(cfm.getData(), 0, cfm.getBytesRead());
                                    chunkCounter++;
                                }
                            } else {
                                fos.close();
                                fos = null;
                                transferMode = false;
                                lpc.update();
                            }
                            continue;
                        }

                        if (am instanceof FilesListMessage) {
                            FilesListMessage flm = (FilesListMessage) am;
                            rpc.refreshRemoteFilesList(flm);
                            lpc.update();
                            continue;
                        }

                        if (am instanceof ErrorMessage) {
                            ErrorMessage em = (ErrorMessage) am;
                            stdLogger.error(em.getMessage());
                            transferMode = false;
                            continue;
                        }

                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                } finally {
                    authPassed = false;
                    lpc.update();
                    Network.stop();
                }
            });
            t.setDaemon(true);
            t.start();
            // посылаем запрос на аутентификацию
            Network.sendMsg(new AuthRequest(tfUsername.getText(), tfPassword.getText(), cbNewUser.isSelected()));

        } else {
            authPassed = false;
            btnConnect.setText("Подключиться");
            rpc.filesTable.getItems().clear();
            rpc.tfRemotePath.clear();
            lpc.update();
            Network.stop();
        }
    }

    public void pressOnCopyBtn(ActionEvent actionEvent) {
        if (!authPassed) return;
        if (lpc.filesTable.isFocused()) {
            try {
                sendFile();
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось передать файл", ButtonType.OK);
                alert.showAndWait();
            }
        } else if (rpc.filesTable.isFocused()) {
            downloadFile();
        }
    }

    public void pressOnDeleteBtn(ActionEvent actionEvent) {
        try {
            if (lpc.filesTable.isFocused() &&
                    (lpc.filesTable.getSelectionModel().getSelectedItem() != null)) {
                Path pathToFile = Paths.get(lpc.tfLocalPath.getText(),
                        lpc.filesTable.getSelectionModel().getSelectedItem().getFilename());
                if ((lpc.filesTable.getSelectionModel().getSelectedItem().getSize() == -1L)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Удаление каталогов пока не реализовано в целях безопасности))", ButtonType.OK);
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Удаление файла");
                    alert.setHeaderText("Удалить файл?");
                    alert.setContentText(pathToFile.toString());
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        Files.delete(pathToFile);
                        lpc.update();
                    }
                }
            } else if (authPassed && rpc.filesTable.isFocused() &&
                    (rpc.filesTable.getSelectionModel().getSelectedItem() != null)) {
                if ((rpc.filesTable.getSelectionModel().getSelectedItem().getSize() == -1L)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Удаление каталогов пока не реализовано в целях безопасности))", ButtonType.OK);
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Удаление файла");
                    alert.setHeaderText("Удалить файл?");
                    alert.setContentText(rpc.filesTable.getSelectionModel().getSelectedItem().getFilename());
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        Network.sendMsg(new FileRequest(rpc.filesTable.getSelectionModel().getSelectedItem().getFilename(),
                                FileRequest.ActionType.DELETE));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось удалить файл", ButtonType.OK);
            alert.showAndWait();
        }
    }


    public void pressOnMkDirBtn(ActionEvent actionEvent) {
        try {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Создание каталога");
            dialog.setHeaderText("Пожалуйста укажите имя каталога:");
            dialog.setContentText("");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (lpc.filesTable.isFocused()) {
                    Files.createDirectory(Paths.get(lpc.tfLocalPath.getText(), result.get()));
                    lpc.update();
                } else if (rpc.filesTable.isFocused()) {
                    Network.sendMsg(new FileRequest(rpc.filesTable.getSelectionModel().getSelectedItem().getFilename(),
                            FileRequest.ActionType.CREATE_DIR));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось создать каталог", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void shutdown() {
        saveSettings();
        Platform.exit();
    }

    public void btnExitAction(ActionEvent actionEvent) {
        shutdown();
    }
}
