package com.myframework.http;

public class JsonResponse {

    private String status;   // "success" ou plus tard "warning"
    private int code;        // 200, 201, 400, 500...
    private Object result;   // objet, liste, map...
    private Integer count;   // seulement utilisé si result est une liste/tableau

    public JsonResponse(String status, int code, Object result, Integer count) {
        this.status = status;
        this.code = code;
        this.result = result;
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public Object getResult() {
        return result;
    }

    public Integer getCount() {
        return count;
    }
}
