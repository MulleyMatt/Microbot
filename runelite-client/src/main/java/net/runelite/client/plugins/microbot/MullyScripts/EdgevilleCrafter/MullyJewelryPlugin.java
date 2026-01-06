package net.runelite.client.plugins.microbot.MullyScripts.EdgevilleCrafter;

import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.MullyScripts.EdgevilleCrafter.Jewelry.Jewelry;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
@PluginDescriptor(
        name = PluginDescriptor.Mully + "Edgeville Crafter",
        description = "Edgeville Diamond necklace jewelry crafter",
        tags = {"crafting", "magic", "microbot", "skilling"},
        enabledByDefault = false
)
public class MullyJewelryPlugin extends Plugin {

    @Inject
    private MullyJewelryConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private MullyJewelryOverlay mullyJewelryOverlay;

    @Inject
    private MullyJewelryScript mullyJewelryScript;

    @Getter
    private Jewelry jewelry;

    @Provides
    MullyJewelryConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MullyJewelryConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        mullyJewelryScript.run();

        if (overlayManager != null) {
            overlayManager.add(mullyJewelryOverlay);
        }
    }

    protected void shutDown() {
        overlayManager.remove(mullyJewelryOverlay);
        mullyJewelryScript.shutdown();
    }

    @Subscribe
    public void onConfigChanged(final ConfigChanged event){
        if (!(event.getGroup().equals(MullyJewelryConfig.configGroup))) return;
    }
}
