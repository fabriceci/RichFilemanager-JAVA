package com.fabriceci.fmc.model;


public class SuccessResponse {

    public SuccessResponse(Object data) {
        this.data = data;
    }

    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
