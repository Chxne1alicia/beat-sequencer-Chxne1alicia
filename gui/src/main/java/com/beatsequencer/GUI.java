package com.beatsequencer;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.net.URI;
import java.net.http.*;

public class GUI extends JFrame {
    private static final String BASE_URL = "http://localhost:7001";
    private static final HttpClient client = HttpClient.newHttpClient();
    private final String[] INSTRUMENTS = {"Kick", "Snare", "Hi-Hat", "Tom"};
    private final int[] MIDI_NOTES = {36, 38, 42, 45};
    private final Color[] ROW_COLORS = {new Color(255, 80, 80), new Color(80, 180, 255), new Color(255, 210, 50), new Color(120, 220, 120)};
    private final Color BG = new Color(18, 18, 28);
    private final Color PANEL_BG = new Color(28, 28, 42);
    private final Color BTN_OFF = new Color(45, 45, 65);
    private final int STEPS = 16;
    private final JToggleButton[][] grid = new JToggleButton[4][STEPS];
    private final JLabel[] stepIndicators = new JLabel[STEPS];
    private Sequencer midiSequencer;
    private javax.swing.Timer metronomeTimer;
    private int tempo = 120;
    private int currentStep = -1;
    private int reverbLevel = 40;
    private int velocityLevel = 100;
    private JLabel statusLabel;

    public GUI() {
        setTitle("Beat Sequencer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildGrid(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
        pack();
        setMinimumSize(new Dimension(900, 430));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 90)));
        JLabel title = new JLabel("BEAT SEQUENCER");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        JTextField nameField = new JTextField("New Pattern", 12);
        styleTextField(nameField);
        JSpinner tempoSpinner = new JSpinner(new SpinnerNumberModel(120, 40, 240, 1));
        tempoSpinner.setPreferredSize(new Dimension(70, 30));
        ((JSpinner.DefaultEditor) tempoSpinner.getEditor()).getTextField().setBackground(new Color(40, 40, 60));
        ((JSpinner.DefaultEditor) tempoSpinner.getEditor()).getTextField().setForeground(Color.WHITE);
        tempoSpinner.addChangeListener(e -> {
            tempo = (int) tempoSpinner.getValue();
            if (metronomeTimer != null && metronomeTimer.isRunning())
                metronomeTimer.setDelay(60000 / tempo / 4);
        });
        JButton saveBtn = styledButton("Save", new Color(80, 180, 100));
        JButton loadBtn = styledButton("Load", new Color(80, 130, 200));
        saveBtn.addActionListener(e -> {
            try { tempo = (int) tempoSpinner.getValue(); savePattern(nameField.getText(), tempo); }
            catch (Exception ex) { setStatus("Error: " + ex.getMessage(), Color.RED); }
        });
        loadBtn.addActionListener(e -> {
            try { String id = JOptionPane.showInputDialog(this, "Enter pattern ID:"); if (id != null) loadPattern(Integer.parseInt(id)); }
            catch (Exception ex) { setStatus("Error: " + ex.getMessage(), Color.RED); }
        });
        JLabel tempoLabel = new JLabel("BPM:");
        tempoLabel.setForeground(new Color(160, 160, 200));
        panel.add(title);
        JSlider reverbSlider = new JSlider(0, 127, 40);
        reverbSlider.setPreferredSize(new java.awt.Dimension(80, 30));
        reverbSlider.setBackground(PANEL_BG);
        reverbSlider.addChangeListener(e -> reverbLevel = reverbSlider.getValue());
        JLabel reverbLabel = new JLabel("Reverb:");
        reverbLabel.setForeground(new Color(160, 160, 200));
        JSlider velSlider = new JSlider(0, 127, 100);
        velSlider.setPreferredSize(new java.awt.Dimension(80, 30));
        velSlider.setBackground(PANEL_BG);
        velSlider.addChangeListener(e -> velocityLevel = velSlider.getValue());
        JLabel velLabel = new JLabel("Velocity:");
        velLabel.setForeground(new Color(160, 160, 200));
        panel.add(reverbLabel);
        panel.add(reverbSlider);
        panel.add(velLabel);
        panel.add(velSlider);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(nameField);
        panel.add(tempoLabel);
        panel.add(tempoSpinner);
        panel.add(saveBtn);
        panel.add(loadBtn);
        return panel;
    }

    JPanel buildGrid() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBackground(BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        JPanel indicatorRow = new JPanel(new GridLayout(1, STEPS + 1, 4, 0));
        indicatorRow.setBackground(BG);
        indicatorRow.add(new JLabel());
        for (int col = 0; col < STEPS; col++) {
            JLabel dot = new JLabel("▼", SwingConstants.CENTER);
            dot.setForeground(new Color(50, 50, 70));
            dot.setFont(new Font("SansSerif", Font.PLAIN, 10));
            dot.setPreferredSize(new Dimension(44, 14));
            stepIndicators[col] = dot;
            indicatorRow.add(dot);
        }
        wrapper.add(indicatorRow, BorderLayout.NORTH);
        JPanel panel = new JPanel(new GridLayout(4, STEPS + 1, 4, 4));
        panel.setBackground(BG);
        for (int row = 0; row < 4; row++) {
            JLabel label = new JLabel(INSTRUMENTS[row]);
            label.setForeground(ROW_COLORS[row]);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setPreferredSize(new Dimension(55, 44));
            panel.add(label);
            for (int col = 0; col < STEPS; col++) {
                JToggleButton btn = new JToggleButton();
                btn.setPreferredSize(new Dimension(44, 44));
                btn.setOpaque(true);
                btn.setBorderPainted(true);
                btn.setFocusPainted(false);
                btn.setContentAreaFilled(true);
                btn.setBackground(BTN_OFF);
                btn.setBorder(col % 4 == 0
                    ? BorderFactory.createMatteBorder(1, 3, 1, 1, new Color(90, 90, 130))
                    : BorderFactory.createLineBorder(new Color(60, 60, 85), 1));
                final int r = row, c = col;
                btn.addItemListener(e -> {
                    if (btn.isSelected()) {
                        btn.setBackground(ROW_COLORS[r]);
                        btn.setBorder(BorderFactory.createLineBorder(ROW_COLORS[r].brighter(), 2));
                    } else {
                        btn.setBackground(BTN_OFF);
                        btn.setBorder(c % 4 == 0
                            ? BorderFactory.createMatteBorder(1, 3, 1, 1, new Color(90, 90, 130))
                            : BorderFactory.createLineBorder(new Color(60, 60, 85), 1));
                    }
                });
                grid[row][col] = btn;
                panel.add(btn);
            }
        }
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 90)));
        JButton playBtn = styledButton("▶  Play", new Color(60, 180, 100));
        JButton stopBtn = styledButton("■  Stop", new Color(200, 80, 80));
        JButton clearBtn = styledButton("Clear", new Color(100, 100, 130));
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(new Color(140, 140, 180));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        playBtn.addActionListener(e -> {
            try { playSequence(); setStatus("Playing...", new Color(100, 220, 100)); }
            catch (Exception ex) { setStatus("MIDI error: " + ex.getMessage(), Color.RED); }
        });
        stopBtn.addActionListener(e -> { stopSequence(); setStatus("Stopped", new Color(200, 100, 100)); });
        clearBtn.addActionListener(e -> {
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < STEPS; c++) { grid[r][c].setSelected(false); grid[r][c].setBackground(BTN_OFF); }
            setStatus("Cleared", new Color(140, 140, 180));
        });
        panel.add(playBtn);
        panel.add(stopBtn);
        panel.add(clearBtn);
        JSlider reverbSlider = new JSlider(0, 127, 40);
        reverbSlider.setPreferredSize(new java.awt.Dimension(80, 30));
        reverbSlider.setBackground(PANEL_BG);
        reverbSlider.addChangeListener(e -> reverbLevel = reverbSlider.getValue());
        JLabel reverbLabel = new JLabel("Reverb:");
        reverbLabel.setForeground(new Color(160, 160, 200));
        JSlider velSlider = new JSlider(0, 127, 100);
        velSlider.setPreferredSize(new java.awt.Dimension(80, 30));
        velSlider.setBackground(PANEL_BG);
        velSlider.addChangeListener(e -> velocityLevel = velSlider.getValue());
        JLabel velLabel = new JLabel("Velocity:");
        velLabel.setForeground(new Color(160, 160, 200));
        panel.add(reverbLabel);
        panel.add(reverbSlider);
        panel.add(velLabel);
        panel.add(velSlider);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(statusLabel);
        return panel;
    }

    void advanceStep() {
        if (currentStep >= 0) stepIndicators[currentStep].setForeground(new Color(50, 50, 70));
        currentStep = (currentStep + 1) % STEPS;
        stepIndicators[currentStep].setForeground(Color.WHITE);
    }

    void stopSequence() {
        if (midiSequencer != null && midiSequencer.isRunning()) midiSequencer.stop();
        if (metronomeTimer != null) metronomeTimer.stop();
        for (JLabel ind : stepIndicators) ind.setForeground(new Color(50, 50, 70));
        currentStep = -1;
    }

    void playSequence() throws Exception {
        stopSequence();
        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();
        midiSequencer = MidiSystem.getSequencer(false);
        midiSequencer.getTransmitter().setReceiver(synth.getReceiver());
        midiSequencer.open();
        Sequence sequence = new Sequence(Sequence.PPQ, 4);
        Track track = sequence.createTrack();
        for (int step = 0; step < STEPS; step++) {
            for (int row = 0; row < 4; row++) {
                if (grid[row][step].isSelected()) {
                    ShortMessage reverb = new ShortMessage(ShortMessage.CONTROL_CHANGE, 9, 91, reverbLevel);
                    track.add(new MidiEvent(reverb, 0L));
                    ShortMessage on = new ShortMessage(ShortMessage.NOTE_ON, 9, MIDI_NOTES[row], velocityLevel);
                    ShortMessage off = new ShortMessage(ShortMessage.NOTE_OFF, 9, MIDI_NOTES[row], 0);
                    track.add(new MidiEvent(on, step * 4L));
                    track.add(new MidiEvent(off, step * 4L + 2));
                }
            }
        }
        midiSequencer.setSequence(sequence);
        midiSequencer.setTempoInBPM(tempo);
        midiSequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        midiSequencer.start();
        int delay = 60000 / tempo / 4;
        metronomeTimer = new javax.swing.Timer(delay, e -> advanceStep());
        metronomeTimer.start();
    }

    void setStatus(String msg, Color color) { statusLabel.setText(msg); statusLabel.setForeground(color); }

    void savePattern(String name, int tempo) throws Exception {
        String json = "{\"name\":\"" + name + "\",\"tempo\":" + tempo + "}";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/patterns"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        setStatus("Saved! " + res.body(), new Color(100, 220, 100));
    }

    void loadPattern(int id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/patterns/" + id)).GET().build();
        client.send(req, HttpResponse.BodyHandlers.ofString());
        setStatus("Loaded pattern " + id, new Color(100, 180, 255));
    }

    void styleTextField(JTextField f) {
        f.setBackground(new Color(40, 40, 60));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 120), 1),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
    }

    JButton styledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(GUI::new);
    }
}
// append marker - do not use
