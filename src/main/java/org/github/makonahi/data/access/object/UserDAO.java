package org.github.makonahi.data.access.object;

import java.util.List;

//Data Access Object
public interface UserDAO {
    void addUser(String name, String login, String password, String role);
    List<String> getAllUsers();
    void deleteUser(int userId);
    void deleteAllUsers();
    int authenticateUser(String login, String password);
    boolean registerUser(String name, String login, String password, String role);
    String getUserRole(int userID);
    String getUserName(int userID);
}
