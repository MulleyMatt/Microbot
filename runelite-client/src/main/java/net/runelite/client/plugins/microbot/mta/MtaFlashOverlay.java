package net.runelite.client.plugins.microbot.mta;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class MtaFlashOverlay extends Overlay
{
	private static final Color GOAL_REACHED_GREEN = new Color(0, 220, 90, 95);
	private static final Color LOW_HP_RED = new Color(255, 25, 25, 115);
	private static final Color WRONG_STAFF_ORANGE = new Color(255, 145, 0, 85);

	private final Client client;
	private final MtaPlugin plugin;

	@Inject
	public MtaFlashOverlay(Client client, MtaPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGHEST);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getCanvas() == null)
		{
			return null;
		}

		MtaPlugin.FlashAlertType alertType = plugin.getActiveFlashAlert();
		if (alertType == MtaPlugin.FlashAlertType.NONE)
		{
			return null;
		}

		Color baseColor;
		switch (alertType)
		{
			case LOW_HP_GRAVEYARD:
				baseColor = LOW_HP_RED;
				break;
			case ROOM_GOAL_REACHED:
				baseColor = GOAL_REACHED_GREEN;
				break;
			case WRONG_STAFF_OR_CAP:
				baseColor = WRONG_STAFF_ORANGE;
				break;
			default:
				return null;
		}

		Dimension canvasSize = client.getCanvas().getSize();
		if (canvasSize == null || canvasSize.width <= 0 || canvasSize.height <= 0)
		{
			return null;
		}

		long now = System.currentTimeMillis();
		double sine = Math.sin(now * (2.0 * Math.PI / 1000.0));
		double factor = 0.25 + 0.75 * ((sine + 1.0) / 2.0);
		int dynamicAlpha = (int) Math.round(baseColor.getAlpha() * factor);
		dynamicAlpha = Math.max(10, Math.min(255, dynamicAlpha));

		Color renderColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), dynamicAlpha);
		Color original = graphics.getColor();
		graphics.setColor(renderColor);
		graphics.fillRect(0, 0, canvasSize.width, canvasSize.height);
		graphics.setColor(original);

		return null;
	}
}
