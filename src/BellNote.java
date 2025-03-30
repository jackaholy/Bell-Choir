package src;

/**
 * Represents a musical note with a specified length.
 */
public class BellNote {
    final Note note;
    final NoteLength length;

    BellNote(Note note, NoteLength length) {
        this.note = note;
        this.length = length;
    }
}