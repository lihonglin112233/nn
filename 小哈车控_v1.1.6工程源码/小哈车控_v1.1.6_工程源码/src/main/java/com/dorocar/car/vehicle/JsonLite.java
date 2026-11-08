package com.dorocar.car.vehicle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public final class JsonLite {
    private int pos;
    private final String src;

    private JsonLite(String str) {
        this.src = str;
    }

    public static Object parse(String str) {
        if (str == null) {
            return null;
        }
        String trim = str.trim();
        if (trim.isEmpty()) {
            return null;
        }
        JsonLite jsonLite = new JsonLite(trim);
        Object readValue = jsonLite.readValue();
        jsonLite.skipWhitespace();
        return readValue;
    }

    private void skipWhitespace() {
        while (this.pos < this.src.length()) {
            char charAt = this.src.charAt(this.pos);
            if (charAt == ' ' || charAt == '\t' || charAt == '\n' || charAt == '\r' || charAt == 65279) {
                this.pos++;
            } else {
                return;
            }
        }
    }

    private char peek() {
        if (this.pos >= this.src.length()) {
            throw new IllegalArgumentException("JSON 意外结束");
        }
        return this.src.charAt(this.pos);
    }

    private Object readValue() {
        skipWhitespace();
        switch (peek()) {
            case '\"':
                return readString();
            case '[':
                return readArray();
            case 'f':
                expect("false");
                return Boolean.FALSE;
            case 'n':
                expect("null");
                return null;
            case 't':
                expect("true");
                return Boolean.TRUE;
            case '{':
                return readObject();
            default:
                return readNumber();
        }
    }

    private void expect(String str) {
        if (!this.src.startsWith(str, this.pos)) {
            throw new IllegalArgumentException("JSON 非法字面量，位置 " + this.pos);
        }
        this.pos += str.length();
    }

    private Map<String, Object> readObject() {
        char peek;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        this.pos++;
        skipWhitespace();
        if (peek() == '}') {
            this.pos++;
            return linkedHashMap;
        }
        do {
            skipWhitespace();
            if (peek() != '\"') {
                throw new IllegalArgumentException("JSON key 必须是字符串，位置 " + this.pos);
            }
            String readString = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw new IllegalArgumentException("JSON 缺少冒号，位置 " + this.pos);
            }
            this.pos++;
            linkedHashMap.put(readString, readValue());
            skipWhitespace();
            peek = peek();
            this.pos++;
            if (peek == '}') {
                return linkedHashMap;
            }
        } while (peek == ',');
        throw new IllegalArgumentException("JSON 对象分隔符错误，位置 " + this.pos);
    }

    private List<Object> readArray() {
        char peek;
        ArrayList arrayList = new ArrayList();
        this.pos++;
        skipWhitespace();
        if (peek() == ']') {
            this.pos++;
            return arrayList;
        }
        do {
            arrayList.add(readValue());
            skipWhitespace();
            peek = peek();
            this.pos++;
            if (peek == ']') {
                return arrayList;
            }
        } while (peek == ',');
        throw new IllegalArgumentException("JSON 数组分隔符错误，位置 " + this.pos);
    }

    private String readString() {
        StringBuilder sb = new StringBuilder();
        this.pos++;
        while (this.pos < this.src.length()) {
            String str = this.src;
            int i = this.pos;
            this.pos = i + 1;
            char charAt = str.charAt(i);
            if (charAt == '\"') {
                return sb.toString();
            }
            if (charAt != '\\') {
                sb.append(charAt);
            } else {
                if (this.pos >= this.src.length()) {
                    throw new IllegalArgumentException("JSON 转义未完成");
                }
                String str2 = this.src;
                int i2 = this.pos;
                this.pos = i2 + 1;
                char charAt2 = str2.charAt(i2);
                switch (charAt2) {
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (this.pos + 4 > this.src.length()) {
                            throw new IllegalArgumentException("JSON unicode 转义不完整");
                        }
                        sb.append((char) Integer.parseInt(this.src.substring(this.pos, this.pos + 4), 16));
                        this.pos += 4;
                        break;
                    default:
                        sb.append(charAt2);
                        break;
                }
            }
        }
        throw new IllegalArgumentException("JSON 字符串未闭合");
    }

    private Double readNumber() {
        char charAt;
        int i = this.pos;
        while (this.pos < this.src.length() && (((charAt = this.src.charAt(this.pos)) >= '0' && charAt <= '9') || charAt == '-' || charAt == '+' || charAt == '.' || charAt == 'e' || charAt == 'E')) {
            this.pos++;
        }
        String substring = this.src.substring(i, this.pos);
        if (substring.isEmpty()) {
            throw new IllegalArgumentException("JSON 数值格式错误，位置 " + i);
        }
        try {
            return Double.valueOf(substring);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("JSON 数值格式错误: " + substring);
        }
    }
}
