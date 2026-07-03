package org.example.common;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private Object data;
    private String error;

    public static Response success(Object data){
        Response r = new Response();
        r.success = true;
        r.data = data;
        return r;
    }

    public static Response error(String error){
        Response r = new Response();
        r.success = false;
        r.error = error;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getError() {
        return error;
    }
}
