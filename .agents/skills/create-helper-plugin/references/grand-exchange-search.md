# Grand Exchange 1-Click Search Filter Integration

This guide details the native RuneLite Grand Exchange search filtering architecture (proven by Quest Helper and Herb Run Helper) to filter the GE search interface to only missing items with a single click.

---

## 1. Core Architecture & CS2 Reentrancy Rules

### Why CS2 Reentrancy Matters
- In Old School RuneScape, the CS2 virtual machine is single-threaded and **NOT reentrant**.
- When the player opens the Grand Exchange search bar, the client executes CS2 script 752 (`[proc,ge_item_search]`).
- At opcode 4210 (`ge_search`), the client triggers RuneLite's `onGrandExchangeSearched(GrandExchangeSearched event)` callback.
- **NEVER** invoke any method that runs a CS2 script inside `onGrandExchangeSearched`. For example:
  - `Quest.getState(client)` calls `client.runScript(ScriptID.QUEST_STATUS_GET, id)`.
  - Running an inner script overwrites the VM's active local stack (`uj.ag`) with the inner script's small local stack.
  - When control returns to script 752, instruction 46 (`istore 5`) crashes with:
    ```
    Client error: 752 34 java.lang.ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 4
    ```
    leaving GE search results completely blank.

### The 4 Rules for Safe GE Search Injection
1. **Pre-compute Tradeable Missing IDs**: Calculate missing item IDs *before* triggering search (in `activate()`), storing them in an in-memory array (`short[] cachedMissingTradeableIds`).
2. **Trigger Native Search via `onKeyListener`**:
   ```java
   Widget geSearchBox = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
   Object[] scriptArgs = geSearchBox.getOnKeyListener();
   client.runScript(scriptArgs);
   geSearchBox.setHidden(true);
   ```
3. **Pure In-Memory Event Handling**: In `onGrandExchangeSearched`, only check input tag, call `event.consume()`, and assign `client.setGeSearchResultIds(cachedMissingTradeableIds)`. Zero script calls, zero container checks.
4. **DO NOT Touch `varc 66`**: Never set `client.setVarcIntValue(66, 1)`. Setting varc 66 triggers widget 10616871's OnTimerListener (script 751), causing repeated crashes every second.

---

## 2. Widget Injection Timing & Positioning

- **Event**: Listen for `@Subscribe ScriptPostFired` where `event.getScriptId() == 750` (`SEARCHBOX_LOADED`).
- **Parent Widget**: `client.getWidget(InterfaceID.Chatbox.MES_LAYER)`.
- **Positioning**:
  - Size: `20x20`
  - Position: `x = 455, y = 0` (places button directly to the left of Quest Helper at `x = 480` with zero overlap).
  - Background sprite: `SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL` (unselected) and `UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED` (active).
  - Child icon: sized `16x16` at `x = 457, y = 2`.

---

## 3. Tradeable Validation

Only pass GE-tradeable item IDs to `setGeSearchResultIds`. Untradeable items (such as untradeable tool variants or quest items) will corrupt the search results:
```java
Set<Integer> tradeable = itemIds.stream()
    .filter(id -> itemManager.getItemComposition(id).isGeTradeable())
    .collect(Collectors.toCollection(TreeSet::new));
short[] ids = Shorts.toArray(tradeable);
```
