package com.code.searchEngine.index.segment;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SegmentReader {

    private final Path segmentDir;
    private final long segmentId;

    private Map<String, List<SegmentPosting>> termIndex = Map.of();
    private Map<UUID, Integer> titleLengths = Map.of();
    private Map<UUID, Integer> textLengths = Map.of();
    private double avgTitleLength = 0.0;
    private double avgTextLength = 0.0;


    public SegmentReader(Path segmentDir){
        this.segmentDir = segmentDir;
        this.segmentId = SegmentFiles.parseId(segmentDir)
                .orElseThrow(() -> new IllegalArgumentException("Not a segment directory: " + segmentDir));
    }

    public void load() throws IOException {
        int expectedDocCount = readMeta();
        readDocLengths();
        readTerms();

        if (textLengths.size() != expectedDocCount) {
            throw new IOException("Corrupt segment " + segmentDir + ": meta says " + expectedDocCount
                    + " docs but doclengths has " + textLengths.size());
        }
        avgTitleLength = titleLengths.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
        avgTextLength = textLengths.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private int readMeta() throws IOException {
        int version = -1, docCount = -1;
        try (BufferedReader r = Files.newBufferedReader(segmentDir.resolve(SegmentFiles.META_FILE))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] kv = line.split("=", 2);
                if (kv.length != 2) continue;
                switch (kv[0]) {
                    case "formatVersion" -> version = Integer.parseInt(kv[1].trim());
                    case "docCount" -> docCount = Integer.parseInt(kv[1].trim());
                    default -> {}
                }
            }
        }
        if (version != SegmentFiles.FORMAT_VERSION) {
            throw new IOException("Unsupported segment format version " + version + " in " + segmentDir
                    + " (expected " + SegmentFiles.FORMAT_VERSION + " — old-format segments must be rebuilt)");
        }
        if (docCount < 0) throw new IOException("Missing docCount in " + segmentDir);
        return docCount;
    }

    private void readDocLengths() throws IOException{
        Map<UUID, Integer> titles = new HashMap<>();
        Map<UUID, Integer> texts = new HashMap<>();

        try (BufferedReader r = Files.newBufferedReader(segmentDir.resolve(SegmentFiles.DOC_LENGTHS_FILE))) {
            String line;
            int lineNo = 0;
            while((line = r.readLine())!= null){
                lineNo++;
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", 3);
                if (parts.length != 3) {
                    throw new IOException("Corrupt " + SegmentFiles.DOC_LENGTHS_FILE + " line " + lineNo + " in " + segmentDir);
                }
                UUID docId = UUID.fromString(parts[0]);
                titles.put(docId, Integer.parseInt(parts[1]));
                texts.put(docId, Integer.parseInt(parts[2]));
            }
        }
        this.titleLengths = titles;
        this.textLengths = texts;
    }

    private void readTerms() throws IOException {
        Map<String, List<SegmentPosting>> result = new HashMap<>();
        try (BufferedReader r = Files.newBufferedReader(segmentDir.resolve(SegmentFiles.TERMS_FILES))) {
            String line;
            int lineNo = 0;
            while ((line = r.readLine()) != null) {
                lineNo++;
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", 5);
                if (parts.length != 5) {
                    throw new IOException("Corrupt " + SegmentFiles.TERMS_FILES + " line " + lineNo + " in " + segmentDir);
                }
                int[] positions = parts[4].isEmpty()
                        ? new int[0]
                        : Arrays.stream(parts[4].split(",")).mapToInt(Integer::parseInt).toArray();

                result.computeIfAbsent(parts[0], t -> new ArrayList<>())
                        .add(new SegmentPosting(UUID.fromString(parts[1]),
                                Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), positions));
            }
        }
        this.termIndex = result;
    }

    public List<SegmentPosting> getPostings(String term) { return termIndex.getOrDefault(term, List.of()); }
    public int docFrequency(String term) { return getPostings(term).size(); }
    public int docCount() { return textLengths.size(); }
    public double averageTitleLength() { return avgTitleLength; }
    public double averageTextLength() { return avgTextLength; }
    public int titleLength(UUID docId) { return titleLengths.getOrDefault(docId, 0); }
    public int textLength(UUID docId) { return textLengths.getOrDefault(docId, 0); }
    public long totalTextTokens() { return textLengths.values().stream().mapToLong(Integer::longValue).sum(); }
    public Set<String> allTerms() { return Collections.unmodifiableSet(termIndex.keySet()); }
    public Set<UUID> allDocIds() { return Collections.unmodifiableSet(textLengths.keySet()); }
    public Path getSegmentDir() { return segmentDir; }
    public long getSegmentId() { return segmentId; }
}
