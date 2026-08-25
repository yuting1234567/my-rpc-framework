package org.example.common;

public class RpcMessage {

    private byte msgType;   //请求、响应、心跳
    private byte serializerCode;  //用的哪种序列化
    private byte status;   //成功、失败
    private long requestId;   //请求编号
    private Object body;   //装 Request或 Response,心跳时为 null

    public byte getMsgType() {
        return msgType;
    }

    public void setMsgType(byte msgType) {
        this.msgType = msgType;
    }

    public byte getSerializerCode() {
        return serializerCode;
    }

    public void setSerializerCode(byte serializerCode) {
        this.serializerCode = serializerCode;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
