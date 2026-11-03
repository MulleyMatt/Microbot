package net.runelite.client.plugins.microbot.mta;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ObjectID;

@Getter
@RequiredArgsConstructor
public enum MtaRoomType
{
	ENCHANTMENT(
		"Enchanting Chamber",
		ObjectID.MAGICTRAINING_ENCHANTDOOR,
		InterfaceID.MAGICTRAINING_ENCHA,
		InterfaceID.MagictrainingEncha.MAGICTRAIN_ENCHA_PTS,
		2000,
		"Water / Accuracy Wand",
		"Cast Lvl-7 Enchant on Dragonstones (14 pts) or Bonus Shape (9 pts)"
	),
	ALCHEMY(
		"Alchemist's Playground",
		ObjectID.MAGICTRAINING_ALCHEMDOOR,
		InterfaceID.MAGICTRAINING_ALCHEM,
		InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_PTS,
		300,
		"Fire Staff",
		"High Alch the 30-coin item & deposit coins in Coin Collector (100c = 1 pt)"
	),
	GRAVEYARD(
		"Creature Graveyard",
		ObjectID.MAGICTRAINING_GRAVEDOOR,
		InterfaceID.MAGICTRAINING_GRAVE,
		InterfaceID.MagictrainingGrave.MAGICTRAIN_GRAVE_PTS,
		200,
		"Earth / Mud Staff",
		"Grab bones until value >= 16, cast Bones to Bananas, deposit in Food Chute"
	),
	TELEKINETIC(
		"Telekinetic Theatre",
		ObjectID.MAGICTRAINING_TELEDOOR,
		InterfaceID.MAGICTRAINING_TELE,
		InterfaceID.MagictrainingTele.MAGICTRAIN_TELE_PTS,
		200,
		"Air Staff",
		"Run to the green tile & cast Telekinetic Grab on the Maze Guardian"
	);

	private final String roomName;
	private final int portalObjectId;
	private final int interfaceGroupId;
	private final int pointsWidgetId;
	private final int defaultTargetPoints;
	private final String recommendedStaffName;
	private final String summaryGuide;
}
