package com.runemod;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;

class RuneMod_Toggle_Overlay extends Overlay
{
    private final RuneModPlugin plugin;

    @Inject
    public RuneMod_Toggle_Overlay(RuneModPlugin plugin)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGH);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
/*        int sizeX = 30;
        int sizeY = 30;
        BufferedImage image = new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                        image.setRGB(x,y, 6640901);
            }
        }

        graphics.drawImage(image,100,100,null);

        System.out.println("rendering");

        graphics.drawString("asasdasf", 100, 100);*/
/*
        if (consumers.isEmpty())
        {
            return null;
        }

        final MainBufferProvider bufferProvider = (MainBufferProvider) client.getBufferProvider();
        final int imageHeight = ((BufferedImage) bufferProvider.getImage()).getHeight();
        final int y = imageHeight - plugin.getReportButton().getHeight() - 1;

        graphics.drawImage(plugin.getReportButton(), REPORT_BUTTON_X_OFFSET, y, null);

        graphics.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics fontMetrics = graphics.getFontMetrics();

        String date = DATE_FORMAT.format(new Date());
        final int dateWidth = fontMetrics.stringWidth(date);
        final int dateHeight = fontMetrics.getHeight();

        final int textX = REPORT_BUTTON_X_OFFSET + plugin.getReportButton().getWidth() / 2 - dateWidth / 2;
        final int textY = y + plugin.getReportButton().getHeight() / 2 + dateHeight / 2;

        graphics.setColor(Color.BLACK);
        graphics.drawString(date, textX + 1, textY + 1);

        graphics.setColor(Color.WHITE);
        graphics.drawString(date, textX, textY);

        // Request the queued screenshots to be taken,
        // now that the timestamp is visible.
        Consumer<Image> consumer;
        while ((consumer = consumers.poll()) != null)
        {
            drawManager.requestNextFrameListener(consumer);
        }
*/

        return null;
    }
}
