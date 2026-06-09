package com.beatsequencer;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.*;

public class GUI extends JFrame {
    private static final String BASE_URL = "http://localhost:7001";
    private static final HttpClient client = HttpClient.newHttpClient();

    private final String[] INSTRUMENTS = {"KICK", "SNARE", "HI-HAT", "TOM"};
    private final int[] MIDI_NOTES = {36, 38, 42, 45};
    private final Color[] ROW_COLORS = {
        new Color(224, 85, 85),
        new Color(85, 136, 224),
        new Color(224, 192, 85),
        new Color(85, 192, 112)
    };
    private final Color APP_BG     = new Color(15, 15, 20);
    private final Color BAR_BG     = new Color(26, 26, 36);
    private final Color STEP_OFF   = new Color(26, 26, 36);
    private final Color BORDER_COL = new Color(42, 42, 58);

    private final int STEPS = 16;
    private final JPanel[][] stepPanels = new JPanel[4][STEPS];
    private final boolean[][] active = new boolean[4][STEPS];
    private final JLabel[] indicators = new JLabel[STEPS];

    private Sequencer midiSequencer;
    private javax.swing.Timer metronomeTimer;
    private int tempo = 120;
    private int currentStep = -1;
    private int reverbLevel = 40;
    private int velocityLevel = 100;
    private final int[] volumeLevels = {100, 100, 100, 100};

    private JLabel statusLabel;
    private JLabel bpmDisplay;

    public GUI() {
        setTitle("Beat Sequencer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(APP_BG);
        setLayout(new BorderLayout(0, 0));
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildGrid(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
        pack();
        setMinimumSize(new Dimension(860, 380));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setBackground(BAR_BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));

        JLabel brand = new JLabel("BEAT SEQ");
        brand.setForeground(new Color(200, 200, 240));
        brand.setFont(new Font("SansSerif", Font.BOLD, 13));

        bar.add(brand);
        bar.add(separator());
        JButton patternsBtn = actionBtn("Patterns ▾", new Color(42, 42, 58));
        patternsBtn.addActionListener(e -> showPatternsMenu(patternsBtn));
        bar.add(patternsBtn);
        bar.add(separator());

        JLabel bpmLabel = smallLabel("BPM");
        bpmDisplay = new JLabel("120");
        bpmDisplay.setForeground(new Color(200, 255, 110));
        bpmDisplay.setFont(new Font("Monospaced", Font.BOLD, 14));
        bpmDisplay.setOpaque(true);
        bpmDisplay.setBackground(APP_BG);
        bpmDisplay.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),
            BorderFactory.createEmptyBorder(3, 10, 3, 10)));

        JButton bpmUp = tinyBtn("+");
        JButton bpmDown = tinyBtn("-");
        bpmUp.addActionListener(e -> changeTempo(5));
        bpmDown.addActionListener(e -> changeTempo(-5));

        bar.add(bpmLabel);
        bar.add(bpmDown);
        bar.add(bpmDisplay);
        bar.add(bpmUp);
        bar.add(separator());
        JButton patternsBtn = actionBtn("Patterns ▾", new Color(42, 42, 58));
        patternsBtn.addActionListener(e -> showPatternsMenu(patternsBtn));
        bar.add(patternsBtn);
        bar.add(separator());

        JButton playBtn = actionBtn("▶  Play", new Color(45, 122, 58));
        JButton stopBtn = actionBtn("■  Stop", new Color(122, 45, 45));
        JButton clearBtn = actionBtn("Clear", new Color(42, 42, 58));
        playBtn.addActionListener(e -> { try { playSequence(); setStatus("Playing", new Color(200, 255, 110)); } catch (Exception ex) { setStatus("Error: " + ex.getMessage(), Color.RED); } });
        stopBtn.addActionListener(e -> { stopSequence(); setStatus("Stopped", new Color(180, 100, 100)); });
        clearBtn.addActionListener(e -> clearGrid());

        bar.add(playBtn);
        bar.add(stopBtn);
        bar.add(clearBtn);
        bar.add(separator());
        JButton patternsBtn = actionBtn("Patterns ▾", new Color(42, 42, 58));
        patternsBtn.addActionListener(e -> showPatternsMenu(patternsBtn));
        bar.add(patternsBtn);
        bar.add(separator());

        JTextField nameField = new JTextField("Groove 01", 10);
        styleInput(nameField);
        JButton saveBtn = actionBtn("Save", new Color(30, 58, 92));
        JButton loadBtn = actionBtn("Load", new Color(42, 42, 58));
        saveBtn.addActionListener(e -> { try { savePattern(nameField.getText()); } catch (Exception ex) { setStatus("Save failed", Color.RED); } });
        loadBtn.addActionListener(e -> { try { String id = JOptionPane.showInputDialog(this, "Pattern ID:"); if (id != null) loadPattern(Integer.parseInt(id)); } catch (Exception ex) { setStatus("Load failed", Color.RED); } });

        bar.add(nameField);
        bar.add(saveBtn);
        bar.add(loadBtn);

        return bar;
    }

    JPanel buildGrid() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBackground(APP_BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 14));

        JPanel indRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        indRow.setBackground(APP_BG);
        indRow.add(spacer(60, 12));
        for (int i = 0; i < STEPS; i++) {
            if (i > 0 && i % 4 == 0) indRow.add(spacer(4, 12));
            JLabel ind = new JLabel("▼", SwingConstants.CENTER);
            ind.setForeground(new Color(40, 40, 60));
            ind.setFont(new Font("SansSerif", Font.PLAIN, 9));
            ind.setPreferredSize(new Dimension(36, 12));
            indicators[i] = ind;
            indRow.add(ind);
            if (i < STEPS - 1) indRow.add(spacer(3, 12));
        }
        wrapper.add(indRow, BorderLayout.NORTH);

        JPanel grid = new JPanel();
        grid.setBackground(APP_BG);
        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));

        for (int row = 0; row < 4; row++) {
            JPanel track = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            track.setBackground(APP_BG);

            JLabel lbl = new JLabel(INSTRUMENTS[row], SwingConstants.RIGHT);
            lbl.setForeground(ROW_COLORS[row]);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setPreferredSize(new Dimension(56, 36));
            track.add(lbl);
            track.add(spacer(4, 36));

            for (int col = 0; col < STEPS; col++) {
                if (col > 0 && col % 4 == 0) track.add(spacer(4, 36));
                final int r = row, c = col;
                JPanel step = new JPanel();
                step.setPreferredSize(new Dimension(36, 36));
                step.setOpaque(true);
                step.setBackground(STEP_OFF);
                step.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
                step.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                step.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        active[r][c] = !active[r][c];
                        step.setBackground(active[r][c] ? ROW_COLORS[r] : STEP_OFF);
                        step.setBorder(BorderFactory.createLineBorder(
                            active[r][c] ? ROW_COLORS[r].brighter() : BORDER_COL, 1));
                    }
                });
                stepPanels[row][col] = step;
                track.add(step);
                if (col < STEPS - 1) track.add(spacer(3, 36));
            }
            final int rowFinal = row;
            JSlider vol = new JSlider(JSlider.HORIZONTAL, 0, 127, 100);
            vol.setPreferredSize(new Dimension(60, 36));
            vol.setBackground(APP_BG);
            vol.addChangeListener(e -> volumeLevels[rowFinal] = vol.getValue());
            track.add(spacer(6, 36));
            track.add(vol);
            grid.add(track);
            if (row < 3) grid.add(spacer(860, 5));
        }
        wrapper.add(grid, BorderLayout.CENTER);
        return wrapper;
    }

    JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        bar.setBackground(BAR_BG);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));

        bar.add(knobGroup("Reverb", new Color(85, 136, 224), 40, v -> reverbLevel = v));
        bar.add(knobGroup("Velocity", new Color(224, 192, 85), 100, v -> velocityLevel = v));
        bar.add(knobGroup("Swing", new Color(85, 192, 112), 0, v -> {}));

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(new Color(80, 80, 100));
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setBackground(BAR_BG);
        statusPanel.add(statusLabel);

        bar.add(Box.createHorizontalStrut(20));
        bar.add(statusLabel);
        return bar;
    }

    JPanel knobGroup(String name, Color color, int initial, java.util.function.IntConsumer onChange) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setBackground(BAR_BG);

        JLabel lbl = new JLabel(name.toUpperCase());
        lbl.setForeground(new Color(80, 80, 100));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        KnobPanel knob = new KnobPanel(color, initial, onChange);
        knob.setAlignmentX(Component.CENTER_ALIGNMENT);

        group.add(lbl);
        group.add(Box.createVerticalStrut(4));
        group.add(knob);
        return group;
    }

    void changeTempo(int delta) {
        tempo = Math.max(40, Math.min(240, tempo + delta));
        bpmDisplay.setText(String.valueOf(tempo));
        if (metronomeTimer != null && metronomeTimer.isRunning())
            metronomeTimer.setDelay(60000 / tempo / 4);
    }

    void clearGrid() {
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < STEPS; c++) {
                active[r][c] = false;
                stepPanels[r][c].setBackground(STEP_OFF);
                stepPanels[r][c].setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
            }
        setStatus("Cleared", new Color(80, 80, 100));
    }

    void advanceStep() {
        if (currentStep >= 0) indicators[currentStep].setForeground(new Color(40, 40, 60));
        currentStep = (currentStep + 1) % STEPS;
        indicators[currentStep].setForeground(new Color(200, 255, 110));
    }

    void stopSequence() {
        if (midiSequencer != null && midiSequencer.isRunning()) midiSequencer.stop();
        if (metronomeTimer != null) metronomeTimer.stop();
        for (JLabel ind : indicators) ind.setForeground(new Color(40, 40, 60));
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
                if (active[row][step]) {
                    ShortMessage reverb = new ShortMessage(ShortMessage.CONTROL_CHANGE, 9, 91, reverbLevel);
                    track.add(new MidiEvent(reverb, 0L));
                    int vel = (int)(velocityLevel * (volumeLevels[row] / 127.0));
                    ShortMessage on = new ShortMessage(ShortMessage.NOTE_ON, 9, MIDI_NOTES[row], Math.max(1, vel));
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
        metronomeTimer = new javax.swing.Timer(60000 / tempo / 4, e -> advanceStep());
        metronomeTimer.start();
    }

    void savePattern(String name) throws Exception {
        stopSequence();
        String json = "{\"name\":\"" + name + "\",\"tempo\":" + tempo + "}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        String respBody = res.body();
        int savedId = -1;
        try {
            String idStr = respBody.replaceAll(".*"id":(\d+).*", "$1");
            savedId = Integer.parseInt(idStr);
        } catch (Exception ignored) {}
        if (savedId > 0) saveBeats(savedId);
        setStatus("Saved pattern " + savedId, new Color(130, 220, 130));
    }

    void loadPattern(int id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns/" + id)).GET().build();
        client.send(req, HttpResponse.BodyHandlers.ofString());
        setStatus("Loaded pattern " + id, new Color(110, 170, 255));
    }

    void setStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    JPanel separator() {
        JPanel s = new JPanel();
        s.setPreferredSize(new Dimension(1, 20));
        s.setBackground(BORDER_COL);
        return s;
    }

    Component spacer(int w, int h) {
        JPanel s = new JPanel();
        s.setPreferredSize(new Dimension(w, h));
        s.setOpaque(false);
        return s;
    }

    JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(90, 90, 110));
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        return l;
    }

    JButton actionBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(new Color(200, 200, 220));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return btn;
    }

    JButton tinyBtn(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(22, 22));
        btn.setBackground(new Color(42, 42, 58));
        btn.setForeground(new Color(160, 160, 200));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    void saveBeats(int patternId) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (int row = 0; row < 4; row++) {
            for (int step = 0; step < STEPS; step++) {
                if (!first) sb.append(",");
                sb.append("{"instrument_id":").append(row + 1)
                  .append(","step":").append(step)
                  .append(","active":").append(active[row][step]).append("}");
                first = false;
            }
        }
        sb.append("]");
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns/" + patternId + "/beats"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(sb.toString())).build();
        client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    void loadBeats(int patternId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns/" + patternId + "/beats")).GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        clearGrid();
        String body = res.body();
        String[] entries = body.replaceAll("[\[\]{}]", "").split(",(?=\s*"instrument_id")");
        for (String entry : entries) {
            try {
                int instrId = Integer.parseInt(entry.replaceAll(".*"instrument_id":(\d+).*", "$1").trim()) - 1;
                int step = Integer.parseInt(entry.replaceAll(".*"step":(\d+).*", "$1").trim());
                boolean isActive = entry.contains(""active":true");
                if (instrId >= 0 void showPatternsMenu(JButton anchor) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(26, 26, 36));
        menu.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/patterns")).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = res.body();
            if (body.equals("[]")) {
                JMenuItem empty = new JMenuItem("No patterns saved");
                empty.setBackground(new Color(26, 26, 36));
                empty.setForeground(new Color(100, 100, 120));
                menu.add(empty);
            } else {
                String[] items = body.split("\},\{");
                for (String item : items) {
                    try {
                        int id = Integer.parseInt(item.replaceAll(".*"id":(\d+).*", "$1").trim());
                        String name = item.replaceAll(".*"name":"([^"]+)".*", "$1").trim();
                        int t = Integer.parseInt(item.replaceAll(".*"tempo":(\d+).*", "$1").trim());
                        JMenuItem mi = new JMenuItem(id + "  " + name + "  (" + t + " BPM)");
                        mi.setBackground(new Color(26, 26, 36));
                        mi.setForeground(new Color(200, 200, 220));
                        mi.setFont(new Font("Monospaced", Font.PLAIN, 12));
                        mi.addActionListener(ev -> {
                            try {
                                tempo = t;
                                bpmDisplay.setText(String.valueOf(t));
                                loadBeats(id);
                                setStatus("Loaded: " + name, new Color(110, 170, 255));
                            } catch (Exception ex) { setStatus("Load failed", Color.RED); }
                        });
                        menu.add(mi);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ex) {
            JMenuItem err = new JMenuItem("Could not connect to server");
            err.setForeground(Color.RED);
            menu.add(err);
        }
        menu.show(anchor, 0, anchor.getHeight());
    }

    void styleInput(JTextField f) {void styleInput(JTextField f) { instrId < 4 void styleInput(JTextField f) {void styleInput(JTextField f) { step >= 0 void styleInput(JTextField f) {void styleInput(JTextField f) { step < STEPS) {
                    active[instrId][step] = isActive;
                    stepPanels[instrId][step].setBackground(isActive ? ROW_COLORS[instrId] : STEP_OFF);
                    stepPanels[instrId][step].setBorder(BorderFactory.createLineBorder(
                        isActive ? ROW_COLORS[instrId].brighter() : BORDER_COL, 1));
                }
            } catch (Exception ignored) {}
        }
    }

    void showPatternsMenu(JButton anchor) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(26, 26, 36));
        menu.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/patterns")).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = res.body();
            if (body.equals("[]")) {
                JMenuItem empty = new JMenuItem("No patterns saved");
                empty.setBackground(new Color(26, 26, 36));
                empty.setForeground(new Color(100, 100, 120));
                menu.add(empty);
            } else {
                String[] items = body.split("\},\{");
                for (String item : items) {
                    try {
                        int id = Integer.parseInt(item.replaceAll(".*"id":(\d+).*", "$1").trim());
                        String name = item.replaceAll(".*"name":"([^"]+)".*", "$1").trim();
                        int t = Integer.parseInt(item.replaceAll(".*"tempo":(\d+).*", "$1").trim());
                        JMenuItem mi = new JMenuItem(id + "  " + name + "  (" + t + " BPM)");
                        mi.setBackground(new Color(26, 26, 36));
                        mi.setForeground(new Color(200, 200, 220));
                        mi.setFont(new Font("Monospaced", Font.PLAIN, 12));
                        mi.addActionListener(ev -> {
                            try {
                                tempo = t;
                                bpmDisplay.setText(String.valueOf(t));
                                loadBeats(id);
                                setStatus("Loaded: " + name, new Color(110, 170, 255));
                            } catch (Exception ex) { setStatus("Load failed", Color.RED); }
                        });
                        menu.add(mi);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ex) {
            JMenuItem err = new JMenuItem("Could not connect to server");
            err.setForeground(Color.RED);
            menu.add(err);
        }
        menu.show(anchor, 0, anchor.getHeight());
    }

    void styleInput(JTextField f) {
        f.setBackground(APP_BG);
        f.setForeground(new Color(200, 200, 220));
        f.setCaretColor(Color.WHITE);
        f.setFont(new Font("SansSerif", Font.PLAIN, 12));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    static class KnobPanel extends JPanel {
        private int value;
        private final Color color;
        private final java.util.function.IntConsumer onChange;
        private int lastY;

        KnobPanel(Color color, int initial, java.util.function.IntConsumer onChange) {
            this.color = color;
            this.value = initial;
            this.onChange = onChange;
            setPreferredSize(new Dimension(36, 36));
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { lastY = e.getY(); }
            });
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    value = Math.max(0, Math.min(127, value + (lastY - e.getY())));
                    lastY = e.getY();
                    onChange.accept(value);
                    repaint();
                }
            });
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2, cy = getHeight() / 2, r = 14;
            g2.setColor(new Color(26, 26, 36));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            g2.setColor(new Color(42, 42, 58));
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(cx - r + 2, cy - r + 2, (r - 2) * 2, (r - 2) * 2, 220, -260);
            g2.setColor(color);
            int sweep = (int) (value / 127.0 * 260);
            g2.drawArc(cx - r + 2, cy - r + 2, (r - 2) * 2, (r - 2) * 2, 220, -sweep);
            double angle = Math.toRadians(220 - sweep);
            int dx = (int) (Math.cos(angle) * (r - 5));
            int dy = (int) (-Math.sin(angle) * (r - 5));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(cx, cy, cx + dx, cy + dy);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(GUI::new);
    }
}
