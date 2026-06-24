package com.senior.analyzer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/file/read")
    public ResponseEntity<?> readFile(@RequestBody Map<String, String> body) {
        String filePath = body.get("path");
        if (filePath == null || filePath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "path is required"));
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            return ResponseEntity.badRequest().body(Map.of("error", "file not found or not readable"));
        }

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String fileName = path.getFileName().toString();
            return ResponseEntity.ok(Map.of("content", content, "fileName", fileName));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/file/list")
    public ResponseEntity<?> listFiles(@RequestBody Map<String, String> body) {
        String dirPath = body.get("path");
        if (dirPath == null || dirPath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "path is required"));
        }

        Path dir = Path.of(dirPath);
        if (!Files.isDirectory(dir)) {
            return ResponseEntity.badRequest().body(Map.of("error", "not a directory"));
        }

        List<Map<String, Object>> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{log,txt,out}")) {
            for (Path entry : stream) {
                files.add(Map.of(
                    "name", entry.getFileName().toString(),
                    "path", entry.toString(),
                    "size", Files.size(entry),
                    "modified", Files.getLastModifiedTime(entry).toMillis()
                ));
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("files", files));
    }
}
