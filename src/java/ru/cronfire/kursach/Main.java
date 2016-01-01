package ru.cronfire.kursach;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

@SuppressWarnings("WeakerAccess") // fxml stuff
public class Main extends Application {

    private static final String version = "1.0.0";
    private static Stage stage;
    private static boolean debug;
    private static boolean checkRegexp = true;

    public static void main(String[] args) {
        for(String s : args)
            if(s.equals("-debug")) {
                debug = true;
                System.out.println("Started with debug = true");
            }
        for(String s : args)
            if(s.equals("-noregexp")) {
                checkRegexp = false;
                System.out.println("Started with checkRegexp = false");
            }
        launch(args);
    }

    @Override
    public void start(final Stage stage) throws Exception {
        Main.stage = stage;
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("resources/auth.fxml"))));
        stage.getIcons().add(new Image(getClass().getResourceAsStream("resources/icon.png")));
        stage.setTitle("Управление сотрудниками v. " + Main.getVersion());
        stage.show();
    }

    public static Stage getStage() {
        return Main.stage;
    }

    public static String getVersion() {
        return Main.version;
    }

    public static boolean isDebug() {
        return debug;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isCheckRegexp() {
        return checkRegexp;
    }
}
