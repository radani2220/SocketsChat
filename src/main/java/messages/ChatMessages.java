package messages;

import java.io.Serializable;
import java.util.List;

public class ChatMessages implements Serializable {
    private List<Message> messages;
    private int onlineCount;

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public int getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(int onlineCount) {
        this.onlineCount = onlineCount;
    }
}