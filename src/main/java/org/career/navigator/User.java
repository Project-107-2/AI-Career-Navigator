package org.career.navigator;

public class User {
  public String name;
  public String email;
  public String passwordHash;

  public User(String name, String email, String passwordHash) {
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
  }
}