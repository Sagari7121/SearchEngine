package com.code.searchEngine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class IndexStorageConfig {

    @Value("${search.index.data-dir}")
    private String dataDir;

    @Bean
    public Path indexRootPath() throws IOException {
        Path path = Path.of(dataDir);
        Files.createDirectories(path);
        return path;
    }
}
