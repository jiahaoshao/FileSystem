package net.fangyi;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class FileSystemNode {
    private String name;
    private boolean isDirectory;
    private String owner;
    private String group;
    private String content;
    private Map<String, FileSystemNode> children;
    private String userPermissions;
    private String groupPermissions;
    private String otherPermissions;
    private FileSystemNode parent;
    private boolean isBeingEdited;

    public FileSystemNode(String name, boolean isDirectory, String owner, String group,
                          String userPermissions, String groupPermissions, String otherPermissions,
                          FileSystemNode parent) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.owner = owner;
        this.group = group;
        this.userPermissions = userPermissions;
        this.groupPermissions = groupPermissions;
        this.otherPermissions = otherPermissions;
        this.parent = parent;
        this.isBeingEdited = false;
        this.children = new HashMap<>();
    }

    public void setPermissions(String newPermissions) {
        if (newPermissions.length() == 9) {
            userPermissions = newPermissions.substring(0, 3);
            groupPermissions = newPermissions.substring(3, 6);
            otherPermissions = newPermissions.substring(6, 9);
        }
    }

    public String getPermissions() {
        return userPermissions + groupPermissions + otherPermissions;
    }

    public String formatPermissions() {
        return (isDirectory ? 'd' : '-') + userPermissions + groupPermissions + otherPermissions + " " + owner + " " + group;
    }
}