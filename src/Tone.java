package src;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Tone {

    private static List<BellNote> song;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java src.Tone <sngFile.txt>");
            return;
        }

        song = loadNotes(args[0]);
        if (song.isEmpty()) {
            System.out.println("No notes loaded. Exiting.");
            return;
        }

        try {
            final AudioFormat af =
                    new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
            Tone t = new Tone(af);
            t.playSong(song);
        } catch (LineUnavailableException e) {
            System.out.println("Line unavailable");
        }
    }

    private static List<BellNote> loadNotes(String filename) {
        final File file = new File(filename);
        final List<BellNote> notes = new ArrayList<>();
        if (file.exists()) {
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] token = line.split(" ");

                    Note noteEnum = Note.valueOf(token[0]);
                    NoteLength lengthEnum = parseNoteLength(token[1]);

                    BellNote note = new BellNote(noteEnum, lengthEnum);
                    notes.add(note);
                }
            } catch (IOException e) {
                System.out.println("Error reading file " + filename);
            }
        } else {
            System.err.println("File " + filename + " not found");
        }
        return notes;
    }

    private static NoteLength parseNoteLength(String numStr) {
        int num = Integer.parseInt(numStr);
        return switch (num) {
            case 1 -> NoteLength.WHOLE;
            case 2 -> NoteLength.HALF;
            case 4 -> NoteLength.QUARTER;
            case 8 -> NoteLength.EIGHTH;
            default -> throw new IllegalArgumentException("Invalid note length number: " + num);
        };
    }

    private final AudioFormat af;

    Tone(AudioFormat af) {
        this.af = af;
    }

    void playSong(List<BellNote> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            for (BellNote bn: song) {
                Thread noteThread = new Thread(() -> playNote(line, bn));
                noteThread.start();
                try {
                    noteThread.join();
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted" + e.getMessage());
                }
            }
            line.drain();
        }
    }

    private void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        // Write the note samples.
        line.write(bn.note.sample(), 0, length);
        // Add a short rest after playing the note.
        line.write(Note.REST.sample(), 0, 50);
    }
}

class BellNote {
    final Note note;
    final NoteLength length;

    BellNote(Note note, NoteLength length) {
        this.note = note;
        this.length = length;
    }
}

enum NoteLength {
    WHOLE(1.0f),
    HALF(0.5f),
    QUARTER(0.25f),
    EIGHTH(0.125f);

    private final int timeMs;

    private NoteLength(float length) {
        timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
    }

    public int timeMs() {
        return timeMs;
    }
}

enum Note {
    // REST Must be the first 'src.Note'
    REST,
    A4,
    A4S,
    B4,
    C4,
    C4S,
    D4,
    D4S,
    E4,
    F4,
    F4S,
    G4,
    G4S,
    A5;

    public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
    public static final int MEASURE_LENGTH_SEC = 1;

    // Circumference of a circle divided by # of samples
    private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

    private final double FREQUENCY_A_HZ = 440.0d;
    private final double MAX_VOLUME = 127.0d;

    private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

    private Note() {
        int n = this.ordinal();
        if (n > 0) {
            // Calculate the frequency!
            final double halfStepUpFromA = n - 1;
            final double exp = halfStepUpFromA / 12.0d;
            final double freq = FREQUENCY_A_HZ * Math.pow(2.0d, exp);

            // Create sinusoidal data sample for the desired frequency
            final double sinStep = freq * step_alpha;
            for (int i = 0; i < sinSample.length; i++) {
                sinSample[i] = (byte)(Math.sin(i * sinStep) * MAX_VOLUME);
            }
        }
    }

    public byte[] sample() {
        return sinSample;
    }
}