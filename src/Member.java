package src;

import javax.sound.sampled.SourceDataLine;

public class Member implements Runnable {

    private final Thread thread;
    private final Note note;
    private final SourceDataLine line;
    private BellNote bellNote;
    private boolean isPlaying = false;
    private boolean myTurn;

    // Constructor
    Member(BellNote bellNote, SourceDataLine line) {
        this.note = bellNote.note;
        this.bellNote = bellNote;
        this.line = line;
        thread = new Thread(this, "Bell Note: " + note);
    }

    public synchronized void startMember() {
        isPlaying = true;
        notify();
    }

    public synchronized void stopMember() {
        isPlaying = false;
    }

    public void addNote(BellNote note, Member member) {
        this.bellNote = note;
    }

    public void giveTurn() {
        synchronized (this) {
            if (myTurn) {
                throw new IllegalStateException("Attempt to give a turn to a player who's hasn't completed the current turn");
            }
            myTurn = true;
            notify();
            while (myTurn) {
                try {
                    wait();
                } catch (InterruptedException ignored) {}
            }
        }
    }

    void playNote() {
        final int ms = Math.min(bellNote.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        // Write the note samples.
        line.write(bellNote.note.sample(), 0, length);
        // Add a short rest after playing the note.
        line.write(Note.REST.sample(), 0, 50);
    }

    @Override
    public void run() {
        isPlaying = true;
        synchronized (this) {
            while (isPlaying) {
                // Wait for my turn
                while (!myTurn) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {}
                }

                // My turn!
                playNote();

                // Done, complete turn and wakeup the waiting process
                myTurn = false;
                notify();
            }
        }
    }
}
