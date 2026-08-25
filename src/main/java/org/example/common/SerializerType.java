package org.example.common;

public enum SerializerType {
    JSON((byte) 0, new JsonSerializer()),
    JAVA((byte) 1, new JavaSerializer());

    private final byte code;
    private final Serializer serializer;

    SerializerType(byte code, Serializer serializer) {
        this.code = code;
        this.serializer = serializer;
    }

    public byte getCode(){
        return code;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public static SerializerType fromCode(byte code){
        for(SerializerType type: values()) {
            if(type.code == code){
                return type;
            }
        }
        throw new IllegalArgumentException("未知的序列化类型：" + code);
    }

}
