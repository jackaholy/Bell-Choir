package src;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Conductor implements Runnable {

    private final Thread thread;
    private final AudioFormat af;
    private final SourceDataLine line;
    private final Map<Note, Member> members = new HashMap<>();
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
            // Tell the conductor to play the song.
            conductor.playSong(song);
            conductor.stopMembers();
        } catch (LineUnavailableException e) {
            System.out.println("Line unavailable");
        }
    }

    public Conductor(AudioFormat af) throws LineUnavailableException {
        thread = new Thread(this, "Conductor");
        this.af = af;
        this.line = AudioSystem.getSourceDataLine(af); // Initialize the line
    }

    private void assignParts() {
        // Assign the notes to each member.
        for (BellNote note : song) {
            members.put(note.getNote(), new Member(note, line)); // Assign the note to the member
            System.out.println("Assigned note: " + note.getNote() + " to member: " + members.get(note.getNote()));
        }

        if (members.isEmpty()) {
            System.err.println("No members available to play notes.");
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
        assignParts();

        for (BellNote note : song) {
            Member member = members.get(note.getNote());
            if (member == null) {
                System.err.println("No member assigned to play: " + note);
            }
            Thread memberThread = new Thread(member);
            
            synchronized (this) {
                // Ensure all members finish playing
                try {
                    memberThread.join();
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted: " + e.getMessage());
                }
            }
        }
        thread.start();
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

    private void startMembers() {
        for (Member member : members.values()) {
            member.startMember();
            System.out.println("Member " + member + " started.");
        }
    }

    private void stopMembers() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted: " + e.getMessage());
        }
        for (Member member : members.values()) {
            member.stopMember();
            System.out.println("Member " + member + " stopped.");
        }
        line.drain();
        line.close();
    }

    @Override
    public void run() {
        startMembers();
        System.out.println("Members started.");
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open(af);
            line.start();
            // Assigns the right member to each note
            for (BellNote note : song) {
                final Note noteToPlay = note.getNote();
                Member member = members.get(noteToPlay);

                if (member == null) {
                    System.err.println("No member assigned to play: " + note);
                    continue;
                }

                synchronized (this) {
                    System.out.println("Playing note: " + note);
                    member.giveTurn();
                }
            }

            line.drain();
        } catch (LineUnavailableException e) {
            System.err.println("Error in Conductor: " + e.getMessage());
        }
    }

}
