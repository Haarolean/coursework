package ru.cronfire.kursach;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import ru.cronfire.kursach.implementations.Department;
import ru.cronfire.kursach.implementations.Employee;

import java.net.URL;
import java.sql.Date;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess, UnusedDeclaration") // for fxml annotated fields and methods
public class Kursach implements Initializable {

    private double xOffset = 0;
    private double yOffset = 0;

    private Employee currentEmployee;
    private Department currentDepartment;

    private String employeeInfoTemplate;
    private String departmentInfoTemplate;
    public static MySQL db = null;

    // Patterns
    public static final Pattern VALID_URI = Pattern.compile("^.*@.*\\..*(:[0-9]{1,65535})?$");
    private static final Pattern VALID_NAME = Pattern.compile("^[А-я]{3,32}$");
    private static final Pattern VALID_DEPARTMENT_NAME = Pattern.compile("^[А-я0-9 ]{3,32}$");
    private static final Pattern VALID_PHONE = Pattern.compile("^[0-9]{1,15}$");
    private static final Pattern VALID_HOURS = Pattern.compile("^[0-9]{1,2}$");
    private static final DateTimeFormatter BIRTH_PATERN = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String ERR_MSG = "Ошибка: ";

    // Injectable fields
    @FXML private ListView<Employee> employeeList; // emp pane
    @FXML protected Label employeeMessage;
    @FXML protected Button dismissEmployeeMessageButton;
    @FXML private Pane employeeActionPane;
    @FXML private Pane employeeInfoPane;
    @FXML private Label employeeInfo;
    @FXML private Pane addEmployeePane; // new emp pane
    @FXML private TextField surname;
    @FXML private TextField name;
    @FXML private TextField patronymic;
    @FXML private TextField phone;
    @FXML private DatePicker birth;
    @FXML private ChoiceBox<Department> employeeAddDepartment;
    @FXML private Pane recordHoursPane; // hours record pane
    @FXML private TextField recordId;
    @FXML private DatePicker recordDate;
    @FXML private TextField recordHours;
    @FXML private TextField lastDate;
    @FXML private TextField hoursCount;
    @FXML private TextField dateCount;
    @FXML private Pane editEmployeePane; // edit emp pane
    @FXML private TextField employeeEditSurname;
    @FXML private TextField employeeEditName;
    @FXML private TextField employeeEditPatronymic;
    @FXML private TextField employeeEditPhone;
    @FXML private DatePicker employeeEditBirth;
    @FXML private ChoiceBox<Department> employeeEditDepartment;
    @FXML private Pane deleteEmployeePane; // delete emp pane
    @FXML private ListView<Department> departmentList; // dep pane
    @FXML protected Label departmentMessage;
    @FXML protected Button dismissDepartmentMessageButton;
    @FXML private Pane departmentActionPane;
    @FXML private Pane departmentInfoPane;
    @FXML private Label departmentInfo;
    @FXML private Pane addDepartmentPane; // new dep pane
    @FXML private TextField departmentAddName;
    @FXML private TextField departmentAddPhone;
    @FXML private Pane editDepartmentPane; // edit dep pane
    @FXML private TextField departmentEditName;
    @FXML private TextField departmentEditPhone;
    @FXML private Pane deleteDepartmentPane; // delete dep pane
    @FXML private TextArea changelog; // others
    @FXML private ImageView notAnEasterEgg;

    public void initialize(URL location, ResourceBundle resources) {
        new Util(this);

        changelog.setText(new Scanner(
                getClass().getResourceAsStream("resources/changelog.txt"), "UTF-8").useDelimiter("\\Z").next());

        Main.getStage().setOnCloseRequest(event -> {
            db.closeConnection();
            Platform.exit();
        });

        // EVENTS
        employeeList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null) {
                return;
            }
            currentEmployee = newValue;
            if(editEmployeePane.isVisible()) {
                updateEditEmployeeDialog();
                return;
            }
            if(recordHoursPane.isVisible()) {
                updateRecordHoursDialog();
                return;
            }
            if(deleteEmployeePane.isVisible()) {
                return;
            }
            if(oldValue == null || employeeInfoPane.isVisible()) {
                showEmployeeInfo();
            }
        });
        departmentList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null) return;
            currentDepartment = newValue;
            if(editDepartmentPane.isVisible()) {
                updateEditDepartmentDialog();
                return;
            }
            if(oldValue == null || departmentInfoPane.isVisible()) {
                showDepartmentInfo();
            }
        });
        recordDate.setDayCellFactory(recordDate -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if(!db.hasRecordedDate(currentEmployee, Date.valueOf(item))) return;
                setStyle("-fx-background-color: #41ff81;");
            }
        });
        birth.setDayCellFactory(birth -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if(!item.isAfter(LocalDate.now().minusYears(18))) return; // Мы же чтим трудовой кодекс? :)
                setStyle("-fx-background-color: #ffc0cb;");
                setDisable(true);
            }
        });
        employeeEditBirth.setDayCellFactory(birthEdit -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if(!item.isAfter(LocalDate.now().minusYears(18))) return;
                setStyle("-fx-background-color: #ffc0cb;");
                setDisable(true);
            }
        });

        employeeInfoTemplate = employeeInfo.getText();
        departmentInfoTemplate = departmentInfo.getText();
        recordDate.setValue(LocalDate.now());

        loadDepartments();
        loadEmployees();
    }

    // FXML stuff
    // employees
    protected void loadEmployees() {
        List<Employee> es = db.getEmployeeList();
        if(es.isEmpty()) {
            Util.showEmployeeMessage("Список работников пуст.");
            return;
        }
        employeeList.setItems(FXCollections.observableArrayList(es));
        employeeList.getSelectionModel().select(0);
    }

    @FXML
    protected void showEmployeeInfo() {
        if(!isEmployeeSelected()) {
            employeeInfo.setText(employeeInfoTemplate);
            return;
        }
        Employee e = currentEmployee;

        employeeInfo.setText(employeeInfoTemplate
                .replace("${id}", String.valueOf(e.getId()))
                .replace("${department}",
                        (currentEmployee.hasDepartment() ? currentEmployee.getDepartment().toString() : "N/A"))
                .replace("${surname}", e.getSurname())
                .replace("${name}", e.getName())
                .replace("${patronymic}", e.getPatronymic())
                .replace("${birth}", e.getBirth().toLocalDate().format(BIRTH_PATERN))
                .replace("${phone}", e.getPhone())
                .replace("${stats1}", String.valueOf(db.getEmployeeStatistics(e, 7)))
                .replace("${stats2}", String.valueOf(db.getEmployeeStatistics(e, 30)))
                .replace("${stats3}", String.valueOf(db.getEmployeeStatistics(e, 365)))
                .replace("${stats4}", String.valueOf(db.getEmployeeStatistics(e)))
        );

        employeeActionPane.getChildren().forEach((node) -> node.setVisible(false));
        employeeInfoPane.setVisible(true);
    }

    @FXML
    protected void toggleRecordHoursDialog() {
        if(!isEmployeeSelected()) return;
        updateRecordHoursDialog();
        employeeActionPane.getChildren().forEach((node) -> node.setVisible(false));
        recordHoursPane.setVisible(true);
    }

    @FXML
    protected void recordHours() {
        if(!VALID_HOURS.matcher(recordHours.getText()).matches()) {
            Util.showEmployeeMessage(ERR_MSG + "Неверно указано количество часов.");
            return;
        }
        db.recordHours(currentEmployee, Date.valueOf(recordDate.getValue()), Integer.parseInt(recordHours.getText()));
        Util.showEmployeeMessage("Часы учтены.");
        updateRecordHoursDialog();
    }

    @FXML
    protected void toggleEditEmployeeDialog() {
        if(!isEmployeeSelected()) return;
        updateEditEmployeeDialog();
        employeeActionPane.getChildren().forEach((node) -> node.setVisible(false));
        editEmployeePane.setVisible(true);
    }

    @FXML
    protected void editEmployee() {
        Department d = employeeEditDepartment.getValue();
        String s = employeeEditSurname.getText();
        String n = employeeEditName.getText();
        String p = employeeEditPatronymic.getText();
        if(employeeEditBirth.getValue() == null) {
            Util.showEmployeeMessage(ERR_MSG + "Неверно указана дата рождения.");
            return;
        }
        Date b = Date.valueOf(employeeEditBirth.getValue());
        String ph = employeeEditPhone.getText();

        if(d.equals(currentEmployee.getDepartment()) && s.equals(currentEmployee.getSurname())
                && n.equals(currentEmployee.getName()) && p.equals(currentEmployee.getPatronymic())
                && b.equals(currentEmployee.getBirth()) && ph.equals(currentEmployee.getPhone())) {
            Util.showEmployeeMessage(ERR_MSG + "Нечего редактировать.");
        }
        if(!checkEmployeeMatch(s, n, p, ph)) return;

        final int position = employeeList.getItems().indexOf(currentEmployee);
        currentEmployee.setDepartment(d);
        currentEmployee.setSurname(s);
        currentEmployee.setName(n);
        currentEmployee.setPatronymic(p);
        currentEmployee.setBirth(b);
        currentEmployee.setPhone(ph);

        db.editEmployee(currentEmployee);
        employeeList.getItems().set(position, currentEmployee);
        sortEmployeeList();
        employeeList.scrollTo(currentEmployee);
        Util.showEmployeeMessage("Сотрудник отредактирован.");
        showEmployeeInfo();
    }

    @FXML
    protected void toggleAddEmployeeDialog() {
        birth.setValue(LocalDate.now().minusYears(18));
        employeeActionPane.getChildren().forEach((node) -> node.setVisible(false));
        addEmployeePane.setVisible(true);
    }

    @FXML
    protected void addEmployee() {
        if(employeeAddDepartment.getValue() == null) {
            Util.showEmployeeMessage(ERR_MSG + "Не выбран отдел.");
            return;
        }
        Integer d = employeeAddDepartment.getValue().getId();
        String s = surname.getText();
        String n = name.getText();
        String p = patronymic.getText();
        if(birth.getValue() == null) {
            Util.showEmployeeMessage(ERR_MSG + "Неверно указана дата рождения.");
            return;
        }
        Date b = Date.valueOf(birth.getValue());
        String ph = phone.getText();

        if(!checkEmployeeMatch(s, n, p, ph)) return;

        final Employee emp = db.addEmployee(d, s, n, p, b, ph);
        employeeList.getItems().add(emp);
        sortEmployeeList();
        employeeList.getSelectionModel().select(emp);
        employeeList.scrollTo(emp);

        Util.showEmployeeMessage("Сотрудник добавлен.");
        showEmployeeInfo();
        surname.setText("");
        name.setText("");
        patronymic.setText("");
        birth.setValue(null);
        phone.setText("");
    }

    @FXML
    protected void toggleDeleteEmployeeDialog() {
        if(!isEmployeeSelected()) return;
        employeeActionPane.getChildren().forEach((node) -> node.setVisible(false));
        deleteEmployeePane.setVisible(true);
    }

    @FXML
    protected void deleteEmployee() {
        db.deleteEmployee(currentEmployee);
        employeeList.getItems().remove(currentEmployee);
        Util.showEmployeeMessage("Сотрудник удалён.");
        showEmployeeInfo();
    }

    // departments
    protected void loadDepartments() {
        List<Department> ds = db.getDepartmentList();
        if(ds.isEmpty()) {
            Util.showDepartmentMessage("Список отделов пуст.");
            return;
        }
        ObservableList<Department> ds1 = FXCollections.observableArrayList(ds);
        departmentList.setItems(ds1);
        employeeAddDepartment.setItems(ds1);
        employeeEditDepartment.setItems(ds1);
        departmentList.getSelectionModel().select(0);
        employeeAddDepartment.getSelectionModel().select(0);
        employeeEditDepartment.getSelectionModel().select(0);
    }

    @FXML
    protected void showDepartmentInfo() {
        if(!isDepartmentSelected()) return;
        Department d = currentDepartment;

        departmentInfo.setText(departmentInfoTemplate
                .replace("${id}", String.valueOf(d.getId()))
                .replace("${name}", d.getName())
                .replace("${phone}", d.getPhone())
                .replace("${count}", String.valueOf(db.getEmployeesCount(d)))
                .replace("${stats1}", String.valueOf(db.getDepartmentStatistics(d, 7)))
                .replace("${stats2}", String.valueOf(db.getDepartmentStatistics(d, 30)))
                .replace("${stats3}", String.valueOf(db.getDepartmentStatistics(d, 365)))
                .replace("${stats4}", String.valueOf(db.getDepartmentStatistics(d)))
        );

        departmentActionPane.getChildren().forEach((node) -> node.setVisible(false));
        departmentInfoPane.setVisible(true);
    }

    @FXML
    protected void toggleEditDepartmentDialog() {
        if(!isDepartmentSelected()) return;
        updateEditDepartmentDialog();
        departmentActionPane.getChildren().forEach((node) -> node.setVisible(false));
        editDepartmentPane.setVisible(true);
    }

    @FXML
    protected void editDepartment() {
        String n = departmentEditName.getText();
        String ph = departmentEditPhone.getText();
        if(n.equals(currentDepartment.getName()) && ph.equals(currentDepartment.getPhone())) {
            Util.showDepartmentMessage(ERR_MSG + "Нечего редактировать.");
            return;
        }

        if(!checkDepartmentMatch(n, ph)) return;

        final int position = departmentList.getItems().indexOf(currentDepartment);
        currentDepartment.setName(n);
        currentDepartment.setPhone(ph);

        db.editDepartment(currentDepartment);
        departmentList.getItems().set(position, currentDepartment);
        sortDepartmentList();
        departmentList.scrollTo(currentDepartment);
        Util.showDepartmentMessage("Отдел отредактирован.");
        showDepartmentInfo();
    }

    @FXML
    protected void toggleAddDepartmentDialog() {
        departmentActionPane.getChildren().forEach((node) -> node.setVisible(false));
        addDepartmentPane.setVisible(true);
    }

    @FXML
    protected void addDepartment() {
        String n = departmentAddName.getText();
        String ph = departmentAddPhone.getText();

        if(!checkDepartmentMatch(n, ph)) return;
        for(Department d : departmentList.getItems()) {
            if(d.getName().equals(n)) {
                Util.showDepartmentMessage(ERR_MSG + "Отдел с таким именем уже существует.");
                return;
            }
        }

        final Department dep = db.addDepartment(n, ph);
        departmentList.getItems().add(dep);
        sortDepartmentList();
        departmentList.getSelectionModel().select(dep);
        departmentList.scrollTo(dep);

        Util.showDepartmentMessage("Отдел добавлен.");
        showDepartmentInfo();
        departmentAddName.setText("");
        departmentAddPhone.setText("");
    }

    @FXML
    protected void toggleDeleteDepartmentDialog() {
        if(!isDepartmentSelected()) return;
        departmentActionPane.getChildren().forEach((node) -> node.setVisible(false));
        deleteDepartmentPane.setVisible(true);
    }

    @FXML
    protected void deleteDepartment() {
        db.deleteDepartment(currentDepartment);
        departmentList.getItems().remove(currentDepartment);
        Util.showDepartmentMessage("Отдел удалён.");
        showDepartmentInfo();
    }

    // Other stuff
    private boolean isEmployeeSelected() {
        if(currentEmployee == null) {
            Util.showEmployeeMessage(ERR_MSG + "Не выбран сотрудник.");
            return false;
        }
        return true;
    }

    private boolean isDepartmentSelected() {
        if(currentDepartment == null) {
            Util.showDepartmentMessage(ERR_MSG + "Не выбран отдел.");
            return false;
        }
        return true;
    }

    private void updateRecordHoursDialog() {
        if(!isEmployeeSelected()) return;
        recordId.setText(String.valueOf(currentEmployee.getId()));
        if(db.getLastRecordedDate(currentEmployee) == null) {
            lastDate.setText("—");
            return;
        }
        lastDate.setText(db.getLastRecordedDate(currentEmployee).toLocalDate().format(BIRTH_PATERN));
        hoursCount.setText(String.valueOf(currentEmployee.getRecordedHoursCount()));
        dateCount.setText(String.valueOf(currentEmployee.getRecordedDateCount()));
    }

    private void updateEditEmployeeDialog() {
        employeeEditSurname.setText(currentEmployee.getSurname());
        employeeEditName.setText(currentEmployee.getName());
        employeeEditPatronymic.setText(currentEmployee.getPatronymic());
        employeeEditPhone.setText(currentEmployee.getPhone());
        employeeEditBirth.setValue(currentEmployee.getBirth().toLocalDate());
        if(currentEmployee.hasDepartment()) employeeEditDepartment.setValue(currentEmployee.getDepartment());
    }

    protected void updateEditDepartmentDialog() {
        departmentEditName.setText(currentDepartment.getName());
        departmentEditPhone.setText(currentDepartment.getPhone());
    }

    private void sortEmployeeList() {
        if(employeeList.getItems().isEmpty()) return;
        employeeList.getItems().sort((o1, o2) -> Collator.getInstance(new Locale("ru", "RU"))
                .compare(o1.getSurname(), o2.getSurname()));
    }

    private void sortDepartmentList() {
        if(departmentList.getItems().isEmpty()) return;
        departmentList.getItems().sort((o1, o2) -> Collator.getInstance(new Locale("ru", "RU"))
                .compare(o1.getName(), o2.getName()));
    }

    /**
     * Check if names, birth and phone match regex
     *
     * @param s  surname
     * @param n  name
     * @param p  patronymic
     * @param ph phone number
     * @return matches or not
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkEmployeeMatch(final String s, final String n, final String p, final String ph) {
        if(!Main.isCheckRegexp()) return true;
        if(!VALID_NAME.matcher(s).matches()) {
            Util.showEmployeeMessage(ERR_MSG + "Неверно указана фамилия.");
            return false;
        }
        if(!VALID_NAME.matcher(n).matches()) {
            Util.showEmployeeMessage(ERR_MSG + "Неверно указано имя.");
            return false;
        }
        if(!VALID_NAME.matcher(p).matches()) {
            Util.showEmployeeMessage(ERR_MSG + "Неверно указано отчество.");
            return false;
        }
        if(!VALID_PHONE.matcher(ph).matches()) {
            Util.showEmployeeMessage(ERR_MSG + "Неверно указан номер телефона.");
            return false;
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkDepartmentMatch(final String n, final String ph) {
        if(!Main.isCheckRegexp()) return true;
        if(!VALID_DEPARTMENT_NAME.matcher(n).matches()) {
            Util.showDepartmentMessage(ERR_MSG + "Неверно указано название отдела.");
            return false;
        }
        if(!VALID_PHONE.matcher(ph).matches()) {
            Util.showDepartmentMessage(ERR_MSG + "Неверно указан номер телефона.");
            return false;
        }
        return true;
    }

    @FXML
    protected void dismissEmployeeMessage() {
        employeeMessage.setVisible(false);
        dismissEmployeeMessageButton.setVisible(false);
    }

    @FXML
    protected void dismissDepartmentMessage() {
        employeeMessage.setVisible(false);
        dismissDepartmentMessageButton.setVisible(false);
    }

    @FXML
    protected void easterEgg() {
        // TOTALLY NOT AN EASTER EGG
        notAnEasterEgg.setVisible(true);
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.1), ae -> notAnEasterEgg.setVisible(false)));
        timeline.play();
    }

}