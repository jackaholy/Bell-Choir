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
        thread.interrupt();
    }

    public void addNote(BellNote note, Member member) {
        this.bellNote = note;
    }

    public void giveTurn() {
        synchronized (this) {
            while (myTurn) {
                try {
                    wait();
                } catch (InterruptedException ignored) {
                }
            }
            System.out.println("Giving a turn to " + this);
            myTurn = true;
            notify();
        }
    }

    void playNote() {
        System.out.println("Playing note on audio line: " + bellNote.note);
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
                System.out.println(Thread.currentThread().getName() + " is waiting for a turn.");
                while (!myTurn) {
                    try {
                        wait(); // Wait for my turn
                    } catch (InterruptedException ignored) {
                    }
                }

                System.out.println(Thread.currentThread().getName() + " is playing note: " + bellNote);
                // Play the note of whichever members turn it is
                playNote();

                // Done, complete turn and wakeup the waiting process
                myTurn = false;
                notify();
            }
        }
    }
}