package src;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Member implements Runnable {

    private final Thread thread;
    private final Note note;
    private final BellNote bellNote;
    private final AudioFormat audioFormat;
    private boolean isPlaying = false;

    // Constructor
    Member(BellNote bellNote, AudioFormat af) {
        this.note = bellNote.note;
        this.bellNote = bellNote;
        this.audioFormat = af;
        thread = new Thread(this);
    }

    private void startMember() {
        thread.start();
        isPlaying = true;
    }

    private void stopMember() {
        isPlaying = false;
        synchronized (this) {
            this.notify();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println("Problem stopping member " + e);
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

    @Override
    public void run() {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat)) {
            {
                line.open(audioFormat);
                line.start();

                playNote(line, bellNote);

                line.drain();
                line.close();
            }
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable " + e);
            throw new RuntimeException(e);
        }
    }
}
