package src;

/**
 * The BellNote class represents a musical note paired with its duration (length).
 * It encapsulates both the pitch (Note) and timing (NoteLength) of a musical note.
 */
public class BellNote {

    // The musical note (pitch)
    final Note note;

    // The duration/length of the note
    final NoteLength length;

    /**
     * Constructs a BellNote with the specified note and length.
     *
     * @param note the musical note (pitch) to be played
     * @param length the duration/length of the note
     */
    BellNote(Note note, NoteLength length) {
        this.note = note;
        this.length = length;
    }

    /**
     * Gets the musical note (pitch) of this BellNote.
     *
     * @return the Note object representing the pitch
     */
    public Note getNote() {
        return note;
    }

    /**
     * Gets the duration/length of this BellNote.
     *
     * @return the NoteLength object representing the duration
     */
    public NoteLength getLength() {
        return length;
    }
}