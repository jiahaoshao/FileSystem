package net.fangyi;

import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

@NoArgsConstructor
public class SimpleShell {
    private FileSystem fs = new FileSystem();

    public List<String> split(String str, char delimiter) {
        List<String> tokens = new ArrayList<>();
        StringTokenizer tokenStream = new StringTokenizer(str, String.valueOf(delimiter));
        while (tokenStream.hasMoreTokens()) {
            tokens.add(tokenStream.nextToken());
        }
        return tokens;
    }

    public void login(List<String> args) {
        if (args.size() != 2) {
            System.err.println("Usage: login <username> <password>");
            return;
        }
        if (!fs.login(args.get(0), args.get(1))) {
            System.err.println("Login failed for user: " + args.get(0));
        } else {
            System.out.println("Logged in as " + args.get(0));
        }
    }

    public void changePassword(List<String> args) {
        if (args.size() != 2) {
            System.err.println("Usage: passwd <username> <newpassword>");
            return;
        }
        fs.changeUserPassword(args.get(0), args.get(1));
    }

    public void editFile(String filename) {
        FileSystemNode fileNode = fs.getNodeByPath(filename);
        if (fileNode == null) {
            System.out.println("File does not exist. Creating new file: " + filename);
            fs.create(filename, false, fs.getCurrentUser().getUsername(), fs.getCurrentUser().getGroup());
            fileNode = fs.getNodeByPath(filename);
        }
        if (fileNode.isDirectory()) {
            System.err.println("Error: Path is a directory");
            return;
        }
        if (!fs.checkPermissions(fileNode, 'w')) {
            System.err.println("Error: Permission denied.");
            return;
        }
        fs.loadFromFile("filesystem_state.txt");
        if (fileNode.isBeingEdited()) {
            System.err.println("Error: File is currently being edited");
            return;
        }
        fileNode.setBeingEdited(true);
        fs.saveToFile("filesystem_state.txt");

        System.out.println("Entering insert mode for file: " + filename);
        System.out.println(fileNode.getContent());

        StringBuilder newContent = new StringBuilder(fileNode.getContent());
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String inputLine = scanner.nextLine();
            if (inputLine.equals(":wq")) {
                fileNode.setContent(newContent.toString());
                System.out.println("File saved and exiting editor.");
                break;
            } else {
                newContent.append(inputLine).append("\n");
            }
        }
        fileNode.setBeingEdited(false);
    }

    public String convertNumericToSymbolic(String numeric) {
        if (numeric.length() != 3) return "";

        StringBuilder symbolic = new StringBuilder();
        String[] rwx = {"---", "--x", "-w-", "-wx", "r--", "r-x", "rw-", "rwx"};

        for (char c : numeric.toCharArray()) {
            if (c < '0' || c > '7') return "";
            symbolic.append(rwx[c - '0']);
        }

        return symbolic.toString();
    }

    public void executeCommand(String command, List<String> args) {
        if (command.equals("login")) {
            login(args);
        } else {
            if (fs.getCurrentUser() == null) {
                System.err.println("Error: No user logged in");
                return;
            } else if (command.equals("chmod")) {
                if (args.size() != 2) {
                    System.err.println("Usage: chmod <permissions> <filename/directory>");
                    return;
                }
                String symbolicPermissions = convertNumericToSymbolic(args.get(0));
                if (symbolicPermissions.isEmpty()) {
                    System.err.println("Invalid numeric permissions: " + args.get(0));
                    return;
                }
                fs.setPermissions(args.get(1), symbolicPermissions);
            } else if (command.equals("getperm")) {
                if (args.size() != 1) {
                    System.err.println("Usage: getperm <filename/directory>");
                    return;
                }
                System.out.println(fs.getPermissions(args.get(0)));
            } else if (command.equals("pwd")) {
                System.out.println(fs.getCurrentDirectoryPath());
            } else if (command.equals("cd")) {
                if (args.size() == 1) {
                    fs.changeDirectory(args.get(0));
                } else {
                    System.err.println("Usage: cd [directory]");
                    return;
                }
            } else if (command.equals("ls")) {
                boolean longFormat = !args.isEmpty() && args.get(0).equals("-l");
                fs.listDirectory(longFormat);
            } else if (command.equals("touch")) {
                if (args.size() != 1) {
                    System.err.println("Usage: touch <filename>");
                    return;
                }
                fs.create(args.get(0), false, fs.getCurrentUser().getUsername(), fs.getCurrentUser().getGroup());
            } else if (command.equals("rm")) {
                if (args.size() != 1) {
                    System.err.println("Usage: rm <filename/directory>");
                    return;
                }
                fs.remove(args.get(0));
            } else if (command.equals("mkdir")) {
                if (args.size() != 1) {
                    System.err.println("Usage: mkdir <directory>");
                    return;
                }
                fs.create(args.get(0), true, fs.getCurrentUser().getUsername(), fs.getCurrentUser().getGroup());
            } else if (command.equals("rmdir")) {
                if (args.size() != 1) {
                    System.err.println("Usage: rmdir <directory>");
                    return;
                }
                fs.remove(args.get(0));
            } else if (command.equals("cat")) {
                if (args.size() != 1) {
                    System.err.println("Usage: cat <filename>");
                    return;
                }
                fs.readFile(args.get(0));
            } else if (command.equals("vim")) {
                if (args.size() != 1) {
                    System.err.println("Usage: vim <filename>");
                    return;
                }
                editFile(args.get(0));
            } else if (command.equals("adduser")) {
                if (args.size() != 2) {
                    System.err.println("Usage: adduser <username> <password>");
                    return;
                }
                fs.addUser(args.get(0), args.get(1), "users");
            } else if (command.equals("su")) {
                if (args.size() != 2) {
                    System.err.println("Usage: su <username> <password>");
                    return;
                }
                if (!fs.login(args.get(0), args.get(1))) {
                    System.err.println("Switch user failed for user: " + args.get(0));
                } else {
                    System.out.println("Switched to user " + args.get(0));
                }
            } else if (command.equals("passwd")) {
                if (args.size() != 2) {
                    System.err.println("Usage: passwd <username> <newpassword>");
                    return;
                }
                fs.changeUserPassword(args.get(0), args.get(1));
            } else {
                System.err.println("Unknown command: " + command);
            }
        }
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            fs.loadFromFile("filesystem_state.txt");
            fs.loadUsersFromFile("users.txt");
            System.out.print(fs.getCurrentDirectoryPath() + "$ ");
            String input = scanner.nextLine();
            if(input.equals("exit")) {
                fs.saveToFile("filesystem_state.txt");
                fs.saveUsersToFile("users.txt");
                System.out.println("Exiting shell.");
                break;
            }
            List<String> tokens = split(input, ' ');
            if (tokens.isEmpty()) continue;
            String command = tokens.getFirst();
            List<String> args = tokens.subList(1, tokens.size());
            executeCommand(command, args);
            fs.saveToFile("filesystem_state.txt");
            fs.saveUsersToFile("users.txt");
        }
    }
}