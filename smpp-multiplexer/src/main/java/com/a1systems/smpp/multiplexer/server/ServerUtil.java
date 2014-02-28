package com.a1systems.smpp.multiplexer.server;

public class ServerUtil {

    public static long getParts(byte[] udh) {
        if (udh[1] == (byte)0x00
                && udh[2] == (byte)0x03) {
            return (long) udh[5];
        } else if (udh[1] == (byte)0x08
                && udh[2] == (byte)0x04) {
            return (long)udh[6];
        } else {
            return 0;
        }
    }

    public static long getCurrentPart(byte[] udh) {
        if (udh[1] == (byte)0x00
                && udh[2] == (byte)0x03) {
            return (long) udh[4];
        } else if (udh[1] == (byte)0x08
                && udh[2] == (byte)0x04) {
            return (long)udh[5];
        } else {
            return 0;
        }
    }

    public static long getSmsId(byte[] udh) {
        if (udh[1] == 0x00
                && udh[2] == 0x03) {
            return (long) udh[3];
        } else if (udh[1] == 0x08
                && udh[2] == 0x04) {
            return (long)(udh[3] * 256 + udh[4]);
        } else {
            return 0;
        }
    }
}
