package com.code.searchEngine.index.segment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public final class SegmentWriter {

    public static Path write(Path indexRoot, long segmentId, SegmentData data) throws IOException{
        Path tmpDir = indexRoot.resolve(SegmentFiles.tmpDirName(segmentId));
        Path finalDir = indexRoot.resolve(SegmentFiles.dirName(segmentId));

        if(Files.exists(finalDir)){
            throw new IOException("Segment already exists: " + finalDir);
        }

        SegmentFiles.deleteRecursively(tmpDir);
        Files.createDirectories(tmpDir);

        try{
            writeTerms(tmpDir, data.terms());
            writeDocLengths(tmpDir, data.docLengths());
            writeMeta(tmpDir, data.docLengths().size());
            Files.move(tmpDir, finalDir, StandardCopyOption.ATOMIC_MOVE);
        }catch (IOException | RuntimeException e){
            SegmentFiles.deleteRecursively(tmpDir);
            throw e;
        }
        return finalDir;
    }

    private static void writeTerms(Path dir, Map<String, List<SegmentPosting>> terms) throws IOException {
        try(BufferedWriter w = Files.newBufferedWriter(dir.resolve(SegmentFiles.TERMS_FILES))) {
            for(Map.Entry<String, List<SegmentPosting>> entry: new TreeMap<>(terms).entrySet()){
                for(SegmentPosting p: entry.getValue()){
                    String positions = Arrays.stream(p.positions())
                            .mapToObj(String::valueOf)
                            .collect(Collectors.joining(","));

                    w.write(entry.getKey());
                    w.write("|");
                    w.write(p.docId().toString());
                    w.write("|");
                    w.write(Integer.toString(p.termFrequency()));
                    w.write("|");
                    w.write(positions);
                    w.newLine();
                }
            }
        }

    }

    private static void writeDocLengths(Path dir, Map<UUID, Integer> docLengths) throws IOException {
        try(BufferedWriter w = Files.newBufferedWriter(dir.resolve(SegmentFiles.DOC_LENGTHS_FILE))) {
            for(Map.Entry<UUID, Integer> e: docLengths.entrySet()){
                w.write(e.getKey().toString());
                w.write('|');
                w.write(Integer.toString(e.getValue()));
                w.newLine();
            }
        }

    }

    private static void writeMeta(Path dir, int docCount) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(dir.resolve(SegmentFiles.META_FILE))){
            w.write("formatVersion="+ SegmentFiles.FORMAT_VERSION);
            w.newLine();
            w.write("docCount=" + docCount);
            w.newLine();
        }
    }
}
