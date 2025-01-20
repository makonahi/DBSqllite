package org.github.makonahi.window;

import org.github.makonahi.data.access.object.UserDAO;
import org.github.makonahi.data.access.object.UserDAOImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

class RegistrationWindow extends JFrame {
    private JTextField loginField;
    private JTextField passwordField;
    private JComboBox<String> roleComboBox;
    private JButton registerButton;
    private JTextField nameField;

    public RegistrationWindow() {
        setTitle("Register");
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));

        JLabel nameLabel = new JLabel("Имя:");
        nameField = new JTextField();

        JLabel loginLabel = new JLabel("Логин:");
        loginField = new JTextField();

        JLabel passwordLabel = new JLabel("Пароль:");
        passwordField = new JTextField();

        JLabel roleLabel = new JLabel("Статус УЗ:");
        roleComboBox = new JComboBox<>(new String[]{"Заказчик", "Исполнитель"});

        registerButton = new JButton("Зарегистрироваться");
        registerButton.addActionListener(new RegisterButtonListener());

        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(loginLabel);
        panel.add(loginField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(roleLabel);
        panel.add(roleComboBox);
        panel.add(new JLabel());
        panel.add(registerButton);

        add(panel);

        setVisible(true);
    }

    private class RegisterButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = nameField.getText();
            String login = loginField.getText();
            String password = passwordField.getText();
            String role = switch ((String) roleComboBox.getSelectedItem()){
                case "Заказчик":
                    yield "customer";
                case "Исполнитель":
                    yield  "employee";
                default:
                    throw new IllegalStateException("Unexpected value: " + (String) roleComboBox.getSelectedItem());
            };



            if (login.isEmpty() || password.isEmpty()){
                JOptionPane.showMessageDialog(RegistrationWindow.this,
                        "Пустые поля.");
                return;
            }

            String databasePath = Paths.get("brewery.db").toAbsolutePath().toString();
            String url = "jdbc:sqlite:" + databasePath;
            try {
                Connection connection = DriverManager.getConnection(url);
                if (connection == null) {
                    JOptionPane.showMessageDialog(RegistrationWindow.this,
                            "Ошибка подключения к БД.");
                    return;
                }

                UserDAO userDAO = new UserDAOImpl(connection);
                boolean success = userDAO.registerUser(name, login, password, role);

                if (success) {
                    JOptionPane.showMessageDialog(RegistrationWindow.this,
                            "Регистрация успешна.");
                    new LoginWindow();
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(RegistrationWindow.this,
                            "Пользователь уже существует.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
