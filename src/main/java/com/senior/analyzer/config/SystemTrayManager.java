package com.senior.analyzer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;

@Component
public class SystemTrayManager {

    private static final Logger log = LoggerFactory.getLogger(SystemTrayManager.class);

    private final Environment env;
    private final ConfigurableApplicationContext context;

    public SystemTrayManager(Environment env, ConfigurableApplicationContext context) {
        this.env = env;
        this.context = context;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!SystemTray.isSupported()) {
            log.warn("System tray não suportado neste sistema");
            return;
        }

        try {
            String port = env.getProperty("server.port", "8080");
            String url = "http://localhost:" + port;

            TrayIcon trayIcon = new TrayIcon(createIcon(), "Senior Watch");
            trayIcon.setImageAutoSize(true);

            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Abrir no browser");
            openItem.addActionListener(e -> openBrowser(url));

            MenuItem statusItem = new MenuItem("Porta: " + port);
            statusItem.setEnabled(false);

            MenuItem exitItem = new MenuItem("Encerrar");
            exitItem.addActionListener(e -> {
                SystemTray.getSystemTray().remove(trayIcon);
                context.close();
                System.exit(0);
            });

            popup.add(openItem);
            popup.addSeparator();
            popup.add(statusItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);
            trayIcon.addActionListener(e -> openBrowser(url));

            SystemTray.getSystemTray().add(trayIcon);
            log.info("Ícone adicionado à bandeja do sistema");
        } catch (Exception e) {
            log.warn("Erro ao criar ícone na bandeja: {}", e.getMessage());
        }
    }

    private Image createIcon() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(59, 130, 246));
        g.fillRoundRect(1, 1, 14, 14, 4, 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString("S", 4, 12);
        g.dispose();
        return img;
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            }
        } catch (Exception e) {
            log.warn("Não foi possível abrir o browser: {}", e.getMessage());
        }
    }
}
