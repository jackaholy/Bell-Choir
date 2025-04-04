package src;

import javax.sound.sampled.SourceDataLine;

/**
 * The Member class represents a thread that plays a musical note.
 * It implements Runnable to allow concurrent execution of note playing.
 */
public class Member implements Runnable {

    // Thread for playing the note
    private final Thread thread;

    // The musical note to be played
    private final Note note;

    // Audio line for playing the note
    private final SourceDataLine line;

    // Flag to control the playing state
    private boolean isPlaying = false;

    // Flag to indicate if it's this member's turn to play
    boolean myTurn;

    /**
     * Constructs a Member instance with the specified BellNote and audio line.
     *
     * @param bellNote the BellNote containing the note to be played
     * @param line the audio line used to play the note
     */
    Member(BellNote bellNote, SourceDataLine line) {
        this.note = bellNote.note;
        this.line = line;
        thread = new Thread(this, "Bell Note: " + note);
    }

    /**
     * Starts the member's thread and sets the playing state to true.
     * Notifies any waiting threads that the member is ready to play.
     */
    public synchronized void startMember() {
        isPlaying = true;
        notify();
    }

    /**
     * Stops the member's thread by setting the playing state to false
     * and interrupting the thread.
     */
    public synchronized void stopMember() {
        isPlaying = false;
        thread.interrupt();
    }

    /**
     * Plays the note for the specified length of time.
     * The note is played on the audio line, followed by a short rest.
     *
     * @param length the length of the note to be played
     */
    void playNote(NoteLength length) {
        System.out.println("Playing note on audio line: " + note);
        final int ms = Math.min(length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int samples = Note.SAMPLE_RATE * ms / 1000;
        // Write the note samples.
        line.write(note.sample(), 0, samples);
        // Add a short rest after playing the note.
        line.write(Note.REST.sample(), 0, 50);
    }

    /**
     * The run method defines the behavior of the member's thread.
     * It waits for its turn to play and continues while isPlaying is true.
     */
    @Override
    public void run() {
        while (isPlaying) {
            // Make sure members aren't playing over one another
            synchronized (this) {
                // If it's not your turn, wait
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