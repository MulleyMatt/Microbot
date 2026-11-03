package net.runelite.client.plugins.microbot.mta;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

@Getter
@RequiredArgsConstructor
public enum MtaAlchemyItem
{
	LEATHER_BOOTS("Leather Boots", ItemID.MAGICTRAINING_LEATHER_BOOTS),
	ADAMANT_KITESHIELD("Adamant Kiteshield", ItemID.MAGICTRAINING_ADAMANT_KITESHIELD),
	ADAMANT_MED_HELM("Helm", ItemID.MAGICTRAINING_ADAMANT_MED_HELM),
	EMERALD("Emerald", ItemID.MAGICTRAINING_EMERALD),
	RUNE_LONGSWORD("Rune Longsword", ItemID.MAGICTRAINING_RUNE_LONGSWORD),
	EMPTY("Empty", -1),
	UNKNOWN("Unknown", -1);

	private final String name;
	private final int itemId;

	public static MtaAlchemyItem findByName(String text)
	{
		if (text == null || text.trim().isEmpty())
		{
			return null;
		}
		String lower = text.toLowerCase();
		for (MtaAlchemyItem item : values())
		{
			if (item == EMPTY || item == UNKNOWN)
			{
				continue;
			}
			if (lower.contains(item.name.toLowerCase()))
			{
				return item;
			}
		}
		return null;
	}

	public static MtaAlchemyItem findByItemId(int itemId)
	{
		if (itemId <= 0)
		{
			return null;
		}
		for (MtaAlchemyItem item : values())
		{
			if (item.itemId == itemId)
			{
				return item;
			}
		}
		return null;
	}
}
