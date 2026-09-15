// File: src/ui/OverdriveUI.java
package ui;

import dsa.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Random;

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
                lblGlobalTimer.setText("NEXT DEADLINE: NO ACTIVE TIMERS");
                lblGlobalTimer.setForeground(Color.LIGHT_GRAY);
            } else {
                long diff = closest - System.currentTimeMillis();
                if (diff <= 0) {
                    lblGlobalTimer.setText("⚠ CRITICAL: A QUEST TIME IS UP!");
                    lblGlobalTimer.setForeground(Color.RED);
                } else {
                    long s = diff / 1000;
                    long d = s / 86400;
                    long h = (s % 86400) / 3600;
                    long m = (s % 3600) / 60;
                    long sec = s % 60;
                    
                    String formatted;
                    if (d > 0) formatted = String.format("%dd %02dh %02dm %02ds", d, h, m, sec);
                    else formatted = String.format("%02d:%02d:%02d", h, m, sec);
                    
                    lblGlobalTimer.setText("NEXT DEADLINE: " + formatted);
                    lblGlobalTimer.setForeground(new Color(0, 200, 255));
                }
            }
            if (table != null) table.repaint();
        });
        uiTimer.start();
    }

    private void initUI() {
        setTitle("OVERDRIVE - Gamified Campaign | Player: " + user.username);
        setSize(1050, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(18, 18, 18));

        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.setBackground(new Color(18, 18, 18));
        
        JPanel headerActions = new JPanel(new BorderLayout());
        headerActions.setBackground(new Color(18, 18, 18));
        
        lblGlobalTimer = new JLabel("NEXT DEADLINE: CALCULATING...", SwingConstants.LEFT);
        lblGlobalTimer.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerActions.add(lblGlobalTimer, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(new Color(18, 18, 18));
        
        JButton btnForfeit = new JButton("FORFEIT");
        btnForfeit.setBackground(new Color(139, 0, 0)); 
        btnForfeit.setForeground(Color.WHITE);
        btnForfeit.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Wipe project?", "Forfeit", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                engine.deleteGraphCSV(user.username); 
                new TaskInputUI(user, "OVERDRIVE").setVisible(true); 
                dispose();
            }
        });
        
        JButton btnLogout = new JButton("LOGOUT");
        btnLogout.addActionListener(e -> { new AuthWindow().setVisible(true); dispose(); });
        
        btnPanel.add(btnForfeit);
        btnPanel.add(btnLogout);
        headerActions.add(btnPanel, BorderLayout.EAST);
        topPanel.add(headerActions);

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        statsPanel.setBackground(new Color(18, 18, 18));
        JLabel lblLevel = new JLabel("LEVEL: " + user.level);
        lblLevel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblLevel.setForeground(new Color(255, 215, 0));
        statsPanel.add(lblLevel);

        JProgressBar xpBar = new JProgressBar(0, 1000);
        xpBar.setValue(user.xp % 1000);
        xpBar.setString("XP to level up: " + (user.xp % 1000) + " / 1000");
        xpBar.setStringPainted(true);
        xpBar.setForeground(new Color(138, 43, 226));
        xpBar.setBackground(new Color(40, 40, 40));
        
        topPanel.add(statsPanel);
        topPanel.add(xpBar);

        String[] cols = {"Quest", "Objective", "Time Left", "Rarity", "Reward", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        
        for (String id : engine.topologicalOrder) {
            Task t = engine.taskMap.get(id);
            model.addRow(new Object[]{t.id, t.name, t, "[" + t.rarity + "]", t, t.isCompleted ? "✅ CLEARED" : "ACTIVE"});
        }
        
        table = new JTable(model);
        table.setRowHeight(35);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(75, 75, 75));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tbl, v, s, f, r, c);
                comp.setBackground(new Color(30, 30, 30));
                
                if (c == 2 && v instanceof Task) {
                    Task t = (Task) v;
                    if (t.timeMode.equals("NONE")) {
                        setText("NO LIMIT");
                        comp.setForeground(Color.LIGHT_GRAY);
                    } else if (t.isCompleted) {
                        setText("--:--:--");
                        comp.setForeground(Color.GRAY);
                    } else {
                        long diff = t.deadlineMs - System.currentTimeMillis();
                        if (diff <= 0) {
                            setText("⚠ TIME UP!");
                            comp.setForeground(Color.RED);
                        } else {
                            long secs = diff / 1000;
                            long d = secs / 86400;
                            long h = (secs % 86400) / 3600;
                            long m = (secs % 3600) / 60;
                            long sec = secs % 60;
                            
                            if (d > 0) setText(String.format("%dd %02d:%02d:%02d", d, h, m, sec));
                            else setText(String.format("%02d:%02d:%02d", h, m, sec));
                            
                            comp.setForeground(Color.WHITE);
                        }
                    }
                    return comp;
                }

                if (c == 4 && v instanceof Task) {
                    Task t = (Task) v;
                    int xp = t.expReward;
                    if (t.isCompleted) {
                        setText("CLAIMED");
                        comp.setForeground(Color.GRAY);
                    } else if (!t.timeMode.equals("NONE") && System.currentTimeMillis() > t.deadlineMs) {
                        setText("+" + Math.max(1, xp / 2) + " XP (LATE)");
                        comp.setForeground(new Color(255, 100, 100)); 
                    } else {
                        setText("+" + xp + " XP");
                        comp.setForeground(new Color(218, 165, 32)); 
                    }
                    return comp;
                }

                String status = (String) tbl.getModel().getValueAt(r, 5);
                if (status.equals("✅ CLEARED")) {
                    comp.setForeground(Color.GRAY);
                } else {
                    comp.setForeground(Color.WHITE);
                }
                return comp;
            }
        });

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionBar.setBackground(new Color(18, 18, 18));
        JComboBox<String> pendingTasks = new JComboBox<>();
        engine.topologicalOrder.stream().map(engine.taskMap::get)
            .filter(t -> !t.isCompleted && t.dependencies.stream().allMatch(dep -> engine.taskMap.get(dep).isCompleted))
            .forEach(t -> pendingTasks.addItem(t.id + " - " + t.name));
            
        actionBar.add(pendingTasks);

        JButton btnComplete = new JButton("TURN IN QUEST");
        btnComplete.addActionListener(e -> {
            if (pendingTasks.getSelectedItem() != null) {
                String selectedId = pendingTasks.getSelectedItem().toString().split(" - ")[0];
                Task t = engine.taskMap.get(selectedId);
                t.isCompleted = true;
                
                int xpToGive = t.expReward;
                if (!t.timeMode.equals("NONE") && System.currentTimeMillis() > t.deadlineMs) {
                    xpToGive = Math.max(1, xpToGive / 2);
                    JOptionPane.showMessageDialog(this, "Deadline missed! Half XP awarded: +" + xpToGive + " XP", "Penalty", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Quest Cleared! +" + xpToGive + " XP", "Success", JOptionPane.INFORMATION_MESSAGE);
                }

                user.addXP(xpToGive); 
                UserManager.saveUsersToCSV(); 
                engine.saveGraphToCSV(user.username);
                getContentPane().removeAll();
                initUI();
                revalidate(); repaint();
            }
        });
        actionBar.add(btnComplete);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);
    }
}