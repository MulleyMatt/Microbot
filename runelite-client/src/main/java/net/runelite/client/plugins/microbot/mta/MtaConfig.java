package net.runelite.client.plugins.microbot.mta;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(MtaConfig.CONFIG_GROUP)
public interface MtaConfig extends Config
{
	String CONFIG_GROUP = "microbotmtahelper";

	@ConfigSection(
		name = "Diary & Point Goals",
		description = "Target Pizazz Points for Bones to Peaches and Lumbridge Hard Diary",
		position = 0
	)
	String diarySection = "diarySection";

	@ConfigSection(
		name = "Supply & Gear Checklist",
		description = "Configure staves, runes, food, and stamina potions for Bank/GE filters",
		position = 1
	)
	String supplySection = "supplySection";

	@ConfigSection(
		name = "Overlays & Audio Alerts",
		description = "Configure 3D room guides, staff reminders, low HP warnings, and goal alerts",
		position = 2
	)
	String alertsSection = "alertsSection";

	// --- Diary & Point Goals ---

	@ConfigItem(
		keyName = "telekineticTargetPoints",
		name = "Telekinetic Target Points",
		description = "Telekinetic Pizazz Points needed for Bones to Peaches (default 200)",
		position = 0,
		section = diarySection
	)
	default int telekineticTargetPoints()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "alchemistTargetPoints",
		name = "Alchemist Target Points",
		description = "Alchemist Pizazz Points needed for Bones to Peaches (default 300)",
		position = 1,
		section = diarySection
	)
	default int alchemistTargetPoints()
	{
		return 300;
	}

	@ConfigItem(
		keyName = "enchantmentTargetPoints",
		name = "Enchantment Target Points",
		description = "Enchantment Pizazz Points needed for Bones to Peaches (default 2000)",
		position = 2,
		section = diarySection
	)
	default int enchantmentTargetPoints()
	{
		return 2000;
	}

	@ConfigItem(
		keyName = "graveyardTargetPoints",
		name = "Graveyard Target Points",
		description = "Graveyard Pizazz Points needed for Bones to Peaches (default 200)",
		position = 3,
		section = diarySection
	)
	default int graveyardTargetPoints()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "trackAlKharidPalaceCast",
		name = "Track Al Kharid Palace Cast",
		description = "Guide the final Lumbridge & Draynor Hard Diary step to cast Bones to Peaches inside Al Kharid Palace",
		position = 4,
		section = diarySection
	)
	default boolean trackAlKharidPalaceCast()
	{
		return true;
	}

	// --- Supply & Gear Checklist ---

	@ConfigItem(
		keyName = "requireRunePouch",
		name = "Check Rune Pouch",
		description = "Include Rune Pouch / Divine Rune Pouch in the supply checklist",
		position = 0,
		section = supplySection
	)
	default boolean requireRunePouch()
	{
		return true;
	}

	@ConfigItem(
		keyName = "requireElementalStaves",
		name = "Check Elemental Staves",
		description = "Check for Air staff (Telekinetic), Fire staff (Alchemy), and Earth/Mud staff (Graveyard)",
		position = 1,
		section = supplySection
	)
	default boolean requireElementalStaves()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeBloodRunesForEnchant7",
		name = "Include Blood Runes (Lvl-7 Enchant)",
		description = "Lvl-7 Enchant requires 1 Cosmic + 20 Soul + 20 Blood runes per cast",
		position = 2,
		section = supplySection
	)
	default boolean includeBloodRunesForEnchant7()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeWaterRunesIfNoMudStaff",
		name = "Include Water Runes (If No Mud Staff)",
		description = "Bones to Bananas/Peaches requires Water runes unless wielding a Mud Battlestaff or Kodai Wand",
		position = 3,
		section = supplySection
	)
	default boolean includeWaterRunesIfNoMudStaff()
	{
		return true;
	}

	@Range(min = 0, max = 12)
	@ConfigItem(
		keyName = "foodCount",
		name = "Food Count (Sharks/Anglers)",
		description = "Number of Sharks or Anglerfish to bring for the Creature Graveyard room",
		position = 4,
		section = supplySection
	)
	default int foodCount()
	{
		return 2;
	}

	@Range(min = 0, max = 8)
	@ConfigItem(
		keyName = "staminaCount",
		name = "Stamina Potions (4-dose)",
		description = "Number of Stamina potions to bring for Telekinetic & Alchemy rooms",
		position = 5,
		section = supplySection
	)
	default int staminaCount()
	{
		return 2;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "runsToPurchase",
		name = "GE Batch Multiplier",
		description = "Multiplier for consumable supplies when using the 1-click GE search filter",
		position = 6,
		section = supplySection
	)
	default int runsToPurchase()
	{
		return 1;
	}

	// --- Overlays & Audio Alerts ---

	@ConfigItem(
		keyName = "showHudOverlay",
		name = "Show Step-by-Step HUD",
		description = "Display the interactive step-by-step HUD panel in the top-left corner",
		position = 0,
		section = alertsSection
	)
	default boolean showHudOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSceneOverlay",
		name = "Show 3D Scene Clickboxes",
		description = "Highlight portals, cupboards, bone piles, bonus shape piles, chutes, and maze tiles in 3D",
		position = 1,
		section = alertsSection
	)
	default boolean showSceneOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInventoryAndSpellOverlay",
		name = "Highlight Inventory & Spells",
		description = "Highlight staves to swap, items to enchant/alch, food to eat, and spells to cast",
		position = 2,
		section = alertsSection
	)
	default boolean showInventoryAndSpellOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashOnRoomGoalReached",
		name = "Flash When Room Goal Reached",
		description = "Flash screen green and play sound the exact moment you reach a room's target points",
		position = 3,
		section = alertsSection
	)
	default boolean flashOnRoomGoalReached()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashOnWrongStaff",
		name = "Alert on Wrong Elemental Staff",
		description = "Highlight inventory staff and pulse orange if you enter a room without its elemental staff equipped",
		position = 4,
		section = alertsSection
	)
	default boolean flashOnWrongStaff()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashOnLowHp",
		name = "Flash on Low HP (Graveyard)",
		description = "Flash screen red and highlight food when hitpoints drop below threshold in Creature Graveyard",
		position = 5,
		section = alertsSection
	)
	default boolean flashOnLowHp()
	{
		return true;
	}

	@Range(min = 5, max = 50)
	@ConfigItem(
		keyName = "graveyardLowHpThreshold",
		name = "Graveyard Low HP Threshold",
		description = "Hitpoints level at or below which to trigger the Eat Food alert in Creature Graveyard",
		position = 6,
		section = alertsSection
	)
	default int graveyardLowHpThreshold()
	{
		return 16;
	}

	@ConfigItem(
		keyName = "playAlertSounds",
		name = "Play Audio Alerts",
		description = "Play async audio notification on room completion or low HP",
		position = 7,
		section = alertsSection
	)
	default boolean playAlertSounds()
	{
		return true;
	}

	@Range(min = 10, max = 100)
	@ConfigItem(
		keyName = "soundVolume",
		name = "Alert Sound Volume",
		description = "Volume for audio notifications (10-100%)",
		position = 8,
		section = alertsSection
	)
	default int soundVolume()
	{
		return 80;
	}

	@Range(min = 0, max = 15)
	@ConfigItem(
		keyName = "soundIntervalSeconds",
		name = "Sound Repeat Interval (s)",
		description = "Seconds between repeated alert sounds (0 = play once per alert)",
		position = 9,
		section = alertsSection
	)
	default int soundIntervalSeconds()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "customSoundPath",
		name = "Custom Sound File (.wav)",
		description = "Optional path to a custom .wav file for alerts",
		position = 10,
		section = alertsSection
	)
	default String customSoundPath()
	{
		return "";
	}

	// --- Persisted Room Points (auto-synced from MTA interfaces) ---

	@ConfigItem(
		keyName = "savedTelekineticPoints",
		name = "Saved Telekinetic Points",
		description = "Auto-synced Telekinetic Pizazz Points",
		hidden = true
	)
	default int savedTelekineticPoints()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "savedAlchemistPoints",
		name = "Saved Alchemist Points",
		description = "Auto-synced Alchemist Pizazz Points",
		hidden = true
	)
	default int savedAlchemistPoints()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "savedEnchantmentPoints",
		name = "Saved Enchantment Points",
		description = "Auto-synced Enchantment Pizazz Points",
		hidden = true
	)
	default int savedEnchantmentPoints()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "savedGraveyardPoints",
		name = "Saved Graveyard Points",
		description = "Auto-synced Graveyard Pizazz Points",
		hidden = true
	)
	default int savedGraveyardPoints()
	{
		return 0;
	}
}
