package com.fabriceci.fmc.model;

public class FileData {

    private String id;
    private FileType type;
    private FileAttributes attributes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public FileAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(FileAttributes attributes) {
        this.attributes = attributes;
    }
}
