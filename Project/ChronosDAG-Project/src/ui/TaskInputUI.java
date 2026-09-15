// File: src/ui/TaskInputUI.java
package ui;

import dsa.GraphEngine;
import dsa.Task;
import dsa.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TaskInputUI extends JFrame {
    private GraphEngine engine;
    private DefaultTableModel tableModel;

    public TaskInputUI(User user, String mode) {
        engine = new GraphEngine();
        
        setTitle("Project Builder - " + mode);
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Add New Task"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtId = new JTextField(12);
        JTextField txtName = new JTextField(12);
        
        // --- NEW: Smart Time Input ---
        JTextField txtDuration = new JTextField(5);
        JComboBox<String> cmbUnit = new JComboBox<>(new String[]{"Hours", "Days"});
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timePanel.add(txtDuration);
        timePanel.add(cmbUnit);
        
        JTextField txtDeps = new JTextField(12);

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(new JLabel("Task ID:"), gbc);
        gbc.gridx = 1; formPanel.add(txtId, gbc);
        
        gbc.gridx = 0; gbc.gridy = ++row; formPanel.add(new JLabel("Task Name:"), gbc);
        gbc.gridx = 1; formPanel.add(txtName, gbc);
        
        gbc.gridx = 0; gbc.gridy = ++row; formPanel.add(new JLabel("Time Estimate:"), gbc);
        gbc.gridx = 1; formPanel.add(timePanel, gbc);
        
        gbc.gridx = 0; gbc.gridy = ++row; formPanel.add(new JLabel("Prerequisites:"), gbc);
        gbc.gridx = 1; formPanel.add(txtDeps, gbc);

        JButton btnAdd = new JButton("Add Task to Graph");
        btnAdd.setBackground(new Color(30, 144, 255));
        btnAdd.addActionListener(e -> {
            try {
                String id = txtId.getText().trim();
                String name = txtName.getText().trim();
                int timeVal = Integer.parseInt(txtDuration.getText().trim());
                List<String> deps = txtDeps.getText().trim().isEmpty() ? 
                    List.of() : Arrays.stream(txtDeps.getText().split(",")).map(String::trim).collect(Collectors.toList());

                if (id.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("ID and Name cannot be empty.");
                if (engine.taskMap.containsKey(id)) throw new IllegalArgumentException("Task ID already exists!");

                // --- AUTO CALCULATE HOURS & LIVE TIMER ---
                int durationInHours = cmbUnit.getSelectedItem().equals("Days") ? timeVal * 24 : timeVal;
                
                Task t = new Task(id, name, durationInHours, null, deps);
                t.timeMode = "TIMER"; // System automatically makes it a live countdown
                t.deadlineMs = System.currentTimeMillis() + (durationInHours * 3600000L); // Hours to Milliseconds
                
                engine.addTask(t);
                tableModel.addRow(new Object[]{id, name, deps.isEmpty() ? "None" : String.join(", ", deps), durationInHours + " Hours"});
                
                txtId.setText(""); txtName.setText(""); txtDuration.setText(""); txtDeps.setText("");
                txtId.requestFocus();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2;
        formPanel.add(btnAdd, gbc);

        String[] cols = {"ID", "Name", "Dependencies", "Total Duration"};
        tableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(tableModel);

        JButton btnLaunch = new JButton("LAUNCH " + mode + " DASHBOARD");
        btnLaunch.setBackground(new Color(50, 205, 50));
        btnLaunch.setPreferredSize(new Dimension(100, 50));
        btnLaunch.addActionListener(e -> {
            if (engine.taskMap.isEmpty()) return;
            try {
                engine.buildGraph();
                if (engine.processProject()) {
                    engine.saveGraphToCSV(user.username);
                    if (mode.equals("OVERWATCH")) new OverwatchUI(user, engine).setVisible(true);
                    else new OverdriveUI(user, engine).setVisible(true);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "DEADLOCK DETECTED!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(formPanel, BorderLayout.WEST);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnLaunch, BorderLayout.SOUTH);
    }
}