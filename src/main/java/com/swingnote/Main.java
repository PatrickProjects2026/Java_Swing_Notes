package com.swingnote;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("The Swing Notice Board needs a graphical desktop.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                JsonStore store = new JsonStore(Path.of("notes.json"));
                new NoticeBoardFrame(store, store.load()).setVisible(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not load notes.json: " + ex.getMessage(),
                        "Notice Board", JOptionPane.ERROR_MESSAGE);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | UnsupportedLookAndFeelException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not start the notice board: " + ex.getMessage(),
                        "Notice Board", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
