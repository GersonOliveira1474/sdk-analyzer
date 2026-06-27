package com.senior.analyzer.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recents")
public class RecentsController {

    private static final int MAX_RECENTS = 10;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Path recentsFile;

    public RecentsController() {
        String appDir = System.getProperty("user.dir");
        this.recentsFile = Path.of(appDir, "recents.json");
    }

    @GetMapping
    public ResponseEntity<?> getRecents() {
        List<Map<String, Object>> recents = load();

        recents.removeIf(entry -> {
            String path = (String) entry.get("path");
            return path == null || !Files.exists(Path.of(path));
        });

        save(recents);
        return ResponseEntity.ok(recents);
    }

    @PostMapping
    public ResponseEntity<?> addRecent(@RequestBody Map<String, String> body) {
        String filePath = body.get("path");
        String fileName = body.get("name");
        if (filePath == null || filePath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "path is required"));
        }
        if (fileName == null || fileName.isBlank()) {
            fileName = Path.of(filePath).getFileName().toString();
        }

        List<Map<String, Object>> recents = load();

        recents.removeIf(entry -> filePath.equals(entry.get("path")));

        long size = 0;
        try { size = Files.size(Path.of(filePath)); } catch (IOException ignored) {}

        recents.addFirst(Map.of(
            "path", filePath,
            "name", fileName,
            "size", size,
            "openedAt", System.currentTimeMillis()
        ));

        if (recents.size() > MAX_RECENTS) {
            recents = new ArrayList<>(recents.subList(0, MAX_RECENTS));
        }

        save(recents);
        return ResponseEntity.ok(recents);
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeRecent(@RequestBody Map<String, String> body) {
        String filePath = body.get("path");
        if (filePath == null || filePath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "path is required"));
        }

        List<Map<String, Object>> recents = load();
        recents.removeIf(entry -> filePath.equals(entry.get("path")));
        save(recents);
        return ResponseEntity.ok(recents);
    }

    private List<Map<String, Object>> load() {
        if (!Files.exists(recentsFile)) return new ArrayList<>();
        try {
            return mapper.readValue(recentsFile.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void save(List<Map<String, Object>> recents) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(recentsFile.toFile(), recents);
        } catch (IOException ignored) {}
    }
}
