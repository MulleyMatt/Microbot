package net.runelite.client.plugins.microbot.wildyslayer;

import com.google.common.base.Strings;
import net.runelite.api.Point;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.function.Supplier;

/**
 * Shared rendering helpers for Wilderness Slayer scene overlays.
 */
public final class WildySlayerOverlayUtils
{
    private WildySlayerOverlayUtils()
    {
    }

    public static void renderTextLocation(
            Graphics2D graphics,
            Point txtLoc,
            String text,
            Color color,
            Supplier<Boolean> outline)
    {
        if (txtLoc == null || Strings.isNullOrEmpty(text))
        {
            return;
        }

        int x = txtLoc.getX();
        int y = txtLoc.getY();
        graphics.setColor(Color.BLACK);

        if (Boolean.TRUE.equals(outline.get()))
        {
            graphics.drawString(text, x, y + 1);
            graphics.drawString(text, x, y - 1);
            graphics.drawString(text, x + 1, y);
            graphics.drawString(text, x - 1, y);
        }
        else
        {
            graphics.drawString(text, x + 1, y + 1);
        }

        graphics.setColor(color);
        graphics.drawString(text, x, y);
    }

    public static void renderPolygon(
            Graphics2D graphics,
            Shape polygon,
            Color color,
            int outlineStroke,
            int fillAlpha)
    {
        if (polygon == null || color == null)
        {
            return;
        }

        Stroke originalStroke = graphics.getStroke();
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(outlineStroke));
        graphics.draw(polygon);

        int clampedAlpha = Math.max(0, Math.min(255, fillAlpha));
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), clampedAlpha));
        graphics.fill(polygon);
        graphics.setStroke(originalStroke);
    }
}
