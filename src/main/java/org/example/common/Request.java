package org.example.common;

import java.io.Serializable;

public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private String serviceName;
    private String methodName;
    private Class<?>[] paramTypes;
    private Object[] args;

    public Request(){
    }

    public Request(String serviceName, String methodName, Class<?>[] paramTypes, Object[] args) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.args = args;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public Object[] getArgs() {
        return args;
    }
}
