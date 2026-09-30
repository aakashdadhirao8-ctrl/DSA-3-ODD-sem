// File: src/ui/OverdriveUI.java
package ui;

import dsa.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

public class OverdriveUI extends JFrame {
    private User user;
    private GraphEngine engine;
    private JLabel lblGlobalTimer;
    private JTable table;

    public OverdriveUI(User user, GraphEngine engine) {
        this.user = user;
        this.engine = engine;
        initUI();
        startLiveTimer(); 
    }

    private void startLiveTimer() {
        javax.swing.Timer uiTimer = new javax.swing.Timer(1000, e -> {
            long closest = Long.MAX_VALUE;
            boolean hasActive = false;
            for (Task t : engine.taskMap.values()) {
                if (!t.isCompleted && !t.timeMode.equals("NONE")) {
                    hasActive = true;
                    if (t.deadlineMs < closest) closest = t.deadlineMs;
                }
            }
            if (!hasActive) {
                lblGlobalTimer.setText("NEXT DEADLINE: NO ACTIVE TIMERS"); lblGlobalTimer.setForeground(new Color(100, 100, 100));
            } else {
                long diff = closest - System.currentTimeMillis();
                if (diff <= 0) {
                    lblGlobalTimer.setText("⚠ CRITICAL: A QUEST TIME IS UP!"); lblGlobalTimer.setForeground(new Color(255, 50, 50));
                } else {
                    long s = diff / 1000, d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
                    String formatted = (d > 0) ? String.format("%dd %02dh %02dm %02ds", d, h, m, sec) : String.format("%02d:%02d:%02d", h, m, sec);
                    lblGlobalTimer.setText("NEXT DEADLINE: " + formatted); lblGlobalTimer.setForeground(new Color(0, 220, 255));
                }
            }
            if (table != null) table.repaint();
        });
        uiTimer.start();
    }

    private void initUI() {
        setTitle("OVERDRIVE - Gamified Campaign | Player: " + user.username);
        setSize(1100, 750); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setLocationRelativeTo(null); setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(15, 15, 18));

        JPanel topPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); Graphics2D g2d = (Graphics2D) g; g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setPaint(new GradientPaint(0, 0, new Color(40, 15, 60), getWidth(), getHeight(), new Color(15, 15, 18)));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        topPanel.setBorder(new EmptyBorder(15, 20, 15, 20)); topPanel.setOpaque(false);

        JPanel headerActions = new JPanel(new BorderLayout()); headerActions.setOpaque(false);
        lblGlobalTimer = new JLabel("NEXT DEADLINE: CALCULATING...", SwingConstants.LEFT); lblGlobalTimer.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerActions.add(lblGlobalTimer, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0)); btnPanel.setOpaque(false);
        JButton btnForfeit = createStyledButton("FORFEIT PROJECT", new Color(180, 40, 40));
        btnForfeit.addActionListener(e -> { if (JOptionPane.showConfirmDialog(this, "Forfeit project?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { engine.deleteActiveFile(); new ModeWindow(user).setVisible(true); dispose(); } });
        JButton btnLogout = createStyledButton("LOGOUT", new Color(60, 60, 60)); btnLogout.addActionListener(e -> { new AuthWindow().setVisible(true); dispose(); });
        btnPanel.add(btnForfeit); btnPanel.add(btnLogout); headerActions.add(btnPanel, BorderLayout.EAST);

        JPanel statsPanel = new JPanel(new BorderLayout(20, 0)); statsPanel.setOpaque(false); statsPanel.setBorder(new EmptyBorder(20, 0, 10, 0));
        JLabel lblLevel = new JLabel("LVL " + user.level); lblLevel.setFont(new Font("Segoe UI", Font.BOLD, 36)); lblLevel.setForeground(new Color(255, 215, 0)); statsPanel.add(lblLevel, BorderLayout.WEST);

        JProgressBar xpBar = new JProgressBar(0, 1000); xpBar.setValue(user.xp % 1000); xpBar.setString((user.xp % 1000) + " / 1000 XP TO NEXT LEVEL");
        xpBar.setStringPainted(true); xpBar.setFont(new Font("Segoe UI", Font.BOLD, 14)); xpBar.setBackground(new Color(30, 30, 30)); xpBar.setForeground(new Color(138, 43, 226)); xpBar.setBorderPainted(false);
        xpBar.setUI(new BasicProgressBarUI() { protected Color getSelectionBackground() { return Color.WHITE; } protected Color getSelectionForeground() { return Color.WHITE; } });
        statsPanel.add(xpBar, BorderLayout.CENTER);

        topPanel.add(headerActions, BorderLayout.NORTH); topPanel.add(statsPanel, BorderLayout.SOUTH);

        String[] cols = {"Quest", "Objective", "Time Left", "Rarity", "Reward", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        table = new JTable(model); table.setRowHeight(45); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(50, 50, 60)); table.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        table.getTableHeader().setBackground(new Color(25, 25, 30)); table.getTableHeader().setForeground(Color.LIGHT_GRAY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14)); table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 45)));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object v, boolean isSel, boolean foc, int r, int c) {
                if (c == 3) {
                    String rarity = v.toString();
                    return new JPanel(new GridBagLayout()) {
                        {
                            setOpaque(true); setBackground(isSel ? new Color(50, 50, 60) : new Color(20, 20, 24));
                            JLabel badge = new JLabel(rarity, SwingConstants.CENTER) {
                                protected void paintComponent(Graphics g) {
                                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                    if (rarity.equals("LEGENDARY")) g2.setColor(new Color(255, 140, 0)); else if (rarity.equals("EPIC")) g2.setColor(new Color(138, 43, 226)); else if (rarity.equals("RARE")) g2.setColor(new Color(30, 144, 255)); else if (rarity.equals("UNCOMMON")) g2.setColor(new Color(50, 205, 50)); else g2.setColor(new Color(150, 150, 150));
                                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15)); g2.dispose(); super.paintComponent(g);
                                }
                            };
                            badge.setForeground(rarity.equals("COMMON") ? Color.BLACK : Color.WHITE); badge.setFont(new Font("Segoe UI", Font.BOLD, 11)); badge.setBorder(new EmptyBorder(4, 12, 4, 12)); add(badge);
                        }
                    };
                }
                Component comp = super.getTableCellRendererComponent(tbl, v, isSel, foc, r, c); ((JComponent) comp).setBorder(new EmptyBorder(0, 15, 0, 15)); comp.setBackground(isSel ? new Color(50, 50, 60) : new Color(20, 20, 24));
                if (c == 2 && v instanceof Task) {
                    Task t = (Task) v;
                    if (t.timeMode.equals("NONE")) { setText("NO LIMIT"); comp.setForeground(new Color(100, 100, 100)); } else if (t.isCompleted) { setText("--:--:--"); comp.setForeground(new Color(100, 100, 100)); } else {
                        long diff = t.deadlineMs - System.currentTimeMillis();
                        if (diff <= 0) { setText("⚠ TIME UP"); comp.setForeground(new Color(255, 80, 80)); } else {
                            long secs = diff / 1000, d = secs / 86400, h = (secs % 86400) / 3600, m = (secs % 3600) / 60, s = secs % 60;
                            setText(d > 0 ? String.format("%dd %02d:%02d:%02d", d, h, m, s) : String.format("%02d:%02d:%02d", h, m, s)); comp.setForeground(Color.WHITE);
                        }
                    }
                    return comp;
                }
                if (c == 4 && v instanceof Task) {
                    Task t = (Task) v;
                    if (t.isCompleted) { setText("CLAIMED"); comp.setForeground(new Color(100, 100, 100)); } else if (!t.timeMode.equals("NONE") && System.currentTimeMillis() > t.deadlineMs) { setText("+" + Math.max(1, t.expReward / 2) + " XP (LATE)"); comp.setForeground(new Color(255, 100, 100)); } else { setText("+" + t.expReward + " XP"); comp.setForeground(new Color(218, 165, 32)); }
                    return comp;
                }
                String status = (String) tbl.getModel().getValueAt(r, 5); comp.setForeground(status.equals("✅ CLEARED") ? new Color(100, 100, 100) : Color.WHITE); return comp;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table); scrollPane.setBorder(BorderFactory.createEmptyBorder()); scrollPane.getViewport().setBackground(new Color(20, 20, 24));
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() { @Override protected void configureScrollBarColors() { this.thumbColor = new Color(70, 70, 80); this.trackColor = new Color(20, 20, 24); } @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); } @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); } private JButton createZeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; } });

        // --- CO2: KMP SEARCH BAR (GAMIFIED THEME) ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        searchPanel.setBackground(new Color(15, 15, 18));
        searchPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 45)));
        JLabel lblSearch = new JLabel("<html><font color='#A0A0A0'>Scout Quest:</font></html>");
        JTextField txtSearch = new JTextField(15);
        txtSearch.setBackground(new Color(30, 30, 35)); txtSearch.setForeground(Color.WHITE); txtSearch.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));
        JButton btnSearch = createStyledButton("KMP SCAN", new Color(138, 43, 226));
        JButton btnClear = createStyledButton("RESET", new Color(70, 70, 80));

        btnSearch.addActionListener(e -> populateTable(engine.searchTasksKMP(txtSearch.getText().trim())));
        btnClear.addActionListener(e -> { txtSearch.setText(""); populateTable(engine.topologicalOrder); });
        
        searchPanel.add(lblSearch); searchPanel.add(txtSearch); searchPanel.add(btnSearch); searchPanel.add(btnClear);

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(searchPanel, BorderLayout.NORTH);
        centerContainer.add(scrollPane, BorderLayout.CENTER);

        populateTable(engine.topologicalOrder);

        // --- BOTTOM ACTION BAR ---
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15)); actionBar.setBackground(new Color(20, 20, 24)); actionBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 40, 45)));
        JComboBox<String> pendingTasks = new JComboBox<>(); pendingTasks.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        engine.topologicalOrder.stream().map(engine.taskMap::get).filter(t -> !t.isCompleted && t.dependencies.stream().allMatch(dep -> { Task d = engine.taskMap.get(dep); return d == null || d.isCompleted; })).forEach(t -> pendingTasks.addItem(t.id + " - " + t.name));
        actionBar.add(pendingTasks);

        JButton btnComplete = createStyledButton("TURN IN QUEST", new Color(0, 150, 136));
        btnComplete.addActionListener(e -> {
            if (pendingTasks.getSelectedItem() != null) {
                String selectedId = pendingTasks.getSelectedItem().toString().split(" - ")[0]; Task t = engine.taskMap.get(selectedId); t.isCompleted = true;
                user.addXP(t.expReward); UserManager.saveUsersToCSV(); engine.saveActiveGraph(); getContentPane().removeAll(); initUI(); revalidate(); repaint();
            }
        });
        actionBar.add(btnComplete);

        JButton btnAssign = createStyledButton("ASSEMBLE PARTY (CO4)", new Color(218, 165, 32));
        btnAssign.addActionListener(e -> showGuildRecruitmentDialog());
        actionBar.add(btnAssign);

        add(topPanel, BorderLayout.NORTH); add(centerContainer, BorderLayout.CENTER); add(actionBar, BorderLayout.SOUTH);
    }

    private void populateTable(java.util.List<String> taskIds) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (String id : taskIds) {
            Task t = engine.taskMap.get(id);
            if(t == null) continue;
            model.addRow(new Object[]{t.id, t.name, t, t.rarity, t, t.isCompleted ? "✅ CLEARED" : "ACTIVE"});
        }
    }

    private void showGuildRecruitmentDialog() {
        JDialog dialog = new JDialog(this, "Guild Assembly (CO4)", true);
        dialog.setSize(600, 400); dialog.setLocationRelativeTo(this); dialog.setLayout(new BorderLayout()); dialog.getContentPane().setBackground(new Color(25, 25, 30));

        Map<String, Integer> workersMap = new HashMap<>();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> workerList = new JList<>(listModel);
        workerList.setBackground(new Color(15, 15, 18)); workerList.setForeground(Color.WHITE); workerList.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); inputPanel.setBackground(new Color(25, 25, 30));
        JTextField txtName = new JTextField(10); JSpinner spinCap = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        JButton btnAdd = createStyledButton("Recruit Member", new Color(138, 43, 226));
        
        inputPanel.add(new JLabel("<html><font color='white'>Hero Name:</font></html>")); inputPanel.add(txtName);
        inputPanel.add(new JLabel("<html><font color='white'>Quest Limit:</font></html>")); inputPanel.add(spinCap); inputPanel.add(btnAdd);

        btnAdd.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (!name.isEmpty() && !workersMap.containsKey(name)) {
                int cap = (int) spinCap.getValue(); workersMap.put(name, cap); listModel.addElement(name + " (Max Quests: " + cap + ")"); txtName.setText("");
            }
        });

        JButton btnRun = createStyledButton("DEPLOY GUILD (MAX FLOW)", new Color(184, 134, 11));
        btnRun.addActionListener(e -> {
            if (workersMap.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Recruit at least one hero."); return; }
            java.util.List<Task> pending = engine.topologicalOrder.stream().map(engine.taskMap::get).filter(t -> !t.isCompleted).toList();
            if (pending.isEmpty()) { JOptionPane.showMessageDialog(dialog, "No active quests."); return; }
            
            GraphEngine.FlowResult result = engine.assignWorkersDynamic(workersMap, pending);
            StringBuilder sb = new StringBuilder("=== GUILD DEPLOYMENT LOG ===\n\n");
            result.assignments.forEach((w, tasks) -> {
                sb.append(w).append(" [").append(tasks.size()).append("/").append(workersMap.get(w)).append(" Quests] -> ");
                sb.append(tasks.isEmpty() ? "Benched" : String.join(", ", tasks)).append("\n");
            });
            sb.append("\n=== UNASSIGNED QUESTS (OVER CAPACITY) ===\n");
            sb.append(result.unassigned.isEmpty() ? "None! Complete victory." : String.join(", ", result.unassigned));
            
            JOptionPane.showMessageDialog(dialog, sb.toString(), "CO4 Flow Results", JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.add(inputPanel, BorderLayout.NORTH); dialog.add(new JScrollPane(workerList), BorderLayout.CENTER);
        JPanel bottom = new JPanel(); bottom.setBackground(new Color(25, 25, 30)); bottom.add(btnRun); dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text); btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(Color.WHITE); btn.setBackground(bg);
        btn.setFocusPainted(false); btn.setBorder(new EmptyBorder(8, 15, 8, 15)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); return btn;
    }
}