package com.senior.analyzer.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senior.analyzer.service.FileWatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LogWebSocketHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "heartbeat");
        t.setDaemon(true);
        return t;
    });

    public LogWebSocketHandler() {
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeats, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), new SessionContext(session));
        log.info("Cliente conectado ({} ativo(s))", sessions.size());
        sendJson(session, Map.of("type", "status", "watching", false, "file", "", "position", 0));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionContext ctx = sessions.remove(session.getId());
        if (ctx != null) {
            ctx.watcher.stop();
        }
        log.info("Cliente desconectado ({} restante(s))", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode msg = mapper.readTree(message.getPayload());
            String type = msg.path("type").asText();
            SessionContext ctx = sessions.get(session.getId());
            if (ctx == null) return;

            switch (type) {
                case "start" -> handleStart(ctx, msg);
                case "stop" -> handleStop(ctx);
                case "ping" -> sendJson(session, Map.of("type", "heartbeat"));
            }
        } catch (Exception e) {
            log.error("Erro ao processar mensagem: {}", e.getMessage());
        }
    }

    private void handleStart(SessionContext ctx, JsonNode msg) {
        String filePath = msg.path("file").asText("");
        if (filePath.isEmpty()) return;

        int backlog = msg.path("backlog").asInt(500);
        Path path = Path.of(filePath);

        ctx.watcher.stop();
        ctx.watcher = new FileWatcherService(line ->
            sendJson(ctx.session, Map.of("type", "line", "data", line))
        );

        List<String> lines = ctx.watcher.readBacklog(path, backlog);
        if (!lines.isEmpty()) {
            sendJson(ctx.session, Map.of("type", "backlog", "lines", lines, "file", filePath));
        }

        ctx.watcher.start(path);
        log.info("Monitorando: {}", filePath);

        sendJson(ctx.session, Map.of(
            "type", "status",
            "watching", true,
            "file", filePath,
            "position", ctx.watcher.getPosition()
        ));
    }

    private void handleStop(SessionContext ctx) {
        String file = ctx.watcher.getFilePath();
        ctx.watcher.stop();
        log.info("Parou de monitorar: {}", file);
        sendJson(ctx.session, Map.of("type", "status", "watching", false, "file", "", "position", 0));
    }

    private void sendHeartbeats() {
        for (SessionContext ctx : sessions.values()) {
            sendJson(ctx.session, Map.of("type", "heartbeat"));
        }
    }

    private void sendJson(WebSocketSession session, Object payload) {
        if (!session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
            }
        } catch (IOException e) {
            log.debug("Erro ao enviar mensagem: {}", e.getMessage());
        }
    }

    private static class SessionContext {
        final WebSocketSession session;
        FileWatcherService watcher;

        SessionContext(WebSocketSession session) {
            this.session = session;
            this.watcher = new FileWatcherService(line -> {});
        }
    }
}
