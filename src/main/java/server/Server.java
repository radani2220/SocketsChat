package server;

import messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.exception.DuplicateUsernameException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static messages.MessageType.LOAD_CHAT;
import static messages.MessageType.SEEN;
import static messages.Status.OFFLINE;
import static messages.Status.ONLINE;


public class Server {

    private static final int PORT = 9001;
    private static HashMap<String, ObjectOutputStream> writers = new HashMap<>();
    private static ArrayList<User> users = new ArrayList<>();
    static Logger logger = LoggerFactory.getLogger(Server.class);
    private static Connection databaseConnection = null;

    public static void main(String[] args) throws Exception {
        logger.info("The chat server is running.");
        loadAllRegisteredUsersFromDatabase();
        ServerSocket listener = new ServerSocket(PORT);

        User user = new User();
        user.setName("Group");
        user.setStatus(ONLINE);
        users.add(user);

        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            listener.close();
        }
    }

    private static void loadAllRegisteredUsersFromDatabase() {
        try {
            //Registering the HSQLDB JDBC driver
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            //Creating the connection with HSQLDB
            databaseConnection = DriverManager.getConnection("jdbc:hsqldb:file:C:\\hsqldb-2.4.1\\hsqldb\\hsqldb\\chat", "SA", "");
            if (databaseConnection != null) {
                System.out.println("Connection created successfully");
                Statement statement;
                try {
                    statement = databaseConnection.createStatement();
                    String query = "SELECT * FROM USER;";
                    ResultSet resultSet = statement.executeQuery(query);
                    while (resultSet.next()) {
                        User user = new User();
                        user.setName(resultSet.getString("USERNAME"));
                        user.setStatus(OFFLINE);
                        users.add(user);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Problem with creating connection");
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private static class Handler extends Thread {
        private String name;
        private Socket clientSocket;
        private Logger logger = LoggerFactory.getLogger(Handler.class);
        private User user;
        private ObjectInputStream input;
        private OutputStream os;
        private ObjectOutputStream output;
        private InputStream is;

        public Handler(Socket socket) throws IOException {
            this.clientSocket = socket;
        }

        public void run() {
            logger.info("Attempting to connect a user...");
            logger.info(clientSocket.toString());
            try {
                is = clientSocket.getInputStream();
                input = new ObjectInputStream(is);
                os = clientSocket.getOutputStream();
                output = new ObjectOutputStream(os);

                UserCredentials userCredentials = (UserCredentials) input.readObject();
                if(!userCredentials.isLogin()) {
                    if(!checkIfUsernameExistsInDatabase(userCredentials.getUsername())) {
                        Statement statement;
                        try {
                            statement = databaseConnection.createStatement();
                            statement.executeUpdate("INSERT INTO USER(USER_ID, USERNAME,PASSWORD) VALUES (null, '" + userCredentials.getUsername() + "', '" + userCredentials.getPassword() + "')");
                            databaseConnection.commit();
                            user = new User();
                            user.setName(userCredentials.getUsername());
                            user.setStatus(OFFLINE);
                            users.add(user);
                            output.writeObject(userCredentials);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else {
                        userCredentials.setUsername(null);
                        output.writeObject(userCredentials);
                    }
                } else {
                    if(checkLoginCredentials(userCredentials)) {
                        user = users.stream().filter(x-> x.getName().equals(userCredentials.getUsername())).findFirst().get();
                        if (user.getStatus() == OFFLINE) {
                            this.name = userCredentials.getUsername();
                            user.setStatus(ONLINE);
                            logger.info(name + " has been added to the list");

                            writers.put(name, output);
                            addToList();
                        } else {
                            logger.error(userCredentials.getUsername() + " is already connected");
                            throw new DuplicateUsernameException(userCredentials.getUsername() + " is already connected");
                        }
                    } else {
                        userCredentials.setUsername(null);
                        output.writeObject(userCredentials);
                    }


                    while (clientSocket.isConnected()) {
                        Message inputmsg = (Message) input.readObject();
                        if (inputmsg != null) {
                            logger.info(inputmsg.getType() + " - " + inputmsg.getFrom() + ": " + inputmsg.getMsg());
                            switch (inputmsg.getType()) {
                                case CONNECTED:
                                    addToList();
                                    break;
                                case USER:
                                    insertMessageInDatabase(inputmsg);
                                    write(inputmsg);
                                    break;
                                case LOAD_CHAT:
                                    loadChat(inputmsg.getFrom(), inputmsg.getTo());
                                    break;
                                case SEEN:
                                    checkUnseen();
                                    break;
                                case MARK_AS_SEEN:
                                    markAsSeen(inputmsg);
                                    break;
                            }
                        }
                    }
                }

            } catch (SocketException socketException) {
                logger.error("Socket Exception for user " + name);
            } catch (DuplicateUsernameException duplicateException){
                logger.error("Duplicate Username : " + name);
            } catch (Exception e){
                logger.error("Exception in run() method for user: " + name, e);
            } finally {
                closeConnections();
            }
        }

        private static synchronized boolean checkIfUsernameExistsInDatabase(String username) {
            Statement statement;
            try {
                statement = databaseConnection.createStatement();
                String query = "SELECT * FROM USER WHERE USERNAME='" + username +"';";
                ResultSet resultSet = statement.executeQuery(query);
                if(!resultSet.next()) {
                    return false;
                } else {
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }

        private synchronized boolean checkLoginCredentials(UserCredentials userCredentials) {
            Statement statement;
            try {
                statement = databaseConnection.createStatement();
                String query = "SELECT * FROM USER WHERE USERNAME='" + userCredentials.getUsername() +
                        "' AND PASSWORD='" + userCredentials.getPassword() + "';";
                ResultSet resultSet = statement.executeQuery(query);
                if(resultSet.next()) {
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        private void insertMessageInDatabase(Message message) {
            Statement statement;
            try {
                statement = databaseConnection.createStatement();
                DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String date = sdf.format(message.getSentAt());
                statement.executeUpdate("INSERT INTO MESSAGE VALUES (null, '" + message.getFrom() + "', '" + message.getTo() + "', '" +
                        message.getMsg() + "', TIMESTAMP '" + date + "', " + message.isSeen() + ");");
                databaseConnection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void checkUnseen() throws IOException {
            Statement statement;
            ArrayList<User> usersSeen = new ArrayList<>(users);
            usersSeen.forEach(x->x.setUnseenMessage(false));
            try {
                statement = databaseConnection.createStatement();
                String query = "SELECT * FROM MESSAGE WHERE TO_USER='" + name + "' AND SEEN=false";
                ResultSet resultSet = statement.executeQuery(query);
                while (resultSet.next()) {
                    String name = resultSet.getString("FROM_USER");
                    System.out.println(name);
                    usersSeen.stream().filter(x->x.getName().equals(name)).findFirst().get().setUnseenMessage(true);
                }
                Message message = new Message();
                message.setUsers(usersSeen);
                message.setType(SEEN);
                message.setFrom(name);
                writers.get(name).writeObject(message);
                writers.get(name).reset();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void markAsSeen(Message message) throws IOException {
            Statement statement;
            try {
                statement = databaseConnection.createStatement();
                if (!message.getFrom().equals("Group")) {
                    String query = "UPDATE MESSAGE SET SEEN=true WHERE FROM_USER='" + message.getFrom() + "' AND TO_USER='" + message.getTo() + "';";
                    statement.executeUpdate(query);
                    databaseConnection.commit();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void loadChat(String from, String to) throws IOException {
            Statement statement;
            try {
                statement = databaseConnection.createStatement();
                if(!to.equals("Group")) {
                    String query = "UPDATE MESSAGE SET SEEN=true WHERE FROM_USER='" + to + "' AND TO_USER='" + from + "';";
                    statement.executeUpdate(query);
                    databaseConnection.commit();
                }
                ResultSet resultSet;
                if(to.equals("Group")) {
                    String query = "SELECT * FROM MESSAGE WHERE TO_USER='Group'";
                    resultSet = statement.executeQuery(query);
                } else {
                    String query = "SELECT * FROM MESSAGE WHERE (FROM_USER='" + from +"' AND TO_USER='" + to + "') OR (FROM_USER='" + to + "' AND TO_USER='" + from + "');";
                    resultSet = statement.executeQuery(query);
                }
                if(resultSet != null) {
                    List<Message> messages = new ArrayList<>();
                    while(resultSet.next()) {
                        Message message = new Message();
                        message.setFrom(resultSet.getString("FROM_USER"));
                        message.setTo(resultSet.getString("TO_USER"));
                        message.setMsg(resultSet.getString("MSG"));
                        message.setSentAt(resultSet.getTimestamp("SENT_AT").toLocalDateTime());
                        message.setType(LOAD_CHAT);
                        messages.add(message);
                    }
                    messages.sort(Comparator.comparing(Message::getSentAt));
                    ChatMessages chatMessages = new ChatMessages();
                    chatMessages.setMessages(messages);
                    long onlineCount = users.stream().filter(user -> user.getStatus() == ONLINE).count();
                    chatMessages.setOnlineCount((int) onlineCount);
                    writers.get(from).writeObject(chatMessages);
                    writers.get(from).reset();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private Message removeFromList() throws IOException {
            logger.debug("removeFromList() method Enter");
            Message msg = new Message();
            msg.setMsg("has left the chat.");
            msg.setType(MessageType.DISCONNECTED);
            msg.setFrom("SERVER");
            msg.setUsers(users);
            write(msg);
            logger.debug("removeFromList() method Exit");
            return msg;
        }

        private Message addToList() throws IOException {
            Message msg = new Message();
            msg.setMsg("Welcome!");
            msg.setType(MessageType.CONNECTED);
            msg.setFrom("SERVER");
            msg.setTo("Group");
            write(msg);
            return msg;
        }

        private void write(Message msg) throws IOException {
            logger.info(msg.toString());
            if(msg.getTo() == null || msg.getTo().equals("Group")) {
                msg.setUsers(users);
                for(Map.Entry<String, ObjectOutputStream> writer : writers.entrySet()) {
                    writer.getValue().writeObject(msg);
                    writer.getValue().reset();
                }
            } else {
                msg.setUsers(users);
                writers.get(msg.getFrom()).writeObject(msg);
                writers.get(msg.getFrom()).reset();
                if(writers.get(msg.getTo()) != null) {
                    //is ONLINE
                    writers.get(msg.getTo()).writeObject(msg);
                    writers.get(msg.getTo()).reset();
                }
            }
        }

        private synchronized void closeConnections()  {
            logger.debug("closeConnections() method Enter");
            if (user != null){
                user.setStatus(OFFLINE);
                logger.info("User object: " + user + " has been removed!");
            }
            if (output != null){
                writers.remove(name);
                logger.info("Writer object: " + user + " has been removed!");
            }
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (input != null){
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                removeFromList();
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("Writers:" + writers.size() + " usersList size:" + users.size());
            logger.debug("closeConnections() method Exit");
        }
    }
}
