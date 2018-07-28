package com.alwaysbaked.contacttocsv;


public class Contact {

    private String Name;
    private String Number;

    public Contact() {
    }

    public Contact(String name, String number) {
        Name = name;
        Number = number;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getNumber() {
        return Number;
    }

    public void setNumber(String number) {
        Number = number;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "Name='" + Name + '\'' +
                ", Number='" + Number + '\'' +
                '}';
    }
}
