package net.runelite.client.plugins.microbot.barbarianfishing;

import net.runelite.api.AnimationID;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BarbarianFishingHelperTest {

    @Test
    public void testBarbarianRodAndBaitCollections() {
        assertTrue("Barbarian rod must be supported", BarbarianFishingSupplyCalculator.BARBARIAN_ROD_IDS.contains(ItemID.BRUT_FISHING_ROD));
        assertTrue("Pearl barbarian rod must be supported", BarbarianFishingSupplyCalculator.BARBARIAN_ROD_IDS.contains(ItemID.FISHINGROD_PEARL_BRUT));
        assertFalse("Standard fly rod is not a barbarian rod", BarbarianFishingSupplyCalculator.BARBARIAN_ROD_IDS.contains(ItemID.FLY_FISHING_ROD));

        assertTrue("Feathers must be supported as bait", BarbarianFishingSupplyCalculator.BAIT_IDS.contains(ItemID.FEATHER));
        assertTrue("Fishing bait must be supported", BarbarianFishingSupplyCalculator.BAIT_IDS.contains(ItemID.FISHING_BAIT));
        assertTrue("Stripy feathers must be supported", BarbarianFishingSupplyCalculator.BAIT_IDS.contains(ItemID.HUNTING_STRIPY_BIRD_FEATHER));
        assertTrue("Fish offcuts must be supported", BarbarianFishingSupplyCalculator.BAIT_IDS.contains(ItemID.BRUT_FISH_CUTS));
        assertTrue("Roe must be supported", BarbarianFishingSupplyCalculator.BAIT_IDS.contains(ItemID.BRUT_ROE));
        assertTrue("Caviar must be supported", BarbarianFishingSupplyCalculator.BAIT_IDS.contains(ItemID.BRUT_CAVIAR));
    }

    @Test
    public void testLeapingFishDropTargetRestriction() {
        assertTrue("Leaping trout should left-click Drop", BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.BRUT_SPAWNING_TROUT));
        assertTrue("Leaping salmon should left-click Drop", BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.BRUT_SPAWNING_SALMON));
        assertTrue("Leaping sturgeon should left-click Drop", BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.BRUT_STURGEON));
        assertEquals("Only the 3 leaping fish should be swapped to Drop", 3, BarbarianFishingSupplyCalculator.LEAPING_FISH_IDS.size());

        // Gear, bait, and cut products must NOT be swapped to left-click Drop
        assertFalse(BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.BRUT_FISHING_ROD));
        assertFalse(BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.FISHINGROD_PEARL_BRUT));
        assertFalse(BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.FEATHER));
        assertFalse(BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.FISHING_BAIT));
        assertFalse(BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.BRUT_ROE));
        assertFalse(BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.BRUT_CAVIAR));
        assertFalse(BarbarianFishingSupplyCalculator.isLeapingFish(ItemID.BRUT_FISH_CUTS));
    }

    @Test
    public void testBarbarianFishingSpotClassification() {
        assertTrue("Otto's Grotto spot must match", BarbarianFishingSceneOverlay.isBarbarianFishingSpot(NpcID._0_39_54_BRUT_FISHING_SPOT));
        assertTrue("Mount Quidamortem spot must match", BarbarianFishingSceneOverlay.isBarbarianFishingSpot(NpcID._0_19_55_BRUT_FISHING_SPOT));
        assertFalse("Unrelated NPC ID must not match", BarbarianFishingSceneOverlay.isBarbarianFishingSpot(NpcID.PISCARILIUSFISH));
    }

    @Test
    public void testFishingAnimations() {
        assertTrue(BarbarianFishingPlugin.FISHING_ANIMS.contains(AnimationID.FISHING_POLE_CAST));
        assertTrue(BarbarianFishingPlugin.FISHING_ANIMS.contains(AnimationID.FISHING_OILY_ROD));
        assertTrue(BarbarianFishingPlugin.FISHING_ANIMS.contains(AnimationID.FISHING_BARBARIAN_ROD));
        assertTrue(BarbarianFishingPlugin.FISHING_ANIMS.contains(AnimationID.FISHING_PEARL_BARBARIAN_ROD));
        assertTrue(BarbarianFishingPlugin.FISHING_ANIMS.contains(AnimationID.FISHING_PEARL_BARBARIAN_ROD_2));

        assertFalse(BarbarianFishingPlugin.FISHING_ANIMS.contains(-1));
        assertFalse(BarbarianFishingPlugin.FISHING_ANIMS.contains(AnimationID.IDLE));
    }

    @Test
    public void testSpecialAttackHarpoonCollections() {
        assertTrue(BarbarianFishingSupplyCalculator.SPEC_HARPOON_IDS.contains(ItemID.DRAGON_HARPOON));
        assertTrue(BarbarianFishingSupplyCalculator.SPEC_HARPOON_IDS.contains(ItemID.CRYSTAL_HARPOON));
        assertTrue(BarbarianFishingSupplyCalculator.SPEC_HARPOON_IDS.contains(ItemID.INFERNAL_HARPOON));
        assertTrue(BarbarianFishingSupplyCalculator.SPEC_HARPOON_IDS.contains(ItemID.TRAILBLAZER_HARPOON));
        assertFalse("Regular harpoon has no special attack", BarbarianFishingSupplyCalculator.SPEC_HARPOON_IDS.contains(ItemID.HARPOON));
        assertFalse("Barbarian rod has no special attack", BarbarianFishingSupplyCalculator.SPEC_HARPOON_IDS.contains(ItemID.BRUT_FISHING_ROD));
    }

    @Test
    public void testStepsAndFlashEnums() {
        for (BarbarianFishingStep step : BarbarianFishingStep.values()) {
            assertNotNull(step.getLabel());
            assertNotNull(step.getDescription());
            assertFalse(step.getLabel().isEmpty());
            assertFalse(step.getDescription().isEmpty());
        }

        assertEquals("Fishing", BarbarianFishingStep.FISHING.getLabel());
        assertEquals("AFK / Inactive", BarbarianFishingStep.IDLE_AFK.getLabel());
        assertEquals("Inventory Full", BarbarianFishingStep.INVENTORY_FULL.getLabel());

        assertEquals("Pulse", BarbarianFishingFlashStyle.PULSE.toString());
        assertEquals("Flash", BarbarianFishingFlashStyle.FLASH.toString());
        assertEquals("Solid", BarbarianFishingFlashStyle.SOLID.toString());

        assertEquals(BarbarianFishingPlugin.FlashType.NONE, BarbarianFishingPlugin.FlashType.valueOf("NONE"));
        assertEquals(BarbarianFishingPlugin.FlashType.INVENTORY_FULL, BarbarianFishingPlugin.FlashType.valueOf("INVENTORY_FULL"));
        assertEquals(BarbarianFishingPlugin.FlashType.AFK, BarbarianFishingPlugin.FlashType.valueOf("AFK"));
        assertEquals(BarbarianFishingPlugin.FlashType.SPECIAL_ATTACK, BarbarianFishingPlugin.FlashType.valueOf("SPECIAL_ATTACK"));
    }

    @Test
    public void testMenuOptionRecognition() {
        BarbarianFishingPlugin plugin = new BarbarianFishingPlugin();

        assertTrue(plugin.isDropOption("Drop"));
        assertTrue(plugin.isDropOption("drop"));
        assertTrue(plugin.isDropOption("<col=ff9040>Drop</col>"));
        assertFalse(plugin.isDropOption("Use"));
        assertFalse(plugin.isDropOption(null));

        assertTrue(plugin.isFishingInteractionOption("Use-rod"));
        assertTrue(plugin.isFishingInteractionOption("Fish"));
        assertTrue(plugin.isFishingInteractionOption("Harpoon"));
        assertFalse(plugin.isFishingInteractionOption("Drop"));
        assertFalse(plugin.isFishingInteractionOption(null));
    }

    @Test
    public void testAfkInactivityThresholdLogic() {
        Instant now = Instant.now();
        Instant stoppedRecent = now.minusMillis(500);
        Instant stoppedOverThreshold = now.minusMillis(2500);

        long thresholdMillis = 2 * 1000L;
        assertFalse(Duration.between(stoppedRecent, now).toMillis() >= thresholdMillis);
        assertTrue(Duration.between(stoppedOverThreshold, now).toMillis() >= thresholdMillis);
    }

    @Test
    public void testSupplyCalculatorWithMockContainers() {
        Client client = mock(Client.class);
        ItemManager itemManager = mock(ItemManager.class);
        ItemContainer inv = mock(ItemContainer.class);
        ItemContainer equip = mock(ItemContainer.class);

        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equip);

        when(equip.getItems()).thenReturn(new Item[]{
                new Item(ItemID.FISHINGROD_PEARL_BRUT, 1),
                new Item(ItemID.TRAWLER_REWARD_HAT, 1),
                new Item(ItemID.SPIRIT_ANGLER_TOP, 1)
        });
        when(inv.getItems()).thenReturn(new Item[]{
                new Item(ItemID.FEATHER, 5000),
                new Item(ItemID.FISHING_BAIT, 250),
                new Item(ItemID.BRUT_SPAWNING_TROUT, 1),
                new Item(ItemID.BRUT_SPAWNING_SALMON, 1),
                new Item(ItemID.BRUT_STURGEON, 1)
        });

        BarbarianFishingSupplyCalculator calc = new BarbarianFishingSupplyCalculator(client, itemManager);

        assertTrue(calc.hasBarbarianRodEquippedOrInInventory());
        assertEquals("Pearl barbarian rod", calc.getRodName());
        assertTrue(calc.hasBaitInInventory());
        assertEquals(5250, calc.getTotalBaitCount());
        assertEquals(3, calc.countLeapingFishInInventory());
        assertEquals(2, calc.countAnglerPieces());
        assertFalse(calc.hasSpecHarpoonEquipped());
    }

    @Test
    public void testLeftClickDropSwapOnlySwapsLeapingFish() {
        BarbarianFishingPlugin plugin = new BarbarianFishingPlugin();
        int inventoryWidgetId = InterfaceID.INVENTORY << 16;

        // 1. Leaping trout: Use is on top, Drop is CC_OP_LOW_PRIORITY -> should swap Drop to top as CC_OP
        Menu rootFish = mock(Menu.class);
        Widget fishWidget = mock(Widget.class);
        when(fishWidget.getId()).thenReturn(inventoryWidgetId);
        when(fishWidget.getItemId()).thenReturn(ItemID.BRUT_SPAWNING_TROUT);

        MenuEntry cancel = mock(MenuEntry.class);
        when(cancel.getOption()).thenReturn("Cancel");
        MenuEntry examine = mock(MenuEntry.class);
        when(examine.getOption()).thenReturn("Examine");
        when(examine.getWidget()).thenReturn(fishWidget);
        MenuEntry drop = mock(MenuEntry.class);
        when(drop.getOption()).thenReturn("Drop");
        when(drop.getWidget()).thenReturn(fishWidget);
        when(drop.getItemId()).thenReturn(ItemID.BRUT_SPAWNING_TROUT);
        when(drop.getType()).thenReturn(MenuAction.CC_OP_LOW_PRIORITY);
        MenuEntry use = mock(MenuEntry.class);
        when(use.getOption()).thenReturn("Use");
        when(use.getWidget()).thenReturn(fishWidget);
        when(use.getType()).thenReturn(MenuAction.WIDGET_TARGET);

        MenuEntry[] fishEntries = new MenuEntry[]{cancel, examine, drop, use};
        when(rootFish.getMenuEntries()).thenReturn(fishEntries);

        assertTrue(plugin.swapDropToTopIfLeapingFish(rootFish));
        assertSame(drop, fishEntries[fishEntries.length - 1]);
        assertSame(use, fishEntries[2]);
        verify(drop).setType(MenuAction.CC_OP);
        verify(rootFish).setMenuEntries(fishEntries);

        // 2. Barbarian rod: must NOT swap Drop to top
        Menu rootRod = mock(Menu.class);
        Widget rodWidget = mock(Widget.class);
        when(rodWidget.getId()).thenReturn(inventoryWidgetId);
        when(rodWidget.getItemId()).thenReturn(ItemID.BRUT_FISHING_ROD);

        MenuEntry rodDrop = mock(MenuEntry.class);
        when(rodDrop.getOption()).thenReturn("Drop");
        when(rodDrop.getWidget()).thenReturn(rodWidget);
        when(rodDrop.getItemId()).thenReturn(ItemID.BRUT_FISHING_ROD);
        MenuEntry rodUse = mock(MenuEntry.class);
        when(rodUse.getOption()).thenReturn("Use");
        when(rodUse.getWidget()).thenReturn(rodWidget);

        MenuEntry[] rodEntries = new MenuEntry[]{cancel, rodDrop, rodUse};
        when(rootRod.getMenuEntries()).thenReturn(rodEntries);

        assertFalse(plugin.swapDropToTopIfLeapingFish(rootRod));
        assertSame(rodUse, rodEntries[rodEntries.length - 1]);
        verify(rootRod, never()).setMenuEntries(rodEntries);
    }
}
