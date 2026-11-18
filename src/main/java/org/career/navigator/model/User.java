package org.career.navigator.model;

public class User {
    public String name;
    public String email;
    public String passwordHash;
    public String grade;
    public String targetRole;

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.grade = "";
        this.targetRole = "";
    }
}

