package client.util;

import messages.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import messages.User;

public class UserListViewItem implements Callback<ListView<User>,ListCell<User>>{
    @Override
    public ListCell<User> call(ListView<User> p) {

        ListCell<User> cell = new ListCell<User>(){

            @Override
            protected void updateItem(User user, boolean bln) {
                super.updateItem(user, bln);
                setGraphic(null);
                setText(null);
                if (user != null) {
                    HBox hBox = new HBox();
                    hBox.setSpacing(10);
                    hBox.setPadding(new Insets(0, 0, 0, 10));

                    Text name = new Text(user.getName());
                    name.setStyle("-fx-font: 18 arial;");

                    ImageView statusImageView = new ImageView();
                    Image statusImage = new Image(getClass().getClassLoader().getResource("images/" + user.getStatus().toString().toLowerCase() + ".png").toString(), 16, 16,true,true);
                    statusImageView.setImage(statusImage);

                    ImageView seenImageView = new ImageView();
                    Image seenImage = new Image(getClass().getClassLoader().getResource("images/inbox.png").toString(), 35, 35, true, true);
                    if(user.isUnseenMessage()) {
                        seenImageView.setImage(seenImage);
                    }

                    hBox.getChildren().addAll(statusImageView,  name, seenImageView);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    setGraphic(hBox);
                }
            }
        };
        return cell;
    }
}