package org.example;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

class PatternEntry {
    private double time;
    private String frequencyCategory;

    public PatternEntry(double time, String frequencyCategory) {
        this.time = time;
        this.frequencyCategory = frequencyCategory;
    }

    public double getTime() {
        return time;
    }

    public String getFrequencyCategory() {
        return frequencyCategory;
    }
}

public class RhythmGame extends JFrame {
    private JPanel gridPanel;
    private JButton[][] buttons;
    private JLabel scoreLabel;
    private JLabel messageLabel;
    private Map<Point, Long> activeButtons;
    private javax.swing.Timer timer;
    private int beatInterval = 100;  // Changed to 100ms for more responsive timing
    private int gridSize = 3;
    private Random random;
    private int score = 0;
    private long activationWindow = 500;
    private List<PatternEntry> activationPattern;
    private int currentStep = 0;
    private JFileChooser fileChooser;
    private File selectedFile;
    private File dataDirectory = new File("data");
    private Clip clip;
    private long musicStartTime;

    public RhythmGame() {
        setTitle("3x3 Keypad Rhythm Game");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout());
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Serif", Font.BOLD, 20));
        messageLabel = new JLabel(" ");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setFont(new Font("Serif", Font.PLAIN, 16));
        topPanel.add(scoreLabel, BorderLayout.WEST);
        topPanel.add(messageLabel, BorderLayout.CENTER);

        JPanel filePanel = new JPanel();
        JButton loadFileButton = new JButton("Load Music File");
        loadFileButton.addActionListener(e -> loadMusicFile());
        filePanel.add(loadFileButton);
        topPanel.add(filePanel, BorderLayout.SOUTH);

        gridPanel = new JPanel(new GridLayout(gridSize, gridSize));
        buttons = new JButton[gridSize][gridSize];
        activeButtons = new HashMap<>();
        random = new Random();

        int[][] keypadNumbers = {{7, 8, 9}, {4, 5, 6}, {1, 2, 3}};
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                JButton button = new JButton(String.valueOf(keypadNumbers[i][j]));
                button.setEnabled(false);
                buttons[i][j] = button;
                gridPanel.add(button);
                int row = i;
                int col = j;
                button.addActionListener(e -> checkButtonHit(row, col));
            }
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
        add(mainPanel);

        timer = new javax.swing.Timer(beatInterval, e -> activatePatternButton());

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e.getKeyCode());
            }
        });

        setFocusable(true);
        setVisible(true);
    }

    private void loadMusicFile() {
        fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            try {
                // Ensure the data directory exists
                if (!dataDirectory.exists()) {
                    dataDirectory.mkdir();
                }

                // Check if data already exists for this music file
                File patternFile = new File(dataDirectory, selectedFile.getName() + ".pattern");
                if (patternFile.exists()) {
                    activationPattern = loadPatternFromFile(patternFile);
                } else {
                    // Load pattern from text file and save to pattern file
                    activationPattern = loadPatternFromTextFile();
                    savePatternToFile(patternFile, activationPattern);
                }

                playMusic(selectedFile);
                currentStep = 0;
                musicStartTime = System.currentTimeMillis();
                timer.start();
                System.out.println("Music and pattern loaded. Timer started.");
            } catch (Exception e) {
                e.printStackTrace();
                showMessage("Error loading music file");
            }
        }
    }

    private List<PatternEntry> loadPatternFromFile(File file) throws IOException {
        List<PatternEntry> pattern = new ArrayList<>();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(" ");
                double time = Double.parseDouble(parts[0]);
                String category = parts[1];
                pattern.add(new PatternEntry(time, category));
            }
        }
        System.out.println("Pattern loaded from file: " + pattern);
        return pattern;
    }

    private List<PatternEntry> loadPatternFromTextFile() throws IOException {
        List<PatternEntry> pattern = new ArrayList<>();
        File patternFile = new File("C:\\Users\\user\\IdeaProjects\\JavaGUI\\data\\nell_기다린다.wav.pattern");
        if (!patternFile.exists()) {
            throw new FileNotFoundException("Pattern file not found: " + patternFile.getAbsolutePath());
        }
        try (Scanner scanner = new Scanner(patternFile)) {
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(" ");
                double time = Double.parseDouble(parts[0]);
                String category = parts[1];
                pattern.add(new PatternEntry(time, category));
            }
        }
        System.out.println("Pattern loaded from text file: " + pattern);
        return pattern;
    }

    private void savePatternToFile(File file, List<PatternEntry> pattern) throws IOException {
        try (PrintWriter writer = new PrintWriter(file)) {
            for (PatternEntry entry : pattern) {
                writer.println(entry.getTime() + " " + entry.getFrequencyCategory());
            }
        }
        System.out.println("Pattern saved to file: " + file.getAbsolutePath());
    }

    private void playMusic(File musicFile) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        stopMusic(); // Stop previously playing music
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(musicFile);
        clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        clip.loop(Clip.LOOP_CONTINUOUSLY); // Play music in loop
        clip.start(); // Start playing music
        System.out.println("Music playing: " + musicFile.getName());
    }

    private void stopMusic() {
        if (clip != null && clip.isOpen()) {
            clip.stop(); // Stop currently playing music
            clip.close(); // Close the clip
        }
    }

    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageLabel.setText(message);
            messageLabel.setForeground(Color.RED);
        });
        System.out.println("Message: " + message);
    }

    private void activatePatternButton() {
        long currentTime = System.currentTimeMillis() - musicStartTime;
        for (Point point : activeButtons.keySet()) {
            buttons[point.x][point.y].setEnabled(false);
        }
        activeButtons.clear();

        if (currentStep < activationPattern.size()) {
            PatternEntry entry = activationPattern.get(currentStep);
            double time = entry.getTime() * 1000;  // Convert to milliseconds
            String category = entry.getFrequencyCategory();

            if (currentTime >= time) {
                Point point = getButtonPosition(category);
                if (point.x != -1 && point.y != -1) {
                    currentStep++;
                    buttons[point.x][point.y].setEnabled(true);
                    activeButtons.put(point, System.currentTimeMillis());
                    System.out.println("Button activated at: " + point);
                }
            }
        } else {
            timer.stop();
            showMessage("Pattern Completed!");
        }
    }

    private Point getButtonPosition(String category) {
        int[] possibleKeys;
        switch (category) {
            case "low":
                possibleKeys = new int[]{1, 2, 3};
                break;
            case "medium":
                possibleKeys = new int[]{4, 5, 6};
                break;
            case "high":
                possibleKeys = new int[]{7, 8, 9};
                break;
            default:
                return new Point(-1, -1);
        }
        int key = possibleKeys[random.nextInt(possibleKeys.length)];
        return getButtonPosition(key);
    }

    private Point getButtonPosition(int key) {
        switch (key) {
            case 1:
                return new Point(2, 0);
            case 2:
                return new Point(2, 1);
            case 3:
                return new Point(2, 2);
            case 4:
                return new Point(1, 0);
            case 5:
                return new Point(1, 1);
            case 6:
                return new Point(1, 2);
            case 7:
                return new Point(0, 0);
            case 8:
                return new Point(0, 1);
            case 9:
                return new Point(0, 2);
            default:
                return new Point(-1, -1);
        }
    }

    private void checkButtonHit(int row, int col) {
        long currentTime = System.currentTimeMillis();
        Point point = new Point(row, col);
        if (!activeButtons.containsKey(point)) {
            showMessage("Miss!");
        } else {
            long activationTime = activeButtons.get(point);
            if (currentTime - activationTime <= activationWindow) {
                buttons[row][col].setEnabled(false);
                activeButtons.remove(point);
                score += 10;
                scoreLabel.setText("Score: " + score);
                showMessage("Hit!");
            } else {
                showMessage("Too late!");
            }
        }
    }

    private void handleKeyPress(int keyCode) {
        int row = -1, col = -1;

        switch (keyCode) {
            case KeyEvent.VK_NUMPAD7:
            case KeyEvent.VK_7:
                row = 0;
                col = 0;
                break;
            case KeyEvent.VK_NUMPAD8:
            case KeyEvent.VK_8:
                row = 0;
                col = 1;
                break;
            case KeyEvent.VK_NUMPAD9:
            case KeyEvent.VK_9:
                row = 0;
                col = 2;
                break;
            case KeyEvent.VK_NUMPAD4:
            case KeyEvent.VK_4:
                row = 1;
                col = 0;
                break;
            case KeyEvent.VK_NUMPAD5:
            case KeyEvent.VK_5:
                row = 1;
                col = 1;
                break;
            case KeyEvent.VK_NUMPAD6:
            case KeyEvent.VK_6:
                row = 1;
                col = 2;
                break;
            case KeyEvent.VK_NUMPAD1:
            case KeyEvent.VK_1:
                row = 2;
                col = 0;
                break;
            case KeyEvent.VK_NUMPAD2:
            case KeyEvent.VK_2:
                row = 2;
                col = 1;
                break;
            case KeyEvent.VK_NUMPAD3:
            case KeyEvent.VK_3:
                row = 2;
                col = 2;
                break;
        }

        if (row != -1 && col != -1) {
            checkButtonHit(row, col);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RhythmGame rhythmGame = new RhythmGame();
            rhythmGame.setVisible(true);
        });
    }
}
