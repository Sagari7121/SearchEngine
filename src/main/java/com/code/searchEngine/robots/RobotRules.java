package com.code.searchEngine.robots;

import java.util.List;

public class RobotRules {

    private final List<String> disallowPatterns;
    private final List<String> allowPatterns;
    private final Integer crawlDelaySeconds;

    public RobotRules(List<String> disallowPatterns, List<String> allowPatterns, Integer crawlDelaySeconds) {
        this.disallowPatterns = disallowPatterns;
        this.allowPatterns = allowPatterns;
        this.crawlDelaySeconds = crawlDelaySeconds;
    }

    public static RobotRules allowAll() {
        return new RobotRules(List.of(), List.of(), null);
    }

    public boolean isAllowed(String path) {
        String bestDisallow = longestMatch(disallowPatterns, path);
        String bestAllow = longestMatch(allowPatterns, path);

        if (bestDisallow == null) return true;
        if (bestAllow == null) return false;
        return bestAllow.length() >= bestDisallow.length();
    }

    private String longestMatch(List<String> patterns, String path) {
        String best = null;
        for (String pattern : patterns) {
            if (matches(pattern, path) && (best == null || pattern.length() > best.length())) {
                best = pattern;
            }
        }
        return best;
    }

    private boolean matches(String pattern, String path) {
        if (pattern.isEmpty()) return false;
        if (pattern.endsWith("*")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return path.startsWith(pattern);
    }

    public Integer getCrawlDelaySeconds() {
        return crawlDelaySeconds;
    }
}