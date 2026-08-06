package config;

import java.util.*;

public class JsonParser {
    private final String src;
    private int ptr = 0;

    private JsonParser(String src) {
        this.src = src;
    }

    public static Object parse(String json) {
        if (json == null) return null;
        JsonParser parser = new JsonParser(json.trim());
        return parser.parseValue();
    }

    private Object parseValue() {
        skipWhitespace();
        if (ptr >= src.length()) return null;
        char c = src.charAt(ptr);
        if (c == '{') {
            return parseObject();
        } else if (c == '[') {
            return parseArray();
        } else if (c == '"') {
            return parseString();
        } else if (Character.isDigit(c) || c == '-') {
            return parseNumber();
        } else if (src.startsWith("true", ptr)) {
            ptr += 4;
            return Boolean.TRUE;
        } else if (src.startsWith("false", ptr)) {
            ptr += 5;
            return Boolean.FALSE;
        } else if (src.startsWith("null", ptr)) {
            ptr += 4;
            return null;
        }
        throw new RuntimeException("Caractere inesperado no índice " + ptr + ": " + c);
    }

    private Map<String, Object> parseObject() {
        ptr++; // Consome '{'
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        if (ptr < src.length() && src.charAt(ptr) == '}') {
            ptr++;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (ptr >= src.length() || src.charAt(ptr) != '"') {
                throw new RuntimeException("Chave string esperada no objeto no índice " + ptr);
            }
            String key = parseString();
            skipWhitespace();
            if (ptr >= src.length() || src.charAt(ptr) != ':') {
                throw new RuntimeException("Caractere ':' esperado no objeto no índice " + ptr);
            }
            ptr++; // Consome ':'
            Object val = parseValue();
            map.put(key, val);
            skipWhitespace();
            if (ptr < src.length() && src.charAt(ptr) == '}') {
                ptr++;
                break;
            }
            if (ptr < src.length() && src.charAt(ptr) == ',') {
                ptr++;
            } else {
                throw new RuntimeException("Caractere ',' ou '}' esperado no objeto no índice " + ptr);
            }
        }
        return map;
    }

    private List<Object> parseArray() {
        ptr++; // Consome '['
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (ptr < src.length() && src.charAt(ptr) == ']') {
            ptr++;
            return list;
        }
        while (true) {
            Object val = parseValue();
            list.add(val);
            skipWhitespace();
            if (ptr < src.length() && src.charAt(ptr) == ']') {
                ptr++;
                break;
            }
            if (ptr < src.length() && src.charAt(ptr) == ',') {
                ptr++;
            } else {
                throw new RuntimeException("Caractere ',' ou ']' esperado no array no índice " + ptr);
            }
        }
        return list;
    }

    private String parseString() {
        ptr++; // Consome '"'
        StringBuilder sb = new StringBuilder();
        while (ptr < src.length()) {
            char c = src.charAt(ptr);
            if (c == '"') {
                ptr++;
                return sb.toString();
            } else if (c == '\\') {
                ptr++;
                if (ptr >= src.length()) throw new RuntimeException("Sequência de escape não terminada");
                char esc = src.charAt(ptr);
                if (esc == '"' || esc == '\\' || esc == '/') {
                    sb.append(esc);
                } else if (esc == 'b') {
                    sb.append('\b');
                } else if (esc == 'f') {
                    sb.append('\f');
                } else if (esc == 'n') {
                    sb.append('\n');
                } else if (esc == 'r') {
                    sb.append('\r');
                } else if (esc == 't') {
                    sb.append('\t');
                } else if (esc == 'u') {
                    ptr++;
                    if (ptr + 4 > src.length()) throw new RuntimeException("Escape unicode não terminado");
                    String hex = src.substring(ptr, ptr + 4);
                    sb.append((char) Integer.parseInt(hex, 16));
                    ptr += 3;
                }
            } else {
                sb.append(c);
            }
            ptr++;
        }
        throw new RuntimeException("String não terminada");
    }

    private Object parseNumber() {
        int start = ptr;
        if (src.charAt(ptr) == '-') ptr++;
        while (ptr < src.length()) {
            char c = src.charAt(ptr);
            if (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                ptr++;
            } else {
                break;
            }
        }
        String numStr = src.substring(start, ptr);
        if (numStr.contains(".")) {
            return Double.parseDouble(numStr);
        } else {
            return Long.parseLong(numStr);
        }
    }

    private void skipWhitespace() {
        while (ptr < src.length()) {
            char c = src.charAt(ptr);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                ptr++;
            } else {
                break;
            }
        }
    }
}
