// File: src/ui/ModeWindow.java
package ui;
import dsa.User;
import dsa.GraphEngine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModeWindow extends JFrame {
    public ModeWindow(User user) {
        setTitle("Select Mode - Welcome " + user.username);
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        add(mainPanel);

        GraphEngine testEngine = new GraphEngine();
        boolean hasSavedProject = testEngine.loadGraphFromCSV(user.username);

        // Big UI Card 1
        JButton btnOverwatch = createModeCard(
            "OVERWATCH", "Enterprise Analytics", 
            new Color(30, 30, 30), new Color(45, 45, 45), new Color(0, 200, 255)
        );
        btnOverwatch.addActionListener(e -> launchMode(user, "OVERWATCH", hasSavedProject, testEngine));

        // Big UI Card 2
        JButton btnOverdrive = createModeCard(
            "OVERDRIVE", "Gamified Campaign", 
            new Color(40, 15, 60), new Color(70, 25, 100), new Color(255, 215, 0)
        );
        btnOverdrive.addActionListener(e -> launchMode(user, "OVERDRIVE", hasSavedProject, testEngine));

        mainPanel.add(btnOverwatch);
        mainPanel.add(btnOverdrive);
    }

    private JButton createModeCard(String title, String sub, Color defaultBg, Color hoverBg, Color accent) {
        // Using HTML to format the large card text cleanly
        String html = String.format("<html><div style='text-align: center;'>" +
                "<h1 style='color: rgb(%d,%d,%d); font-size: 24px; margin-bottom: 5px;'>%s</h1>" +
                "<p style='color: white; font-size: 14px;'>%s</p>" +
                "</div></html>", accent.getRed(), accent.getGreen(), accent.getBlue(), title, sub);
                
        JButton btn = new JButton(html);
        btn.setBackground(defaultBg);
        btn.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                btn.setBackground(hoverBg); 
                btn.setBorder(BorderFactory.createLineBorder(accent, 2));
            }
            public void mouseExited(MouseEvent e) { 
                btn.setBackground(defaultBg); 
                btn.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 2));
            }
        });
        return btn;
    }

    private void launchMode(User user, String mode, boolean hasSavedProject, GraphEngine engine) {
        if (hasSavedProject) {
            int choice = JOptionPane.showOptionDialog(this, 
                "You have a saved project. Do you want to continue it?", 
                "Project Found", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, 
                null, new String[]{"Continue Saved Project", "Start New Project"}, "Continue Saved Project");
            
            if (choice == JOptionPane.YES_OPTION) {
                if (mode.equals("OVERWATCH")) new OverwatchUI(user, engine).setVisible(true);
                else new OverdriveUI(user, engine).setVisible(true);
                dispose();
                return;
            }
        }
        new TaskInputUI(user, mode).setVisible(true);
        dispose();
    }
}