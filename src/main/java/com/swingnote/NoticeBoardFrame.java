package com.swingnote;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

final class NoticeBoardFrame extends JFrame {
    private final JsonStore store;
    private final BoardData data;
    private final DefaultListModel<BoardData.Category> categoryModel = new DefaultListModel<>();
    private final DefaultListModel<BoardData.Note> noteModel = new DefaultListModel<>();
    private final JList<BoardData.Category> categoryList = new JList<>(categoryModel);
    private final JList<BoardData.Note> noteList = new JList<>(noteModel);
    private final JTextField categoryNameField = new JTextField(18);
    private final JComboBox<String> categoryTypeBox = new JComboBox<>(new String[] { "note", "web" });
    private final JTextField noteNameField = new JTextField(26);
    private final JTextArea noteTextArea = new JTextArea(12, 26);
    private final JComboBox<BoardData.Category> noteCategoryBox = new JComboBox<>();
    private BoardData.Note selectedNote;
    private BoardData.Category selectedCategory;
    private boolean refreshingLists;

    NoticeBoardFrame(JsonStore store, BoardData data) {
        super("Swing Notice Board");
        this.store = store;
        this.data = data;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 500));
        setSize(900, 600);
        setLocationRelativeTo(null);
        buildInterface();
        refreshLists();
    }

    private void buildInterface() {
        JPanel categoriesPanel = buildCategoriesPanel();
        JPanel notesPanel = buildNotesPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categoriesPanel, notesPanel);
        splitPane.setResizeWeight(0.28);
        splitPane.setDividerLocation(250);
        add(splitPane, BorderLayout.CENTER);

        categoryList.addListSelectionListener(this::categorySelected);
        noteList.addListSelectionListener(this::noteSelected);
    }

    private JPanel buildCategoriesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 6));
        panel.add(new JLabel("Categories"), BorderLayout.NORTH);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(categoryList), BorderLayout.CENTER);

        JPanel editor = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = constraints();
        addRow(editor, constraints, 0, "Name", categoryNameField);
        addRow(editor, constraints, 1, "Type", categoryTypeBox);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton save = new JButton("Save");
        JButton delete = new JButton("Delete");
        JButton clear = new JButton("Clear");
        save.addActionListener(event -> saveCategory());
        delete.addActionListener(event -> deleteCategory());
        clear.addActionListener(event -> clearCategoryEditor());
        buttons.add(save);
        buttons.add(delete);
        buttons.add(clear);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        editor.add(buttons, constraints);
        panel.add(editor, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildNotesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 12));
        panel.add(new JLabel("Notes"), BorderLayout.NORTH);
        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(noteList), BorderLayout.CENTER);

        JPanel editor = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = constraints();
        noteTextArea.setLineWrap(true);
        noteTextArea.setWrapStyleWord(true);
        addRow(editor, constraints, 0, "Name", noteNameField);
        addRow(editor, constraints, 1, "Category", noteCategoryBox);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        editor.add(new JScrollPane(noteTextArea), constraints);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton save = new JButton("Save");
        JButton delete = new JButton("Delete");
        JButton clear = new JButton("New note");
        save.addActionListener(event -> saveNote());
        delete.addActionListener(event -> deleteNote());
        clear.addActionListener(event -> clearNoteEditor());
        buttons.add(save);
        buttons.add(delete);
        buttons.add(clear);
        constraints.gridy = 3;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        editor.add(buttons, constraints);
        panel.add(editor, BorderLayout.SOUTH);
        return panel;
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        return constraints;
    }

    private static void addRow(JPanel panel, GridBagConstraints constraints,
            int row, String label, java.awt.Component field) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(field, constraints);
    }

    private void categorySelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || refreshingLists) {
            return;
        }
        selectedCategory = categoryList.getSelectedValue();
        if (selectedCategory == null) {
            clearCategoryEditor();
            return;
        }
        categoryNameField.setText(selectedCategory.name);
        categoryTypeBox.setSelectedItem(selectedCategory.type);
    }

    private void noteSelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || refreshingLists) {
            return;
        }
        selectedNote = noteList.getSelectedValue();
        if (selectedNote == null) {
            clearNoteEditor();
            return;
        }
        noteNameField.setText(selectedNote.name);
        noteTextArea.setText(selectedNote.note);
        for (int i = 0; i < noteCategoryBox.getItemCount(); i++) {
            if (noteCategoryBox.getItemAt(i).id.equals(selectedNote.categoryId)) {
                noteCategoryBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private void saveCategory() {
        String name = categoryNameField.getText().trim();
        if (name.isEmpty()) {
            showError("Enter a category name.");
            return;
        }
        String type = (String) categoryTypeBox.getSelectedItem();
        boolean isNew = selectedCategory == null;
        String previousName = isNew ? null : selectedCategory.name;
        String previousType = isNew ? null : selectedCategory.type;
        if (selectedCategory == null) {
            selectedCategory = new BoardData.Category(BoardData.newId(), name, type);
            data.categories.add(selectedCategory);
        } else {
            selectedCategory.name = name;
            selectedCategory.type = type;
        }
        if (!persist()) {
            if (isNew) {
                data.categories.remove(selectedCategory);
                selectedCategory = null;
            } else {
                selectedCategory.name = previousName;
                selectedCategory.type = previousType;
            }
            return;
        }
        refreshLists();
        selectCategory(selectedCategory);
    }

    private void deleteCategory() {
        if (selectedCategory == null) {
            showError("Select a category to delete.");
            return;
        }
        for (BoardData.Note note : data.notes) {
            if (note.categoryId.equals(selectedCategory.id)) {
                showError("Move or delete this category's notes before deleting it.");
                return;
            }
        }
        if (JOptionPane.showConfirmDialog(this,
                "Delete category '" + selectedCategory.name + "'?",
                "Delete category", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        BoardData.Category deletedCategory = selectedCategory;
        int deletedIndex = data.categories.indexOf(deletedCategory);
        data.categories.remove(deletedCategory);
        if (!persist()) {
            data.categories.add(deletedIndex, deletedCategory);
            return;
        }
        selectedCategory = null;
        clearCategoryEditor();
        refreshLists();
    }

    private void saveNote() {
        String name = noteNameField.getText().trim();
        BoardData.Category category = (BoardData.Category) noteCategoryBox.getSelectedItem();
        if (name.isEmpty()) {
            showError("Enter a note name.");
            return;
        }
        if (category == null) {
            showError("Create a category before saving a note.");
            return;
        }
        boolean isNew = selectedNote == null;
        String previousName = isNew ? null : selectedNote.name;
        String previousText = isNew ? null : selectedNote.note;
        String previousCategoryId = isNew ? null : selectedNote.categoryId;
        if (selectedNote == null) {
            selectedNote = new BoardData.Note(BoardData.newId(), name,
                    noteTextArea.getText(), category.id);
            data.notes.add(selectedNote);
        } else {
            selectedNote.name = name;
            selectedNote.note = noteTextArea.getText();
            selectedNote.categoryId = category.id;
        }
        if (!persist()) {
            if (isNew) {
                data.notes.remove(selectedNote);
                selectedNote = null;
            } else {
                selectedNote.name = previousName;
                selectedNote.note = previousText;
                selectedNote.categoryId = previousCategoryId;
            }
            return;
        }
        refreshLists();
        selectNote(selectedNote);
    }

    private void deleteNote() {
        if (selectedNote == null) {
            showError("Select a note to delete.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "Delete note '" + selectedNote.name + "'?",
                "Delete note", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        BoardData.Note deletedNote = selectedNote;
        int deletedIndex = data.notes.indexOf(deletedNote);
        data.notes.remove(deletedNote);
        if (!persist()) {
            data.notes.add(deletedIndex, deletedNote);
            return;
        }
        selectedNote = null;
        clearNoteEditor();
        refreshLists();
    }

    private boolean persist() {
        try {
            store.save(data);
            return true;
        } catch (IOException ex) {
            showError("Could not save notes.json: " + ex.getMessage());
            return false;
        }
    }

    private void refreshLists() {
        refreshingLists = true;
        categoryModel.clear();
        for (BoardData.Category category : data.categories) {
            categoryModel.addElement(category);
        }
        noteModel.clear();
        for (BoardData.Note note : data.notes) {
            noteModel.addElement(note);
        }
        List<BoardData.Category> categories = new ArrayList<>(data.categories);
        noteCategoryBox.removeAllItems();
        for (BoardData.Category category : categories) {
            noteCategoryBox.addItem(category);
        }
        categoryList.setCellRenderer((list, category, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(category.name + " (" + category.type + ")");
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        noteList.setCellRenderer((list, note, index, isSelected, cellHasFocus) -> {
            String categoryName = data.categories.stream()
                    .filter(category -> category.id.equals(note.categoryId))
                    .map(category -> category.name)
                    .findFirst().orElse("Unknown category");
            JLabel label = new JLabel(note.name + "  —  " + categoryName);
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        refreshingLists = false;
    }

    private void selectCategory(BoardData.Category category) {
        int index = data.categories.indexOf(category);
        if (index >= 0) {
            categoryList.setSelectedIndex(index);
        }
    }

    private void selectNote(BoardData.Note note) {
        int index = data.notes.indexOf(note);
        if (index >= 0) {
            noteList.setSelectedIndex(index);
        }
    }

    private void clearCategoryEditor() {
        selectedCategory = null;
        categoryNameField.setText("");
        categoryTypeBox.setSelectedItem("note");
        categoryList.clearSelection();
    }

    private void clearNoteEditor() {
        selectedNote = null;
        noteNameField.setText("");
        noteTextArea.setText("");
        if (noteCategoryBox.getItemCount() > 0) {
            noteCategoryBox.setSelectedIndex(0);
        }
        noteList.clearSelection();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Notice Board", JOptionPane.ERROR_MESSAGE);
    }
}
