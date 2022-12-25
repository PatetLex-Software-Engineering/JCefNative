package org.cef.ui;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

public class GhostPanel extends JPanel {

    private CefBrowser browser;
    private BufferedImage image;

    public GhostPanel() {
        this("https://www.google.com/");
    }

    public GhostPanel(String defaultUrl) {
        CefApp app;
        if (CefApp.getState() == CefApp.CefAppState.NONE || CefApp.getState() == CefApp.CefAppState.NEW) {
            CefSettings settings = new CefSettings();
            settings.windowless_rendering_enabled = false;
            app = CefApp.getInstance(settings);
        } else {
            app = CefApp.getInstance();
        }
        CefClient client = app.createClient();
        this.browser = client.createBrowser(defaultUrl, true, false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(this.image, 0, 0, this);
    }

    public BufferedImage getImage() {
        this.browser.createScreenshot(false).whenComplete(new BiConsumer<BufferedImage, Throwable>() {
            @Override
            public void accept(BufferedImage bufferedImage, Throwable throwable) {
                GhostPanel.this.image = bufferedImage;
            }
        });
        return this.image;
    }
}
