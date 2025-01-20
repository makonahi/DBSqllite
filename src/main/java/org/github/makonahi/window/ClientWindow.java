package org.github.makonahi.window;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;

class ClientWindow extends JFrame {
    private JTable ordersTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> productComboBox;
    private JTextField quantityField;
    private JButton addOrderButton, refreshButton, closeOrderButton, cancelOrderButton;
    private int customerId;
    private Connection connection;

    public ClientWindow(int customerId, Connection connection, String userName) {
        this.customerId = customerId;
        this.connection = connection;

        setTitle("Вы вошли как: " + userName);
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Создание модели таблицы
        tableModel = new DefaultTableModel(new String[]{"OrderID", "Product Name", "Description", "Price", "Quantity", "Status", "Total Price"}, 0);
        ordersTable = new JTable(tableModel);
        ordersTable.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer()); // Настройка цветов статуса

        // Поля для ввода нового заказа
        productComboBox = new JComboBox<>();
        loadProductsToComboBox();
        quantityField = new JTextField(5);
        addOrderButton = new JButton("Add Order");

        // Кнопки управления заказами
        refreshButton = new JButton("Refresh");
        closeOrderButton = new JButton("Закрыть заказ");
        cancelOrderButton = new JButton("Отменить заказ");

        closeOrderButton.setEnabled(false);
        cancelOrderButton.setEnabled(false);

        // Верхняя панель (кнопка обновления)
        JPanel topPanel = new JPanel();
        topPanel.add(refreshButton);

        // Центральная панель (таблица заказов)
        JScrollPane tableScrollPane = new JScrollPane(ordersTable);

        // Нижняя панель (создание нового заказа)
        JPanel orderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        orderPanel.add(new JLabel("Product:"));
        orderPanel.add(productComboBox);
        orderPanel.add(new JLabel("Quantity:"));
        orderPanel.add(quantityField);
        orderPanel.add(addOrderButton);
        orderPanel.add(closeOrderButton);
        orderPanel.add(cancelOrderButton);

        // Основная панель
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(orderPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Обработчики кнопок
        addOrderButton.addActionListener(e -> addOrder());
        refreshButton.addActionListener(e -> loadOrders());
        closeOrderButton.addActionListener(e -> closeOrder());
        cancelOrderButton.addActionListener(e -> cancelOrder());

        ordersTable.getSelectionModel().addListSelectionListener(e -> updateButtonsState());


        loadOrders();
        setVisible(true);
    }

    private void loadOrders() {
        tableModel.setRowCount(0);
        String query = """
                SELECT o.OrderID, p.Name, p.Description, p.Price, o.Quantity, o.Status, (p.Price * o.Quantity) AS TotalPrice
                FROM Orders o
                JOIN Products p ON o.ProductID = p.ProductID
                WHERE o.CustomerID = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getInt("OrderID"), rs.getString("Name"), rs.getString("Description"),
                            rs.getDouble("Price"), rs.getInt("Quantity"), rs.getString("Status"),
                            rs.getDouble("TotalPrice")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки заказа: " + e.getMessage());
        }

    }

    private void addOrder() {
        String productName = (String) productComboBox.getSelectedItem();
        String quantityText = quantityField.getText().trim();

        if (productName == null || productName.isEmpty() || quantityText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Product and quantity cannot be empty!");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Введите число в поле 'Количество'.");
            return;
        }

        if (quantity==0||quantity<0||quantity>15000) {
            JOptionPane.showMessageDialog(this, "Введите число в поле 'Количество'.");
            return;
        }

        String queryProductID = "SELECT ProductID FROM Products WHERE Name = ?";
        String queryInsertOrder = "INSERT INTO Orders (CustomerID, ProductID, Quantity, Status) VALUES (?, ?, ?, 'Поиск исполнителя')";

        try (PreparedStatement stmtProductID = connection.prepareStatement(queryProductID);
             PreparedStatement stmtInsertOrder = connection.prepareStatement(queryInsertOrder)) {

            stmtProductID.setString(1, productName);
            try (ResultSet rs = stmtProductID.executeQuery()) {
                if (rs.next()) {
                    int productId = rs.getInt("ProductID");

                    stmtInsertOrder.setInt(1, customerId);
                    stmtInsertOrder.setInt(2, productId);
                    stmtInsertOrder.setInt(3, quantity);
                    stmtInsertOrder.executeUpdate();
                    loadOrders();

                    JOptionPane.showMessageDialog(this, "Заказ успешно добавлен.");
                    quantityField.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "Продукт не найден.");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка добавления заказа: " + e.getMessage());
        }
    }

    private void closeOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) return;

        int orderId = (int) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 5);

        if (!"Выполнен".equals(status)) {
            JOptionPane.showMessageDialog(this, "Можно закрыть только выполненный заказ!");
            return;
        }

        updateOrderStatus(orderId, "Закрыт");
        loadOrders();
    }

    private void cancelOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) return;

        int orderId = (int) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 5);

        if (!"Поиск исполнителя".equals(status)) {
            JOptionPane.showMessageDialog(this, "Можно отменить только заказ в поиске исполнителя!");
            return;
        }

        updateOrderStatus(orderId, "Отменен");
        loadOrders();
    }

    private void updateOrderStatus(int orderId, String newStatus) {
        String query = "UPDATE Orders SET Status = ? WHERE OrderID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка изменения статуса: " + e.getMessage());
        }
    }

    private void updateButtonsState() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            closeOrderButton.setEnabled(false);
            cancelOrderButton.setEnabled(false);
            return;
        }

        String status = (String) tableModel.getValueAt(selectedRow, 5);
        closeOrderButton.setEnabled("Выполнен".equals(status));
        cancelOrderButton.setEnabled("Поиск исполнителя".equals(status));
    }

    private void loadProductsToComboBox() {
        String query = "SELECT Name FROM Products";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                productComboBox.addItem(rs.getString("Name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage());
        }
    }
}

class StatusCellRenderer extends DefaultTableCellRenderer {
    @Override
    protected void setValue(Object value) {
        if (value != null) {
            String status = value.toString();
            setText(status);
            setFont(getFont().deriveFont(Font.BOLD));

            switch (status) {
                case "Выполнен" -> setForeground(new Color(34, 177, 76));
                case "Поиск исполнителя", "В процессе изготовления" -> setForeground(new Color(255, 140, 0));
                case "Отменен" -> setForeground(Color.RED);
                case "Закрыт" -> setForeground(Color.GRAY);
                default -> setForeground(Color.BLACK);
            }
        }
    }
}
