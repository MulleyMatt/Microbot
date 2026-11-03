package net.runelite.client.plugins.microbot.muspah.overlays;

import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.microbot.muspah.MuspahConfig;
import net.runelite.client.plugins.microbot.muspah.MuspahForm;
import net.runelite.client.plugins.microbot.muspah.MuspahPlugin;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.TextComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Displays the Current and Next Muspah carapace forms and Ranged weapon reminder
 * (Bowfa vs Sapphire Bolts + Smite), modeled after Zulrah Overlay Helper's PhaseOverlay.
 */
public class MuspahPhaseOverlay extends OverlayPanel
{
    private static final int DEFAULT_DIMENSION = 55;
    private static final int GAP = 1;

    private final MuspahPlugin plugin;
    private final MuspahConfig config;
    private final SkillIconManager skillIconManager;
    private final SpriteManager spriteManager;

    @Inject
    private MuspahPhaseOverlay(
            MuspahPlugin plugin,
            MuspahConfig config,
            SkillIconManager skillIconManager,
            SpriteManager spriteManager)
    {
        this.plugin = plugin;
        this.config = config;
        this.skillIconManager = skillIconManager;
        this.spriteManager = spriteManager;

        setResizable(false);
        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        panelComponent.setPreferredSize(new Dimension(DEFAULT_DIMENSION, DEFAULT_DIMENSION));
        panelComponent.setBorder(new Rectangle());
        panelComponent.setGap(new Point(GAP, GAP));
        panelComponent.setOrientation(ComponentOrientation.HORIZONTAL);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (config.phaseDisplayType() == MuspahConfig.DisplayType.OFF
                || config.phaseDisplayType() == MuspahConfig.DisplayType.TILE
                || plugin.getMuspahNpc() == null
                || plugin.getMuspahNpc().isDead()
                || plugin.getCurrentForm() == null)
        {
            return null;
        }

        MuspahForm currentForm = plugin.getCurrentForm();
        MuspahForm nextForm = currentForm.getNextRotationForm();
        MuspahConfig.DisplayMode mode = config.phaseDisplayMode();

        if (mode == MuspahConfig.DisplayMode.CURRENT || mode == MuspahConfig.DisplayMode.BOTH)
        {
            panelComponent.getChildren().add(buildFormBox(currentForm, "Current", false));
        }

        if ((mode == MuspahConfig.DisplayMode.NEXT || mode == MuspahConfig.DisplayMode.BOTH) && nextForm != null)
        {
            panelComponent.getChildren().add(buildFormBox(nextForm, "Next", true));
        }

        if (panelComponent.getChildren().isEmpty())
        {
            return null;
        }

        Dimension dimension = super.render(graphics);
        displayPhaseHeader(graphics);
        return dimension;
    }

    private InfoBoxComponent buildFormBox(MuspahForm form, String label, boolean isNext)
    {
        InfoBoxComponent box = new InfoBoxComponent();
        box.setText(label);
        box.setOutline(config.textOutline());
        box.setColor(!isNext ? Color.GREEN : Color.RED);
        box.setImage(getFormIcon(form));
        box.setBackgroundColor(form.getColorWithAlpha(65));
        box.setPreferredSize(new Dimension(DEFAULT_DIMENSION, DEFAULT_DIMENSION));
        return box;
    }

    private BufferedImage getFormIcon(MuspahForm form)
    {
        if (form == MuspahForm.PRAYER_SHIELD)
        {
            BufferedImage smiteSprite = spriteManager.getSprite(SpriteID.PRAYER_SMITE, 0);
            if (smiteSprite != null)
            {
                return smiteSprite;
            }
        }
        return skillIconManager.getSkillImage(Skill.RANGED, false);
    }

    private void displayPhaseHeader(Graphics2D graphics)
    {
        Rectangle bounds = panelComponent.getBounds();
        if (bounds == null || bounds.width <= 0)
        {
            return;
        }

        MuspahForm currentForm = plugin.getCurrentForm();
        String text;
        Color color;

        if (plugin.getMagicAttackTicks() > 0)
        {
            text = "PRAY MAGIC! (" + plugin.getMagicAttackTicks() + "t)";
            color = new Color(255, 80, 255);
        }
        else if (plugin.getShockwaveTicks() > 0)
        {
            text = "HIDE BEHIND SPIKE! (" + plugin.getShockwaveTicks() + "t)";
            color = Color.ORANGE;
        }
        else if (currentForm == MuspahForm.PRAYER_SHIELD)
        {
            text = "Sapphire Bolts + Smite";
            color = Color.CYAN;
        }
        else
        {
            text = currentForm.getDisplayName() + ": " + currentForm.getWeaponPrompt();
            color = Color.GREEN;
        }

        TextComponent textComponent = new TextComponent();
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        textComponent.setPosition(new Point(
                bounds.x + (bounds.width - textWidth) / 2,
                bounds.y - 3
        ));
        textComponent.setText(text);
        textComponent.setColor(color);
        textComponent.setOutline(config.textOutline());
        textComponent.render(graphics);
    }
}
