// File: src/ui/OverwatchUI.java
package ui;

import dsa.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

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
                lblGlobalTimer.setForeground(Color.LIGHT_GRAY);
            } else {
                long diff = closest - System.currentTimeMillis();
                if (diff <= 0) {
                    lblGlobalTimer.setText("⚠ TIME UP!");
                    lblGlobalTimer.setForeground(new Color(255, 69, 0)); 
                } else {
                    long s = diff / 1000;
                    long d = s / 86400;
                    long h = (s % 86400) / 3600;
                    long m = (s % 3600) / 60;
                    long sec = s % 60;
                    
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
        setSize(1050, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(18, 18, 18));

        JPanel metricsPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        metricsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        metricsPanel.setBackground(new Color(18, 18, 18));
        
        int totalTasks = engine.taskMap.size();
        int completedTasks = (int) engine.taskMap.values().stream().filter(t -> t.isCompleted).count();
        int completionPercentage = totalTasks == 0 ? 0 : (completedTasks * 100) / totalTasks;

        lblGlobalTimer = new JLabel("CALCULATING...", SwingConstants.CENTER);
        lblGlobalTimer.setFont(new Font("Segoe UI", Font.BOLD, 15)); 
        
        JPanel timerCard = new JPanel(new GridLayout(2, 1));
        timerCard.setBackground(new Color(30, 30, 30));
        timerCard.setBorder(BorderFactory.createLineBorder(new Color(0, 200, 255), 2));
        JLabel lblTimerTitle = new JLabel("NEXT DEADLINE", SwingConstants.CENTER);
        lblTimerTitle.setForeground(Color.LIGHT_GRAY);
        timerCard.add(lblTimerTitle);
        timerCard.add(lblGlobalTimer);

        metricsPanel.add(timerCard);
        metricsPanel.add(createMetricCard("TOTAL TASKS", String.valueOf(totalTasks), new Color(30, 144, 255)));
        metricsPanel.add(createMetricCard("COMPLETED", completedTasks + " (" + completionPercentage + "%)", new Color(50, 205, 50)));
        metricsPanel.add(createMetricCard("MIN DURATION", engine.totalProjectDuration + " Hours", new Color(186, 85, 211)));
        
        JButton btnLogout = new JButton("LOGOUT");
        btnLogout.setBackground(new Color(50, 50, 50));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.addActionListener(e -> { 
            if(uiTimer != null) uiTimer.stop(); 
            new AuthWindow().setVisible(true); 
            dispose(); 
        });
        metricsPanel.add(btnLogout);

        String[] cols = {"ID", "Task Name", "Time Left", "EST", "EFT", "Slack", "Critical", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        for (String id : engine.topologicalOrder) {
            Task t = engine.taskMap.get(id);
            String status = t.isCompleted ? "✅ DONE" : "PENDING";
            model.addRow(new Object[]{t.id, t.name, t, t.est, t.eft, t.slack, t.isCritical ? "⚠ YES" : "NO", status});
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
                
                if (c == 2 && v instanceof Task) {
                    Task t = (Task) v;
                    comp.setBackground(new Color(30, 30, 30));
                    if (t.timeMode.equals("NONE")) {
                        setText("NO LIMIT");
                        comp.setForeground(Color.DARK_GRAY);
                    } else if (t.isCompleted) {
                        setText("--:--:--");
                        comp.setForeground(Color.GRAY);
                    } else {
                        long diff = t.deadlineMs - System.currentTimeMillis();
                        if (diff <= 0) {
                            setText("⚠ TIME UP");
                            comp.setForeground(new Color(255, 100, 100));
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

                String status = (String) tbl.getModel().getValueAt(r, 7);
                String crit = (String) tbl.getModel().getValueAt(r, 6);
                
                if (status.equals("✅ DONE")) {
                    comp.setForeground(new Color(50, 205, 50)); 
                    comp.setBackground(new Color(25, 25, 25));
                } else if (crit.equals("⚠ YES")) {
                    comp.setForeground(Color.WHITE);
                    comp.setBackground(new Color(100, 20, 20)); 
                } else {
                    comp.setForeground(Color.LIGHT_GRAY);
                    comp.setBackground(new Color(30, 30, 30));
                }
                return comp;
            }
        });

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        actionBar.setBackground(new Color(18, 18, 18));
        actionBar.add(new JLabel("Select Task to Complete:"));
        
        JComboBox<String> pendingTasks = new JComboBox<>();
        engine.topologicalOrder.stream()
            .map(engine.taskMap::get)
            .filter(t -> !t.isCompleted)
            .forEach(t -> pendingTasks.addItem(t.id + " - " + t.name));
        actionBar.add(pendingTasks);

        JButton btnComplete = new JButton("MARK AS COMPLETE");
        btnComplete.setBackground(new Color(0, 128, 128));
        btnComplete.setForeground(Color.WHITE);
        btnComplete.addActionListener(e -> {
            if (pendingTasks.getSelectedItem() != null) {
                String selectedId = pendingTasks.getSelectedItem().toString().split(" - ")[0];
                Task t = engine.taskMap.get(selectedId);
                t.isCompleted = true;
                
                int xpToGive = t.expReward;
                if (!t.timeMode.equals("NONE") && System.currentTimeMillis() > t.deadlineMs) {
                    xpToGive = Math.max(1, xpToGive / 2);
                    JOptionPane.showMessageDialog(this, "Task finished late. Half XP awarded: +" + xpToGive + " XP", "Late Completion", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Task finished on time! Full XP awarded: +" + xpToGive + " XP", "Task Cleared", JOptionPane.INFORMATION_MESSAGE);
                }

                user.addXP(xpToGive); 
                UserManager.saveUsersToCSV(); 
                engine.saveGraphToCSV(user.username); 
                
                if(uiTimer != null) uiTimer.stop();
                getContentPane().removeAll();
                initUI();
                startLiveTimer();
                revalidate();
                repaint();
            }
        });
        actionBar.add(btnComplete);

        JButton btnForfeit = new JButton("FORFEIT PROJECT");
        btnForfeit.setBackground(new Color(139, 0, 0)); 
        btnForfeit.setForeground(Color.WHITE);
        btnForfeit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to forfeit? All tasks will be deleted, but you will keep your XP.", 
                "Forfeit Project", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
            if (confirm == JOptionPane.YES_OPTION) {
                engine.deleteGraphCSV(user.username); 
                if(uiTimer != null) uiTimer.stop();
                new TaskInputUI(user, "OVERWATCH").setVisible(true); 
                dispose();
            }
        });
        actionBar.add(btnForfeit);

        add(metricsPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);
    }

    private JPanel createMetricCard(String title, String value, Color color) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(new Color(30, 30, 30));
        card.setBorder(BorderFactory.createLineBorder(color, 2));
        
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setForeground(Color.LIGHT_GRAY);
        
        JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblValue.setForeground(color);
        
        card.add(lblTitle);
        card.add(lblValue);
        return card;
    }
}