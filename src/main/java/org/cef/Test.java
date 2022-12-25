package org.cef;

import org.cef.ui.GhostPanel;
import org.cef.ui.WebPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

public class Test {
    public static void main(String[] args) {

        WebPanel panel = new WebPanel();

        JFrame t = new JFrame("true");
        t.setBounds(0, 0, 500, 500);
        t.add(panel);
        t.setVisible(true);

        JFrame f = new JFrame("fake");
        f.setBounds(0, 0, 500, 500);
        f.add(new GhostPanel());
        f.setVisible(true);
    }
}
