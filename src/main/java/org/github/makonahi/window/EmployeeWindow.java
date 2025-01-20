package org.github.makonahi.window;

import javax.swing.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class EmployeeWindow extends JFrame {
    private JTable allOrdersTable, myOrdersTable;
    private DefaultTableModel allOrdersModel, myOrdersModel;
    private JButton takeOrderButton, cancelOrderButton, completeOrderButton;
    private int employeeId;
    private Connection connection;

    public EmployeeWindow(int employeeId, Connection connection, String username) {
        this.employeeId = employeeId;
        this.connection = connection;

        setTitle("Вы вошли как: "+username);
        setSize(900, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        allOrdersModel = new DefaultTableModel(new String[]{"OrderID", "Product", "Description", "Price", "Quantity", "Status", "Total Price"}, 0);
        myOrdersModel = new DefaultTableModel(new String[]{"OrderID", "Product", "Description", "Price", "Quantity", "Status", "Total Price"}, 0);

        allOrdersTable = new JTable(allOrdersModel);
        myOrdersTable = new JTable(myOrdersModel);

        takeOrderButton = new JButton("Взять заказ");
        cancelOrderButton = new JButton("Отменить заказ");
        completeOrderButton = new JButton("Выполнить заказ");

        takeOrderButton.setEnabled(false);
        cancelOrderButton.setEnabled(false);
        completeOrderButton.setEnabled(false);

        JPanel tablesPanel = new JPanel(new GridLayout(1, 2));
        tablesPanel.add(new JScrollPane(allOrdersTable));
        tablesPanel.add(new JScrollPane(myOrdersTable));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(takeOrderButton);
        buttonsPanel.add(cancelOrderButton);
        buttonsPanel.add(completeOrderButton);

        add(tablesPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        loadAllOrders();
        loadMyOrders();

        allOrdersTable.getSelectionModel().addListSelectionListener(e -> updateButtonsState());
        myOrdersTable.getSelectionModel().addListSelectionListener(e -> updateButtonsState());

        takeOrderButton.addActionListener(e -> takeOrder());
        cancelOrderButton.addActionListener(e -> cancelOrder());
        completeOrderButton.addActionListener(e -> completeOrder());

        setVisible(true);
    }

    private void loadAllOrders() {
        allOrdersModel.setRowCount(0);
        String query = """
                SELECT o.OrderID, p.Name, p.Description, p.Price, o.Quantity, o.Status, (p.Price * o.Quantity) AS TotalPrice
                FROM Orders o
                JOIN Products p ON o.ProductID = p.ProductID
                """;
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                allOrdersModel.addRow(new Object[]{
                        rs.getInt("OrderID"), rs.getString("Name"), rs.getString("Description"),
                        rs.getDouble("Price"), rs.getInt("Quantity"), rs.getString("Status"),
                        rs.getDouble("TotalPrice")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки заказов: " + e.getMessage());
        }
    }

    private void loadMyOrders() {
        myOrdersModel.setRowCount(0);
        String query = """
                SELECT o.OrderID, p.Name, p.Description, p.Price, o.Quantity, o.Status, (p.Price * o.Quantity) AS TotalPrice
                FROM Orders o
                JOIN Products p ON o.ProductID = p.ProductID
                WHERE o.Status = 'В процессе изготовления'
                """;
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                myOrdersModel.addRow(new Object[]{
                        rs.getInt("OrderID"), rs.getString("Name"), rs.getString("Description"),
                        rs.getDouble("Price"), rs.getInt("Quantity"), rs.getString("Status"),
                        rs.getDouble("TotalPrice")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки ваших заказов: " + e.getMessage());
        }
    }

    private void takeOrder() {
        int selectedRow = allOrdersTable.getSelectedRow();
        if (selectedRow == -1) return;

        int orderId = (int) allOrdersModel.getValueAt(selectedRow, 0);
        String status = (String) allOrdersModel.getValueAt(selectedRow, 5);

        if (!"Поиск исполнителя".equals(status)) {
            JOptionPane.showMessageDialog(this, "Этот заказ уже в работе!");
            return;
        }

        updateStatus(orderId, "В процессе изготовления");
        loadAllOrders();
        loadMyOrders();
    }

    private void cancelOrder() {
        int selectedRow = myOrdersTable.getSelectedRow();
        if (selectedRow == -1) return;

        int orderId = (int) myOrdersModel.getValueAt(selectedRow, 0);
        String status = (String) myOrdersModel.getValueAt(selectedRow, 5);

        if (!"В процессе изготовления".equals(status)) {
            JOptionPane.showMessageDialog(this, "Этот заказ нельзя отменить!");
            return;
        }

        updateStatus(orderId, "Поиск исполнителя");
        loadAllOrders();
        loadMyOrders();
    }

    private void completeOrder() {
        int selectedRow = myOrdersTable.getSelectedRow();
        if (selectedRow == -1) return;

        int orderId = (int) myOrdersModel.getValueAt(selectedRow, 0);
        String status = (String) myOrdersModel.getValueAt(selectedRow, 5);

        if (!"В процессе изготовления".equals(status)) {
            JOptionPane.showMessageDialog(this, "Этот заказ нельзя выполнить!");
            return;
        }

        updateStatus(orderId, "Выполнен");
        loadAllOrders();
        loadMyOrders();
    }

    private void updateStatus(int orderId, String newStatus) {
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
        int selectedLeftRow = allOrdersTable.getSelectedRow();
        int selectedRightRow = myOrdersTable.getSelectedRow();

        if (selectedLeftRow != -1) {
            String status = (String) allOrdersModel.getValueAt(selectedLeftRow, 5);
            takeOrderButton.setEnabled("Поиск исполнителя".equals(status));
        } else {
            takeOrderButton.setEnabled(false);
        }

        if (selectedRightRow != -1) {
            String status = (String) myOrdersModel.getValueAt(selectedRightRow, 5);
            cancelOrderButton.setEnabled("В процессе изготовления".equals(status));
            completeOrderButton.setEnabled("В процессе изготовления".equals(status));
        } else {
            cancelOrderButton.setEnabled(false);
            completeOrderButton.setEnabled(false);
        }
    }
}


