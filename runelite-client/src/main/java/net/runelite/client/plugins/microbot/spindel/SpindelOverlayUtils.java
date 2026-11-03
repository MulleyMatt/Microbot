package net.runelite.client.plugins.microbot.spindel;

import com.google.common.base.Strings;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.function.Supplier;

/**
 * Shared rendering helpers for Spindel scene and widget overlays.
 */
public final class SpindelOverlayUtils
{
    private SpindelOverlayUtils()
    {
    }

    public static float getAlphaTime()
    {
        return Math.abs(System.currentTimeMillis() % 2000L - 1000L) / 1000.0f;
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

    public static void renderWidgetPolygon(
            Graphics2D graphics,
            Widget widget,
            Color color,
            boolean outlineOnly,
            boolean flash)
    {
        if (widget == null || color == null)
        {
            return;
        }

        Rectangle widgetBounds = widget.getBounds();
        if (widgetBounds == null || widgetBounds.isEmpty())
        {
            return;
        }

        Composite originalComposite = graphics.getComposite();
        Stroke originalStroke = graphics.getStroke();

        if (flash)
        {
            float alpha = Math.max(0.35f, getAlphaTime());
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        }

        if (outlineOnly)
        {
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(2.0f));
            graphics.draw(widgetBounds);
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
            graphics.fill(widgetBounds);
        }
        else
        {
            graphics.setColor(color);
            graphics.fill(widgetBounds);
        }

        graphics.setComposite(originalComposite);
        graphics.setStroke(originalStroke);
    }
}
