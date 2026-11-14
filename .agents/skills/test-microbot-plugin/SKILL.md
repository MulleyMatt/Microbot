---
name: test-microbot-plugin
description: >-
  Executes fast, headless unit tests for Microbot helper plugins in seconds without launching the game. Validates supply math, step progression, and item IDs, avoiding expensive full shaded-jar compile and deploy loops.
---

# Test Microbot Plugin Skill

Use this skill to quickly verify and debug plugin logic (supply math, state transitions, item IDs, and bank calculations) in **3-5 seconds** without waiting 60+ seconds for `./gradlew :client:assemble` or manual in-game testing via the Jagex Launcher.

---

## 1. Quick Test Execution Commands

### Run Tests for a Specific Plugin
```powershell
.\gradlew.bat :client:runUnitTests --tests net.runelite.client.plugins.microbot.<feature>.*
```

### Run a Single Test Class
```powershell
.\gradlew.bat :client:runUnitTests --tests net.runelite.client.plugins.microbot.<feature>.<Feature>Test
```

### Run All Custom Helper Plugins & UI Tests Together
```powershell
.\gradlew.bat :client:runUnitTests --tests "net.runelite.client.plugins.microbot.ui.MicrobotPluginListPanelModeTest" --tests "net.runelite.client.plugins.microbot.herbrun.*" --tests "net.runelite.client.plugins.microbot.redwood.*" --tests "net.runelite.client.plugins.microbot.muspah.*" --tests "net.runelite.client.plugins.microbot.mta.*" --tests "net.runelite.client.plugins.microbot.spindel.*" --tests "net.runelite.client.plugins.microbot.wildyslayer.*" --tests "net.runelite.client.plugins.microbot.barbarianfishing.*" --tests "net.runelite.client.plugins.microbot.cachewarningfilter.*"
```

### Run a Single Test Method
```powershell
.\gradlew.bat :client:runUnitTests --tests net.runelite.client.plugins.microbot.<feature>.<Feature>Test.<testMethodName>
```

---

## 2. Standard Unit Test Structure

Every helper plugin should have a corresponding test class under:
`runelite-client/src/test/java/net/runelite/client/plugins/microbot/<feature>/<Feature>Test.java`

### Core Assertions to Cover:
1. **Supply Multiplication**: Verify required supplies scale correctly with `batchCount` (e.g. 1 run vs 5 runs).
2. **Item Availability Math**: Verify `ownedCount` is correctly calculated when items are in Inventory, Equipment, or Bank.
3. **Step Transitions**: Verify state machine advances only when all required conditions are satisfied.
4. **Item ID Sanity**: Verify that item IDs used for GE purchasing are valid and tradeable.

---

## 3. Mocking Patterns (Zero-Client Testing)

### A. Container & Supply Math Mocking
Use Mockito to simulate game containers without needing a live game instance:

```java
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MyPluginSupplyTest {

    @Test
    public void testSupplyCalculation() {
        Client client = mock(Client.class);
        ItemContainer inventory = mock(ItemContainer.class);
        
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
        when(inventory.count(1234)).thenReturn(2);

        // Execute supply calculator logic
        int missing = MySupplyCalculator.calculateMissing(client, 1234, 5);
        assertEquals(3, missing);
    }
}
```

### B. Left-Click `MenuEntry` Swapping (`onPostMenuSort`)
Mock `Menu`, inventory `Widget` (`InterfaceID.INVENTORY << 16`), and `MenuEntry[]` to assert that `Drop` moves to `entries[entries.length - 1]` and upgrades `MenuAction.CC_OP_LOW_PRIORITY` to `MenuAction.CC_OP` only for target items:

```java
int inventoryWidgetId = InterfaceID.INVENTORY << 16;
Menu root = mock(Menu.class);
Widget itemWidget = mock(Widget.class);
when(itemWidget.getId()).thenReturn(inventoryWidgetId);
when(itemWidget.getItemId()).thenReturn(ItemID.BRUT_SPAWNING_TROUT);

MenuEntry cancel = mock(MenuEntry.class);
when(cancel.getOption()).thenReturn("Cancel");
MenuEntry drop = mock(MenuEntry.class);
when(drop.getOption()).thenReturn("Drop");
when(drop.getWidget()).thenReturn(itemWidget);
when(drop.getItemId()).thenReturn(ItemID.BRUT_SPAWNING_TROUT);
when(drop.getType()).thenReturn(MenuAction.CC_OP_LOW_PRIORITY);
MenuEntry use = mock(MenuEntry.class);
when(use.getOption()).thenReturn("Use");
when(use.getWidget()).thenReturn(itemWidget);

MenuEntry[] entries = new MenuEntry[]{cancel, drop, use};
when(root.getMenuEntries()).thenReturn(entries);

assertTrue(plugin.swapDropToTopIfLeapingFish(root));
assertSame(drop, entries[entries.length - 1]);
verify(drop).setType(MenuAction.CC_OP);
verify(root).setMenuEntries(entries);
```

### C. Pure Static Decision Helpers (Anti-PK, Auto-Prayer, Auto-Thrall, Alerts)
Extract tick, varbit, and combat-bracket checks into package-private or `public static` pure helpers (like `WildySlayerPlugin.isThreatPlayer(...)`, `WildySlayerPlugin.shouldTriggerSeedPod(...)`, and `MuspahPlugin.shouldAttemptThrallCast(...)`) so edge cases can be tested exhaustively without mocking `Client`:

```java
assertTrue(WildySlayerPlugin.isThreatPlayer(
        100, 125, 25, false, false, true, false, WildySlayerConfig.PkerDetectionMode.COMBAT_BRACKET));
assertFalse(WildySlayerPlugin.shouldTriggerSeedPod(
        true, true, 31, 30, false, true, 10, 0)); // Above wildy 30
```

---

## 4. Diagnostic Checklist When Tests Fail

- **Check item IDs**: Confirm IDs match `net.runelite.api.gameval.ItemID` rather than hardcoded magic numbers.
- **Check batch logic**: Ensure math handles `batchCount = 1` and `batchCount = 0` gracefully.
- **Check nullability**: Ensure code checks `itemContainer == null` before calling methods on it.
