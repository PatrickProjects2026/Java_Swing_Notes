package com.swingnote;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class BoardData {
    final List<Category> categories = new ArrayList<>();
    final List<Note> notes = new ArrayList<>();

    static String newId() {
        return UUID.randomUUID().toString();
    }

    static final class Category {
        final String id;
        String name;
        String type;

        Category(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }

    static final class Note {
        final String id;
        String name;
        String note;
        String categoryId;

        Note(String id, String name, String note, String categoryId) {
            this.id = id;
            this.name = name;
            this.note = note;
            this.categoryId = categoryId;
        }
    }
}
