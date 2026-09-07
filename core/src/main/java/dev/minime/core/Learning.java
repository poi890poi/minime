package dev.minime.core;

import java.util.*;

/** Stores only explicit choices; caller controls persistence and secure-field isolation. */
public interface Learning {
    int count(String context, String raw, String choice);
    void choose(String context, String raw, String choice);
    default List<Candidate> custom(String raw) { return Collections.emptyList(); }
    Learning NONE = new Learning() {
        public int count(String c, String r, String v) { return 0; }
        public void choose(String c, String r, String v) { }
    };
}
