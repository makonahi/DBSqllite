package org.github.makonahi.window;

import org.github.makonahi.data.access.object.UserDAO;
import org.github.makonahi.data.access.object.UserDAOImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LoginWindow extends JFrame {
    private JTextField loginField;
    private JTextField passwordField;
    private JButton loginButton;
    private JButton registerButton;

    public LoginWindow() {
        setTitle("Вход");
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));

        JLabel loginLabel = new JLabel("Логин:");
        loginField = new JTextField();

        JLabel passwordLabel = new JLabel("Пароль:");
        passwordField = new JTextField();

        loginButton = new JButton("Вход");
        loginButton.addActionListener(new LoginButtonListener());

        registerButton = new JButton("Регистрация");
        registerButton.addActionListener(e -> {
            new RegistrationWindow();
            dispose();
        });

        panel.add(loginLabel);
        panel.add(loginField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(new JLabel());
        panel.add(loginButton);
        panel.add(new JLabel());
        panel.add(registerButton);

        add(panel);

        setVisible(true);
    }

    private class LoginButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String login = loginField.getText();
            String password = passwordField.getText();

            if (login.isEmpty() || password.isEmpty()){
                JOptionPane.showMessageDialog(LoginWindow.this,
                        "Пустые поля.");
                return;
            }


            String databasePath = Paths.get("brewery.db").toAbsolutePath().toString();
            String url = "jdbc:sqlite:" + databasePath;
            try {
                Connection connection = DriverManager.getConnection(url);
                if (connection == null) {
                    JOptionPane.showMessageDialog(LoginWindow.this,
                            "Ошибка подключения к БД.");
                    return;
                }

                UserDAO userDAO = new UserDAOImpl(connection);
                int userID=userDAO.authenticateUser(login, password);
                String role = userDAO.getUserRole(userID);
                String username = userDAO.getUserName(userID);

                if (role == null) {
                    JOptionPane.showMessageDialog(LoginWindow.this, "Неверный логин или пароль.");
                } else if (role.equals("customer")) {
                    JOptionPane.showMessageDialog(LoginWindow.this, "Вы вошли как заказчик.");
                    new ClientWindow(userID, connection, username);
                    dispose();
                } else if (role.equals("employee")) {
                    JOptionPane.showMessageDialog(LoginWindow.this, "Вы вошли как исполнитель.");
                    new EmployeeWindow(userID, connection,username);
                    dispose();
                }
            } catch (SQLException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new LoginWindow();
    }
}

