package org.github.makonahi;

import org.github.makonahi.data.access.object.UserDAO;
import org.github.makonahi.data.access.object.UserDAOImpl;

import java.nio.file.Paths;
import java.sql.*;

public class Main {
    public static void main(String[] args) {
        String databasePath = Paths.get("brewery.db").toAbsolutePath().toString();
        String url = "jdbc:sqlite:" + databasePath;
        try (
            Connection connection = DriverManager.getConnection(url);
        )
        {
            UserDAO userDAO = new UserDAOImpl(connection);

            System.out.println("All users:");
            for (String user : userDAO.getAllUsers()) {
                System.out.println(user);
            }

        }
        catch(SQLException e) {
            e.printStackTrace(System.err);
        }
    }
}