package dev.minime.core;

import java.util.*;
import java.util.regex.Pattern;

public final class IntentClassifier {
    private static final Pattern CODE=Pattern.compile(".*[_/\\\\.:#=+{}\\[\\]();<>$%&*?!,\"-].*");
    // Domain vocabulary for mixed technical typing, not Chinese ranking overrides.
    private static final Set<String> COMMANDS=new HashSet<>(Arrays.asList(
        "adb","git","ssh","scp","sftp","curl","wget","npm","npx","pnpm","yarn","pip","gradle",
        "bash","zsh","cmd","pwsh","sudo","chmod","chown","mkdir","rmdir","grep","awk","sed","cd","ls","pwd"));
    public static boolean technicalWord(String raw) { return COMMANDS.contains(raw); }
    public enum Intent { CHINESE_PHONETIC, LATIN_LITERAL, AMBIGUOUS, URL_EMAIL, CODE_IDENTIFIER, NUMBER_ALNUM }
    public Intent classify(String raw, boolean zhuyin, boolean literalField, PhoneticDictionary dictionary) {
        return classify(raw,zhuyin,literalField,dictionary,false);
    }
    public Intent classify(String raw, boolean zhuyin, boolean literalField, PhoneticDictionary dictionary,boolean latinContext) {
        return classify(raw,zhuyin,literalField,dictionary,latinContext,true);
    }
    public Intent classify(String raw, boolean zhuyin, boolean literalField, PhoneticDictionary dictionary,boolean latinContext,boolean chineseEnabled) {
        if (literalField) return Intent.LATIN_LITERAL;
        if (technicalWord(raw)) return Intent.CODE_IDENTIFIER;
        if (raw.contains("@") || raw.contains("://")) return Intent.URL_EMAIL;
        if (raw.codePoints().anyMatch(Character::isDigit)) return Intent.NUMBER_ALNUM;
        if (CODE.matcher(raw).matches()) return Intent.CODE_IDENTIFIER;
        if (raw.codePoints().anyMatch(IntentClassifier::isZhuyin)) return Intent.CHINESE_PHONETIC;
        if (zhuyin || raw.codePoints().anyMatch(Character::isUpperCase)) return Intent.LATIN_LITERAL;
        if (dictionary == null) return Intent.LATIN_LITERAL;
        boolean english = dictionary.isEnglish(raw,latinContext);
        // Without Chinese, non-English phonetics always resolve to literal intent.
        // Keep the legality check for English ambiguity; it remains observable.
        if(!chineseEnabled && !english)return Intent.LATIN_LITERAL;
        boolean legal = dictionary.legalPinyin(raw);
        if (english && legal) return Intent.AMBIGUOUS;
        if (english || !legal) return Intent.LATIN_LITERAL;
        return dictionary.hasCompletePinyin(raw) ? Intent.CHINESE_PHONETIC : Intent.LATIN_LITERAL;
    }
    public static boolean isZhuyin(int c) { return (c >= 0x3105 && c <= 0x3129) || "ˊˇˋ˙ˉ".indexOf(c) >= 0; }
}
