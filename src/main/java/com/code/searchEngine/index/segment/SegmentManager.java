package com.code.searchEngine.index.segment;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Slf4j
@Component
public class SegmentManager {

    private final Path indexRoot;
    private final AtomicLong segmentCounter = new AtomicLong(0);

    private final List<SegmentReader> activeSegments = new CopyOnWriteArrayList<>();

    public SegmentManager(Path indexRoot){
        this.indexRoot = indexRoot;
    }

    @PostConstruct
    public void loadExistingSegments() throws IOException{
        List<Path> candidates = new ArrayList<>();
        long maxId = 0;

        try(Stream<Path> stream = Files.list(indexRoot)){
            for (Path dir : (Iterable<Path>) stream.filter(Files::isDirectory)::iterator) {
                if(SegmentFiles.isTmp(dir)){
                    log.warn("Removing incomplete segment from a previous run: {}", dir);
                    SegmentFiles.deleteRecursively(dir);
                    continue;
                }
                OptionalLong id = SegmentFiles.parseId(dir);
                if(id.isEmpty()){
                    log.warn("Ignoring directory that doesn't match segment_<n>: {}", dir);
                    continue;
                }

                maxId = Math.max(maxId, id.getAsLong());
                candidates.add(dir);
            }
        }

        candidates.sort(Comparator.comparingLong(d -> SegmentFiles.parseId(d).getAsLong()));

        for(Path dir: candidates){
            SegmentReader reader = new SegmentReader(dir);
            reader.load();
            activeSegments.add(reader);
        }

        segmentCounter.set(maxId);
        log.info("Loaded {} segments from {}, next segment id = {}", activeSegments.size(), indexRoot, maxId + 1);
    }

    public long nextSegmentId(){
        return segmentCounter.incrementAndGet();
    }

    public SegmentReader flush(SegmentData data) throws IOException{
        if(data.isEmpty()) return null;

        long id = nextSegmentId();
        Path dir = SegmentWriter.write(indexRoot, id, data);

        SegmentReader reader = new SegmentReader(dir);
        reader.load();
        activeSegments.add(reader);
        return reader;
    }

    public SegmentReader replaceWithMerged(List<SegmentReader> inputs, SegmentData merged) throws IOException{
        long id = nextSegmentId();
        Path dir = SegmentWriter.write(indexRoot, id, merged);

        SegmentReader mergedReader = new SegmentReader(dir);
        mergedReader.load();

        synchronized (this){
            activeSegments.removeAll(inputs);
            activeSegments.add(mergedReader);
        }

        for(SegmentReader old: inputs){
            SegmentFiles.deleteRecursively(old.getSegmentDir());
        }

        return mergedReader;
    }

    public List<SegmentReader> getActiveSegments() {
        return List.copyOf(activeSegments);
    }

    public Path getIndexRoot() {
        return indexRoot;
    }
}