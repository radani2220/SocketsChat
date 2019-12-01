package client.controller;

import client.ChatLauncher;
import client.controller.ChatController;
import client.thread.Listener;
import client.util.ResizeHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameTextField;
    @FXML private TextField passwordTextField;
    private ChatController chatController;
    private static LoginController instance;
    private Scene scene;

    public LoginController() {
        instance = this;
    }

    public static LoginController getInstance() {
        return instance;
    }

    public void loginButtonAction() throws IOException {
        String username = usernameTextField.getText();
        String password = passwordTextField.getText();

        FXMLLoader fmxlLoader = new FXMLLoader(getClass().getResource("/views/ChatView.fxml"));
        Parent window = (Pane) fmxlLoader.load();
        chatController = fmxlLoader.<ChatController>getController();
        Listener listener = new Listener(username, password, false, true);
        listener.setChatController(chatController);
        Thread x = new Thread(listener);
        x.start();
        this.scene = new Scene(window);
    }

    public void registerButtonAction() throws IOException  {
        FXMLLoader fmxlLoader = new FXMLLoader(getClass().getResource("/views/RegisterView.fxml"));
        Parent window = (Pane) fmxlLoader.load();
        this.scene = new Scene(window);

        Platform.runLater(() -> {
            Stage stage = (Stage) usernameTextField.getScene().getWindow();
            stage.setWidth(350);
            stage.setHeight(600);
            stage.setOnCloseRequest((WindowEvent e) -> {
                Platform.exit();
                System.exit(0);
            });
            stage.setScene(this.scene);
            ResizeHelper.addResizeListener(stage);
            stage.centerOnScreen();
        });
    }

    public void showChatScene() throws IOException {
        Platform.runLater(() -> {
            Stage stage = (Stage) usernameTextField.getScene().getWindow();
            stage.setResizable(true);
            stage.setWidth(1040);
            stage.setHeight(620);

            stage.setOnCloseRequest((WindowEvent e) -> {
                Platform.exit();
                System.exit(0);
            });
            stage.setScene(this.scene);
            stage.setMinWidth(800);
            stage.setMinHeight(300);
            ResizeHelper.addResizeListener(stage);
            stage.centerOnScreen();
            try {
                chatController.setUsernameLabel(usernameTextField.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void closeSystem(){
        Platform.exit();
        System.exit(0);
    }

    public void minimizeWindow(){
        ChatLauncher.getPrimaryStage().setIconified(true);
    }

    public void showErrorDialog(String message) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Connection error!");
            alert.setHeaderText(message);
            alert.setContentText("Can not connect with the server!");
            alert.showAndWait();
        });

    }
}
