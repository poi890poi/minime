package dev.minime.ime;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

final class EditorPolicy {
    final boolean secure, privateField, literal, numeric;
    EditorPolicy(EditorInfo info) {
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        secure = (cls == InputType.TYPE_CLASS_TEXT && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
            || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
            || (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        numeric = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE || cls == InputType.TYPE_CLASS_DATETIME;
        privateField = secure || (info.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0;
        literal = secure || numeric || cls == InputType.TYPE_NULL || (cls == InputType.TYPE_CLASS_TEXT &&
            (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            || variation == InputType.TYPE_TEXT_VARIATION_URI || (info.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0));
    }
    static int action(EditorInfo info) {
        if ((info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return EditorInfo.IME_ACTION_NONE;
        if (info.actionLabel != null && info.actionId != 0) return info.actionId;
        return info.imeOptions & EditorInfo.IME_MASK_ACTION;
    }
    static String enterLabel(EditorInfo info) {
        if ((info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0 && info.actionLabel != null) return info.actionLabel.toString();
        switch (action(info)) {
            case EditorInfo.IME_ACTION_GO: return "Go";
            case EditorInfo.IME_ACTION_SEARCH: return "Search";
            case EditorInfo.IME_ACTION_SEND: return "Send";
            case EditorInfo.IME_ACTION_NEXT: return "Next";
            case EditorInfo.IME_ACTION_DONE: return "Done";
            case EditorInfo.IME_ACTION_PREVIOUS: return "Previous";
            default: return "↵";
        }
    }
}
