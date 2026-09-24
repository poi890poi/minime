package dev.minime.core;

import java.util.regex.Pattern;

/** Immutable expressions; every call owns a fresh Matcher. */
final class InputValidators {
    private InputValidators() {}
    private static final Pattern PINYIN=Pattern.compile("[a-zv]+(?:'[a-zv]+)*");
    private static final Pattern ENGLISH_WORD=Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)*");
    private static final Pattern ENGLISH_BARE=Pattern.compile("[A-Za-z]+");
    private static final Pattern ENGLISH_PREFIX=Pattern.compile("[A-Za-z]+(?:'[A-Za-z]*)?");
    private static final Pattern ENGLISH_CORRECTION=Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)?");
    static boolean pinyin(String raw) {return PINYIN.matcher(raw).matches();}
    static boolean englishWord(String raw) {return ENGLISH_WORD.matcher(raw).matches();}
    static boolean englishBare(String raw) {return ENGLISH_BARE.matcher(raw).matches();}
    static boolean englishPrefix(String raw) {return ENGLISH_PREFIX.matcher(raw).matches();}
    static boolean englishCorrection(String raw) {return ENGLISH_CORRECTION.matcher(raw).matches();}
}
