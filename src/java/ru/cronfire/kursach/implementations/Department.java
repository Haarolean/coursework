package ru.cronfire.kursach.implementations;

public class Department {

    private final int id;
    private String name;
    private String phone;

    public Department(final int id, final String name, final String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "#" + id + " " + name;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Department that = (Department) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
