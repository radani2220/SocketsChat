package client.thread;

import client.controller.ChatController;
import client.controller.RegisterController;
import client.controller.LoginController;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import messages.ChatMessages;
import messages.Message;
import messages.MessageType;
import messages.UserCredentials;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

import static messages.MessageType.*;

public class Listener implements Runnable{

    private Socket socket;

    private static String username;
    private String password;

    private ChatController chatController;
    private RegisterController registerController;

    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;

    private boolean registrationProcess;
    private boolean loginProcess;


    public Listener(String username, String password, boolean registrationProcess, boolean loginProcess) {
        this.username = username;
        this.password = password;
        this.loginProcess = loginProcess;
        this.registrationProcess = registrationProcess;
    }

    public void setChatController(ChatController chatController) {
        this.chatController = chatController;
    }

    public void setRegisterController(RegisterController registerController) {
        this.registerController = registerController;
    }

    public void run() {
        if(socket == null) {
            try {
                String hostname = "localhost";
                int port = 9001;
                socket = new Socket(hostname, port);
                OutputStream outputStream = socket.getOutputStream();
                objectOutputStream = new ObjectOutputStream(outputStream);
                InputStream is = socket.getInputStream();
                objectInputStream = new ObjectInputStream(is);
            } catch (IOException e) {
                LoginController.getInstance().showErrorDialog("Could not connect to server");
            }
        }

        if(registrationProcess) {
            try {
                register(username, password);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(loginProcess) {
            try {
                if(login(username, password)) {
                    LoginController.getInstance().showChatScene();
                    connect();
                    checkUnseen();
                    while (socket.isConnected()) {
                        Object inputStreamObject = objectInputStream.readObject();
                        Message message = null;
                        ChatMessages chatMessages = null;
                        if(inputStreamObject instanceof Message) {
                            message = (Message) inputStreamObject;
                        } else if(inputStreamObject instanceof ChatMessages) {
                            chatMessages = (ChatMessages) inputStreamObject;
                        }

                        if (message != null) {
                            switch (message.getType()) {
                                case CONNECTED:
                                    chatController.setUserList(message);
                                    break;
                                case DISCONNECTED:
                                    chatController.setUserList(message);
                                    break;
                                case SEEN:
                                    chatController.checkUnseen(message);
                                    break;
                                case USER:
                                    chatController.addToChat(message);
                                    if(!message.getFrom().equals(username)) {
                                        if(message.getFrom().equals(chatController.getSelectedUser())) {
                                            markAsSeen();
                                        }
                                        checkUnseen();
                                    }
                                    break;
                            }
                        } else if(chatMessages != null) {
                            chatController.loadChat(chatMessages);
                        }
                    }
                }
                else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Unsuccessful controller");
                        alert.setContentText("Username or password are incorrect!");
                        alert.showAndWait();
                    });
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                chatController.logoutScene();
            }
        }
    }

    private void register(String username, String password) throws IOException  {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername(username);
        userCredentials.setPassword(password);
        userCredentials.setLogin(false);
        objectOutputStream.writeObject(userCredentials);
        objectOutputStream.flush();
        try {
            UserCredentials result = (UserCredentials) objectInputStream.readObject();
            if(result.getUsername() != null) {
                socket.close();
                loginProcess = true;
                registrationProcess = false;
                registerController.showLoginScene();
            } else {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Unsuccessful registration");
                    alert.setContentText("Username is already taken!");
                    alert.showAndWait();
                });
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean login(String username, String password) throws IOException {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername(username);
        userCredentials.setPassword(password);
        userCredentials.setLogin(true);
        objectOutputStream.writeObject(userCredentials);
        objectOutputStream.flush();
        try {
            Object signInResponse = objectInputStream.readObject();
            if(signInResponse instanceof Message) {
                if(((Message) signInResponse).getType() == CONNECTED) {
                    return true;
                }
            } else if(signInResponse instanceof UserCredentials) {
                if(userCredentials.getUsername() == null) {
                    return false;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void connect() throws IOException {
        Message message = new Message();
        message.setFrom(username);
        message.setType(CONNECTED);
        objectOutputStream.writeObject(message);
    }

    private static void checkUnseen() throws IOException {
        Message message = new Message();
        message.setFrom(username);
        message.setType(SEEN);
        objectOutputStream.writeObject(message);
    }

    private void markAsSeen() throws IOException {
        Message message = new Message();
        message.setFrom(chatController.getSelectedUser());
        message.setTo(username);
        message.setType(MARK_AS_SEEN);
        objectOutputStream.writeObject(message);
    }

    public static void send(String msg, String to) throws IOException {
        Message message = new Message();
        message.setFrom(username);
        message.setTo(to);
        message.setType(MessageType.USER);
        message.setMsg(msg);
        message.setSentAt(LocalDateTime.now());
        message.setSeen(false);
        objectOutputStream.writeObject(message);
        objectOutputStream.flush();
    }

    public static void load(String to) throws IOException {
        Message createMessage = new Message();
        createMessage.setFrom(username);
        createMessage.setTo(to);
        createMessage.setType(MessageType.LOAD_CHAT);
        objectOutputStream.writeObject(createMessage);
        objectOutputStream.flush();
        checkUnseen();
    }

}
