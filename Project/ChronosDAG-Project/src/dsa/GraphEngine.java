// File: src/dsa/GraphEngine.java
package dsa;
import java.util.*;

public class GraphEngine {
    public Map<String, Task> taskMap = new LinkedHashMap<>();
    public Map<String, List<String>> adjList = new HashMap<>();
    public Map<String, Integer> inDegree = new HashMap<>();
    
    public List<String> topologicalOrder = new ArrayList<>();
    public int totalProjectDuration = 0;

    public void addTask(Task task) {
        taskMap.put(task.id, task);
        adjList.putIfAbsent(task.id, new ArrayList<>());
        inDegree.putIfAbsent(task.id, 0);
    }

    public void buildGraph() {
        for (Task task : taskMap.values()) {
            for (String depId : task.dependencies) {
                if (!taskMap.containsKey(depId)) {
                    throw new IllegalArgumentException("Invalid dependency: '" + depId + "'. No such Task ID exists!");
                }
                adjList.get(depId).add(task.id);
                inDegree.put(task.id, inDegree.get(task.id) + 1);
            }
        }
    }

    public boolean processProject() {
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> currentInDegree = new HashMap<>(inDegree);

        for (Map.Entry<String, Integer> entry : currentInDegree.entrySet()) {
            if (entry.getValue() == 0) queue.offer(entry.getKey());
        }

        topologicalOrder.clear();
        while (!queue.isEmpty()) {
            String u = queue.poll();
            topologicalOrder.add(u);
            for (String v : adjList.get(u)) {
                currentInDegree.put(v, currentInDegree.get(v) - 1);
                if (currentInDegree.get(v) == 0) queue.offer(v);
            }
        }

        if (topologicalOrder.size() != taskMap.size()) return false; 

        totalProjectDuration = 0;
        for (String uId : topologicalOrder) {
            Task u = taskMap.get(uId);
            u.eft = u.est + u.duration; // Duration is now strictly in HOURS
            totalProjectDuration = Math.max(totalProjectDuration, u.eft);
            for (String vId : adjList.get(uId)) {
                Task v = taskMap.get(vId);
                v.est = Math.max(v.est, u.eft);
            }
        }

        for (Task task : taskMap.values()) task.lft = totalProjectDuration;
        
        for (int i = topologicalOrder.size() - 1; i >= 0; i--) {
            String uId = topologicalOrder.get(i);
            Task u = taskMap.get(uId);
            for (String vId : adjList.get(uId)) {
                Task v = taskMap.get(vId);
                u.lft = Math.min(u.lft, v.lst);
            }
            
            u.lst = u.lft - u.duration;
            u.slack = u.lft - u.eft; 
            u.isCritical = (u.slack == 0); 

            // --- NEW: TIME-DRIVEN RARITY ALGORITHM ---
            int prereqs = u.dependencies.size();
            int h = u.duration; // h = Total Hours
            
            if (h <= 5 && prereqs == 0) {
                u.rarity = "COMMON";
            } else if (h <= 12 && prereqs <= 1) {
                u.rarity = "UNCOMMON";
            } else if (h <= 48 && prereqs == 2) {
                u.rarity = "RARE";
            } else if (h <= 72) { // 3 Days
                u.rarity = "EPIC";
            } else if (h <= 168 && prereqs >= 10) { // 1 Week + Heavy prereqs
                u.rarity = "LEGENDARY";
            } else {
                // Safe Fallbacks based on critical path limits
                if (h > 168) u.rarity = "LEGENDARY";
                else if (u.isCritical) u.rarity = "EPIC";
                else u.rarity = "UNCOMMON";
            }

            int multi = switch(u.rarity) {
                case "LEGENDARY" -> 5; case "EPIC" -> 4; case "RARE" -> 3;
                case "UNCOMMON" -> 2; default -> 1; 
            };
            u.expReward = (u.duration * 10) * multi;
        }
        return true; 
    }

    public void saveGraphToCSV(String username) {
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(username + "_tasks.csv"))) {
            for (Task t : taskMap.values()) {
                String deps = String.join(";", t.dependencies);
                String limit = t.timeLimit == null ? "" : t.timeLimit.toString();
                out.println(t.id + "," + t.name + "," + t.duration + "," + limit + "," + deps + "," + t.isCompleted + "," + t.timeMode + "," + t.deadlineMs);
            }
        } catch (Exception e) { System.err.println("Error saving tasks: " + e.getMessage()); }
    }

    public boolean loadGraphFromCSV(String username) {
        java.io.File file = new java.io.File(username + "_tasks.csv");
        if (!file.exists()) return false;
        
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                Task t = new Task(parts[0], parts[1], Integer.parseInt(parts[2]), 
                                  parts[3].isEmpty() ? null : Integer.parseInt(parts[3]), 
                                  parts[4].isEmpty() ? new ArrayList<>() : Arrays.asList(parts[4].split(";")));
                
                t.isCompleted = Boolean.parseBoolean(parts[5]);
                if (parts.length > 6) t.timeMode = parts[6];
                if (parts.length > 7) t.deadlineMs = Long.parseLong(parts[7]);
                
                addTask(t);
            }
            buildGraph();
            return processProject();
        } catch (Exception e) { return false; }
    }

    public void deleteGraphCSV(String username) {
        java.io.File file = new java.io.File(username + "_tasks.csv");
        if (file.exists()) file.delete();
    }
}