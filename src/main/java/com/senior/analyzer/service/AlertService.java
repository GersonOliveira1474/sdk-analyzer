package com.senior.analyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final Pattern LATENCY_PATTERN = Pattern.compile("(\\d+)ms");

    private final Deque<LineEvent> recentLines = new ConcurrentLinkedDeque<>();
    private final Set<String> firedRuleIds = Collections.synchronizedSet(new HashSet<>());
    private static final int MAX_RECENT = 500;
    private static final long WINDOW_MS = 2 * 60 * 1000;

    public record Alert(String ruleId, String severity, String title, String description, long timestamp) {}

    private record LineEvent(String line, long timestamp) {}

    public void reset() {
        recentLines.clear();
        firedRuleIds.clear();
    }

    public List<Alert> processLines(List<String> lines) {
        long now = System.currentTimeMillis();
        for (String line : lines) {
            recentLines.addLast(new LineEvent(line, now));
        }

        while (recentLines.size() > MAX_RECENT) {
            recentLines.pollFirst();
        }

        List<Alert> alerts = new ArrayList<>();

        checkErrors(lines, now, alerts);
        checkNackRecurrent(now, alerts);
        checkHighLatency(lines, now, alerts);
        checkDeviceOffline(lines, now, alerts);

        return alerts;
    }

    private void checkErrors(List<String> lines, long now, List<Alert> alerts) {
        long errorCount = lines.stream()
            .filter(l -> l.contains(" ERROR ") || l.contains("[ERROR]"))
            .count();

        if (errorCount > 0) {
            String id = "log-error";
            alerts.add(new Alert(id, "ALTO",
                errorCount + " erro(s) detectados",
                errorCount + " linhas com nível ERROR nas novas entradas",
                now));
        }
    }

    private void checkNackRecurrent(long now, List<Alert> alerts) {
        long cutoff = now - WINDOW_MS;
        long nackCount = recentLines.stream()
            .filter(e -> e.timestamp >= cutoff)
            .filter(e -> {
                String lower = e.line.toLowerCase();
                return lower.contains("nack") || (lower.contains("falha") && lower.contains("comunicação"));
            })
            .count();

        if (nackCount >= 3) {
            String id = "nack-recurrent";
            if (!firedRuleIds.contains(id)) {
                firedRuleIds.add(id);
                alerts.add(new Alert(id, "CRITICO",
                    nackCount + " NACKs detectados",
                    nackCount + " falhas de comunicação em 2 minutos",
                    now));
            }
        } else {
            firedRuleIds.remove("nack-recurrent");
        }
    }

    private void checkHighLatency(List<String> lines, long now, List<Alert> alerts) {
        long slowCount = lines.stream()
            .filter(l -> {
                Matcher m = LATENCY_PATTERN.matcher(l);
                return m.find() && Integer.parseInt(m.group(1)) > 2000;
            })
            .count();

        if (slowCount >= 3) {
            alerts.add(new Alert("high-latency", "MEDIO",
                slowCount + " respostas lentas",
                slowCount + " eventos com tempo de resposta acima de 2000ms",
                now));
        }
    }

    private void checkDeviceOffline(List<String> lines, long now, List<Alert> alerts) {
        long offlineCount = lines.stream()
            .filter(l -> {
                String lower = l.toLowerCase();
                return (lower.contains("offline") && !lower.contains("online")) ||
                       (lower.contains("falha") && lower.contains("dispositivo"));
            })
            .count();

        if (offlineCount > 0) {
            String id = "device-offline";
            if (!firedRuleIds.contains(id)) {
                firedRuleIds.add(id);
                alerts.add(new Alert(id, "CRITICO",
                    "Dispositivo(s) offline detectado(s)",
                    offlineCount + " evento(s) de dispositivo offline nas novas entradas",
                    now));
            }
        }
    }
}
