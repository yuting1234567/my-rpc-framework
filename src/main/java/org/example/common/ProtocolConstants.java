package org.example.common;

public class ProtocolConstants {
    public static final int MAGIC = 0X72706301;
    public static final byte VERSION = 1;
    public static final int HEADER_LENGTH = 20;

    public static final byte TYPE_REQUEST = 1;
    public static final byte TYPE_RESPONSE  = 2;
    public static final byte TYPE_HEARTBEAT = 3;
}
