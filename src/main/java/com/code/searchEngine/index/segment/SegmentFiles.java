package com.code.searchEngine.index.segment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.OptionalLong;
import java.util.stream.Stream;

public final class SegmentFiles {

    public static final String TERMS_FILES = "terms.dat";
    public static final String DOC_LENGTHS_FILE = "doclengths.dat";
    public static final String META_FILE = "met.dat";
    public static final int FORMAT_VERSION = 1;

    private static final String PREFIX = "segment_";
    private static final String TMP_SUFFIX = ".tmp";

    private SegmentFiles() {}

    public static String dirName(long id){
        return PREFIX + id;
    }

    public static String tmpDirName(long id){
        return dirName(id) + TMP_SUFFIX;
    }

    public static boolean isTmp(Path dir){
        String name = dir.getFileName().toString();
        return name.startsWith(PREFIX) && name.endsWith(TMP_SUFFIX);
    }

    public static OptionalLong parseId(Path dir){
        String name = dir.getFileName().toString();
        if(!name.startsWith(PREFIX) || name.endsWith(TMP_SUFFIX)) return OptionalLong.empty();

        try{
            return OptionalLong.of(Long.parseLong(name.substring(PREFIX.length())));
        }catch (Exception e){
            return OptionalLong.empty();
        }
    }

    public static void deleteRecursively(Path dir) throws IOException{
        if(!Files.exists(dir)) return ;
        try (Stream<Path> walk = Files.walk(dir)){
            for (Path p : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(p);
            }
        }
    }
}
