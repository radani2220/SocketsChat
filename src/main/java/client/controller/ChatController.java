package client.controller;

import client.ChatLauncher;
import client.thread.Listener;
import client.util.UserListViewItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import messages.ChatMessages;
import messages.Message;
import messages.User;
import messages.bubble.BubbleSpec;
import messages.bubble.BubbledLabel;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static messages.Status.ONLINE;

public class ChatController implements Initializable {

    @FXML private TextArea messageBox;
    @FXML private Label usernameLabel;
    @FXML private Label onlineCountLabel;
    @FXML private Label labelChatWith;
    @FXML private ListView userList;
    @FXML ListView chatPane;
    @FXML BorderPane borderPane;

    private String selectedUser;
    private List<Message> messages = new ArrayList<>();
    private boolean requestFinished = true;

    private double xOffset;
    private double yOffset;

    public String getSelectedUser() {
        return selectedUser;
    }

    @FXML public void handleMouseClick(MouseEvent arg0) {
        if(requestFinished) {
            requestFinished = false;
            int index = userList.getSelectionModel().getSelectedIndex();
            User user = (User) userList.getItems().get(index);
            this.selectedUser = user.getName();
            this.labelChatWith.setText(selectedUser);

            if(!selectedUser.equals(usernameLabel.getText())) {
                chatPane.getItems().clear();
                try {
                    loadMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void checkUnseen(Message msg) throws IOException {
        Platform.runLater(() -> {
            msg.getUsers().removeIf(x -> x.getName().equals(usernameLabel.getText()));
            Optional<User> first = msg.getUsers().stream().filter(x -> x.getName().equals(selectedUser)).findFirst();
            if(first.isPresent()) {
                first.get().setUnseenMessage(false);
            }
            msg.getUsers().sort((o1, o2) -> o2.getStatus().toString().compareTo(o1.getStatus().toString()));
            ObservableList<User> users = FXCollections.observableList(msg.getUsers());
            userList.setItems(users);
            userList.setCellFactory(new UserListViewItem());
        });
    }

    public void sendButtonAction() throws IOException {
        String msg = messageBox.getText();
        if (!messageBox.getText().isEmpty()) {
            Listener.send(msg, selectedUser);
            messageBox.clear();
        }
    }

    private synchronized void loadMessages() throws IOException {
       Listener.load(selectedUser);
    }

    public void loadChat(ChatMessages chatMessages) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        Platform.runLater(() -> {
            Task<List<HBox>> messages = new Task<List<HBox>>() {
                @Override
                public List<HBox> call() throws Exception {
                    List<HBox> all = new ArrayList<>();
                    for (Message msg : chatMessages.getMessages()) {
                        if (msg.getFrom().equals(usernameLabel.getText())) {

                            BubbledLabel bl6 = new BubbledLabel();
                            bl6.setText(msg.getMsg());

                            bl6.setBackground(new Background(new BackgroundFill(Color.CORNFLOWERBLUE,
                                    null, null)));
                            HBox x = new HBox();
                            Text sentAt = new Text(formatter.format(msg.getSentAt()));
                            sentAt.setStyle("-fx-font: 12 arial;");
                            sentAt.setFill(Color.WHITE);
                            x.setMaxWidth(chatPane.getWidth() - 20);
                            x.setAlignment(Pos.CENTER_RIGHT);
                            x.setSpacing(10);
                            bl6.setBubbleSpec(BubbleSpec.FACE_RIGHT_CENTER);
                            x.getChildren().addAll(sentAt, bl6);

                            all.add(x);
                        } else {
                            BubbledLabel bl6 = new BubbledLabel();
                            if(msg.getTo().equals("Group")) {
                                bl6.setText(msg.getFrom() + ": " + msg.getMsg());
                            } else {
                                bl6.setText(msg.getMsg());
                            }
                            bl6.setBackground(new Background(new BackgroundFill(Color.DARKSEAGREEN, null, null)));
                            HBox x = new HBox();
                            x.setSpacing(10);
                            x.setAlignment(Pos.CENTER_LEFT);
                            Text sentAt = new Text(formatter.format(msg.getSentAt()));
                            sentAt.setStyle("-fx-font: 12 arial;");
                            sentAt.setFill(Color.WHITE);
                            bl6.setBubbleSpec(BubbleSpec.FACE_LEFT_CENTER);
                            x.getChildren().addAll(bl6, sentAt);
                            all.add(x);
                        }
                    }
                    return all;
                }
            };

            messages.setOnSucceeded(event -> {
                chatPane.getItems().addAll(messages.getValue());
                chatPane.scrollTo(chatPane.getItems().size());
                requestFinished = true;
            });

            Thread t = new Thread(messages);
            t.setDaemon(true);
            t.start();

            setOnlineLabel(chatMessages.getOnlineCount()-1);
        });

    }

    public synchronized void addToChat(Message msg) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        messages.add(msg);
        long onlineCount = msg.getUsers().stream().filter(user -> user.getStatus() == ONLINE).count();
        setOnlineLabel((int) onlineCount - 1);
        if((!selectedUser.equals("Group") && (msg.getFrom().equals(usernameLabel.getText()) && msg.getTo().equals(selectedUser))) ||
                (!selectedUser.equals("Group") && (msg.getFrom().equals(selectedUser) && msg.getTo().equals(usernameLabel.getText()))) ||
                (selectedUser.equals("Group") && msg.getTo().equals(selectedUser))) {
            Task<HBox> othersMessages = new Task<HBox>() {
                @Override
                public HBox call() throws Exception {
                    BubbledLabel bl6 = new BubbledLabel();
                    if(msg.getTo().equals("Group")) {
                        bl6.setText(msg.getFrom() + ": " + msg.getMsg());
                    } else {
                        bl6.setText(msg.getMsg());
                    }
                    bl6.setBackground(new Background(new BackgroundFill(Color.DARKSEAGREEN,null, null)));
                    HBox x = new HBox();
                    x.setSpacing(10);
                    x.setAlignment(Pos.CENTER_LEFT);
                    bl6.setBubbleSpec(BubbleSpec.FACE_LEFT_CENTER);
                    Text sentAt = new Text(formatter.format(msg.getSentAt()));
                    sentAt.setStyle("-fx-font: 12 arial;");
                    sentAt.setFill(Color.WHITE);
                    x.getChildren().addAll(bl6, sentAt);

                    return x;
                }
            };

            othersMessages.setOnSucceeded(event -> {
                chatPane.getItems().add(othersMessages.getValue());
                chatPane.scrollTo(chatPane.getItems().size());
            });

            Task<HBox> yourMessages = new Task<HBox>() {
                @Override
                public HBox call() throws Exception {
                    BubbledLabel bl6 = new BubbledLabel();
                    bl6.setText(msg.getMsg());
                    bl6.setBackground(new Background(new BackgroundFill(Color.CORNFLOWERBLUE,
                            null, null)));
                    HBox x = new HBox();
                    x.setSpacing(10);
                    x.setMaxWidth(chatPane.getWidth() - 20);
                    x.setAlignment(Pos.CENTER_RIGHT);
                    bl6.setBubbleSpec(BubbleSpec.FACE_RIGHT_CENTER);
                    Text sentAt = new Text(formatter.format(msg.getSentAt()));
                    sentAt.setStyle("-fx-font: 12 arial;");
                    sentAt.setFill(Color.WHITE);
                    x.getChildren().addAll(sentAt, bl6);


                    return x;
                }
            };
            yourMessages.setOnSucceeded(event -> {
                chatPane.getItems().add(yourMessages.getValue());
                chatPane.scrollTo(chatPane.getItems().size());
            });

            if (msg.getFrom().equals(usernameLabel.getText())) {
                Thread t2 = new Thread(yourMessages);
                t2.setDaemon(true);
                t2.start();
            } else {
                Thread t = new Thread(othersMessages);
                t.setDaemon(true);
                t.start();
            }
        }
    }

    public void setUsernameLabel(String username) throws IOException {
        this.usernameLabel.setText(username);
        loadMessages();
    }

    private void setOnlineLabel(int usercount) {
        Platform.runLater(() -> onlineCountLabel.setText(String.valueOf(usercount - 1)));
    }

    public void setUserList(Message msg) {
        Platform.runLater(() -> {
            msg.getUsers().removeIf(x -> x.getName().equals(usernameLabel.getText()));
            msg.getUsers().sort((o1, o2) -> o2.getStatus().toString().compareTo(o1.getStatus().toString()));
            ObservableList<User> users = FXCollections.observableList(msg.getUsers());
            userList.setItems(users);
            userList.setCellFactory(new UserListViewItem());
            long onlineCount = users.stream().filter(user -> user.getStatus() == ONLINE).count();
            setOnlineLabel((int)onlineCount);
        });
    }

    public void sendMethod(KeyEvent event) throws IOException {
        if (event.getCode() == KeyCode.ENTER) {
            sendButtonAction();
        }
    }

    @FXML
    public void closeApplication()throws IOException {
        Platform.exit();
        System.exit(0);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
                /* Drag and Drop */
        borderPane.setOnMousePressed(event -> {
            xOffset = ChatLauncher.getPrimaryStage().getX() - event.getScreenX();
            yOffset = ChatLauncher.getPrimaryStage().getY() - event.getScreenY();
            borderPane.setCursor(Cursor.CLOSED_HAND);
        });

        borderPane.setOnMouseDragged(event -> {
            ChatLauncher.getPrimaryStage().setX(event.getScreenX() + xOffset);
            ChatLauncher.getPrimaryStage().setY(event.getScreenY() + yOffset);

        });

        borderPane.setOnMouseReleased(event -> {
            borderPane.setCursor(Cursor.DEFAULT);
        });

        /* Added selectedUser prevent the enter from adding a new line selectedUser inputMessageBox */
        messageBox.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                try {
                    sendButtonAction();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ke.consume();
            }
        });

        selectedUser = "Group";

    }

    public void logoutScene() {
        Platform.runLater(() -> {
            FXMLLoader fmxlLoader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Parent window = null;
            try {
                window = (Pane) fmxlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Stage stage = ChatLauncher.getPrimaryStage();
            Scene scene = new Scene(window);
            stage.setMaxWidth(350);
            stage.setMaxHeight(600);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.centerOnScreen();
        });
    }
}