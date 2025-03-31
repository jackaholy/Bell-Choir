package src;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Conductor implements Runnable {

    private final Thread thread;
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
            Conductor conductor = new Conductor(af);
            conductor.playSong(song);
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

    void playSong(List<BellNote> song) throws LineUnavailableException {
        List<Thread> members = new ArrayList<>();
        for (BellNote bn : song) {
            Member member = new Member(bn, af);
            Thread memberThread = new Thread(member);
            members.add(memberThread);
            memberThread.start();
        }

        // Ensure all members finish playing
        for (Thread memberThread : members) {
            try {
                memberThread.join();
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }
    }

    private final AudioFormat af;

    /**
     * Constructs a Conductor with the specified audio format.
     *
     * @param af The audio format to use for playback
     */
    public Conductor(AudioFormat af) {
        thread = new Thread(this, "Conductor");
        this.af = af;
    }

    static NoteLength parseNoteLength(String numStr) {
        int num = Integer.parseInt(numStr);
        return switch (num) {
            case 1 -> NoteLength.WHOLE;
            case 2 -> NoteLength.HALF;
            case 4 -> NoteLength.QUARTER;
            case 8 -> NoteLength.EIGHTH;
            default -> throw new IllegalArgumentException("Invalid note length number: " + num);
        };
    }

    /**
     * Starts playing the current song.
     */
    public void playSong() {
        thread.start();
    }

    @Override
    public void run() {
        List<Thread> members = new ArrayList<>();
        for (BellNote bn : song) {
            Member member = new Member(bn, af);
            Thread memberThread = new Thread(member);
            members.add(memberThread);
            memberThread.start();
        }

        // Ensure all members finish playing
        for (Thread memberThread : members) {
            try {
                memberThread.join();
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }
    }
}
