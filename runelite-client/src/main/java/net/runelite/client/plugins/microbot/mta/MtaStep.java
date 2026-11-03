package net.runelite.client.plugins.microbot.mta;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MtaStep
{
	PREPARE_SUPPLIES("Prepare Supplies", "Withdraw or buy missing runes, staves, food & staminas"),
	TRAVEL_TO_MTA("Travel to MTA", "Teleport to Emir's Arena and enter Mage Training Arena north"),
	GET_PIZAZZ_HAT("Get Pizazz Hat", "Talk to the Entrance Guardian to receive a Pizazz Progress Hat"),
	MTA_LOBBY("Go to MTA & Choose a Room", "Go to the Magic Training Arena and choose a room"),
	IN_ENCHANTMENT_ROOM("Enchanting Chamber", "Pick up Dragonstones or Bonus Shapes and cast Lvl-7 Enchant"),
	IN_ALCHEMY_ROOM("Alchemist's Playground", "Take the 30-coin item from the highlighted cupboard and High Alch it"),
	IN_GRAVEYARD_ROOM("Creature Graveyard", "Grab bones until value >= 16, cast Bones to Bananas, deposit in Chute"),
	IN_TELEKINETIC_ROOM("Telekinetic Theatre", "Stand on the green tile and cast Telekinetic Grab on the Maze Guardian"),
	BUY_BONES_TO_PEACHES("Buy Bones to Peaches", "Climb the lobby stairs and buy Bones to Peaches from Rewards Guardian"),
	TRAVEL_TO_PALACE("Go to Al Kharid Palace", "Bring 1 Bones, Nature/Water/Earth runes to Al Kharid Palace"),
	CAST_BONES_TO_PEACHES("Cast Bones to Peaches", "Stand inside Al Kharid Palace and cast Bones to Peaches!"),
	DIARY_COMPLETE("Diary Task Complete!", "Lumbridge & Draynor Hard Diary task completed!");

	private final String title;
	private final String defaultInstruction;
}
