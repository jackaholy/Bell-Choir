package src;

/**
 * Enum representing musical notes with their corresponding frequencies.
 * Includes functionality to generate audio samples for each note.
 */
public enum Note {
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

    // Audio configuration constants
    public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz sampling rate
    public static final int MEASURE_LENGTH_SEC = 1; // Duration of one measure in seconds

    // Circumference of a circle divided by # of samples
    private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

    // Audio generation parameters
    private final double FREQUENCY_A_HZ = 440.0d; // Reference frequency (A4)
    private final double MAX_VOLUME = 127.0d; // Maximum amplitude for 8-bit audio

    // Sample data for this note (pre-generated in constructor)
    private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

    /**
     * Constructor generates the audio sample for each note.
     * For non-REST notes, creates a sinusoidal wave of the appropriate frequency.
     */
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
    /**
     * Gets the audio sample data for this note.
     * For REST notes, returns silence (zero-filled array).
     *
     * @return byte array containing the audio samples
     */
    public byte[] sample() {
        return sinSample;
    }
}
