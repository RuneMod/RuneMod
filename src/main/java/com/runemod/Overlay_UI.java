package com.runemod;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Overlay_UI extends JDialog {
    public Overlay_UI(Frame owner, RuneModPlugin plugin) {
        super(owner);
        setUndecorated(true);
        setBackground(new Color(255, 0, 0, 20));
        setSize(400, 300);
        setLocationRelativeTo(owner);
        setFocusableWindowState(false);
        runeModPlugin = plugin;

        setVisible(true);

        new Timer(200, e -> repaint()).start();
    }

    public RuneModPlugin runeModPlugin;

    BufferedImage image;

    @Override
    public void paint(Graphics g) {
/*        if (!isOpaque()) {
            Graphics gg = g;
            gg.setColor(getBackground());
            ((Graphics2D)gg).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
            gg.fillRect(0, 0, getWidth(), getHeight());
        }*/

/*        super.paint(g);

        // Cast to Graphics2D for advanced control
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Set the transparency level (0.0f is fully transparent, 1.0f is fully opaque)
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g2d.setComposite(ac);

        // Draw a rectangle with transparency
        g2d.setColor(new Color(155,0,0,2));
        g2d.fillRect(50, 50, 200, 100);*/

        System.out.println("paint");
        if(runeModPlugin!=null) {
            if(runeModPlugin.client!=null) {
                if(runeModPlugin.client.getBufferProvider() != null) {
                    int bufferWidth = runeModPlugin.client.getBufferProvider().getWidth();
                    int bufferHeight = runeModPlugin.client.getBufferProvider().getHeight();

                    if (image == null || image.getWidth() != bufferWidth || image.getHeight() != bufferHeight) {
                        image = new BufferedImage(runeModPlugin.client.getBufferProvider().getWidth(), runeModPlugin.client.getBufferProvider().getHeight(), BufferedImage.TYPE_INT_ARGB);
                        System.out.println("made new buffered image");
                    }


/*
                    JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
                    Graphics window_g = window.getGraphics();
                    window_g.setColor(new Color(155,0,0,40));
                    ((Graphics2D)window_g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
                    window_g.fillRect(0, 0, runeModPlugin.client.getCanvasWidth(), runeModPlugin.client.getCanvasHeight());

                    Graphics window_g_01 = runeModPlugin.client.getCanvas().getParent().getGraphics();
                    window_g_01.setColor(new Color(155,0,0,40));
                    ((Graphics2D)window_g_01).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
                    window_g_01.fillRect(0, 0, runeModPlugin.client.getCanvasWidth(), runeModPlugin.client.getCanvasHeight());

                    Graphics window_g_02 = runeModPlugin.client.getCanvas().getParent().getParent().getGraphics();
                    window_g_02.setColor(new Color(155,0,0,40));
                    ((Graphics2D)window_g_02).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
                    window_g_02.fillRect(0, 0, runeModPlugin.client.getCanvasWidth(), runeModPlugin.client.getCanvasHeight());

                    Graphics window_g_03 = runeModPlugin.client.getCanvas().getParent().getParent().getParent().getGraphics();
                    window_g_03.setColor(new Color(155,0,0,40));
                    ((Graphics2D)window_g_03).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
                    window_g_03.fillRect(0, 0, runeModPlugin.client.getCanvasWidth(), runeModPlugin.client.getCanvasHeight());
*/

                    System.arraycopy(runeModPlugin.client.getBufferProvider().getPixels(), 0, ((DataBufferInt) image.getRaster().getDataBuffer()).getData(), 0, runeModPlugin.client.getBufferProvider().getPixels().length-1);

                    g.setColor(new Color(155,0,0,40));
                    ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
                    g.fillRect(0, 0, runeModPlugin.client.getCanvasWidth(), runeModPlugin.client.getCanvasHeight());

                    g.drawImage(image,0,0, runeModPlugin.client.getCanvas().getBounds().width, runeModPlugin.client.getCanvas().getBounds().height  , null);
                }
            }
        }
    }
}