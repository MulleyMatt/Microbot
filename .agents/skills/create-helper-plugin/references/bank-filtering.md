# Bank Filtering & Search Callback System

This guide explains how to inject custom item filters directly into the RuneLite bank interface using native CS2 callbacks without having to manually reposition or reconstruct bank widgets.

---

## 1. The 3 Native CS2 Callbacks

RuneLite exposes three callbacks via `ScriptCallbackEvent` that drive the bank's internal layout engine:

| Event Name | Script Source | Role & Implementation |
|---|---|---|
| `"bankBuildTab"` | Script 277 (`BankMainBuild.rs2asm`) | When filtering is active, set `intStack[intStackSize - 1] = 1`. This puts the bank into single-tab filter mode (`iload 42 = 1`). |
| `"getSearchingTagTab"` | Script 276 (`TriggerBankLayout.rs2asm`) | When filtering is active, set `intStack[intStackSize - 1] = 1`. This signals that custom tag search is active and tells the bank to evaluate all items across tabs. |
| `"bankSearchFilter"` | Script 279 (`BankSearchFilter.rs2asm`) | Fired for each bank slot. Item ID is at `intStack[intStackSize - 1]`. Set `intStack[intStackSize - 2] = 1` to show the item, or `0` to hide it (`cc_sethide 1`). |

When an item is hidden with `0`, the client automatically packs all remaining matching items into clean contiguous rows with proper scrollbar height.

---

## 2. EventBus Priority

Always register `ScriptCallbackEvent` with priority `-1`:
```java
@Subscribe(priority = -1)
public void onScriptCallbackEvent(ScriptCallbackEvent event) {
    ...
}
```
Priority `-1` ensures your filter runs after default-priority plugins (such as BankTags), guaranteeing that your filter state takes precedence.

---

## 3. Dynamic Bank Layout & Widget Injection

### Triggering Layout Updates
Whenever items are withdrawn or deposited, call `bankSearch.layoutBank()` on the `ClientThread`:
```java
clientThread.invokeLater(bankSearch::layoutBank);
```

### Bank Universe Button Injection
To add a toggle button on the bank frame:
- **Parent**: `client.getWidget(InterfaceID.Bankmain.UNIVERSE)`
- **Position**: `x = 380, y = 5`, size `25x25` (places it neatly to the left of Quest Helper at `x = 408`).
- **Sprites**: `SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL` and `UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED`.

### Title Bar Customization
On `ScriptPreFired` with `scriptId == ScriptID.BANKMAIN_FINISHBUILDING`, customize the title:
```java
Widget bankTitle = client.getWidget(InterfaceID.Bankmain.TITLE);
if (bankTitle != null && active) {
    bankTitle.setText("Helper Supplies <col=ff9040>(Missing)</col>");
}
```
