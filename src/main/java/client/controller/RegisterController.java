package client.controller;

import client.ChatLauncher;
import client.thread.Listener;
import client.util.ResizeHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;

public class RegisterController {

    @FXML
    private TextField usernameTextField;
    @FXML
    private TextField passwordTextField;

    public void registerButtonAction() {
        String username = usernameTextField.getText();
        String password = passwordTextField.getText();
        if(!username.isEmpty() && !password.isEmpty()) {
            Listener listener = new Listener(username, password,true, false);
            listener.setRegisterController(this);
            Thread x = new Thread(listener);
            x.start();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registration");
            alert.setHeaderText("Empty fields");
            alert.setContentText("Please fill username and password fields!");
            alert.showAndWait();
        }
    }

    public void showLoginScene() throws IOException {
        FXMLLoader fmxlLoader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
        Parent window = (Pane) fmxlLoader.load();
        Scene scene = new Scene(window);

        Platform.runLater(() -> {
            Stage stage = (Stage) usernameTextField.getScene().getWindow();
            stage.setOnCloseRequest((WindowEvent e) -> {
                Platform.exit();
                System.exit(0);
            });
            stage.setScene(scene);
            ResizeHelper.addResizeListener(stage);
            stage.centerOnScreen();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registration");
            alert.setHeaderText("Successful registration");
            alert.setContentText("Registration was successful!");
            alert.showAndWait();
        });
    }

    public void closeSystem(){
        Platform.exit();
        System.exit(0);
    }

    public void minimizeWindow(){
        ChatLauncher.getPrimaryStage().setIconified(true);
    }

    public void backPressed() throws IOException  {
        FXMLLoader fmxlLoader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
        Parent window = (Pane) fmxlLoader.load();
        Scene scene = new Scene(window);

        Platform.runLater(() -> {
            Stage stage = (Stage) usernameTextField.getScene().getWindow();
            stage.setOnCloseRequest((WindowEvent e) -> {
                Platform.exit();
                System.exit(0);
            });
            stage.setScene(scene);
            ResizeHelper.addResizeListener(stage);
            stage.centerOnScreen();
        });
    }

}
