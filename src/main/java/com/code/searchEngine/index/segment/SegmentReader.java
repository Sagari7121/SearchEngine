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
    private Map<UUID, Integer> docLengths = Map.of();
    private double avgDocLength = 0.0;
    private long totalTokens = 0;


    public SegmentReader(Path segmentDir){
        this.segmentDir = segmentDir;
        this.segmentId = SegmentFiles.parseId(segmentDir)
                .orElseThrow(() -> new IllegalArgumentException("Not a segment directory: " + segmentDir));
    }

    public void load() throws IOException{
        int expectedDocCount = readMeta();
        this.docLengths = readDocLengths();
        this.termIndex = readTerms();
        this.totalTokens = docLengths.values().stream().mapToLong(Integer::longValue).sum();

        if(docLengths.size() != expectedDocCount){
            throw new IOException("Corrupt segment " + segmentDir + ": meta says " + expectedDocCount
                    + " docs but doclengths has " + docLengths.size());
        }
        this.avgDocLength = docLengths.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private int readMeta() throws IOException{
        int version = -1;
        int docCount = -1;

        try(BufferedReader r = Files.newBufferedReader(segmentDir.resolve(SegmentFiles.META_FILE))){
            String line;
            while((line = r.readLine()) != null){
                String[] kv = line.split("=", 2);
                if(kv.length != 2) continue;
                switch (kv[0]){
                    case "formatVersion" -> version = Integer.parseInt(kv[1].trim());
                    case "docCount" -> docCount = Integer.parseInt(kv[1].trim());
                    default -> {
                    }
                }
            }

            if(version != SegmentFiles.FORMAT_VERSION){
                throw new IOException("Unsupported segment format version " + version + " in " + segmentDir);
            }
            if(docCount < 0){
                throw new IOException("Missing docCount in " + segmentDir);
            }
            return docCount;
        }
    }

    private Map<UUID, Integer> readDocLengths() throws IOException{
        Map<UUID, Integer> result = new HashMap<>();
        try (BufferedReader r = Files.newBufferedReader(segmentDir.resolve(SegmentFiles.DOC_LENGTHS_FILE))){
            String line;
            int lineNo = 0;
            while((line = r.readLine())!= null){
                lineNo++;
                if(line.isEmpty()) continue;
                String[] parts = line.split("\\|", 2);
                if(parts.length != 2){
                    throw new IOException("Corrupt " + SegmentFiles.DOC_LENGTHS_FILE + " line " + lineNo + " in " + segmentDir);
                }
                result.put(UUID.fromString(parts[0]), Integer.parseInt(parts[1].trim()));
            }
        }
        return result;
    }

    private Map<String, List<SegmentPosting>> readTerms() throws IOException{
        Map<String, List<SegmentPosting>> result = new HashMap<>();

        try(BufferedReader r = Files.newBufferedReader(segmentDir.resolve(SegmentFiles.TERMS_FILES))){
            String line;
            int lineNo=0;
            while((line =r.readLine()) != null){
                lineNo++;
                if(line.isEmpty()) continue;

                String[] parts = line.split("\\|", 4);
                if(parts.length != 4){
                    throw new IOException("Corrupt " + SegmentFiles.TERMS_FILES + " line " + lineNo + " in " + segmentDir);
                }
                int[] positions = parts[3].isEmpty()
                        ? new int[0]
                        : Arrays.stream(parts[3].split(",")).mapToInt(Integer::parseInt).toArray();

                result.computeIfAbsent(parts[0], t -> new ArrayList<>())
                        .add(new SegmentPosting(UUID.fromString(parts[1]), Integer.parseInt(parts[2]), positions));
            }
        }
        return result;
    }

    public List<SegmentPosting> getPostings(String term) {
        return termIndex.getOrDefault(term, List.of());
    }

    public int docFrequency(String term){
        return getPostings(term).size();
    }

    public int docCount(){
        return docLengths.size();
    }

    public double averageDocLength() {
        return avgDocLength;
    }

    public int docLength(UUID docId) {
        return docLengths.getOrDefault(docId, 0);
    }

    public Set<String> allTerms() {
        return Collections.unmodifiableSet(termIndex.keySet());
    }

    public Set<UUID> allDocIds() {
        return Collections.unmodifiableSet(docLengths.keySet());
    }

    public long totalTokens() {
        return totalTokens;
    }

    public Path getSegmentDir() {
        return segmentDir;
    }

    public long getSegmentId() {
        return segmentId;
    }
}
