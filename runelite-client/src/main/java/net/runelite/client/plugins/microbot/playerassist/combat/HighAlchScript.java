package net.runelite.client.plugins.microbot.playerassist.combat;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.playerassist.PlayerAssistConfig;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Item;
import net.runelite.client.plugins.microbot.util.item.Rs2ExplorersRing;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.settings.Rs2Settings;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class HighAlchScript extends Script {

    int alchemyValueWarningVarbit = 6091;

    public boolean run(PlayerAssistConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || !config.toggleHighAlchProfitable()) return;
                List<Rs2Item> items = Rs2Inventory.getList(Rs2Item::isHaProfitable);

                if (items.isEmpty()) return;

                if (Rs2ExplorersRing.hasRing() && Rs2ExplorersRing.hasCharges()) {
                    for (Rs2Item item: items) {
                        Rs2ExplorersRing.highAlch(item);
                    }
                    Rs2ExplorersRing.closeInterface();
                } else  if (Rs2Magic.canCast(MagicAction.HIGH_LEVEL_ALCHEMY)) {
                    for (Rs2Item item: items) {
                        Rs2Magic.alch(item);
                        if (item.getHaPrice() > Rs2Settings.getMinimumItemValueAlchemyWarning()) {
                            sleepUntil(() -> Rs2Widget.hasWidget("Proceed to cast High Alchemy on it"));
                            if (Rs2Widget.hasWidget("Proceed to cast High Alchemy on it")) {
                                Rs2Keyboard.keyPress('1');
                            }
                        }
                    }
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }


    public void shutdown() {
        super.shutdown();
    }
}