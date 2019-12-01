package messages;

import java.io.Serializable;

public class User implements Serializable {

    private String name;
    private boolean unseenMessage;
    private Status status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isUnseenMessage() {
        return unseenMessage;
    }

    public void setUnseenMessage(boolean unseenMessage) {
        this.unseenMessage = unseenMessage;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
