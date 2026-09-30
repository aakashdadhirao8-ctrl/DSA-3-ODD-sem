// File: src/ui/OverwatchUI.java
package ui;

import dsa.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

public class OverwatchUI extends JFrame {
    private User user;
    private GraphEngine engine;
    private JLabel lblGlobalTimer;
    private JTable table;
    private Timer uiTimer;

    public OverwatchUI(User user, GraphEngine engine) {
        this.user = user;
        this.engine = engine;
        initUI();
        startLiveTimer(); 
    }

    private void startLiveTimer() {
        uiTimer = new Timer(1000, e -> {
            long closest = Long.MAX_VALUE;
            boolean hasActive = false;
            for (Task t : engine.taskMap.values()) {
                if (!t.isCompleted && !t.timeMode.equals("NONE")) {
                    hasActive = true;
                    if (t.deadlineMs < closest) closest = t.deadlineMs;
                }
            }
            if (!hasActive) {
                lblGlobalTimer.setText("NO ACTIVE TIMERS");
                lblGlobalTimer.setForeground(new Color(100, 100, 100));
            } else {
                long diff = closest - System.currentTimeMillis();
                if (diff <= 0) {
                    lblGlobalTimer.setText("⚠ TIME UP!");
                    lblGlobalTimer.setForeground(new Color(255, 69, 0)); 
                } else {
                    long s = diff / 1000, d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
                    if (d > 0) lblGlobalTimer.setText(String.format("%dd %02dh %02dm", d, h, m)); 
                    else lblGlobalTimer.setText(String.format("%02d:%02d:%02d", h, m, sec));
                    lblGlobalTimer.setForeground(new Color(0, 200, 255));
                }
            }
            if (table != null) table.repaint();
        });
        uiTimer.start();
    }

    private void initUI() {
        setTitle("OVERWATCH - Enterprise Analytics | " + user.username);
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(15, 15, 18));

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(new Color(15, 15, 18));
        topContainer.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel metricsPanel = new JPanel(new GridLayout(1, 5, 15, 15));
        metricsPanel.setBackground(new Color(15, 15, 18));
        
        int totalTasks = engine.taskMap.size();
        int completedTasks = (int) engine.taskMap.values().stream().filter(t -> t.isCompleted).count();
        int completionPercentage = totalTasks == 0 ? 0 : (completedTasks * 100) / totalTasks;

        lblGlobalTimer = new JLabel("CALCULATING...", SwingConstants.CENTER);
        lblGlobalTimer.setFont(new Font("Segoe UI", Font.BOLD, 16)); 
        JPanel timerCard = createMetricCard("NEXT DEADLINE", "", new Color(0, 200, 255));
        timerCard.add(lblGlobalTimer);

        metricsPanel.add(timerCard);
        metricsPanel.add(createMetricCard("TOTAL TASKS", String.valueOf(totalTasks), new Color(30, 144, 255)));
        metricsPanel.add(createMetricCard("COMPLETED", completedTasks + " (" + completionPercentage + "%)", new Color(50, 205, 50)));
        metricsPanel.add(createMetricCard("MIN DURATION", engine.totalProjectDuration + " Hours", new Color(186, 85, 211)));
        
        JButton btnLogout = createStyledButton("LOGOUT", new Color(40, 40, 45), Color.WHITE);
        btnLogout.addActionListener(e -> { if(uiTimer != null) uiTimer.stop(); new AuthWindow().setVisible(true); dispose(); });
        JPanel logoutPanel = new JPanel(new GridBagLayout());
        logoutPanel.setBackground(new Color(15, 15, 18)); logoutPanel.add(btnLogout);
        metricsPanel.add(logoutPanel);

        topContainer.add(metricsPanel, BorderLayout.CENTER);

        // --- TABLE SETUP ---
        String[] cols = {"ID", "Task Name", "Time Left", "EST", "EFT", "Slack", "Critical", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        table = new JTable(model);
        table.setRowHeight(40); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(45, 45, 55)); table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setBackground(new Color(22, 22, 26)); table.getTableHeader().setForeground(new Color(150, 150, 150));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13)); table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 45)));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object v, boolean isSel, boolean foc, int r, int c) {
                if (c == 6) {
                    String crit = v.toString();
                    return new JPanel(new GridBagLayout()) {
                        {
                            setOpaque(true); setBackground(isSel ? new Color(45, 45, 55) : new Color(20, 20, 24));
                            if (crit.equals("⚠ YES")) {
                                JLabel badge = new JLabel("CRITICAL", SwingConstants.CENTER) {
                                    protected void paintComponent(Graphics g) {
                                        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                        g2.setColor(new Color(180, 40, 40)); g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                                        g2.dispose(); super.paintComponent(g);
                                    }
                                };
                                badge.setForeground(Color.WHITE); badge.setFont(new Font("Segoe UI", Font.BOLD, 11)); badge.setBorder(new EmptyBorder(3, 10, 3, 10)); add(badge);
                            } else {
                                JLabel lbl = new JLabel("-"); lbl.setForeground(new Color(100, 100, 100)); add(lbl);
                            }
                        }
                    };
                }
                Component comp = super.getTableCellRendererComponent(tbl, v, isSel, foc, r, c);
                ((JComponent) comp).setBorder(new EmptyBorder(0, 15, 0, 15)); comp.setBackground(isSel ? new Color(45, 45, 55) : new Color(20, 20, 24));
                if (c == 2 && v instanceof Task) {
                    Task t = (Task) v;
                    if (t.timeMode.equals("NONE")) { setText("NO LIMIT"); comp.setForeground(new Color(100, 100, 100)); } 
                    else if (t.isCompleted) { setText("--:--:--"); comp.setForeground(new Color(100, 100, 100)); } 
                    else {
                        long diff = t.deadlineMs - System.currentTimeMillis();
                        if (diff <= 0) { setText("⚠ TIME UP"); comp.setForeground(new Color(255, 100, 100)); } 
                        else {
                            long secs = diff / 1000, d = secs / 86400, h = (secs % 86400) / 3600, m = (secs % 3600) / 60, sec = secs % 60;
                            setText(d > 0 ? String.format("%dd %02d:%02d:%02d", d, h, m, sec) : String.format("%02d:%02d:%02d", h, m, sec)); comp.setForeground(Color.WHITE);
                        }
                    }
                    return comp;
                }
                String status = (String) tbl.getModel().getValueAt(r, 7);
                if (c == 7) comp.setForeground(status.equals("✅ DONE") ? new Color(50, 205, 50) : new Color(150, 150, 150));
                else comp.setForeground(status.equals("✅ DONE") ? new Color(100, 100, 100) : Color.WHITE);
                return comp;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); scrollPane.getViewport().setBackground(new Color(20, 20, 24));
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = new Color(70, 70, 80); this.trackColor = new Color(20, 20, 24); }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });

        // --- CO2: KMP SEARCH BAR ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        searchPanel.setBackground(new Color(15, 15, 18));
        searchPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 45)));
        JLabel lblSearch = new JLabel("<html><font color='#A0A0A0'>Find Task:</font></html>");
        JTextField txtSearch = new JTextField(15);
        txtSearch.setBackground(new Color(30, 30, 35)); txtSearch.setForeground(Color.WHITE); txtSearch.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));
        JButton btnSearch = createStyledButton("SEARCH (KMP)", new Color(70, 130, 180), Color.WHITE);
        JButton btnClear = createStyledButton("CLEAR", new Color(70, 70, 80), Color.WHITE);

        btnSearch.addActionListener(e -> populateTable(engine.searchTasksKMP(txtSearch.getText().trim())));
        btnClear.addActionListener(e -> { txtSearch.setText(""); populateTable(engine.topologicalOrder); });
        
        searchPanel.add(lblSearch); searchPanel.add(txtSearch); searchPanel.add(btnSearch); searchPanel.add(btnClear);

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(searchPanel, BorderLayout.NORTH);
        centerContainer.add(scrollPane, BorderLayout.CENTER);

        // Populate initial table
        populateTable(engine.topologicalOrder);

        // --- BOTTOM ACTION BAR ---
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        actionBar.setBackground(new Color(22, 22, 26)); actionBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 40, 45)));
        
        JComboBox<String> pendingTasks = new JComboBox<>();
        engine.topologicalOrder.stream().map(engine.taskMap::get).filter(t -> !t.isCompleted).forEach(t -> pendingTasks.addItem(t.id + " - " + t.name));
        actionBar.add(pendingTasks);

        JButton btnComplete = createStyledButton("MARK COMPLETE", new Color(0, 128, 128), Color.WHITE);
        btnComplete.addActionListener(e -> {
            if (pendingTasks.getSelectedItem() != null) {
                String selectedId = pendingTasks.getSelectedItem().toString().split(" - ")[0];
                Task t = engine.taskMap.get(selectedId); t.isCompleted = true;
                user.addXP(t.expReward); UserManager.saveUsersToCSV(); engine.saveActiveGraph(); 
                if(uiTimer != null) uiTimer.stop(); getContentPane().removeAll(); initUI(); startLiveTimer(); revalidate(); repaint();
            }
        });
        actionBar.add(btnComplete);

        JButton btnAssign = createStyledButton("WORKER ALLOCATION (CO4)", new Color(218, 165, 32), Color.BLACK);
        btnAssign.addActionListener(e -> showWorkerAssignmentDialog());
        actionBar.add(btnAssign);

        add(topContainer, BorderLayout.NORTH); add(centerContainer, BorderLayout.CENTER); add(actionBar, BorderLayout.SOUTH);
    }

    private void populateTable(java.util.List<String> taskIds) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (String id : taskIds) {
            Task t = engine.taskMap.get(id);
            if(t == null) continue;
            String status = t.isCompleted ? "✅ DONE" : "PENDING";
            model.addRow(new Object[]{t.id, t.name, t, t.est, t.eft, t.slack, t.isCritical ? "⚠ YES" : "NO", status});
        }
    }

    private void showWorkerAssignmentDialog() {
        JDialog dialog = new JDialog(this, "Dynamic Workforce Allocation (CO4)", true);
        dialog.setSize(600, 400); dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout()); dialog.getContentPane().setBackground(new Color(25, 25, 30));

        Map<String, Integer> workersMap = new HashMap<>();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> workerList = new JList<>(listModel);
        workerList.setBackground(new Color(15, 15, 18)); workerList.setForeground(Color.WHITE); workerList.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); inputPanel.setBackground(new Color(25, 25, 30));
        JTextField txtName = new JTextField(10); JSpinner spinCap = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        JButton btnAdd = createStyledButton("Add Worker", new Color(50, 150, 250), Color.WHITE);
        
        inputPanel.add(new JLabel("<html><font color='white'>Name:</font></html>")); inputPanel.add(txtName);
        inputPanel.add(new JLabel("<html><font color='white'>Capacity (Tasks):</font></html>")); inputPanel.add(spinCap); inputPanel.add(btnAdd);

        btnAdd.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (!name.isEmpty() && !workersMap.containsKey(name)) {
                int cap = (int) spinCap.getValue(); workersMap.put(name, cap); listModel.addElement(name + " (Capacity: " + cap + ")"); txtName.setText("");
            }
        });

        JButton btnRun = createStyledButton("RUN MAX FLOW ASSIGNMENT", new Color(50, 205, 50), Color.BLACK);
        btnRun.addActionListener(e -> {
            if (workersMap.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Add at least one worker."); return; }
            java.util.List<Task> pending = engine.topologicalOrder.stream().map(engine.taskMap::get).filter(t -> !t.isCompleted).toList();
            if (pending.isEmpty()) { JOptionPane.showMessageDialog(dialog, "No pending tasks."); return; }
            
            GraphEngine.FlowResult result = engine.assignWorkersDynamic(workersMap, pending);
            StringBuilder sb = new StringBuilder("=== EDMONDS-KARP ASSIGNMENT ===\n\n");
            result.assignments.forEach((w, tasks) -> {
                sb.append(w).append(" [").append(tasks.size()).append("/").append(workersMap.get(w)).append("] -> ");
                sb.append(tasks.isEmpty() ? "None" : String.join(", ", tasks)).append("\n");
            });
            sb.append("\n=== LEFTOVER TASKS (OVER CAPACITY) ===\n");
            sb.append(result.unassigned.isEmpty() ? "None!" : String.join(", ", result.unassigned));
            
            JOptionPane.showMessageDialog(dialog, sb.toString(), "Flow Results", JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.add(inputPanel, BorderLayout.NORTH); dialog.add(new JScrollPane(workerList), BorderLayout.CENTER);
        JPanel bottom = new JPanel(); bottom.setBackground(new Color(25, 25, 30)); bottom.add(btnRun); dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel createMetricCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel(new GridLayout(2, 1)) {
            @Override protected void paintComponent(Graphics g) { super.paintComponent(g); g.setColor(accentColor); g.fillRect(0, 0, 4, getHeight()); }
        };
        card.setBackground(new Color(24, 24, 28)); card.setBorder(new EmptyBorder(10, 15, 10, 10));
        JLabel lblTitle = new JLabel(title); lblTitle.setForeground(new Color(140, 140, 150)); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel lblValue = new JLabel(value); lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20)); lblValue.setForeground(Color.WHITE);
        card.add(lblTitle); if (!value.isEmpty()) card.add(lblValue); return card;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text); btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(fg); btn.setBackground(bg);
        btn.setFocusPainted(false); btn.setBorder(new EmptyBorder(8, 15, 8, 15)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); return btn;
    }
}