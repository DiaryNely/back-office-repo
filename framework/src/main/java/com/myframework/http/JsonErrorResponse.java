package com.myframework.http;

public class JsonErrorResponse {

    private String status = "error";
    private int code = 500;
    private String message;
    private String details;

    public JsonErrorResponse(String message, String details) {
        this.message = message;
        this.details = details;
    }

    public String getStatus() { return status; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
}
