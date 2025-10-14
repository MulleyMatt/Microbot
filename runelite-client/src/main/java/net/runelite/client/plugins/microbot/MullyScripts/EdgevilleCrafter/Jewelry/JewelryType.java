package net.runelite.client.plugins.microbot.MullyScripts.EdgevilleCrafter.Jewelry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;

@Getter
@RequiredArgsConstructor
public enum JewelryType {
    GOLD(ItemID.GOLD_BAR),
    SILVER(ItemID.SILVER_BAR);

    private final int itemID;
}
