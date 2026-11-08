package com.dorocar.car.vehicle;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/* loaded from: classes.dex */
final class Base64Codec {
    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final int[] LOOKUP = new int[256];

    static {
        Arrays.fill(LOOKUP, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            LOOKUP[ALPHABET[i]] = i;
        }
        LOOKUP['='] = -2;
    }

    private Base64Codec() {
    }

    static String encode(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(((bArr.length + 2) / 3) * 4);
        int i = 0;
        while (true) {
            int i2 = i + 2;
            if (i2 >= bArr.length) {
                break;
            }
            int i3 = (bArr[i2] & 255) | ((bArr[i] & 255) << 16) | ((bArr[i + 1] & 255) << 8);
            sb.append(ALPHABET[(i3 >> 18) & 63]);
            sb.append(ALPHABET[(i3 >> 12) & 63]);
            sb.append(ALPHABET[(i3 >> 6) & 63]);
            sb.append(ALPHABET[i3 & 63]);
            i += 3;
        }
        int length = bArr.length - i;
        if (length == 1) {
            int i4 = (bArr[i] & 255) << 16;
            sb.append(ALPHABET[(i4 >> 18) & 63]);
            sb.append(ALPHABET[(i4 >> 12) & 63]);
            sb.append("==");
        } else if (length == 2) {
            int i5 = ((bArr[i + 1] & 255) << 8) | ((bArr[i] & 255) << 16);
            sb.append(ALPHABET[(i5 >> 18) & 63]);
            sb.append(ALPHABET[(i5 >> 12) & 63]);
            sb.append(ALPHABET[(i5 >> 6) & 63]);
            sb.append('=');
        }
        return sb.toString();
    }

    static byte[] decode(String str) {
        char charAt;
        if (str == null) {
            return new byte[0];
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(((str.length() * 3) / 4) + 3);
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < str.length() && (charAt = str.charAt(i3)) != '='; i3++) {
            if (charAt != '\n' && charAt != '\r' && charAt != ' ' && charAt != '\t') {
                int i4 = charAt < 256 ? LOOKUP[charAt] : -1;
                if (i4 < 0) {
                    throw new IllegalArgumentException("非法 Base64 字符: " + charAt);
                }
                i = (i << 6) | i4;
                i2 += 6;
                if (i2 >= 8) {
                    i2 -= 8;
                    byteArrayOutputStream.write((i >> i2) & 255);
                }
            }
        }
        return byteArrayOutputStream.toByteArray();
    }
}
