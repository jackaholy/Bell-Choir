package src;

import javax.sound.sampled.SourceDataLine;

public class Member implements Runnable {

    private final Thread thread;
    private final Note note;
    private final SourceDataLine line;
    private boolean isPlaying = false;
    boolean myTurn;

    // Constructor
    Member(BellNote bellNote, SourceDataLine line) {
        this.note = bellNote.note;
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

    void playNote(NoteLength length) {
        System.out.println("Playing note on audio line: " + note);
        final int ms = Math.min(length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int samples = Note.SAMPLE_RATE * ms / 1000;
        // Write the note samples.
        line.write(note.sample(), 0, samples);
        // Add a short rest after playing the note.
        line.write(Note.REST.sample(), 0, 50);
    }

    @Override
    public void run() {
        while (isPlaying) {
            synchronized (this) {
                while (!myTurn) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                myTurn = false;
            }
        }
    }
}