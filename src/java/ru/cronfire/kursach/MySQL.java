package ru.cronfire.kursach;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import ru.cronfire.kursach.implementations.Department;
import ru.cronfire.kursach.implementations.Employee;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class MySQL {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection = null;
    private Statement statement = null;

    /**
     * Create new BundleMySQL instance
     *
     * @param host     MySQL host
     * @param port     MySQL port
     * @param database MySQL database
     * @param username MySQL username
     * @param password MySQL password
     */
    public MySQL(final String host, final int port, final String database, final String username, final String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    /**
     * Attempt to establish MySQL connection
     */
    public Connection retrieveConnection(final boolean supressExceptions) {

        try {

            if(connection != null && !connection.isClosed() && connection.isValid(3)) {
                return connection;
            }

            Properties info = new Properties();

            info.put("user", username);
            info.put("password", password);
            info.put("useUnicode", "true");
            info.put("characterEncoding", "utf8");

            final String path = "jdbc:mysql://" + host + ":" + port + "/" + database;

            Class.forName("com.mysql.jdbc.Driver");

            connection = (Connection) DriverManager.getConnection(path, info);

            return connection;

        } catch(SQLException e) {
            if(!supressExceptions) {
                System.out.println("Unable to establish MySQL connection!");
                Util.showExceptionDialog(e);
            }
        } catch(ClassNotFoundException e) {
            if(!supressExceptions) {
                System.out.println("Database driver not found!");
                Util.showExceptionDialog(e);
            }
        }
        return null;
    }

    private Connection retrieveConnection() {
        return retrieveConnection(false);
    }

    /**
     * Attempt to create MySQL tables
     */
    public void createTables() {
        Scanner s = new Scanner(getClass().getResourceAsStream("resources/db.sql")).useDelimiter(";");
        while(s.hasNext()) createTable(s.next());
    }

    /**
     * Attempt to create table.
     *
     * @param query Table create query
     */
    @SuppressWarnings("WeakerAccess")
    public void createTable(final String query) {
        connection = retrieveConnection();

        try {

            statement = (Statement) connection.createStatement();
            statement.execute(query);

        } catch(SQLException e) {
            System.out.println("Unable to create table!");
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }

    }

    /**
     * Attempt to close MySQL connection
     */
    public void closeConnection() {
        if(connection == null) return;
        try {
            if(connection.isClosed()) return;
            connection.close();
        } catch(SQLException e) {
            System.out.println("Error while closing a connection!");
            Util.showExceptionDialog(e);
        }

    }

    /**
     * Attempt to close opened MySQL resources.
     *
     * @param statement statement
     */
    private void closeStatement(Statement statement) {
        if(statement == null) return;
        try {
            if(statement.isClosed()) return;
            statement.close();
        } catch(SQLException e) {
            System.out.println("Error while closing a statement!");
            Util.showExceptionDialog(e);
        }

    }

    /**
     * Get the list of existing employees.
     *
     * @return List<Employee>
     */
    public List<Employee> getEmployeeList() {
        connection = retrieveConnection();

        List<Employee> employees = new ArrayList<>();

        final String query = "SELECT * FROM `employees` ORDER BY `surname` ASC";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            while(rs.next()) {
                employees.add(new Employee(rs.getInt("id"), getDepartment(rs.getInt("department")),
                        rs.getString("surname"), rs.getString("name"), rs.getString("patronymic"), rs.getDate("birth"),
                        rs.getString("phone")));
            }

            return employees;

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
            return null;
        } finally {
            closeStatement(statement);
        }

    }

    /**
     * Get the list of existing departments.
     *
     * @return List<Department>
     */
    public List<Department> getDepartmentList() {
        connection = retrieveConnection();

        List<Department> departments = new ArrayList<>();

        final String query = "SELECT `id`, `name`, `phone` FROM `departments` ORDER BY `id` ASC";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            while(rs.next()) {
                departments.add(new Department(rs.getInt("id"), rs.getString("name"), rs.getString("phone")));
            }

            return departments;

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
            return null;
        } finally {
            closeStatement(statement);
        }

    }

    /**
     * Add new employee.
     *
     * @param d  Department id
     * @param s  Surname
     * @param n  Name
     * @param p  Patronymic
     * @param b  Birth
     * @param ph Phone
     */
    public Employee addEmployee(final int d, final String s, final String n, final String p, final Date b, final String ph) {
        connection = retrieveConnection();

        final String query = "INSERT INTO `employees` (`department`, `surname`, `name`, `patronymic`, `birth`, `phone`) " +
                "VALUES(?, ?, ?, ?, ?, ?)";

        try {

            PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, d);
            statement.setString(2, s);
            statement.setString(3, n);
            statement.setString(4, p);
            statement.setDate(5, b);
            statement.setString(6, ph);
            if(Main.isDebug()) Util.log(statement, null);
            statement.executeUpdate();

            ResultSet rs = statement.getGeneratedKeys();
            if(rs.first()) return new Employee(rs.getInt(1), getDepartment(d), s, n, p, b, ph);

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        return null;
    }

    /**
     * Edit existing employee.
     *
     * @param emp Employee
     */
    public void editEmployee(final Employee emp) {
        connection = retrieveConnection();

        final String query = "UPDATE `employees` SET `department` = ?, `surname` = ?, `name` = ?, `patronymic` = ?, `birth` = ?, `phone` = ?";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, emp.getDepartment().getId());
            statement.setString(2, emp.getSurname());
            statement.setString(3, emp.getName());
            statement.setString(4, emp.getPatronymic());
            statement.setDate(5, emp.getBirth());
            statement.setString(6, emp.getPhone());
            if(Main.isDebug()) Util.log(statement, null);
            statement.executeUpdate();

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }

    }

    /**
     * Delete employee.
     *
     * @param emp Employee
     */
    public void deleteEmployee(final Employee emp) {
        connection = retrieveConnection();

        final String query = "DELETE FROM `employees` WHERE `id` = ?";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, emp.getId());
            if(Main.isDebug()) Util.log(statement, null);
            statement.executeUpdate();

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Get department by its id.
     *
     * @param id department id
     * @return Department
     */
    private Department getDepartment(final int id) {
        connection = retrieveConnection();

        final String query = "SELECT `name`, `phone` FROM `departments` WHERE `id` = ? LIMIT 1";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            if(rs.first()) return new Department(id, rs.getString("name"), rs.getString("phone"));

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        return null;
    }

    /**
     * Count hours worked by an employee for the specified day.
     *
     * @param emp   Employee
     * @param date  Date to record for
     * @param hours How many hours he has worked that day
     */
    public void recordHours(final Employee emp, final Date date, final int hours) {
        connection = retrieveConnection();

        final String query = "INSERT INTO `workhours` (`date`, `employee`, `hours`) VALUES(?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE `hours` = VALUES(`hours`)";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setDate(1, date);
            statement.setInt(2, emp.getId());
            statement.setInt(3, hours);
            if(Main.isDebug()) Util.log(statement, null);
            statement.executeUpdate();

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Get the most latest date recorded for an employee.
     *
     * @param emp Employee
     * @return java.sql.Date
     */
    public Date getLastRecordedDate(final Employee emp) {
        connection = retrieveConnection();

        final String query = "SELECT `date` FROM `workhours` WHERE `employee` = ? ORDER BY `date` DESC LIMIT 1";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, emp.getId());
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            if(rs.first()) return rs.getDate("date");


        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        return null;
    }

    /**
     * Check if an employee has recorded specified date.
     *
     * @param emp  Employee
     * @param date java.sql.Data
     * @return boolean
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasRecordedDate(final Employee emp, final Date date) {
        connection = retrieveConnection();

        final String query = "SELECT `date` FROM `workhours` WHERE `employee` = ? AND `date` = ? LIMIT 1";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, emp.getId());
            statement.setDate(2, date);
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            return rs.first();

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        throw new NullPointerException();
    }

    public int getRecordedHoursCount(final Employee emp) {
        connection = retrieveConnection();

        final String query = "SELECT SUM(`hours`) AS `total` FROM `workhours` WHERE `employee` = ?";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, emp.getId());
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            if(rs.first()) return rs.getInt("total");

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        throw new NullPointerException();
    }

    public int getRecordedDatesCount(final Employee emp) {
        connection = retrieveConnection();

        final String query = "SELECT COUNT(`date`) AS `total` FROM `workhours` WHERE `employee` = ?";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, emp.getId());
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            if(rs.first()) return rs.getInt("total");

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        throw new NullPointerException();
    }

    public int getEmployeeStatistics(final Employee emp, final int days) {
        connection = retrieveConnection();

        String query = "SELECT SUM(`hours`) AS `total` FROM `workhours` WHERE `employee` = ?";

        if(days != 0) {
            query += " AND `date` > NOW() - INTERVAL ? DAY";
        }

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, emp.getId());
            if(days != 0) statement.setInt(2, days);
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            if(rs.first()) return rs.getInt("total");

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        throw new NullPointerException();
    }

    public int getEmployeeStatistics(final Employee emp) {
        return this.getEmployeeStatistics(emp, 0);
    }

    public int getEmployeesCount(final Department dep) {
        connection = retrieveConnection();

        final String query = "SELECT COUNT(`id`) AS `total` FROM `employees` WHERE `department` = ?";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, dep.getId());
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            if(rs.first()) return rs.getInt("total");

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        throw new NullPointerException();
    }

    public int getDepartmentStatistics(final Department dep, final int days) {
        connection = retrieveConnection();

        String query = "SELECT SUM(`hours`) AS `total` FROM `workhours` WHERE " +
                "(SELECT `department` FROM `employees` WHERE `id` = `employee`) = ?";

        if(days != 0) {
            query += " AND `date` > NOW() - INTERVAL ? DAY";
        }

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, dep.getId());
            if(days != 0) statement.setInt(2, days);
            if(Main.isDebug()) Util.log(statement, null);
            ResultSet rs = statement.executeQuery();

            if(rs.first()) return rs.getInt("total");

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        throw new NullPointerException();
    }

    public int getDepartmentStatistics(final Department dep) {
        return this.getDepartmentStatistics(dep, 0);
    }

    /**
     * Add new department.
     *
     * @param n  Department name
     * @param ph Department phone
     * @return Department
     */
    public Department addDepartment(final String n, final String ph) {
        connection = retrieveConnection();

        final String query = "INSERT INTO `departments` (`name`, `phone`) VALUES(?, ?)";

        try {

            PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, n);
            statement.setString(2, ph);
            if(Main.isDebug()) Util.log(statement, null);
            statement.executeUpdate();

            ResultSet rs = statement.getGeneratedKeys();
            if(rs.first()) return new Department(rs.getInt(1), n, ph);

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
        return null;
    }

    /**
     * Edit existing department.
     *
     * @param dep Department
     */
    public void editDepartment(final Department dep) {
        connection = retrieveConnection();

        final String query = "UPDATE `departments` SET `name` = ?, `phone` = ? WHERE `id` = ? LIMIT 1";

        try {

            PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, dep.getName());
            statement.setString(2, dep.getPhone());
            statement.setInt(3, dep.getId());
            if(Main.isDebug()) Util.log(statement, null);
            statement.executeUpdate();

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Delete department.
     *
     * @param dep Department
     */
    public void deleteDepartment(final Department dep) {
        connection = retrieveConnection();

        final String query = "DELETE FROM `departments` WHERE `id` = ?";

        try {

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, dep.getId());
            if(Main.isDebug()) Util.log(statement, null);
            statement.executeUpdate();

        } catch(SQLException e) {
            Util.showExceptionDialog(e);
        } finally {
            closeStatement(statement);
        }
    }
}