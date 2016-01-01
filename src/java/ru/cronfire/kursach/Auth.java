package ru.cronfire.kursach;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Auth implements Initializable {

    @FXML private TextField URIField;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Button authButton;
    @FXML private Label authMessage;

    public void initialize(URL location, ResourceBundle resources) {
        URIField.setOnKeyPressed(event -> {
            if(event.getCode() != KeyCode.ENTER) return;
            loginField.requestFocus();
        });
        loginField.setOnKeyPressed(event -> {
            if(event.getCode() != KeyCode.ENTER) return;
            passwordField.requestFocus();
        });
        passwordField.setOnKeyPressed(event -> {
            if(event.getCode() != KeyCode.ENTER) return;
            authButton.requestFocus();
            auth();
        });
    }

    @SuppressWarnings("WeakerAccess")
    @FXML
    public void auth() {
        String URI = URIField.getText();
        String login = loginField.getText();
        String password = passwordField.getText();

        if(password.isEmpty() || !Kursach.VALID_URI.matcher(URI).matches()) {
            showAuthMessage("Error: Invalid URI, login or password.");
            return;
        }

        String database = URI.split("@")[0];
        String host = URI.split("@")[1].split(":")[0];
        int port = URI.contains(":") ? Integer.parseInt(URI.split("@")[1].split(":")[1]) : 3306;

        if((Kursach.db =
                new MySQL(host, port, database, login, password)).retrieveConnection(!Main.isDebug()) != null) {
            Kursach.db.createTables();

            try {
                Main.getStage().setScene(
                        new Scene(FXMLLoader.load(this.getClass().getResource("resources/kursach.fxml"))));
                Main.getStage().show();
            } catch(IOException e) {
                Util.showExceptionDialog(e);
            }
        } else {
            showAuthMessage("Database connection error!");
        }
    }

    private void showAuthMessage(String message) {
        authMessage.setText(message);
        authMessage.setVisible(true);
    }

}
