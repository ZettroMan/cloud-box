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
import java.nio.file.Paths;
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
    VBox localPanel, remotePanel;

    LocalPanelController lpc;
    RemotePanelController rpc;
    private boolean authPassed = false;
    private FileOutputStream fos = null;
    private int chunkCounter = 0;
    ConfirmationMessage confirmationMessage = new ConfirmationMessage();
    FilesListRequest filesListRequest = new FilesListRequest();
    private boolean transferMode = false;
    Logger console = Logger.getLogger("console");
    Logger debug = Logger.getLogger("debug");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        restoreSettings();
        lpc = (LocalPanelController) localPanel.getProperties().get("ctrl");
        rpc = (RemotePanelController) remotePanel.getProperties().get("ctrl");
        lpc.update();
    }

    private void restoreSettings() {
        try (FileInputStream fis = new FileInputStream("Settings.cbx");
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            ProgramSettings programSettings = (ProgramSettings) ois.readObject();
            tfUsername.textProperty().setValue(programSettings.getUsername());
            tfPassword.textProperty().setValue(programSettings.getPassword());
            tfServerAddress.textProperty().setValue(programSettings.getServerAddress());
            tfServerPort.textProperty().setValue(programSettings.getServerPort());
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile() throws IOException {
        if (lpc.filesTable.getSelectionModel().getSelectedItem() != null) {
            ChunkedFileMessage cfm = new ChunkedFileMessage(
                    Paths.get(CloudBoxClient.clientStoragePath +
                            lpc.filesTable.getSelectionModel().getSelectedItem()), 1024 * 1024);
            Network.sendMsg(cfm);
            while (cfm.readNextChunk() != -1) {
                Network.sendMsg(cfm);
            }
            Network.sendMsg(cfm);
            cfm.close();
        }
    }

    public void downloadFile() {
        if (rpc.filesTable.getSelectionModel().getSelectedItem() != null) {
            Network.sendMsg(new FileRequest(rpc.filesTable.getSelectionModel().getSelectedItem().toString()));
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
                    while (true) {
                        //if (!transferMode & authPassed) Network.sendMsg(filesListRequest);
                        lpc.update();
                        AbstractMessage am = Network.readObject();
                        if (am instanceof AuthAnswer) {
                            debug.info("AuthAnswer received");
                            AuthAnswer authAnswer = (AuthAnswer) am;
                            if (authAnswer.getAuthResult() == AuthAnswer.AuthResult.PASSED) {
                                authPassed = true;
                                console.info("Успешная аутентификация");
                                Platform.runLater(() -> {
                                    btnConnect.setText("Отключиться");
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Успешная аутентификация", ButtonType.OK);
                                    alert.showAndWait();
                                });
                                Network.sendMsg(filesListRequest);
                                continue;
                            }
                            authPassed = false;
                            console.info("Неудачная аутентификация");
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Указаны неверные данные для аутентификации", ButtonType.OK);
                                alert.showAndWait();
                            });
                            btnConnect.setText("Подключиться");
                            rpc.filesTable.getItems().clear();
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
                                    fos = new FileOutputStream(CloudBoxClient.clientStoragePath + cfm.getFileName());
                                    chunkCounter = 1;
                                } else {
                                    fos.write(cfm.getData(), 0, cfm.getBytesRead());
                                    chunkCounter++;
                                }
                            } else {
                                //fos.flush();  - вероятно это излишне
                                fos.close();
                                fos = null;
                                transferMode = false;
                            }
                            continue;
                        }

                        if (am instanceof FilesListMessage) {
                            FilesListMessage flm = (FilesListMessage) am;
                            rpc.refreshRemoteFilesList(flm);
                            continue;
                        }

                        if (am instanceof ErrorMessage) {
                            ErrorMessage em = (ErrorMessage) am;
                            debug.error(em.getMessage());
                            transferMode = false;
                            continue;
                        }

                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                } finally {
                    authPassed = false;
                    Network.stop();
                }
            });
            t.setDaemon(true);
            t.start();
            // посылаем запрос на аутентификацию
            Network.sendMsg(new AuthRequest(tfUsername.getText(), tfPassword.getText()));

        } else {
            authPassed = false;
            btnConnect.setText("Подключиться");
            rpc.filesTable.getItems().clear();
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
                alert.show();
            }
        } else if (rpc.filesTable.isFocused()) {
            downloadFile();
        }
    }

    public void pressOnDeleteBtn(ActionEvent actionEvent) {
        if (!authPassed) return;
        // some code
    }

    public void pressOnMkDirBtn(ActionEvent actionEvent) {
    }

    public void shutdown() {
        saveSettings();
        console.info("Настройки сохранены!");
        Platform.exit();
    }

    public void btnExitAction(ActionEvent actionEvent) {
        shutdown();
    }
}
