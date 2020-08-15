package com.zettro.java.cloudbox.client;

import com.zettro.java.cloudbox.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import sun.nio.ch.Net;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private boolean authPassed = false;
    private String username = "user1";
    private FileOutputStream fos = null;
    private int chunkCounter = 0;
    ConfirmationMessage confirmationMessage = new ConfirmationMessage();

    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> filesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        Thread t = new Thread(() -> {
            try {
                // заглушка для аутентификации на сервере
                Network.sendAuth(username);

                while (true) {
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
                     //   System.out.println("ChunkedFileMessage received");
                        Network.sendMsg(confirmationMessage);  // шлем подтверждение получения блока

                        ChunkedFileMessage cfm = (ChunkedFileMessage) am;
                     //   System.out.println(cfm.getBytesRead() + " bytes received in chunk #" + chunkCounter);
                        if (cfm.getBytesRead() != -1) {
                            if (cfm.getBytesRead() == 0) {
                               // System.out.println("Creating file " + username + "/" + cfm.getFilename());
                                fos = new FileOutputStream(CloudBoxClient.clientStoragePath + username + "/" + cfm.getFilename());
                                chunkCounter = 1;
                                //Files.write(Paths.get(CloudBoxClient.clientStoragePath + userName + "/" + cfm.getFilename()), new byte[0], StandardOpenOption.CREATE);
                            } else {
                              //  System.out.println("Writing " + chunkCounter + " chunk to " + username + "/" + cfm.getFilename() + "; Bytes read = " + cfm.getBytesRead());
                                fos.write(cfm.getData(), 0, cfm.getBytesRead());
                                chunkCounter++;
                                // Files.write(Paths.get(CloudBoxClient.clientStoragePath + userName + "/" + cfm.getFilename()), cfm.getData(), StandardOpenOption.APPEND);
                            }
                        } else {
                                fos.flush();
                                fos.close();
                                fos = null;
                         }
                        continue;
                    }

                    if (am instanceof ErrorMessage) {
                        ErrorMessage em = (ErrorMessage) am;
                        System.out.println(em.getMessage());
                        continue;
                    }

                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        if (tfFileName.getLength() > 0) {
            Network.sendMsg(new FileRequest(tfFileName.getText()));
            tfFileName.clear();
        }
    }

    public void refreshLocalFilesList() {
        Platform.runLater(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get(CloudBoxClient.clientStoragePath + username + "/"))
                        .filter(p -> !Files.isDirectory(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void pressOnSendBtn(ActionEvent actionEvent) throws IOException {
        if (filesList.getSelectionModel().getSelectedItem() != null) {
            ChunkedFileMessage cfm = new ChunkedFileMessage(
                    Paths.get(CloudBoxClient.clientStoragePath + username + "/" +
                            filesList.getSelectionModel().getSelectedItem()), 1024 * 1024);
            Network.sendMsg(cfm);
            while (cfm.readNextChunk() != -1) {
                Network.sendMsg(cfm);
            }
            Network.sendMsg(cfm);
            cfm.close();
        }
    }

    public void pressOnRefreshBtn(ActionEvent actionEvent) {
        refreshLocalFilesList();
    }
}
