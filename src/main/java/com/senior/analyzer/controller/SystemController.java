package com.senior.analyzer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);
    private static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/GersonOliveira1474/sdk-analyzer/releases/latest";

    @Value("${app.version:1.0.0}")
    private String version;

    private String cachedLatestVersion;
    private String cachedDownloadUrl;
    private long lastUpdateCheck;

    private final Environment env;

    public SystemController(Environment env) {
        this.env = env;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String port = env.getProperty("server.port", "8080");
        String javaVersion = System.getProperty("java.version");
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "unknown";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("name", "Senior Watch");
        result.put("version", version);
        result.put("port", port);
        result.put("uptime", uptimeMs / 1000);
        result.put("javaVersion", javaVersion);
        result.put("hostname", hostname);
        result.put("ips", getLocalIPs());

        checkForUpdate();
        if (cachedLatestVersion != null && !cachedLatestVersion.equals(version)) {
            result.put("updateAvailable", true);
            result.put("latestVersion", cachedLatestVersion);
            result.put("downloadUrl", cachedDownloadUrl);
        }

        return result;
    }

    private void checkForUpdate() {
        if (System.currentTimeMillis() - lastUpdateCheck < 3_600_000) return; // 1h cache
        lastUpdateCheck = System.currentTimeMillis();

        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_RELEASES_URL))
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(Duration.ofSeconds(10))
                .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                String tagName = extractJsonString(body, "tag_name");
                String htmlUrl = extractJsonString(body, "html_url");
                if (tagName != null) {
                    cachedLatestVersion = tagName.replaceFirst("^v", "");
                    cachedDownloadUrl = htmlUrl;
                }
            }
        } catch (Exception e) {
            log.debug("Falha ao verificar atualizações: {}", e.getMessage());
        }
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    private List<String> getLocalIPs() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}
        return ips;
    }
}
