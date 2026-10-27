package com.swingnote;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonStoreSmokeTest {
    private JsonStoreSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("swing-note-test-");
        Path file = directory.resolve("notes.json");
        try {
            JsonStore store = new JsonStore(file);
            BoardData original = new BoardData();
            original.categories.add(new BoardData.Category("cat-1", "Ideas", "web"));
            original.notes.add(new BoardData.Note(
                    "note-1", "Quote \"this\"", "Line one\nLine two — café", "cat-1"));

            store.save(original);
            String json = Files.readString(file, StandardCharsets.UTF_8);
            check(json.contains("\"cats\""), "JSON should include cats.");
            check(json.contains("\"cat\""), "JSON should include the note category reference.");

            BoardData restored = store.load();
            check(restored.categories.size() == 1, "Category should round-trip.");
            check("web".equals(restored.categories.get(0).type), "Category type should round-trip.");
            check(restored.notes.size() == 1, "Note should round-trip.");
            check("Quote \"this\"".equals(restored.notes.get(0).name), "Quotes should round-trip.");
            check("Line one\nLine two — café".equals(restored.notes.get(0).note),
                    "Newlines and Unicode should round-trip.");
            check("cat-1".equals(restored.notes.get(0).categoryId),
                    "Category reference should round-trip.");

            Files.writeString(file, "{ invalid", StandardCharsets.UTF_8);
            try {
                store.load();
                throw new AssertionError("Malformed JSON should be rejected.");
            } catch (IOException expected) {
                // Expected: corrupted input must be reported rather than silently discarded.
            }
            System.out.println("JsonStore smoke test passed.");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
