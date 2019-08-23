package com.blacktec.think.scorer.Utils;

/**
 * Created by Think on 2019/8/21.
 */

public class ByteArrayUtils {
    public static int byteArrayToInt(byte[] b)
    {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }
    public static int byteArrayToInt(byte[] b,int off)
    {
        return b[off+3] & 0xFF |
                (b[off+2] & 0xFF) << 8 |
                (b[off+1] & 0xFF) << 16 |
                (b[off] & 0xFF) << 24;
    }
    public static byte[] intToByteArray(int a) {
        return new byte[]
            {(byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF),
                    (byte) ((a >> 8) & 0xFF), (byte) (a & 0xFF)
            };
    }
}
