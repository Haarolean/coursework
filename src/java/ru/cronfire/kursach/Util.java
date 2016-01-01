package ru.cronfire.kursach;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

class Util {

    private static Kursach kursach;

    public Util(final Kursach kursach) {
        Util.kursach = kursach;
    }

    public static void showEmployeeMessage(String message) {
        kursach.employeeMessage.setText(message);
        kursach.employeeMessage.setVisible(true);
        kursach.dismissEmployeeMessageButton.setVisible(true);
    }

    public static void showDepartmentMessage(String message) {
        kursach.departmentMessage.setText(message);
        kursach.departmentMessage.setVisible(true);
        kursach.dismissDepartmentMessageButton.setVisible(true);
    }

    public static void showExceptionDialog(Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Dialog");
        alert.setHeaderText("An exception has occured!");
        alert.setContentText("Exception message: " + ex.getMessage());

        // Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    public static void log(Statement s, Exception e) {
        if(s != null) System.out.println(s);
        if(e != null) {
            Util.showExceptionDialog(e);
            Logger.getLogger("huckster").log(Level.SEVERE, "", e);
        }
        System.out.println("=============================================================================");
    }
}
