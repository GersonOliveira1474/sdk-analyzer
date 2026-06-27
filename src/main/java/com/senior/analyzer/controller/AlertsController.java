package com.senior.analyzer.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertsController {

    private static final int MAX_HISTORY = 100;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Path historyFile;
    private final Path rulesFile;

    public AlertsController() {
        String appDir = System.getProperty("user.dir");
        this.historyFile = Path.of(appDir, "alerts-history.json");
        this.rulesFile = Path.of(appDir, "alert-rules.json");
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(loadHistory());
    }

    @PostMapping("/history")
    public ResponseEntity<?> addToHistory(@RequestBody Map<String, Object> alert) {
        List<Map<String, Object>> history = loadHistory();

        alert.put("savedAt", System.currentTimeMillis());
        history.addFirst(alert);

        if (history.size() > MAX_HISTORY) {
            history = new ArrayList<>(history.subList(0, MAX_HISTORY));
        }

        saveJson(historyFile, history);
        return ResponseEntity.ok(Map.of("count", history.size()));
    }

    @PostMapping("/history/acknowledge")
    public ResponseEntity<?> acknowledgeAlert(@RequestBody Map<String, String> body) {
        String ruleId = body.get("ruleId");
        if (ruleId == null) return ResponseEntity.badRequest().body(Map.of("error", "ruleId required"));

        List<Map<String, Object>> history = loadHistory();
        for (Map<String, Object> entry : history) {
            if (ruleId.equals(entry.get("ruleId"))) {
                entry.put("acknowledged", true);
            }
        }
        saveJson(historyFile, history);
        return ResponseEntity.ok(Map.of("acknowledged", ruleId));
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory() {
        saveJson(historyFile, new ArrayList<>());
        return ResponseEntity.ok(Map.of("cleared", true));
    }

    @GetMapping("/rules")
    public ResponseEntity<?> getRules() {
        return ResponseEntity.ok(loadRules());
    }

    @PutMapping("/rules")
    public ResponseEntity<?> saveRules(@RequestBody List<Map<String, Object>> rules) {
        saveJson(rulesFile, rules);
        return ResponseEntity.ok(Map.of("saved", rules.size()));
    }

    private List<Map<String, Object>> loadHistory() {
        return loadJsonList(historyFile);
    }

    private List<Map<String, Object>> loadRules() {
        return loadJsonList(rulesFile);
    }

    private List<Map<String, Object>> loadJsonList(Path file) {
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            return mapper.readValue(file.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void saveJson(Path file, Object data) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
        } catch (IOException ignored) {}
    }
}
