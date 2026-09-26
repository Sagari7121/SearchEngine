package com.code.searchEngine.utils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Analyzer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]+");

    public List<String> tokenize(String text){
        if(text == null || text.isBlank()) return List.of();

        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()){
            tokens.add(matcher.group());
        }
        return tokens;
    }
}
