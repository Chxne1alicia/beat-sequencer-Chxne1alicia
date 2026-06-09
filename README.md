# Beat Sequencer

An interactive drum machine and beat sequencer built in Java. Create, save, and play rhythm patterns through three client interfaces backed by a REST API and SQLite database.

## Architecture

The project follows a multi-tier client-server architecture:

```
beat-sequencer/
├── server/     # Javalin REST API + SQLite database
├── cli/        # Command-line client
├── tui/        # Terminal UI client (Lanterna)
├── gui/        # Graphical UI client (Swing + MIDI playback)
└── tests/      # JUnit 5 tests
```

All three clients communicate with the server through the REST API. The server manages all pattern and beat data in a SQLite database.

## Features

- **GUI** — Dark themed beat grid with per-instrument color coding, metronome step indicator, MIDI playback, BPM control, reverb/velocity/swing knobs, per-instrument volume and pan sliders, dynamic instrument rows (up to 8), and a patterns menu for saving and loading
- **CLI** — Create, list, get, and delete patterns from the terminal
- **TUI** — Interactive terminal menu for browsing and managing patterns
- **REST API** — Full CRUD endpoints for patterns and beats
- **Logging** — Request and error logging via Logback (console + file)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| REST Server | Javalin 6 |
| Database | SQLite (via JDBC) |
| GUI | Java Swing |
| MIDI Playback | javax.sound.midi |
| TUI | Lanterna 3 |
| JSON | Jackson |
| Logging | SLF4J + Logback |
| Testing | JUnit 5 |
| Build | Maven |

## Database Schema

```sql
instruments (id, name, midi_note)
patterns    (id, name, tempo)
beats       (id, pattern_id, instrument_id, step, active)
```

## REST API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | /patterns | List all patterns |
| POST | /patterns | Create a pattern |
| GET | /patterns/{id} | Get a pattern |
| PUT | /patterns/{id} | Update a pattern |
| DELETE | /patterns/{id} | Delete a pattern |
| GET | /patterns/{id}/beats | Get beat grid |
| PUT | /patterns/{id}/beats | Update beat grid |

## Setup

**Requirements:** Java 21+, Maven 3.9+

```bash
git clone https://github.com/Chxne1alicia/beat-sequencer-Chxne1alicia
cd beat-sequencer-Chxne1alicia
```

## Running

Start the server first (required for all clients):
```bash
mvn exec:java -pl server
```

Then in a separate terminal, run any client:
```bash
# GUI
mvn exec:java -pl gui

# CLI
mvn exec:java -pl cli

# TUI
mvn exec:java -pl tui
```

## Running Tests

```bash
mvn install -pl . -N
mvn install -pl server
mvn test -pl tests
```

## GUI Guide

- **Toggle beats** by clicking the grid squares
- **Play/Stop** to start and stop MIDI playback
- **BPM +/-** to adjust tempo
- **Drag knobs** up/down to adjust Reverb, Velocity, and Swing
- **Volume/Pan sliders** on each instrument row
- **Instrument dropdown** to change the drum sound per row
- **+ Add Instrument** to add up to 8 instrument rows
- **Save** to save the current pattern and beat grid to the database
- **Patterns ▾** to load a previously saved pattern

## CLI Commands

```
list      — list all patterns
create    — create a new pattern
get       — get a pattern by ID
delete    — delete a pattern by ID
quit      — exit
```

## Logs

Server logs are written to `server/logs/app.log` and the console.
