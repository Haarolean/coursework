package ru.cronfire.kursach.implementations;

import ru.cronfire.kursach.Kursach;

import java.sql.Date;

public class Employee {

    private final int id;
    private Department department;
    private String surname;
    private String name;
    private String patronymic;
    private Date birth;
    private String phone;

    public Employee(int id, Department department, String surname, String name, String patronymic, Date birth, String phone) {
        this.id = id;
        this.department = department;
        this.surname = surname;
        this.name = name;
        this.patronymic = patronymic;
        this.birth = birth;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public boolean hasDepartment() {
        return this.department != null;
    }

    /**
     * Get department of Employee.
     * WARNING: Can be null.
     *
     * @return Department or null if not defined
     */
    public Department getDepartment() {
        return department;
    }

    public String getSurname() {
        return surname;
    }

    public String getName() {
        return name;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public Date getBirth() {
        return birth;
    }

    public String getPhone() {
        return phone;
    }

    public int getRecordedHoursCount() {
        return Kursach.db.getRecordedHoursCount(this);
    }

    public int getRecordedDateCount() {
        return Kursach.db.getRecordedDatesCount(this);
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPatronymic(String patronymic) {
        this.patronymic = patronymic;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return surname + " " + name + " " + patronymic;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Employee employee = (Employee) o;

        return id == employee.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
