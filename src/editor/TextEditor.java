package editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;


public class TextEditor extends JFrame{
	
	private JTabbedPane tabbedPane;
    private JMenuBar menuBar;
    private JMenu fileMenu, editMenu, searchMenu, viewMenu, helpMenu;
    private JMenuItem newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, exitMenuItem;
    private JMenuItem cutMenuItem, copyMenuItem, pasteMenuItem, findMenuItem, replaceMenuItem;
    private JCheckBoxMenuItem wordWrapMenuItem;
    private JMenuItem aboutMenuItem;

    // Data Structures
    private List<File> recentFiles;
    private Map<Component, File> openFiles; // Tracks open files/tabs

    // Flags and Variables
    private boolean isWordWrapEnabled;
    private String currentFileName;
    private boolean isFileModified;
    private Timer autoSaveTimer;

    // Database (For demonstration, using a simple file system)
    private String databasePath = "database/"; 

    public TextEditor() {
        super();
        setTitle("Text Editor"); // Set the window title
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize data structures
        recentFiles = new ArrayList<>();
        openFiles = new HashMap<>();

        // Initialize components
        createMenuBar();
        createTabbedPane();

        // Set layout
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        // Initialize flags
        isWordWrapEnabled = false;
        isFileModified = false;

        // Auto-Save (Save every 1 minute)
        autoSaveTimer = new Timer(60000, e -> autoSave());
        autoSaveTimer.start();

        // Initialize database (create directory if it doesn't exist)
        File dbDir = new File(databasePath);
        if (!dbDir.exists()) {
            dbDir.mkdir();
        }

        setVisible(true);
    }


	private void createMenuBar() {
        menuBar = new JMenuBar();

        // File Menu
        fileMenu = new JMenu("File");
        newMenuItem = new JMenuItem("New");
        openMenuItem = new JMenuItem("Open");
        saveMenuItem = new JMenuItem("Save");
        saveAsMenuItem = new JMenuItem("Save As");
        JMenuItem increaseFontSizeItem = new JMenuItem("Increase Font Size");
        JMenuItem decreaseFontSizeItem = new JMenuItem("Decrease Font Size");
        exitMenuItem = new JMenuItem("Exit");

        // Action Listeners for File Menu
        newMenuItem.addActionListener(e -> createNewTab());
        openMenuItem.addActionListener(e -> openFile());
        saveMenuItem.addActionListener(e -> saveFile());
        saveAsMenuItem.addActionListener(e -> saveFileAs());
        increaseFontSizeItem.addActionListener(e -> changeFontSize(2));
        decreaseFontSizeItem.addActionListener(e -> changeFontSize(-2));
        exitMenuItem.addActionListener(e -> System.exit(0));

        // Add items to File Menu
        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        // Edit Menu
        editMenu = new JMenu("Edit");
        cutMenuItem = new JMenuItem("Cut");
        copyMenuItem = new JMenuItem("Copy");
        pasteMenuItem = new JMenuItem("Paste");

        // Action Listeners for Edit Menu (using default actions)
        cutMenuItem.addActionListener(new DefaultEditorKit.CutAction());
        copyMenuItem.addActionListener(new DefaultEditorKit.CopyAction());
        pasteMenuItem.addActionListener(new DefaultEditorKit.PasteAction());

        // Add items to Edit Menu
        editMenu.add(cutMenuItem);
        editMenu.add(copyMenuItem);
        editMenu.add(pasteMenuItem);

        // Search Menu
        searchMenu = new JMenu("Search");
        findMenuItem = new JMenuItem("Find");
        replaceMenuItem = new JMenuItem("Replace");

        // Action Listeners for Search Menu
        findMenuItem.addActionListener(e -> findText());
        replaceMenuItem.addActionListener(e -> replaceText());

        // Add items to Search Menu
        searchMenu.add(findMenuItem);
        searchMenu.add(replaceMenuItem);

        // View Menu
        viewMenu = new JMenu("View");
        wordWrapMenuItem = new JCheckBoxMenuItem("Word Wrap");

        // Action Listener for View Menu
        wordWrapMenuItem.addActionListener(e -> toggleWordWrap());

        // Add items to View Menu
        viewMenu.add(wordWrapMenuItem);
        viewMenu.add(increaseFontSizeItem);
        viewMenu.add(decreaseFontSizeItem);

        // Help Menu
        helpMenu = new JMenu("Help");
        aboutMenuItem = new JMenuItem("About");

        // Action Listener for Help Menu
        aboutMenuItem.addActionListener(e -> showAboutDialog());

        // Add items to Help Menu
        helpMenu.add(aboutMenuItem);

        // Add menus to Menu Bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(searchMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        // Set Menu Bar
        setJMenuBar(menuBar);
    }

    private void createTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
    }

    private void createNewTab() {
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        textArea.setLineWrap(isWordWrapEnabled);
        textArea.setWrapStyleWord(true);

        // Add document listener to track changes
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                isFileModified = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                isFileModified = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                isFileModified = true;
            }
        });

        // Create new tab
        JComponent tabComponent = new JPanel();
        tabComponent.setLayout(new BorderLayout());
        tabComponent.add(new JScrollPane(textArea), BorderLayout.CENTER);

        tabbedPane.addTab("Untitled", tabComponent);

        // Set current file name
        currentFileName = "Untitled";
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");
        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // Check if file is already open
            if (openFiles.containsValue(selectedFile)) {
                JOptionPane.showInputDialog(this, "File is already open.");
                return;
            }

            // Read file content
            try {
                String fileContent = readFile(selectedFile);
                JTextArea textArea = new JTextArea(fileContent);
                textArea.setFont(new Font("Arial", Font.PLAIN, 14));
                textArea.setLineWrap(isWordWrapEnabled);
                textArea.setWrapStyleWord(true);

                // Add document listener to track changes
                textArea.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        isFileModified = true;
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        isFileModified = true;
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        isFileModified = true;
                    }
                });

                // Create new tab
                JComponent tabComponent = new JPanel();
                tabComponent.setLayout(new BorderLayout());
                tabComponent.add(new JScrollPane(textArea), BorderLayout.CENTER);

                tabbedPane.addTab(selectedFile.getName(), tabComponent);

                // Set current file name
                currentFileName = selectedFile.getName();

                // Add file to recent files
                recentFiles.add(selectedFile);
                openFiles.put(tabComponent, selectedFile);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage());
            }
        }
    }

    private void saveFile() {
        if (currentFileName.equals("Untitled")) {
            saveFileAs();
        } else {
            File file = new File(databasePath + currentFileName);
            try {
                writeFile(file, getTextAreaContent());
                isFileModified = false;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
            }
        }
    }

    private void saveFileAs() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");
        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // Check if file already exists
            if (selectedFile.exists()) {
                int response = JOptionPane.showConfirmDialog(this, "File already exists. Overwrite?");
                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                writeFile(selectedFile, getTextAreaContent());
                isFileModified = false;
                currentFileName = selectedFile.getName();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
            }
        }
    }

    private void autoSave() {
        if (isFileModified) {
            saveFile();
        }
    }

    private String getTextAreaContent() {
        JTextArea textArea = (JTextArea) ((JScrollPane) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0)).getViewport().getView();
        return textArea.getText();
    }

    private void readFileIntoTextArea(File file) throws IOException {
        String fileContent = readFile(file);
        JTextArea textArea = (JTextArea) ((JScrollPane) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0)).getViewport().getView();
        textArea.setText(fileContent);
    }

    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private void writeFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    private void findText() {
        String textToFind = JOptionPane.showInputDialog(this, "Enter text to find:");
        if (textToFind != null) {
            JTextArea textArea = (JTextArea) ((JScrollPane) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0)).getViewport().getView();
            Highlighter highlighter = textArea.getHighlighter();
            highlighter.removeAllHighlights();

            try {
                int startIndex = 0;
                while (startIndex < textArea.getText().length()) {
                    int index = textArea.getText().indexOf(textToFind, startIndex);
                    if (index == -1) {
                        break;
                    }
                    highlighter.addHighlight(index, index + textToFind.length(), DefaultHighlighter.DefaultPainter);
                    startIndex = index + textToFind.length();
                }
            } catch (BadLocationException e) {
                JOptionPane.showMessageDialog(this, "Error finding text: " + e.getMessage());
            }
        }
    }

    private void replaceText() {
        String textToFind = JOptionPane.showInputDialog(this, "Enter text to find:");
        String replacementText = JOptionPane.showInputDialog(this, "Enter replacement text:");
        if (textToFind != null && replacementText != null) {
            JTextArea textArea = (JTextArea) ((JScrollPane) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0)).getViewport().getView();
            String content = textArea.getText();
            content = content.replace(textToFind, replacementText);
            textArea.setText(content);
        }
    }

    private void toggleWordWrap() {
        isWordWrapEnabled = wordWrapMenuItem.isSelected();
        JTextArea textArea = (JTextArea) ((JScrollPane) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0)).getViewport().getView();
        textArea.setLineWrap(isWordWrapEnabled);
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this, "Text Editor\nVersion 1.0\nCopyright 2023", "About", JOptionPane.INFORMATION_MESSAGE);
    }
    private void changeFontSize(int delta) {
        JTextArea textArea = (JTextArea) ((JScrollPane) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0)).getViewport().getView();
        Font currentFont = textArea.getFont();
        int newSize = Math.max(10, currentFont.getSize() + delta); // Prevent font size from becoming too small
        textArea.setFont(new Font(currentFont.getFontName(), currentFont.getStyle(), newSize));
    }
       public void removeBook(String bookName) {
if (books.remove(bookName)) {
System.out.println(bookName + "removed from library.");
} else {
System.out.println(bookName + "not found in library.");
}
}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TextEditor());
    }

}