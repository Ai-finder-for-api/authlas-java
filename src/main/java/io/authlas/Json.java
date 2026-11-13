package io.authlas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader for the objects authlas returns. Supports objects, arrays, strings, numbers, booleans and null.
 * Dependency-free so the jar has no runtime requirements.
 */
final class Json {
    private final String s;
    private int pos;

    private Json(String s) {
        this.s = s;
    }

    /** Parses a JSON object. Throws {@link AuthlasException} when the text is not an object. */
    static Map<String, Object> object(String text) {
        if (text == null) throw new AuthlasException("authlas returned no data");
        Json p = new Json(text);
        p.skip();
        Object v = p.value();
        if (!(v instanceof Map)) throw new AuthlasException("expected a JSON object from authlas");
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) v;
        return m;
    }

    private Object value() {
        skip();
        if (pos >= s.length()) throw error("unexpected end of JSON");
        char c = s.charAt(pos);
        switch (c) {
            case '{': return obj();
            case '[': return arr();
            case '"': return str();
            case 't': expect("true"); return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null"); return null;
            default: return num();
        }
    }

    private Map<String, Object> obj() {
        Map<String, Object> m = new LinkedHashMap<>();
        pos++; // {
        skip();
        if (peek() == '}') { pos++; return m; }
        while (true) {
            skip();
            String k = str();
            skip();
            if (peek() != ':') throw error("expected ':'");
            pos++;
            m.put(k, value());
            skip();
            char c = peek();
            pos++;
            if (c == '}') return m;
            if (c != ',') throw error("expected ',' or '}'");
        }
    }

    private List<Object> arr() {
        List<Object> a = new ArrayList<>();
        pos++; // [
        skip();
        if (peek() == ']') { pos++; return a; }
        while (true) {
            a.add(value());
            skip();
            char c = peek();
            pos++;
            if (c == ']') return a;
            if (c != ',') throw error("expected ',' or ']'");
        }
    }

    private String str() {
        if (peek() != '"') throw error("expected string");
        pos++;
        StringBuilder b = new StringBuilder();
        while (true) {
            if (pos >= s.length()) throw error("unterminated string");
            char c = s.charAt(pos++);
            if (c == '"') return b.toString();
            if (c != '\\') { b.append(c); continue; }
            char e = s.charAt(pos++);
            switch (e) {
                case '"': b.append('"'); break;
                case '\\': b.append('\\'); break;
                case '/': b.append('/'); break;
                case 'b': b.append('\b'); break;
                case 'f': b.append('\f'); break;
                case 'n': b.append('\n'); break;
                case 'r': b.append('\r'); break;
                case 't': b.append('\t'); break;
                case 'u': b.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16)); pos += 4; break;
                default: throw error("bad escape");
            }
        }
    }

    private Object num() {
        int start = pos;
        while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
        if (start == pos) throw error("unexpected character");
        String t = s.substring(start, pos);
        if (t.contains(".") || t.contains("e") || t.contains("E")) return Double.parseDouble(t);
        return Long.parseLong(t);
    }

    private void expect(String word) {
        if (!s.startsWith(word, pos)) throw error("expected " + word);
        pos += word.length();
    }

    private char peek() {
        if (pos >= s.length()) throw error("unexpected end of JSON");
        return s.charAt(pos);
    }

    private void skip() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
    }

    private AuthlasException error(String msg) {
        return new AuthlasException("bad JSON from authlas: " + msg + " at " + pos);
    }
}
