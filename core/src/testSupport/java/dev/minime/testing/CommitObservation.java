package dev.minime.testing;

/** Test-only observation of a whole-token Space commit, not candidate correctness. */
public final class CommitObservation {
    private CommitObservation() {}
    public static boolean finished(int composingEnd,String editorText,String typed,String ownedRaw) {
        // TextWatcher can run inside Editor.commit, before the engine clears raw.
        // Same-text acceptance becomes observable once that owned spelling clears.
        return composingEnd<0 && (!editorText.equals(typed) || ownedRaw.isEmpty());
    }
}
