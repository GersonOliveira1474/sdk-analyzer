package com.senior.analyzer.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FileWatcherService {

    private final Consumer<String> lineHandler;
    private Path filePath;
    private long position;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> task;
    private volatile boolean running;

    public FileWatcherService(Consumer<String> lineHandler) {
        this.lineHandler = lineHandler;
    }

    public synchronized void start(Path path) {
        stop();
        this.filePath = path;
        this.running = true;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "file-watcher-" + path.getFileName());
            t.setDaemon(true);
            return t;
        });
        this.task = executor.scheduleWithFixedDelay(this::poll, 0, 200, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        running = false;
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        position = 0;
    }

    public boolean isRunning() {
        return running;
    }

    public String getFilePath() {
        return filePath != null ? filePath.toString() : "";
    }

    public long getPosition() {
        return position;
    }

    public List<String> readBacklog(Path path, int maxLines) {
        this.filePath = path;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            this.position = Files.size(path);
            if (maxLines > 0 && lines.size() > maxLines) {
                return lines.subList(lines.size() - maxLines, lines.size());
            }
            return lines;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private void poll() {
        if (!running || filePath == null) return;

        try {
            long fileSize = Files.size(filePath);

            if (fileSize < position) {
                position = 0;
            }
            if (fileSize == position) {
                return;
            }

            try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
                raf.seek(position);
                String line;
                while ((line = raf.readLine()) != null) {
                    if (!line.isEmpty()) {
                        lineHandler.accept(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
                    }
                }
                position = raf.getFilePointer();
            }
        } catch (IOException ignored) {
        }
    }
}
