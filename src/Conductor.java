package src;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The Conductor class orchestrates the playing of a musical composition.
 * It manages multiple Member threads (each representing a musical note) and
 * synchronizes their playback through a shared audio line.
 */
public class Conductor implements Runnable {

    // The conductor's control thread
    private final Thread thread;

    // Audio format configuration
    private final AudioFormat af;

    // Shared audio output line
    private final SourceDataLine line;

    // Maps notes to their player threads
    private final Map<Note, Member> members = new HashMap<>();

    // The musical composition to be played
    private static List<BellNote> song;

    /**
     * Main entry point for the application. Loads a song file and initiates playback.
     *
     * @param args Command line arguments (expects a single filename argument)
     */
    public static void main(String[] args) {
        // Handle no length for a note
        if (args.length == 0) {
            System.out.println("Usage: java src.Tone <sngFile.txt>");
            return;
        }
        // Handle no notes loaded
        song = loadNotes(args[0]);
        if (song.isEmpty()) {
            System.out.println("No notes loaded. Exiting.");
            return;
        }

        try {
            final AudioFormat af =
                    new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
            Conductor conductor = new Conductor(af);
            // Tell the conductor to play the song.
            conductor.playSong(song);
            // Tell the conductor to stop the members from playing.
            conductor.stopMembers();
        } catch (LineUnavailableException e) {
            System.out.println("Line unavailable");
        }
    }

    /**
     * Constructs a Conductor with the specified audio format.
     *
     * @param af The audio format configuration for playback
     * @throws LineUnavailableException if the audio line cannot be opened
     */
    public Conductor(AudioFormat af) throws LineUnavailableException {
        thread = new Thread(this, "Conductor");
        this.af = af;
        this.line = AudioSystem.getSourceDataLine(af); // Initialize the line
    }

    /**
     * Assigns each unique note in the song to a dedicated Member thread.
     * Creates a one-to-one mapping between notes and players.
     */
    private void assignParts() {
        // Assign the notes to each member.
        for (BellNote note : song) {
            members.put(note.getNote(), new Member(note, line)); // Assign the note to the member
            System.out.println("Assigned note: " + note.getNote() + " to member: " + members.get(note.getNote()));
        }
        // Catch if there's no members to play notes.
        if (members.isEmpty()) {
            System.err.println("No members available to play notes.");
        }
    }

    /**
     * Loads musical notes from a text file and converts them to BellNote objects.
     *
     * @param filename Path to the song definition file
     * @return List of BellNote objects representing the song
     */
    private static List<BellNote> loadNotes(String filename) {
        final File file = new File(filename);
        final List<BellNote> notes = new ArrayList<>();

        // Check if file exists before attempting to read
        if (file.exists()) {
            // Use try-with-resources to ensure Scanner is closed automatically
            try (Scanner scanner = new Scanner(file)) {

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();

                    // Skip empty lines
                    if (line.isEmpty()) {
                        continue;
                    }

                    // Split line into components (note and length)
                    String[] token = line.split(" ");

                    // Skip lines without exactly 2 tokens
                    if (token.length != 2) {
                        System.err.printf("Invalid format on line '%s' (expected 'NOTE LENGTH')", line);
                        continue;
                    }

                    /*
                    This try-catch loop is created with help from chat.deepseek.com.
                    I asked it to give me some edge cases and error handling
                     */
                    try {
                        // Convert first token to Note enum
                        Note noteEnum = Note.valueOf(token[0]);
                        // Convert second token to NoteLength enum
                        NoteLength lengthEnum = parseNoteLength(token[1]);

                        // Create BellNote object and add to list
                        BellNote note = new BellNote(noteEnum, lengthEnum);
                        notes.add(note);

                    } catch (IllegalArgumentException e) {
                        System.err.println("Skipping invalid note definition: " + line);
                        // Continue with next line instead of failing entire load
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading file " + filename);
            }
        } else {
            System.err.println("File " + filename + " not found");
        }
        return notes;
    }

    /**
     * Initiates playback of the musical composition.
     *
     * @param song The list of BellNotes to be played
     * @throws LineUnavailableException if the audio line cannot be opened
     */
    void playSong(List<BellNote> song) throws LineUnavailableException {
        // Assign each member to note
        assignParts();

        // Start all member threads
        for (Member member : members.values()) {
            new Thread(member, "Member").start();
        }

        synchronized (this) {
            // Ensure all members finish playing
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }

        // Start the conductor thread
        thread.start();
    }

    /**
     * Converts numeric string representation to NoteLength enum values.
     *
     * @param numStr String representation of note length (1, 2, 4, or 8)
     * @return Corresponding NoteLength enum value
     * @throws IllegalArgumentException if the input is not a valid note length
     */
    static NoteLength parseNoteLength(String numStr) {
        // Convert each note number into a note length
        int num = Integer.parseInt(numStr);
        // Figure out the desired note length
        return switch (num) {
            case 1 -> NoteLength.WHOLE;
            case 2 -> NoteLength.HALF;
            case 4 -> NoteLength.QUARTER;
            case 8 -> NoteLength.EIGHTH;
            default -> throw new IllegalArgumentException("Invalid note length number: " + num);
        };
    }

    /**
     * Signals all Member threads to begin playback.
     */
    private void startMembers() {
        for (Member member : members.values()) {
            member.startMember();
            System.out.println("Member " + member + " started.");
        }
    }

    /**
     * Stops all Member threads and cleans up audio resources.
     * Ensures proper shutdown by draining and closing the audio line.
     */
    private void stopMembers() {
        try {
            // Wait for the conductor thread to finish processing
            thread.join();
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted: " + e.getMessage());
        }
        // Stop each member thread in the orchestra
        for (Member member : members.values()) {
            member.stopMember();
            System.out.println("Member " + member + " stopped.");
        }
        line.drain();
        line.close();
    }

    /**
     * The conductor's main control loop. Coordinates note playback by:
     * 1. Opening the audio line
     * 2. Starting all members
     * 3. Sequentially triggering each note's playback
     * 4. Stopping all the members
     */
    @Override
    public void run() {
        try {
            // Look at the next line and try starting a member thread.
            line.open(af);
            line.start();
            startMembers();
            /*
            The order of this for loop is created with help from chat.deepseek.com.
            I asked it to help me fix a previously not working loop.
            */
            // Give each member a note to play.
            for (BellNote note : song) {
                Member member = members.get(note.getNote());
                // Handle if no members threads are created yet.
                if (member != null) {
                    synchronized (member) {
                        member.myTurn = true;
                        member.playNote(note.getLength());
                        member.notify();
                    }
                    // Add delay between notes if needed
                    Thread.sleep(note.getLength().timeMs() / 2);
                }
            }
        } catch (LineUnavailableException | InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        } finally {
            // Stop all the members in the choir.
            stopMembers();
        }
    }
}