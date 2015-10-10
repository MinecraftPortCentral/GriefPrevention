package me.ryanhamshire.GriefPrevention;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WordFinder {

    private Pattern pattern;

    WordFinder(List<String> wordsToFind) {
        StringBuilder patternBuilder = new StringBuilder();
        for (String word : wordsToFind) {
            patternBuilder.append("|(([^\\w]|^)" + Pattern.quote(word) + "([^\\w]|$))");
        }

        String patternString = patternBuilder.toString();
        if (patternString.length() > 1) {
            // trim extraneous leading pipe (|)
            patternString = patternString.substring(1);
        }

        this.pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    boolean hasMatch(String input) {
        Matcher matcher = this.pattern.matcher(input);
        return matcher.find();
    }
}
