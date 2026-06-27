package com.senior.analyzer.controller;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private static final String VERSION = "1.0.0";

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

        return Map.of(
            "name", "Senior Watch",
            "version", VERSION,
            "port", port,
            "uptime", uptimeMs / 1000,
            "javaVersion", javaVersion,
            "hostname", hostname,
            "ips", getLocalIPs()
        );
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
