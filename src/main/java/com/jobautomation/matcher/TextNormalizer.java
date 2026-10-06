package com.jobautomation.matcher;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Text normalisation shared by the matcher and the question engine. */
public final class TextNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern HTML_ENTITY = Pattern.compile("&(nbsp|amp|lt|gt|quot|#39|#160);");

    private TextNormalizer() {
    }

    /**
     * Lower-case, strip accents and HTML, unify punctuation to spaces but keep the characters
     * that are part of technology names ({@code + # . -}), collapse whitespace.
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String t = HTML_TAG.matcher(text).replaceAll(" ");
        t = HTML_ENTITY.matcher(t).replaceAll(" ");
        t = Normalizer.normalize(t, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        t = t.toLowerCase(Locale.ROOT);
        t = t.replace('–', '-').replace('—', '-').replace(' ', ' ');
        t = t.replaceAll("[\\r\\n\\t]", " ");
        t = t.replaceAll("[,;:/()\\[\\]{}|\"'“”‘’!?*_=<>~`]", " ");
        t = t.replaceAll("(?<=\\s)-(?=\\s)", " ");
        t = WHITESPACE.matcher(t).replaceAll(" ").trim();
        return t;
    }

    /** Canonical skill name via the taxonomy (e.g. "SpringBoot" -> "spring boot"). */
    public static String canonicalSkill(String skill) {
        return SkillTaxonomy.canonical(skill);
    }

    /** Word-boundary aware containment for a phrase in normalised text. */
    public static boolean containsPhrase(String normalizedText, String phrase) {
        if (normalizedText == null || phrase == null || phrase.isBlank()) {
            return false;
        }
        return SkillTaxonomy.phrasePattern(normalize(phrase)).matcher(normalizedText).find();
    }
}
