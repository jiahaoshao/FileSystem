package net.fangyi;

import lombok.Data;

import java.io.*;
import java.util.*;

@Data
public class FileSystem {
    private FileSystemNode root;
    private FileSystemNode current;
    private FileSystemNode home;
    private Map<String, User> users;
    private User currentUser = null;

    FileSystem() {
        root = new FileSystemNode("/", true, "root", "root", "rwx", "r-x", "r-x", null);
        current = root;
        home = new FileSystemNode("root", true, "root", "root", "rwx", "r-x", "r-x", root);
        root.getChildren().put("root", home);
        users = new HashMap<>();
        users.put("root", new User("root", "password", "root", "/root"));
    }

    FileSystemNode getNodeByPath(String path) {
        if (path.equals("/")) return root;

        if (path.isEmpty() || path.charAt(0) != '/') {
            System.err.println("Error: Only absolute paths are supported.");
            return null;
        }

        String[] parts = path.split("/");
        FileSystemNode node = root;
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (!node.getChildren().containsKey(part)) return null;
                node = node.getChildren().get(part);
            }
        }
        return node;
    }

    static String encodeContent(String content) {
        return content.replace("\\", "\\\\").replace("\n", "\\n");
    }

    static String decodeContent(String encodedContent) {
        return encodedContent.replace("\\n", "\n").replace("\\\\", "\\");
    }

    void saveToFile(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            saveNode(out, root, "");
        } catch (IOException e) {
            System.err.println("Error opening file for writing: " + filename);
        }
    }

    void saveNode(PrintWriter out, FileSystemNode node, String prefix) {
        if (node == null) return;

        out.println(prefix + node.getName() + " " + (node.isDirectory() ? "d" : "f") + " " + node.getOwner() + " " + node.getGroup() + " "
                + node.getUserPermissions() + node.getGroupPermissions() + node.getOtherPermissions() + " " + node.isBeingEdited() + " "
                + encodeContent(node.getContent()));

        for (FileSystemNode child : node.getChildren().values()) {
            saveNode(out, child, prefix + node.getName() + "/");
        }
    }

    void saveUsersToFile(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            for (User user : users.values()) {
                out.println(user.getUsername() + " " + user.getPassword() + " " + user.getGroup() + " " + user.getHomeDirectory());
            }
        } catch (IOException e) {
            System.err.println("Error opening file for writing: " + filename);
        }
    }

    void loadFromFile(String filename) {

        File file = new File(filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating file: " + filename);
                return;
            }
        }

        try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(" ");
                String path = parts[0];
                String type = parts[1];
                String owner = parts[2];
                String group = parts[3];
                String permissions = parts[4];
                boolean isBeingEdited = Boolean.parseBoolean(parts[5]);
                String content = parts[6];

                FileSystemNode node = getNodeByPath(path);
                if (node != null) {
                    node.setOwner(owner);
                    node.setGroup(group);
                    node.setPermissions(permissions);
                    node.setBeingEdited(isBeingEdited);
                    if (!type.equals("d")) {
                        node.setContent(decodeContent(content));
                    }
                } else {
                    create(path, type.equals("d"), owner, group);
                    node = getNodeByPath(path);
                    if (node != null) {
                        node.setPermissions(permissions);
                        node.setBeingEdited(isBeingEdited);
                        if (!type.equals("d")) {
                            node.setContent(decodeContent(content));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error opening file for reading: " + filename);
        }
    }

    void loadUsersFromFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating file: " + filename);
                return;
            }
        }
        try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(" ");
                String username = parts[0];
                String password = parts[1];
                String group = parts[2];
                String homeDirectory = parts[3];
                users.put(username, new User(username, password, group, homeDirectory));
            }
        } catch (IOException e) {
            System.err.println("Error opening file for reading: " + filename);
        }
    }

    void setPermissions(String path, String newPermissions) {
        FileSystemNode node = getNodeByPath(path);
        if (node == null) {
            System.err.println("Error: File or directory does not exist");
            return;
        }
        node.setPermissions(newPermissions);
    }

    String getPermissions(String path) {
        FileSystemNode node = getNodeByPath(path);
        if (node == null) {
            System.err.println("Error: File or directory does not exist");
            return "";
        }
        return node.getPermissions();
    }

    void addUser(String username, String password, String group) {
        String homeDir = "/" + username;
        if (users.containsKey(username)) {
            System.err.println("Error: User already exists");
            return;
        }
        users.put(username, new User(username, password, group, homeDir));
        if (getNodeByPath(homeDir) == null) {
            create(homeDir, true, username, group);
        }
    }

    void changeUserPassword(String username, String newPassword) {
        User user = users.get(username);
        if (user == null) {
            System.err.println("Error: User does not exist");
            return;
        }
        user.setPassword(newPassword);
        System.out.println("Password changed successfully for user: " + username);
    }

    boolean login(String username, String password) {
        User user = users.get(username);
        if (user == null || !user.getPassword().equals(password)) {
            System.err.println("Error: Invalid username or password");
            return false;
        }
        currentUser = user;
        changeDirectory(currentUser.getHomeDirectory());
        return true;
    }

    boolean checkPermissions(FileSystemNode node, char permissionType) {
        String permissions = node.getPermissions();
        boolean hasPermission = false;

        if (node.getOwner().equals(currentUser.getUsername())) {
            hasPermission = permissions.charAt(permissionType == 'r' ? 0 : (permissionType == 'w' ? 1 : 2)) != '-';
        } else if (node.getGroup().equals(currentUser.getGroup())) {
            hasPermission = permissions.charAt(permissionType == 'r' ? 3 : (permissionType == 'w' ? 4 : 5)) != '-';
        } else {
            hasPermission = permissions.charAt(permissionType == 'r' ? 6 : (permissionType == 'w' ? 7 : 8)) != '-';
        }
        return hasPermission;
    }

    void create(String path, boolean isDirectory, String owner, String group) {
        FileSystemNode node = getNodeByPath(path);
        if (node != null) {
            System.err.println("Error: File or directory already exists");
            return;
        }

        int lastSlashPos = path.lastIndexOf('/');
        String parentPath;
        String name;
        if (lastSlashPos != -1) {
            parentPath = path.substring(0, lastSlashPos);
            name = path.substring(lastSlashPos + 1);
            if (parentPath.isEmpty()) {
                parentPath = "/";
            }
        } else {
            parentPath = "/";
            name = path;
        }

        FileSystemNode parent = getNodeByPath(parentPath);
        if (parent == null || !parent.isDirectory()) {
            System.err.println("Error: Invalid path");
            return;
        }

        FileSystemNode newNode = new FileSystemNode(name, isDirectory, owner, group, "rwx", "r-x", "r-x", parent);
        parent.getChildren().put(name, newNode);
    }

    void remove(String path) {
        if (path.isEmpty() || path.charAt(0) != '/') {
            System.err.println("Error: Only absolute paths are supported");
            return;
        }

        int lastSlashPos = path.lastIndexOf('/');
        String parentPath;
        if (lastSlashPos == 0) {
            parentPath = "/";
        } else {
            parentPath = path.substring(0, lastSlashPos);
        }
        String name = path.substring(lastSlashPos + 1);
        FileSystemNode parent = getNodeByPath(parentPath);

        if (parent == null || !parent.isDirectory() || !parent.getChildren().containsKey(name)) {
            System.err.println("Error: File or directory does not exist");
            return;
        }

        parent.getChildren().remove(name);
    }

    void changeDirectory(String path) {
        if (path.isEmpty() || path.charAt(0) != '/') {
            System.err.println("Error: Only absolute paths are supported");
            return;
        }

        FileSystemNode node = getNodeByPath(path);
        if (node == null || !node.isDirectory()) {
            System.err.println("Error: Directory does not exist");
            return;
        }

        if (!checkPermissions(node, 'r')) {
            System.err.println("Error: Permission denied");
            return;
        }

        current = node;
    }

    void listDirectory(boolean longFormat) {
        if (longFormat) {
            for (FileSystemNode child : current.getChildren().values()) {
                System.out.println(String.format("%-10s %s", child.formatPermissions(), child.getName()));
            }
        } else {
            for (FileSystemNode child : current.getChildren().values()) {
                System.out.println((child.isDirectory() ? "d " : "- ") + child.getName());
            }
        }
    }

    void readFile(String path) {
        FileSystemNode node = getNodeByPath(path);
        if (node == null || node.isDirectory()) {
            System.err.println("Error: Invalid file path");
            return;
        }

        if (!checkPermissions(node, 'r')) {
            System.err.println("Error: Permission denied");
            return;
        }

        loadFromFile("filesystem_state.txt");
        if (node.isBeingEdited()) {
            System.err.println("Error: File is currently being edited");
            return;
        }

        System.out.println(node.getContent());
    }

    String getCurrentDirectoryPath() {
        List<String> parts = new ArrayList<>();
        FileSystemNode node = current;
        while (node != null && node != root) {
            parts.add(node.getName());
            node = node.getParent();
        }
        Collections.reverse(parts);
        return "/" + String.join("/", parts) + "/";
    }
}

