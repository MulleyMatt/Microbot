# Game Entities, Object IDs & Domain Gotchas

This reference catalogs verified game object IDs, widget IDs, and gotchas discovered during the development of previous Microbot helper plugins.

---

## 1. Woodcutting & Redwoods (Woodcutting Guild)

- **Platform Navigation**:
  - Upper platform rope ladder (plane 1 down to ground): `ObjectID.WCGUILD_REDWOOD_ROPETOP = 28858`.
  - Ground level rope ladder (plane 0 up to platform): `ObjectID.WCGUILD_REDWOOD_ROPEBOTTOM = 28857`.
  - Ground level bank chest: `ObjectID.WCGUILD_BANKCHEST = 28861`.
- **Chop Spots**:
  - Ready chop spots: `ObjectID.REDWOODTREE_L = 29668`, `ObjectID.REDWOODTREE_R = 29670`.
  - Depleted cut spots: `ObjectID.REDWOODTREE_L_CUT = 29669`, `ObjectID.REDWOODTREE_R_CUT = 29671`.
- **Special Attack Energy**:
  - `VarPlayerID.SA_ENERGY` stores energy where `1000 = 100%`.
  - Axes with the "Lumber Up" spec (+3 WC boost): `DRAGON_AXE`, `DRAGON_AXE_2H`, `CRYSTAL_AXE`, `CRYSTAL_AXE_2H`, `INFERNAL_AXE`, `_3A_AXE`, `TRAIL_GILDED_AXE`.
  - Axe must be equipped in the weapon slot to execute special attack.

---

## 2. Farming & Herb Patches

- **Teleport Items**:
  - POH House Tabs: `ItemID.POH_TABLET_TELEPORTTOHOUSE` (useful for Hosidius portal and Varlamore nexus/portal).
  - Basalt: `ItemID.STRONGHOLD_TELEPORT_BASALT`, `ItemID.WEISS_TELEPORT_BASALT`.
  - Tablets: `ItemID.LUNAR_TABLET_CATHERBY_TELEPORT`, `ItemID.POH_TABLET_ARDOUGNETELEPORT`, `ItemID.POH_TABLET_FALADORTELEPORT`, `ItemID.POH_TABLET_CAMELOTTELEPORT`.
- **Tools**:
  - `ItemID.MAGIC_SECATEURS` (or regular secateurs), `ItemID.SPADE`, `ItemID.DIBBER`, `ItemID.BUCKET_ULTRACOMPOST`, `ItemID.BOTTOMLESS_COMPOST_BUCKET`.

---

## 3. Chat Filtering via CS2 & Logback

- **ScriptCallbackEvent `"chatFilterCheck"`**:
  - Stack state:
    - `intStack[intStackSize - 3]`: Display flag (1 = show, 0 = hide).
    - `intStack[intStackSize - 2]`: `ChatMessageType` ordinal.
    - `intStack[intStackSize - 1]`: Unique `MessageNode` ID.
    - `objectStack[objectStackSize - 1]`: Message string.
  - Set `intStack[intStackSize - 3] = 0;` to hide.
- **Immediate Chat Refresh**:
  - Call `clientThread.invokeLater(client::refreshChat)` when filter settings change.
- **Logback Appender Filter**:
  - Pair CS2 filter with `GameChatAppender.addCustomFilter(Predicate<ILoggingEvent>)` to prevent blocked engine messages from filling the 100-message chat history ring buffer.

---

## 4. Phantom Muspah & Boss Prayer Overlays

- **Carapace Form NPC IDs**:
  - Green (Ranged): `NpcID.PHANTOM_MUSPAH = 12077` (Quest: `12073`).
  - Brown (Melee): `NpcID.PHANTOM_MUSPAH_12078 = 12078` (Quest: `12074`).
  - Teal (Prayer Shield): `NpcID.PHANTOM_MUSPAH_12079 = 12079` (Quest: `12075`).
  - Green (Post-Shield): `NpcID.PHANTOM_MUSPAH_12080 = 12080` (Quest: `12076`).
  - Dark (Teleporting / Shadow Orbs): `NpcID.PHANTOM_MUSPAH_12082 = 12082` (Quest: `12081`).
  - Wandering Shadow Orb: `NpcID.MUSPAH_PROJECTILE = 12083`.
- **Attack & Special Animations (`net.runelite.api.gameval.AnimationID`)**:
  - Charged Magic Corruption Attack (4-tick windup, pray Magic): `NPC_MUSPAH_ATTACK_MAGIC_01 = 9918`.
  - Standard Melee Attack: `NPC_MUSPAH_ATTACK_MELEE_01 = 9920`.
  - Standard Ranged Attack: `NPC_MUSPAH_ATTACK_RANGED_01 = 9922`.
  - Floor Spike Summon: `NPC_MUSPAH_ATTACK_SUMMON_01 = 9923`.
  - Center Shockwave Explosion (6-tick windup, hide behind spike): `NPC_MUSPAH_ATTACK_EXPLOSION_01 = 9925`.
  - Teleport Orbs Barrage: `NPC_MUSPAH_TELEPORT_DISAPPEAR_01 = 9928`, `NPC_MUSPAH_TELEPORT_ATTACK_01 = 9930`.
- **Floor Spike Objects (`net.runelite.api.gameval.ObjectID1`) & Spotanims**:
  - Pre-spike ground object: `MUSPAH_SPIKE_PRE = 46693`; warning spotanims: `2335`, `2336`.
  - Stationary floor spikes: `46694`, `46695`, `46696`.
  - Homing / moving floor spike: `MUSPAH_SPIKE_FOLLOW = 46697`.
  - Form transformation gotcha: A spike always spawns on the player's current tile whenever Muspah switches forms (`NpcChanged`).
- **Prayer Tab Marker & Sprites**:
  - Check if Prayer tab is open via `client.getVarcIntValue(171) == 5`.
  - Prayer widgets: `InterfaceID.Prayerbook.PRAYER13` (Magic), `PRAYER14` (Missiles), `PRAYER15` (Melee), `PRAYER18` (Smite).
  - Prayer sprites: `SpriteID.PRAYER_PROTECT_FROM_MAGIC = 127`, `PRAYER_PROTECT_FROM_MISSILES = 128`, `PRAYER_PROTECT_FROM_MELEE = 129`, `PRAYER_SMITE = 132`.

---

## 5. Mage Training Arena (MTA) & Lumbridge Hard Diary

- **Regions & Portals**:
  - Surface Lobby / Shop Region: `13363` (`x=3353..3374, y=3296..3325`; Plane 0 = Lobby portals, Plane 1 = Rewards Guardian `NpcID.MAGICTRAINING_GUARD_REWARDS = 5984`; also detectable via `InterfaceID.MAGICTRAINING_MAIN` / `InterfaceID.MAGICTRAINING_SHOP` widgets).
  - Underground Rooms Regions: `13462` (Plane 0 = Enchanting, Plane 1 = Graveyard, Plane 2 = Alchemy) and `13463` (Telekinetic Theatre).
  - Room Portals: `MAGICTRAINING_TELEDOOR = 23673`, `MAGICTRAINING_ENCHANTDOOR = 23674`, `MAGICTRAINING_ALCHEMDOOR = 23675`, `MAGICTRAINING_GRAVEDOOR = 23676`, `MAGICTRAINING_RETURNDOOR = 23677`.
- **Points & Diary Varbits/VarPlayers**:
  - Bones to Peaches unlocked: `VarbitID.MAGICTRAINING_BONESPEACHES = 1505` (`== 1`).
  - Lumbridge & Draynor Hard Diary "Cast Bones to Peaches in Al Kharid Palace": `(client.getVarpValue(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY) & (1 << 25)) != 0` inside zone `(3282..3303, 3159..3166, plane 0)`.
  - Room Point Widgets: `InterfaceID.MagictrainingTele.MAGICTRAIN_TELE_PTS`, `MagictrainingAlchem.MAGICTRAIN_ALCHEM_PTS`, `MagictrainingEncha.MAGICTRAIN_ENCHA_PTS`, `MagictrainingGrave.MAGICTRAIN_GRAVE_PTS`, and overview `InterfaceID.MagictrainingMain` (`553`).
- **Room Object & Item IDs**:
  - **Enchanting**: Shape piles `MAGICTRAINING_ENCHAN_SHAPEPILE1..4 = 23694..23697` (Cube, Cylinder, Icosahedron, Pentamid); bonus shape indicated by visible widget among `MagictrainingEncha.MAGICTRAIN_CUBE / CYLINDER / PENTAMID / ICOSAHEDRON`. Dragonstone ground spawn: `ItemID.MAGICTRAINING_DRAGONSTONE = 6903`; Orb deposit hole: `ObjectID.MAGICTRAINING_ENCHA_HOLE = 23698`.
  - **Alchemy**: Cupboards `MAGICTRAINING_ALCHEM_CUPBOARD0..5 = 23678..23689` (clockwise order: Boots -> Kiteshield -> Med Helm -> Emerald -> Longsword -> Empty); Coin Collector: `ObjectID.MAGICTRAINING_COIN_COLLECTOR = 10734`.
  - **Graveyard**: Bone piles `MAGICTRAINING_BONES_PILE1..4 = 10725..10728`; bone items `MAGICTRAINING_BONES1..4 = 6904..6907` (worth 1..4 bananas); Food Chute: `ObjectID.MAGICTRAINING_GRAVE_FOODSHUTE = 10735`; chute remainder varbit: `VarbitID.MAGICTRAINING_ENCH_CONVCOUNT = 1506`.
  - **Telekinetic**: Maze walls `MAGICTRAINING_MINIWALL = 10755`, finish pit `MAGICTRAINING_MINI_STATUE_PITFALL = 23672`, Maze Guardian `NpcID.MAGICTRAINING_GUARD_MAZE_INCOMPLETE = 6777`, `MOVING = 6778`, `COMPLETE = 6779`.

---

## 6. Spindel & Venenatis (Wilderness Boss Caves)

- **Regions & Wilderness Levels**:
  - **Web Chasm (Spindel)**: Region `6580` (`x=1600..1663, y=11520..11583`), Level `29` Wilderness (allows instant Level-30 teleports like `ItemID.MM2_ROYAL_SEED_POD = 19564`).
  - **Silk Chasm (Venenatis)**: Region `13727` (`x=3392..3455, y=10176..10239`), Level `35` Wilderness.
- **Boss & Minion NPC IDs (`net.runelite.api.gameval.NpcID`)**:
  - Spindel: `NpcID.VENENATIS_SINGLES = 11998`; Venenatis: `NpcID.VENENATIS = 6610`.
  - Spiderlings: `NpcID.SPINDEL_SPIDERLING = 12001`, `NpcID.VENENATIS_SPIDERLING = 12000`.
- **Attack Animations (`AnimationID`) & Projectiles (`SpotanimID`)**:
  - 16-attack cycle alternating 8 Ranged (`NPC_VENENATIS_RANGED_01 = 9989`, projectile `FX_VENENATIS_RANGED_PROJECTILE = 2356`) and 8 Magic (`NPC_VENENATIS_MAGIC_01 = 9990`, projectile `FX_VENENATIS_MAGIC_PROJECTILE = 2358`); Melee (`NPC_VENENATIS_MELEE_01 = 9991`); Death (`NPC_VENENATIS_DEATH_01 = 9992`).
- **Expanding Web Hazard & Cave Exit Objects (`ObjectID1`)**:
  - Web projectile: `FX_VENENATIS_WEB_PROJECTILE = 2360`; Web objects: `WBR_VENENATIS_WEB_CENTRE = 47084`, `WBR_VENENATIS_WEB_EDGE = 47085`, `WBR_VENENATIS_WEB_CORNER = 47086`, `VENENATIS_WEBBED_FLOOR = 26764` (expands to a 7x7 area around center).
  - Cave Exits: `WILD_BOSS_ESCAPE_CAVE_EXIT_VENENATIS = 47155`, `WILD_BOSS_ESCAPE_CAVE_EXIT01 = 47147`, `WILD_BOSS_ESCAPE_CAVE_EXIT02 = 47148`.
- **Client-Thread Safe Inventory Teleport**:
  - Never call `Rs2Inventory.interact(...)` on the client thread because `Rs2Tab.switchTo(InterfaceTab.INVENTORY)` calls `sleepUntil(...)`. Instead, invoke `MenuAction.CC_OP` directly on `InterfaceID.Inventory.ITEMS` (`param0 = slot`, `identifier = 2` for `"Commune"` on `ItemID.MM2_ROYAL_SEED_POD`) via `Microbot.doInvoke`.

---

## 7. Barbarian Fishing (Otto's Grotto & Mount Quidamortem)

- **Fishing Spot NPC IDs (`net.runelite.api.gameval.NpcID`)**:
  - Otto's Grotto: `NpcID._0_39_54_BRUT_FISHING_SPOT = 1542`.
  - Mount Quidamortem (Chambers of Xeric exterior): `NpcID._0_19_55_BRUT_FISHING_SPOT = 7323`.
- **Rods & Bait (`net.runelite.api.gameval.ItemID`)**:
  - Rods: `ItemID.BRUT_FISHING_ROD`, `ItemID.FISHINGROD_PEARL_BRUT`.
  - Bait: `ItemID.FEATHER`, `ItemID.FISHING_BAIT`, `ItemID.HUNTING_STRIPY_BIRD_FEATHER`, `ItemID.BRUT_FISH_CUTS`, `ItemID.BRUT_ROE`, `ItemID.BRUT_CAVIAR`.
- **Leaping Fish (`ItemID`)**:
  - `ItemID.BRUT_SPAWNING_TROUT`, `ItemID.BRUT_SPAWNING_SALMON`, `ItemID.BRUT_STURGEON`.
- **Fishing Animations (`net.runelite.api.AnimationID`)**:
  - `FISHING_OILY_ROD`, `FISHING_POLE_CAST`, `FISHING_BARBARIAN_ROD`, `FISHING_PEARL_BARBARIAN_ROD`, `FISHING_PEARL_BARBARIAN_ROD_2`.
- **Special Attack Harpoons (`VarPlayerID.SA_ENERGY >= 1000`)**:
  - `DRAGON_HARPOON`, `INFERNAL_HARPOON`, `CRYSTAL_HARPOON`, `TRAILBLAZER_HARPOON`, `TRAILBLAZER_HARPOON_NO_INFERNAL`, `TRAILBLAZER_RELOADED_HARPOON`, `TRAILBLAZER_RELOADED_HARPOON_NO_INFERNAL`.

---

## 8. Wilderness Slayer & Anti-PK Detection

- **Wilderness Presence**:
  - Check `client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1` or `Rs2Pvp.getWildernessLevelFrom(worldPoint) > 0`.
- **Wilderness Level Resolution**:
  - Parse `InterfaceID.PvpIcons.WILDERNESSLEVEL` widget text (`"Level:\\s*(\\d+)"`) when `!wildyWidget.isHidden()`, falling back to `Rs2Pvp.getWildernessLevelFrom(worldPoint)` (which covers surface Wilderness, Wilderness Slayer Cave, Revenant Caves, and boss caves).
- **Teleblock Detection**:
  - `client.getVarbitValue(VarbitID.TELEBLOCK_CYCLES) > 0`.
- **Attackable Combat Bracket**:
  - Compare `Math.abs(localCombat - otherCombat) <= Math.max(1, Math.max(localWildy, otherWildy)) + (WorldType.isPvpWorld(client.getWorldType()) ? 15 : 0)`, factoring in `other.isFriend() || other.isClanMember() || other.isFriendsChatMember()`.

