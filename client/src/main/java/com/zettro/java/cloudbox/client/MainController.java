package com.zettro.java.cloudbox.client;

import com.zettro.java.cloudbox.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

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
    public ListView localFilesList;
    @FXML
    public ListView remoteFilesList;
    @FXML
    public Button btnConnect;
    @FXML
    public TextField tfLocalPath;

    private boolean authPassed = false;
    private FileOutputStream fos = null;
    private int chunkCounter = 0;
    ConfirmationMessage confirmationMessage = new ConfirmationMessage();
    FilesListRequest filesListRequest = new FilesListRequest();
    private boolean transferMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        restoreSettings();
        refreshLocalFilesList();
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

    public void refreshLocalFilesList() {
        Platform.runLater(() -> {
            try {
                localFilesList.getItems().clear();
                Files.list(Paths.get(CloudBoxClient.clientStoragePath))
                        .filter(p -> !Files.isDirectory(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> localFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void refreshRemoteFilesList(FilesListMessage flm) {
        Platform.runLater(() -> {
            remoteFilesList.getItems().clear();
            flm.getFiles().stream().map(FileInfo::getFileName).forEach(remoteFilesList.getItems()::add);
        });
    }

    public void sendFile() throws IOException {
        if (localFilesList.getSelectionModel().getSelectedItem() != null) {
            ChunkedFileMessage cfm = new ChunkedFileMessage(
                    Paths.get(CloudBoxClient.clientStoragePath +
                            localFilesList.getSelectionModel().getSelectedItem()), 1024 * 1024);
            Network.sendMsg(cfm);
            while (cfm.readNextChunk() != -1) {
                Network.sendMsg(cfm);
            }
            Network.sendMsg(cfm);
            cfm.close();
        }
    }

    public void downloadFile() {
        if (remoteFilesList.getSelectionModel().getSelectedItem() != null) {
            Network.sendMsg(new FileRequest(remoteFilesList.getSelectionModel().getSelectedItem().toString()));
        }
    }

    public void pressOnLocalUpBtn(ActionEvent actionEvent) {
    }

    public void pressOnLocalRefreshBtn(ActionEvent actionEvent) {
        refreshLocalFilesList();
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
                        if (!transferMode & authPassed) Network.sendMsg(filesListRequest);
                        refreshLocalFilesList();
                        AbstractMessage am = Network.readObject();
                        // System.out.println("Message received");
                        if (am instanceof AuthAnswer) {
                            System.out.println("AuthAnswer received");
                            AuthAnswer authAnswer = (AuthAnswer) am;
                            if (authAnswer.getAuthResult() == AuthAnswer.AuthResult.PASSED) {
                                authPassed = true;
                                System.out.println("Authentication passed");
                                continue;
                            }
                            System.out.println("Authentication failed");
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
                                fos.flush();
                                fos.close();
                                fos = null;
                                transferMode = false;
                            }
                            continue;
                        }

                        if (am instanceof FilesListMessage) {
                            FilesListMessage flm = (FilesListMessage) am;
                            refreshRemoteFilesList(flm);
                            continue;
                        }

                        if (am instanceof ErrorMessage) {
                            ErrorMessage em = (ErrorMessage) am;
                            System.out.println(em.getMessage());
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
            btnConnect.textProperty().setValue("Отключиться");
            // посылаем запрос на аутентификацию
            Network.sendMsg(new AuthRequest(tfUsername.getText()));

        } else {
            authPassed = false;
            btnConnect.textProperty().setValue("Соединиться");
            Network.stop();
        }
    }

    public void pressOnCopyBtn(ActionEvent actionEvent) {
        if (!authPassed) return;
        if (localFilesList.isFocused()) {
            try {
                sendFile();
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось передать файл", ButtonType.OK);
                alert.show();
            }
        } else if (remoteFilesList.isFocused()) {
            downloadFile();
        }
    }

    public void pressOnDeleteBtn(ActionEvent actionEvent) {
        if (!authPassed) return;
        // some code
    }

    public void shutdown() {
        saveSettings();
        System.out.println("Settings saved!");
        Platform.exit();
    }

    public void pressOnMkDirBtn(ActionEvent actionEvent) {
    }

    public void btnExitAction(ActionEvent actionEvent) {
        shutdown();
    }
}
