package com.example.gateway.model;

public class ApiResponse<T> {
    private String message;
    private String status;
    private boolean success;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }

    public ApiResponse(String message, String status, boolean success, T data) {
        this.message = message;
        this.status = status;
        this.success = success;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}