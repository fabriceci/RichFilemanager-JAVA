package com.fabriceci.fmc.model;

public class FileAttributes {

    private String name;
    private String path;
    private int readable = 1;
    private int writable = 1;
    private String created;
    private String modified;
    private Long timestamp;
    private Double height;
    private Double width;
    private Long size;
    private String content;


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public int getReadable() {
        return readable;
    }

    public void setReadable(int readable) {
        this.readable = readable;
    }

    public int getWritable() {
        return writable;
    }

    public void setWritable(int writable) {
        this.writable = writable;
    }

    public boolean isReadable(){
        return this.readable == 1;
    }

    public boolean isWritable(){
        return this.writable == 1;
    }
}
