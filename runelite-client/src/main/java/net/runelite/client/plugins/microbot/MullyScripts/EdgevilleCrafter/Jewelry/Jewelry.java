package net.runelite.client.plugins.microbot.MullyScripts.EdgevilleCrafter.Jewelry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;

@Getter
@RequiredArgsConstructor
public enum Jewelry {

    DIAMOND_NECKLACE("diamond necklace", ItemID.DIAMOND_NECKLACE, Gem.DIAMOND, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 56),
    DIAMOND_BRACELET("diamond bracelet", ItemID.DIAMOND_BRACELET, Gem.DIAMOND, ItemID.BRACELET_MOULD, JewelryType.GOLD, 58),
    DIAMOND_AMULET("diamond amulet", ItemID.DIAMOND_AMULET_U, Gem.DIAMOND, ItemID.AMULET_MOULD, JewelryType.GOLD, 70),
    DRAGONSTONE_RING("dragonstone ring", ItemID.DRAGONSTONE_RING, Gem.DRAGONSTONE, ItemID.RING_MOULD, JewelryType.GOLD, 55),
    DRAGON_NECKLACE("dragon necklace", ItemID.DRAGON_NECKLACE, Gem.DRAGONSTONE, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 72),
    DRAGONSTONE_BRACELET("dragon bracelet", ItemID.DRAGONSTONE_BRACELET, Gem.DRAGONSTONE, ItemID.BRACELET_MOULD, JewelryType.GOLD, 74),
    DRAGONSTONE_AMULET("dragonstone amulet", ItemID.DRAGONSTONE_AMULET_U, Gem.DRAGONSTONE, ItemID.AMULET_MOULD, JewelryType.GOLD, 80),
    ONYX_RING("onyx ring", ItemID.ONYX_RING, Gem.ONYX, ItemID.RING_MOULD, JewelryType.GOLD, 67),
    ONYX_NECKLACE("onyx necklace", ItemID.ONYX_NECKLACE, Gem.ONYX, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 82),
    ONYX_BRACELET("onyx bracelet", ItemID.ONYX_BRACELET, Gem.ONYX, ItemID.BRACELET_MOULD, JewelryType.GOLD, 84),
    ONYX_AMULET("onyx amulet", ItemID.ONYX_AMULET_U, Gem.ONYX, ItemID.AMULET_MOULD, JewelryType.GOLD, 90),
    ZENYTE_RING("zenyte ring", ItemID.ZENYTE_RING, Gem.ZENYTE, ItemID.RING_MOULD, JewelryType.GOLD, 89),
    ZENYTE_NECKLACE("zenyte necklace", ItemID.ZENYTE_NECKLACE, Gem.ZENYTE, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 92),
    ZENYTE_BRACELET("zenyte bracelet", ItemID.ZENYTE_BRACELET, Gem.ZENYTE, ItemID.BRACELET_MOULD, JewelryType.GOLD, 95),
    ZENYTE_AMULET("zenyte amulet", ItemID.ZENYTE_AMULET_U, Gem.ZENYTE, ItemID.AMULET_MOULD, JewelryType.GOLD, 98);

    private final String itemName;
    private final int itemID;
    private final Gem gem;
    private final int toolItemID;
    private final JewelryType jewelryType;
    private final int levelRequired;
}