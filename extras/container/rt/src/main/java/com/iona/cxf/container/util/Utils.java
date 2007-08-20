/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container.util;

import java.io.IOException;
import java.io.InputStream;

public final class Utils {
    
    static final int BLOCK_SIZE = 4096;

    private Utils() {

    }

    public static byte [] streamToByteArray(InputStream istream) throws IOException {
        // start off with the max of either BLOCK_SIZE or the available length
        // thus if the entire stream is available, then we load it
        // in one pass
        int max = istream.available();
        if (max < BLOCK_SIZE) {
            max = BLOCK_SIZE;
        }
        byte byteArray[] = new byte[max];

        int off = 0;
        int len = istream.read(byteArray, off, max);
        while (len != -1) {
            off += len;

            // if there is less than 1K left, double the size of
            // the buffer.  (This makes loading n + log(n))
            // (just adding 1K to the buffer each time is n + n)
            if ((max - off) < 1024) {
                max *= 2;

                byte tempArray[] = new byte[max];
                System.arraycopy(byteArray, 0, tempArray, 0, off);
                byteArray = tempArray;
            }

            len = istream.read(byteArray, off, max - off);
        }
        // trim down to the actual size.
        byte tempArray[] = new byte[off];
        System.arraycopy(byteArray, 0, tempArray, 0, off);
        byteArray = tempArray;
        return byteArray;
    }
    
    public static byte[] streamToByteArray(InputStream stream, int len)
        throws IOException {
        if (len == -1) {
            return streamToByteArray(stream);
        }

        byte byteArray[] = new byte[len];        
        int off = 0;
        int cnt = stream.read(byteArray, off, len - off);
        while (cnt != -1 && off < len) {
            off += cnt;
            cnt = stream.read(byteArray, off, len - off);
        }
        if (off != len) {
            byte tempArray[] = new byte[off];
            System.arraycopy(byteArray, 0, tempArray, 0, off);
            byteArray = tempArray;
        }
        return byteArray;
    }

}
