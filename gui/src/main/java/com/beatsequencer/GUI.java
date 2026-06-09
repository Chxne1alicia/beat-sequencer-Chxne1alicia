package com.beatsequencer;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;

public class GUI extends JFrame {
    private static final String BASE_URL = "http://localhost:7001";
    private static final HttpClient client = HttpClient.newHttpClient();

    private final String[] INSTRUMENTS = {"Kick", "Snare", "Hi-Hat", "Tom"};
    private final int[] MIDI_NOTES = {36, 38, 42, 45};
    private final int STEPS = 16;
    private final JToggleButton[][] grid = new JToggleButton[4][STEPS];

    private Sequencer midiSequencer;
    private int tempo = 120;
    private int currentPatternId = -1;

    public GUI() {
        setTitle("Beat Sequencer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildGrid(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        JTextField nameField = new JTextField("New Pattern", 12);
        JSpinner tempoSpinner = new JSpinner(new SpinnerNumberModel(120, 40, 240, 1));
        JButton saveBtn = new JButton("Save Pattern");
        JButton loadBtn = new JButton("Load Pattern");

        saveBtn.addActionListener(e -> {
            try {
                tempo = (int) tempoSpinner.getValue();
                savePattern(nameField.getText(), tempo);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        loadBtn.addActionListener(e -> {
            try {
                String id = JOptionPane.showInputDialog(this, "Enter pattern ID:");
                if (id != null) loadPattern(Integer.parseInt(id));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Tempo:"));
        panel.add(tempoSpinner);
        panel.add(saveBtn);
        panel.add(loadBtn);
        return panel;
    }

    JPanel buildGrid() {
        JPanel panel = new JPanel(new GridLayout(4, STEPS + 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int row = 0; row < 4; row++) {
            JLabel label = new JLabel(INSTRUMENTS[row], SwingConstants.RIGHT);
            label.setPreferredSize(new Dimension(60, 30));
            panel.add(label);
            for (int col = 0; col < STEPS; col++) {
                JToggleButton btn = new JToggleButton();
                btn.setPreferredSize(new Dimension(40, 40));
                btn.setBackground(Color.DARK_GRAY);
                btn.addItemListener(e -> {
                    btn.setBackground(btn.isSelected() ? Color.GREEN : Color.DARK_GRAY);
                });
                grid[row][col] = btn;
                panel.add(btn);
            }
        }
        return panel;
    }

    JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        JButton playBtn = new JButton("▶ Play");
        JButton stopBtn = new JButton("■ Stop");
        JButton clearBtn = new JButton("Clear");

        playBtn.addActionListener(e -> {
            try { playSequence(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "MIDI error: " + ex.getMessage()); }
        });

        stopBtn.addActionListener(e -> {
            if (midiSequencer != null && midiSequencer.isRunning()) midiSequencer.stop();
        });

        clearBtn.addActionListener(e -> {
            for (JToggleButton[] row : grid)
                for (JToggleButton btn : row) {
                    btn.setSelected(false);
                    btn.setBackground(Color.DARK_GRAY);
                }
        });

        panel.add(playBtn);
        panel.add(stopBtn);
        panel.add(clearBtn);
        return panel;
    }

    void playSequence() throws Exception {
        if (midiSequencer != null && midiSequencer.isRunning()) midiSequencer.stop();

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
                    ShortMessage on = new ShortMessage(ShortMessage.NOTE_ON, 9, MIDI_NOTES[row], 100);
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
    }

    void savePattern(String name, int tempo) throws Exception {
        String json = "{\"name\":\"" + name + "\",\"tempo\":" + tempo + "}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        JOptionPane.showMessageDialog(this, "Saved! " + res.body());
    }

    void loadPattern(int id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns/" + id))
            .GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        currentPatternId = id;
        JOptionPane.showMessageDialog(this, "Loaded: " + res.body());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}
