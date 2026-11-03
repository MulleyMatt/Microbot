package net.runelite.client.plugins.microbot.mta;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

import java.awt.Color;

@Getter
@RequiredArgsConstructor
public enum MtaEnchantShape
{
	CUBE(
		"Red Cube",
		ItemID.MAGICTRAINING_ENCHAN_CUBE,
		ObjectID.MAGICTRAINING_ENCHAN_SHAPEPILE1,
		InterfaceID.MagictrainingEncha.MAGICTRAIN_CUBE,
		new Color(255, 80, 80)
	),
	CYLINDER(
		"Green Cylinder",
		ItemID.MAGICTRAINING_ENCHAN_CYLINDER,
		ObjectID.MAGICTRAINING_ENCHAN_SHAPEPILE2,
		InterfaceID.MagictrainingEncha.MAGICTRAIN_CYLINDER,
		new Color(80, 255, 120)
	),
	ICOSAHEDRON(
		"Yellow Icosahedron",
		ItemID.MAGICTRAINING_ENCHAN_ICOSAHENDRON,
		ObjectID.MAGICTRAINING_ENCHAN_SHAPEPILE3,
		InterfaceID.MagictrainingEncha.MAGICTRAIN_ICOSAHEDRON,
		new Color(255, 225, 60)
	),
	PENTAMID(
		"Blue Pentamid",
		ItemID.MAGICTRAINING_ENCHAN_PENTAMID,
		ObjectID.MAGICTRAINING_ENCHAN_SHAPEPILE4,
		InterfaceID.MagictrainingEncha.MAGICTRAIN_PENTAMID,
		new Color(80, 180, 255)
	);

	private final String displayName;
	private final int itemId;
	private final int pileObjectId;
	private final int bonusWidgetId;
	private final Color themeColor;

	public static MtaEnchantShape fromPileObjectId(int objectId)
	{
		for (MtaEnchantShape shape : values())
		{
			if (shape.pileObjectId == objectId)
			{
				return shape;
			}
		}
		return null;
	}

	public static MtaEnchantShape fromItemId(int itemId)
	{
		for (MtaEnchantShape shape : values())
		{
			if (shape.itemId == itemId)
			{
				return shape;
			}
		}
		return null;
	}
}
