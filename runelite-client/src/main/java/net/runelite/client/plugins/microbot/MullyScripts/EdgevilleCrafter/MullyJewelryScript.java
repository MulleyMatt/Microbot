package net.runelite.client.plugins.microbot.MullyScripts.EdgevilleCrafter;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.MullyScripts.EdgevilleCrafter.Jewelry.State;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class MullyJewelryScript extends Script {

    // Edgeville furnace constants
    private static final WorldPoint EDGEVILLE_FURNACE_LOCATION = new WorldPoint(3109, 3499, 0);
    private static final int EDGEVILLE_FURNACE_OBJECT_ID = ObjectID.FURNACE_16469;

    private final MullyJewelryPlugin plugin;
    public static State state;

    @Inject
    public MullyJewelryScript(MullyJewelryPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean run() {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyCraftingSetup();
        Rs2AntibanSettings.dynamicActivity = true;
        Rs2Walker.disableTeleports = true;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();
                if (setFullView()) return;

                if (hasStateChanged()) {
                    state = updateState();
                }

                if (state == null)
                {
                    state = State.BANKING;
                }

                if (Rs2Player.isMoving() || Rs2Player.isAnimating() || Rs2Antiban.getCategory().isBusy()) return;
                if (Rs2AntibanSettings.actionCooldownActive) return;

                switch (state) {
                    case BANKING:
                        boolean isBankOpen = Rs2Bank.isNearBank(BankLocation.EDGEVILLE, 20) ?
                                Rs2Bank.openBank() :
                                Rs2Bank.walkToBankAndUseBank(BankLocation.EDGEVILLE);

                        if (!isBankOpen || !Rs2Bank.isOpen()) return;

                        int maxPerTrip = 13;
                        int availableBars = Rs2Bank.count(ItemID.GOLD_BAR);
                        int craftAmount = Math.min(maxPerTrip, availableBars);

                        if (craftAmount > 0) {
                            if (!Rs2Inventory.isEmpty()){
                                Rs2Bank.depositAllExcept(ItemID.NECKLACE_MOULD);
                                Rs2Inventory.waitForInventoryChanges(1800);
                            }

                            if (!Rs2Inventory.hasItem(ItemID.NECKLACE_MOULD)) {
                                if (!Rs2Bank.hasItem(ItemID.NECKLACE_MOULD)) {
                                    Microbot.showMessage("Missing tool item");
                                    shutdown();
                                    return;
                                }
                                Rs2Bank.withdrawOne(ItemID.NECKLACE_MOULD);
                                Rs2Inventory.waitForInventoryChanges(1800);
                            }

                            Rs2Bank.withdrawX(ItemID.DIAMOND, craftAmount);
                            Rs2Inventory.waitForInventoryChanges(1800);
                            Rs2Bank.withdrawX(ItemID.GOLD_BAR, craftAmount);
                            Rs2Inventory.waitForInventoryChanges(1800);

                            Rs2Bank.closeBank();
                            sleepUntil(() -> !Rs2Bank.isOpen());
                            return;
                        }

                        if (!Rs2Inventory.isEmpty()) {
                            Rs2Bank.depositAll();
                        }
                        Microbot.showMessage("Crafting has been completed!");
                        shutdown();
                        return;
                    case CRAFTING:
                        TileObject furnaceObject = Rs2GameObject.getGameObject(EDGEVILLE_FURNACE_OBJECT_ID);

                        if (furnaceObject == null) {
                            Rs2Walker.walkTo(EDGEVILLE_FURNACE_LOCATION);
                            return;
                        }

                        if (!Rs2Camera.isTileOnScreen(furnaceObject.getLocalLocation())) {
                            Rs2Camera.turnTo(furnaceObject.getLocalLocation());
                            return;
                        }

                        Rs2GameObject.interact(furnaceObject, "smelt");
                        sleepUntilTrue(Rs2Widget::isGoldCraftingWidgetOpen, 500, 20000);
                        Rs2Widget.clickWidget("diamond necklace");
                        Rs2Antiban.actionCooldown();
                        Rs2Antiban.takeMicroBreakByChance();
                        break;
                    default:
                        // Handle any other states that shouldn't occur in simplified version
                        break;
                }

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean setFullView() {
        boolean changed = false;
        if (Rs2Camera.getZoom() > 200) {
            Rs2Camera.setZoom(200);
            changed = true;
        }
        if (Rs2Camera.getPitch() < 380) {
            Rs2Camera.setPitch(383);
            changed = true;
        }
        int yaw = Rs2Camera.getYaw();
        if (yaw > 16 && yaw < 2032) {
            Rs2Camera.setYaw(0);
            changed = true;
        }
        return changed;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
        state = null;
    }

    private boolean hasStateChanged() {
        // If state is null (on plugin start)
        if (state == null) return true;
        // If the player should bank (has finished crafting jewelry)
        if (state != State.BANKING && shouldBank()) return true;
        return state == State.BANKING && isCrafting();
    }

    private State updateState() {
        if (shouldBank()) return State.BANKING;
        if (isCrafting()) return State.CRAFTING;
        return null;
    }

    private boolean shouldBank() {
        return hasFinishedCrafting();
    }

    private boolean hasFinishedCrafting() {
        if (!Rs2Inventory.hasItem(ItemID.NECKLACE_MOULD)) return false;

        boolean hasCraftingItem = Rs2Inventory.hasItem(ItemID.GOLD_BAR);
        boolean hasCutGem = Rs2Inventory.hasItem(ItemID.DIAMOND);

        return Rs2Inventory.hasItem(ItemID.DIAMOND_NECKLACE) && !hasCraftingItem && !hasCutGem;
    }

    private boolean isCrafting() {
        if(!Rs2Inventory.hasItem(ItemID.NECKLACE_MOULD)) return false;

        boolean hasCraftingItem = Rs2Inventory.hasItem(ItemID.GOLD_BAR);
        boolean hasCutGem = Rs2Inventory.hasItem(ItemID.DIAMOND);

        return hasCraftingItem && hasCutGem;
    }
}
