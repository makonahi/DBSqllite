package org.github.makonahi.data.access.object;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {
    private Connection conn;

    public UserDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void addUser(String name, String login, String password, String role) {
        String sql = "INSERT INTO Users (Name, Login, Password, Role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, login);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
            System.out.printf("%s added successfully.\n", role.toUpperCase());
        } catch (SQLException e) {
            System.out.println("Ошибка добавления пользователя: " + e.getMessage());
        }
    }

    @Override
    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String user = "ID: " + rs.getInt("UserID") +
                        ", Name: " + rs.getString("Name") +
                        ", Role: " + rs.getString("Role");
                users.add(user);
            }
        } catch (SQLException e) {
            System.out.println("Ошибка получения юзеров: " + e.getMessage());
        }
        return users;
    }

    @Override
    public void deleteUser(int userId) {
        String sql = "DELETE FROM Users WHERE UserID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            System.out.println("User deleted successfully.");
        } catch (SQLException e) {
            System.out.println("Ошибка удаления пользователя из БД: " + e.getMessage());
        }
    }

    @Override
    public void deleteAllUsers() {
        String sql = "DELETE FROM Users";
        String resetInc = "DELETE FROM sqlite_sequence WHERE name = 'Users'";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(resetInc);
            stmt.executeUpdate(sql);
            System.out.println("БД юзеров удалена.");
        } catch (SQLException e) {
            System.out.println("Ошибка удаления БД пользователей: " + e.getMessage());
        }
    }

    @Override
    public int authenticateUser(String login, String password) {
        String query = "SELECT UserID FROM Users WHERE Login = ? AND Password = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, login);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("UserID");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during authentication: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public String getUserRole(int userID) {
        String query = "SELECT Role FROM Users WHERE UserID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Role");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during authentication: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getUserName(int userID) {
        String query = "SELECT Name FROM Users WHERE UserID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Name");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during authentication: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean registerUser(String name, String login, String password, String role) {
        String sql = "INSERT INTO Users (Name, Login, Password, Role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, login);
            stmt.setString(3, password);
            stmt.setString(4, role);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Ошибка регистрации: " + e.getMessage());
            return false;
        }
    }


}

