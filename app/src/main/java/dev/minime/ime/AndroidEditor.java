package dev.minime.ime;

import android.view.KeyEvent;
import android.view.inputmethod.*;
import dev.minime.core.CompositionEngine;
import java.util.function.Supplier;

/** The only boundary that mutates an Android editor. It never reads surrounding text. */
final class AndroidEditor implements CompositionEngine.Editor {
    private final Supplier<InputConnection> connection;
    private final Supplier<EditorInfo> info;
    private final SelectionState selection;
    AndroidEditor(Supplier<InputConnection> connection, Supplier<EditorInfo> info) { this(connection,info,new SelectionState()); }
    AndroidEditor(Supplier<InputConnection> connection, Supplier<EditorInfo> info,SelectionState selection) {
        this.connection=connection; this.info=info; this.selection=selection;
    }
    public void composing(String text) { InputConnection c=connection.get(); if(c!=null) { selection.write(text,true); c.setComposingText(text,1); } }
    public void commit(String text) {
        InputConnection c=connection.get(); if(c==null) return;
        selection.write(text,false);
        c.beginBatchEdit(); try { c.commitText(text,1); } finally { c.endBatchEdit(); }
    }
    public void finish() { selection.finish(); InputConnection c=connection.get(); if(c!=null) c.finishComposingText(); }
    public void delete() {
        InputConnection c=connection.get(); if(c==null) return;
        selection.delete();
        // Delegate selected-range/grapheme deletion to the editor without collecting its content.
        c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DEL));
        c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DEL));
    }
    public void enter() {
        InputConnection c=connection.get(); EditorInfo i=info.get(); if(c==null || i==null) return;
        int action=EditorPolicy.action(i);
        if(action!=EditorInfo.IME_ACTION_NONE && action!=EditorInfo.IME_ACTION_UNSPECIFIED && c.performEditorAction(action)) return;
        c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
        c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));
    }
}
