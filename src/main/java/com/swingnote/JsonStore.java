package com.swingnote;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonStore {
    private final Path file;

    JsonStore(Path file) {
        this.file = file;
    }

    BoardData load() throws IOException {
        if (!Files.exists(file)) {
            return new BoardData();
        }

        Object parsed = new Parser(Files.readString(file, StandardCharsets.UTF_8)).parse();
        Map<?, ?> root = asObject(parsed, "root");
        BoardData data = new BoardData();

        for (Object value : asArray(root.get("cats"), "cats")) {
            Map<?, ?> category = asObject(value, "category");
            data.categories.add(new BoardData.Category(
                    requiredString(category, "id"),
                    requiredString(category, "name"),
                    requiredString(category, "type")));
        }

        for (Object value : asArray(root.get("notes"), "notes")) {
            Map<?, ?> note = asObject(value, "note");
            data.notes.add(new BoardData.Note(
                    requiredString(note, "id"),
                    requiredString(note, "name"),
                    requiredString(note, "note"),
                    requiredString(note, "cat")));
        }
        return data;
    }

    void save(BoardData data) throws IOException {
        String json = serialize(data);
        Path absoluteFile = file.toAbsolutePath();
        Path parent = absoluteFile.getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "notes-", ".tmp");
        try {
            Files.writeString(temporaryFile, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, absoluteFile,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String serialize(BoardData data) {
        StringBuilder json = new StringBuilder("{\n  \"cats\": [");
        for (int i = 0; i < data.categories.size(); i++) {
            BoardData.Category category = data.categories.get(i);
            json.append(i == 0 ? "\n" : ",\n")
                    .append("    {\"id\": ").append(quote(category.id))
                    .append(", \"name\": ").append(quote(category.name))
                    .append(", \"type\": ").append(quote(category.type)).append("}");
        }
        if (!data.categories.isEmpty()) {
            json.append('\n').append("  ");
        }
        json.append("],\n  \"notes\": [");
        for (int i = 0; i < data.notes.size(); i++) {
            BoardData.Note note = data.notes.get(i);
            json.append(i == 0 ? "\n" : ",\n")
                    .append("    {\"id\": ").append(quote(note.id))
                    .append(", \"name\": ").append(quote(note.name))
                    .append(", \"note\": ").append(quote(note.note))
                    .append(", \"cat\": ").append(quote(note.categoryId)).append("}");
        }
        if (!data.notes.isEmpty()) {
            json.append('\n').append("  ");
        }
        return json.append("]\n}\n").toString();
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        result.append(String.format("\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
            }
        }
        return result.append('"').toString();
    }

    private static Map<?, ?> asObject(Object value, String name) throws IOException {
        if (!(value instanceof Map)) {
            throw new IOException("Expected a JSON object for " + name + ".");
        }
        return (Map<?, ?>) value;
    }

    private static List<?> asArray(Object value, String name) throws IOException {
        if (!(value instanceof List)) {
            throw new IOException("Expected a JSON array for " + name + ".");
        }
        return (List<?>) value;
    }

    private static String requiredString(Map<?, ?> object, String key) throws IOException {
        Object value = object.get(key);
        if (!(value instanceof String)) {
            throw new IOException("Expected a string for '" + key + "'.");
        }
        return (String) value;
    }

    private static final class Parser {
        private final String input;
        private int position;

        Parser(String input) {
            this.input = input;
        }

        Object parse() throws IOException {
            Object value = readValue();
            skipWhitespace();
            if (position != input.length()) {
                fail("Unexpected content after the JSON value");
            }
            return value;
        }

        private Object readValue() throws IOException {
            skipWhitespace();
            if (position >= input.length()) {
                return fail("Unexpected end of input");
            }
            char character = input.charAt(position);
            if (character == '{') {
                return readObject();
            }
            if (character == '[') {
                return readArray();
            }
            if (character == '"') {
                return readString();
            }
            if (character == 't') {
                readLiteral("true", Boolean.TRUE);
                return Boolean.TRUE;
            }
            if (character == 'f') {
                readLiteral("false", Boolean.FALSE);
                return Boolean.FALSE;
            }
            if (character == 'n') {
                readLiteral("null", null);
                return null;
            }
            return readNumber();
        }

        private Map<String, Object> readObject() throws IOException {
            position++;
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            while (true) {
                skipWhitespace();
                if (position >= input.length() || input.charAt(position) != '"') {
                    return fail("Expected an object key");
                }
                String key = readString();
                skipWhitespace();
                expect(':');
                if (object.containsKey(key)) {
                    return fail("Duplicate object key '" + key + "'");
                }
                object.put(key, readValue());
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> readArray() throws IOException {
            position++;
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return array;
            }
            while (true) {
                array.add(readValue());
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                expect(',');
            }
        }

        private String readString() throws IOException {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < input.length()) {
                char character = input.charAt(position++);
                if (character == '"') {
                    return value.toString();
                }
                if (character < 0x20) {
                    return fail("Unescaped control character in string");
                }
                if (character != '\\') {
                    value.append(character);
                    continue;
                }
                if (position >= input.length()) {
                    return fail("Unterminated escape sequence");
                }
                char escaped = input.charAt(position++);
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        value.append(escaped);
                        break;
                    case 'b':
                        value.append('\b');
                        break;
                    case 'f':
                        value.append('\f');
                        break;
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'u':
                        value.append(readUnicodeEscape());
                        break;
                    default:
                        return fail("Invalid escape sequence");
                }
            }
            return fail("Unterminated string");
        }

        private char readUnicodeEscape() throws IOException {
            if (position + 4 > input.length()) {
                return fail("Incomplete Unicode escape");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(input.charAt(position++), 16);
                if (digit < 0) {
                    return fail("Invalid Unicode escape");
                }
                value = value * 16 + digit;
            }
            return (char) value;
        }

        private Number readNumber() throws IOException {
            int start = position;
            if (consume('-') && position == input.length()) {
                return fail("Invalid number");
            }
            if (consume('0')) {
                if (position < input.length() && Character.isDigit(input.charAt(position))) {
                    return fail("Leading zero in number");
                }
            } else {
                readDigits();
            }
            if (consume('.')) {
                readDigits();
            }
            if (consume('e') || consume('E')) {
                if (!consume('+')) {
                    consume('-');
                }
                readDigits();
            }
            if (start == position) {
                return fail("Invalid JSON value");
            }
            try {
                return Double.valueOf(input.substring(start, position));
            } catch (NumberFormatException ex) {
                return fail("Invalid number");
            }
        }

        private void readDigits() throws IOException {
            int start = position;
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                position++;
            }
            if (start == position) {
                fail("Expected a digit");
            }
        }

        private void readLiteral(String literal, Object value) throws IOException {
            if (!input.startsWith(literal, position)) {
                fail("Invalid JSON value");
            }
            position += literal.length();
        }

        private void expect(char expected) throws IOException {
            if (!consume(expected)) {
                fail("Expected '" + expected + "'");
            }
        }

        private boolean consume(char expected) {
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private <T> T fail(String message) throws IOException {
            throw new IOException(message + " at character " + position + ".");
        }
    }
}
