package com.code.searchEngine.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class UrlNormalizer {

    private static final int MAX_URL_LENGTH = 255;
    private static final int MAX_PATH_DEPTH = 10;
    private static final int MAX_QUERY_PARAMS = 5;

    private static final Set<String> TRACKING_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "sessionid", "sid", "phpsessid", "jsessionid", "ref"
    );

    private static final Pattern SKIP_EXTENSIONS = Pattern.compile(
            ".*\\.(jpg|jpeg|png|gif|svg|webp|pdf|zip|gz|mp4|mp3|avi|css|js|ico|exe|dmg)$",
            Pattern.CASE_INSENSITIVE);

    /** Returns normalized URL, or null if the URL should be dropped. */
    public String normalize(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > MAX_URL_LENGTH) return null;
        try {
            URI uri = new URI(raw.trim()).normalize();
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) return null;
            if (uri.getHost() == null) return null;

            String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();
            if (SKIP_EXTENSIONS.matcher(path).matches()) return null;

            // depth + repeated segment check (/a/b/a/b/a/b is a classic trap)
            String[] segments = path.split("/");
            if (segments.length > MAX_PATH_DEPTH) return null;
            if (hasRepeatingSegments(segments)) return null;

            // strip tracking params, sort the rest so ?a=1&b=2 == ?b=2&a=1
            String query = null;
            if (uri.getQuery() != null) {
                List<String> params = Arrays.stream(uri.getQuery().split("&"))
                        .filter(p -> !TRACKING_PARAMS.contains(p.split("=")[0].toLowerCase()))
                        .sorted()
                        .toList();
                if (params.size() > MAX_QUERY_PARAMS) return null;
                if (!params.isEmpty()) query = String.join("&", params);
            }

            // drop fragment, lowercase host, drop default ports
            int port = uri.getPort();
            if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) port = -1;

            return new URI(scheme, null, uri.getHost().toLowerCase(), port, path, query, null).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasRepeatingSegments(String[] segments) {
        Map<String, Integer> counts = new HashMap<>();
        for (String s : segments) {
            if (s.isEmpty()) continue;
            if (counts.merge(s, 1, Integer::sum) > 2) return true;
        }
        return false;
    }
}