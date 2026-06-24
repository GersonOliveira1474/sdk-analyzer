package com.senior.analyzer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Component
public class BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    private final Environment env;

    public BrowserLauncher(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String port = env.getProperty("server.port", "8080");

        log.info("═══════════════════════════════════════════════════");
        log.info("  SDK Analyzer iniciado");
        log.info("═══════════════════════════════════════════════════");

        List<String> ips = getLocalIPs();
        for (String ip : ips) {
            log.info("  Endereço: {}:{}", ip, port);
        }

        log.info("  Local:    localhost:{}", port);
        log.info("═══════════════════════════════════════════════════");

        String openBrowser = env.getProperty("browser.open.on.start", "true");
        if ("true".equalsIgnoreCase(openBrowser)) {
            openBrowser("http://localhost:" + port);
        }
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                log.info("Browser aberto em {}", url);
            } else {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            }
        } catch (Exception e) {
            log.warn("Não foi possível abrir o browser: {}", e.getMessage());
            log.info("Acesse manualmente: {}", url);
        }
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
        } catch (Exception ignored) {
        }
        return ips;
    }
}
