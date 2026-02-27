package net.multyfora.exampleStuff;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class HelloController {
    @FXML private TextField nameField;
    @FXML private Label outputLabel;

    @FXML
    private void handleButton() {
        outputLabel.setText("Hello, " + nameField.getText() + "!");
    }
}