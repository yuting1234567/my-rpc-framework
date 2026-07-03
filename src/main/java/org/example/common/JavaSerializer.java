package org.example.common;

import java.io.*;

public class JavaSerializer implements Serializer{
    @Override
    public byte[] serialize(Object obj) {
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos)){
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try(ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis)){
            Object obj = ois.readObject();
            return clazz.cast(obj);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
