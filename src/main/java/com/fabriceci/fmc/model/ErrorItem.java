package com.fabriceci.fmc.model;

import java.util.List;

public class ErrorItem {

    private String id;
    private int code;
    private String message;
    private List<String> arguments;

    public ErrorItem(String message) {
        this(message, null);
    }

    public ErrorItem(String message, List<String> arguments) {
        this.id = "server";
        this.code = 500;
        this.message = message;
        this.arguments = arguments;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
