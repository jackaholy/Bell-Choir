package src;

/**
 * Enum representing musical note durations.
 * Converts musical timing notation (whole, half, etc.) to milliseconds.
 */
public enum NoteLength {
    WHOLE(1.0f),
    HALF(0.5f),
    QUARTER(0.25f),
    EIGHTH(0.125f);

    // Duration in milliseconds
    private final int timeMs;

    /**
     * Constructor converts musical duration to milliseconds.
     *
     * @param length The duration as a fraction of a whole note
     */
    NoteLength(float length) {
        timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
    }

    /**
     * Gets the duration of this note in milliseconds.
     *
     * @return duration in milliseconds
     */
    public int timeMs() {
        return timeMs;
    }
}
