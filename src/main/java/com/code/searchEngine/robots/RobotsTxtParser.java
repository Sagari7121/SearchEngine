package com.code.searchEngine.robots;

import java.util.ArrayList;
import java.util.List;

public class RobotsTxtParser {

    public static RobotRules parse(String content, String botUserAgent){
        if (content == null || content.isBlank()) return RobotRules.allowAll();

        List<String> disallow = new ArrayList<>();
        List<String> allow = new ArrayList<>();
        Integer crawlDelay = null;

        boolean inWildcardGroup = false;
        boolean inOurGroup = false;
        boolean sawOurGroup = false;

        for(String rawLine: content.split("\n")) {
            String line = stripComment(rawLine).trim();
            if(line.isEmpty()) continue;

            int colon = line.indexOf(':');
            if (colon < 0) continue;

            String key = line.substring(0, colon).trim().toLowerCase();
            String value = line.substring(colon+1).trim();

            switch (key){
                case "user-agent" -> {
                    boolean matchesUs = value.equalsIgnoreCase(botUserAgent);
                    boolean isWildcard = value.equals("*");

                    if(matchesUs){
                        inOurGroup = true;
                        sawOurGroup = true;
                        inWildcardGroup = false;
                    }
                    else if(isWildcard && !sawOurGroup){
                        inWildcardGroup = true;
                        inOurGroup = false;
                    } else {
                        inOurGroup = false;
                        inWildcardGroup = false;
                    }
                }
                case "disallow" -> {
                    if ((inOurGroup || inWildcardGroup) && !value.isEmpty()) disallow.add(value);
                }
                case "allow" -> {
                    if ((inOurGroup || inWildcardGroup) && !value.isEmpty()) allow.add(value);
                }
                case "crawl-delay" -> {
                    if (inOurGroup || inWildcardGroup) {
                        try {
                            crawlDelay = (int) Double.parseDouble(value); // some sites use decimals like "0.5"
                        } catch (NumberFormatException ignored) {}
                    }
                }
                default -> {}
            }
        }
        return new RobotRules(disallow, allow, crawlDelay);
    }

    private static String stripComment(String text){
        int hash = text.indexOf('#');
        return hash >= 0 ? text.substring(0, hash) : text;
    }
}
