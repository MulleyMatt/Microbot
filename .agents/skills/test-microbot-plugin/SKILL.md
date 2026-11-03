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

---

## 4. Diagnostic Checklist When Tests Fail

- **Check item IDs**: Confirm IDs match `net.runelite.api.gameval.ItemID` rather than hardcoded magic numbers.
- **Check batch logic**: Ensure math handles `batchCount = 1` and `batchCount = 0` gracefully.
- **Check nullability**: Ensure code checks `itemContainer == null` before calling methods on it.
