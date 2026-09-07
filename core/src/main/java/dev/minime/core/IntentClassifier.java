package dev.minime.core;

import java.util.*;

public final class IntentClassifier {
    public enum Intent { CHINESE_PHONETIC, LATIN_LITERAL, AMBIGUOUS, URL_EMAIL, CODE_IDENTIFIER, NUMBER_ALNUM }
    public Intent classify(String raw, boolean zhuyin, boolean literalField, PhoneticDictionary dictionary) {
        if (literalField) return Intent.LATIN_LITERAL;
        if (raw.contains("@") || raw.contains("://")) return Intent.URL_EMAIL;
        if (raw.codePoints().anyMatch(Character::isDigit)) return Intent.NUMBER_ALNUM;
        if (raw.matches(".*[_/\\\\.:#=+{}\\[\\]();<>$%&*?!,\"-].*")) return Intent.CODE_IDENTIFIER;
        if (raw.codePoints().anyMatch(IntentClassifier::isZhuyin)) return Intent.CHINESE_PHONETIC;
        if (zhuyin || raw.codePoints().anyMatch(Character::isUpperCase)) return Intent.LATIN_LITERAL;
        if (dictionary == null) return Intent.LATIN_LITERAL;
        boolean legal = dictionary.legalPinyin(raw);
        boolean english = dictionary.isEnglish(raw);
        if (english && legal) return Intent.AMBIGUOUS;
        if (english || !legal) return Intent.LATIN_LITERAL;
        return dictionary.hasCompletePinyin(raw) ? Intent.CHINESE_PHONETIC : Intent.LATIN_LITERAL;
    }
    public static boolean isZhuyin(int c) { return (c >= 0x3105 && c <= 0x3129) || "ˊˇˋ˙ˉ".indexOf(c) >= 0; }
}
