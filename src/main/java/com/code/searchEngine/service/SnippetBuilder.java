package com.code.searchEngine.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SnippetBuilder {

    private SnippetBuilder(){}

    public static String build(String text, Set<String> queryTerms){
        if(text == null || text.isEmpty()) return "";

        String[] words = text.split("\\s+");
        int hitIndex = -1;
        for(int i=0;i<words.length;i++){
            String cleaned = words[i].toLowerCase().replaceAll("[^a-z0-9]", "");
            if (queryTerms.contains(cleaned)) {
                hitIndex = i;
                break;
            }
        }

        List<String> all = Arrays.asList(words);
        if (hitIndex == -1) return String.join(" ", all.subList(0, Math.min(20, words.length)));

        int start = Math.max(0, hitIndex - 8);
        int end = Math.min(words.length, hitIndex + 12);
        return "..." + String.join(" ", all.subList(start, end)) + "...";
    }
}
