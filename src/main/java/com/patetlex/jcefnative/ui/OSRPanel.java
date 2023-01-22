package com.patetlex.jcefnative.ui;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class OSRPanel extends JPanel {

    private static final List<OSRPanel> PANELS = new ArrayList<>();

    private WebPanel webPanel;
    private JFrame hiddenFrame;
    private BufferedImage image;

    public OSRPanel() {
        this("https://www.google.com/");
    }

    public OSRPanel(String defaultUrl) {
        this.image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        this.webPanel = new WebPanel(defaultUrl, true);
        this.hiddenFrame = new JFrame();
        this.hiddenFrame.setType(Window.Type.UTILITY);
        this.hiddenFrame.add(this.webPanel);
        this.hiddenFrame.setVisible(true);
        this.hiddenFrame.setResizable(false);
        this.hiddenFrame.setFocusable(false);
        this.webPanel.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                super.onLoadEnd(browser, frame, httpStatusCode);
                OSRPanel.this.repaint();
            }
        });
        PANELS.add(this);
        for (MouseListener listener : this.webPanel.getBrowser().getUIComponent().getMouseListeners()) {
            this.addMouseListener(listener);
        }
        for (MouseMotionListener listener : this.webPanel.getBrowser().getUIComponent().getMouseMotionListeners()) {
            this.addMouseMotionListener(listener);
        }
        for (MouseWheelListener listener : this.webPanel.getBrowser().getUIComponent().getMouseWheelListeners()) {
            this.addMouseWheelListener(listener);
        }
        for (KeyListener listener : this.webPanel.getBrowser().getUIComponent().getKeyListeners()) {
            this.addKeyListener(listener);
        }
        this.setFocusable(true);
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                OSRPanel.this.hiddenFrame.setBounds(-(e.getComponent().getWidth() + OSRPanel.this.hiddenFrame.getInsets().left + OSRPanel.this.hiddenFrame.getInsets().right) + 10, -(e.getComponent().getHeight() + OSRPanel.this.hiddenFrame.getInsets().top + OSRPanel.this.hiddenFrame.getInsets().bottom) + 10, e.getComponent().getWidth() + OSRPanel.this.hiddenFrame.getInsets().left + OSRPanel.this.hiddenFrame.getInsets().right, e.getComponent().getHeight() + OSRPanel.this.hiddenFrame.getInsets().top + OSRPanel.this.hiddenFrame.getInsets().bottom);
                OSRPanel.this.webPanel.setBounds(0, 0, e.getComponent().getWidth(), e.getComponent().getHeight());
                OSRPanel.this.hiddenFrame.revalidate();
            }
        });
    }

    private CompletableFuture<BufferedImage> current;
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (this.current == null) {
            this.current = this.webPanel.getBrowser().createScreenshot(false);
            this.current.whenComplete(new BiConsumer<BufferedImage, Throwable>() {
                @Override
                public void accept(BufferedImage bufferedImage, Throwable throwable) {
                    OSRPanel.this.current = null;
                    OSRPanel.this.image = bufferedImage;
                }
            });
        }
        g.drawImage(this.image, 0, 0, this);
        repaint();
    }

    public BufferedImage getImage() {
        return this.image;
    }
}
