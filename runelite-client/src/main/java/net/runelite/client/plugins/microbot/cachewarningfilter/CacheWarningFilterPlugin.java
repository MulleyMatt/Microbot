package net.runelite.client.plugins.microbot.cachewarningfilter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.GameChatAppender;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Cache Warning Filter",
        description = "Hides 'Mismatch in overlaid cache archive hash' warnings from the chat",
        tags = {"cache", "filter", "chat", "microbot", "warning", "helper"},
        enabledByDefault = true,
        alwaysOn = true
)
@Slf4j
public class CacheWarningFilterPlugin extends Plugin {

    public static final String CACHE_HASH_MISMATCH_FRAGMENT = "Mismatch in overlaid cache archive hash";
    public static final String OVERLAID_CACHE_FRAGMENT = "overlaid cache";

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private CacheWarningFilterConfig config;

    @Inject
    private CacheWarningFilterOverlay overlay;

    @Getter
    private final AtomicInteger blockedCount = new AtomicInteger(0);

    private final Predicate<ILoggingEvent> appenderFilter = this::shouldFilterLogEvent;

    private List<String> cachedCustomKeywords = Collections.emptyList();

    @Provides
    CacheWarningFilterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(CacheWarningFilterConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        updateKeywords();
        GameChatAppender.addCustomFilter(appenderFilter);
        if (overlayManager != null && overlay != null) {
            overlayManager.add(overlay);
        }

        // Immediately refresh the chat to remove any existing mismatch warnings that were already logged
        if (client != null && clientThread != null) {
            clientThread.invokeLater(client::refreshChat);
        }
        log.info("Cache Warning Filter started");
    }

    @Override
    protected void shutDown() throws Exception {
        GameChatAppender.removeCustomFilter(appenderFilter);
        if (overlayManager != null && overlay != null) {
            overlayManager.remove(overlay);
        }

        // Refresh the chat to restore normal display
        if (client != null && clientThread != null) {
            clientThread.invokeLater(client::refreshChat);
        }
        log.info("Cache Warning Filter stopped");
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!"cachewarningfilter".equals(event.getGroup())) {
            return;
        }
        updateKeywords();
        if (client != null && clientThread != null) {
            clientThread.invokeLater(client::refreshChat);
        }
    }

    private void updateKeywords() {
        if (config == null) {
            return;
        }
        String raw = config.customFilterWords();
        if (raw == null || raw.trim().isEmpty()) {
            cachedCustomKeywords = Collections.emptyList();
            return;
        }
        cachedCustomKeywords = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Determines whether a chat message matches any of the filter criteria.
     */
    public boolean shouldBlockMessage(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        if (config != null && config.hideHashMismatch() && message.contains(CACHE_HASH_MISMATCH_FRAGMENT)) {
            return true;
        }

        if (config != null && config.hideAllOverlaidCacheWarnings() && message.toLowerCase().contains(OVERLAID_CACHE_FRAGMENT)) {
            return true;
        }

        for (String keyword : cachedCustomKeywords) {
            if (message.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Filter callback for GameChatAppender to drop events before they reach client.addChatMessage().
     */
    private boolean shouldFilterLogEvent(ILoggingEvent event) {
        if (event == null) {
            return false;
        }

        String formatted = event.getFormattedMessage();
        if (shouldBlockMessage(formatted)) {
            blockedCount.incrementAndGet();
            return true;
        }

        String raw = event.getMessage();
        if (shouldBlockMessage(raw)) {
            blockedCount.incrementAndGet();
            return true;
        }

        return false;
    }

    /**
     * RuneLite CS2 chatFilterCheck callback.
     * When CS2 evaluates messages to draw in the chat window, setting intStack[intStackSize - 3] = 0
     * completely prevents the message from being displayed.
     */
    @Subscribe(priority = -1)
    public void onScriptCallbackEvent(ScriptCallbackEvent event) {
        if (!"chatFilterCheck".equals(event.getEventName())) {
            return;
        }

        if (client == null) {
            return;
        }

        int[] intStack = client.getIntStack();
        int intStackSize = client.getIntStackSize();
        Object[] objectStack = client.getObjectStack();
        int objectStackSize = client.getObjectStackSize();

        if (intStack == null || objectStack == null || intStackSize < 3 || objectStackSize < 1) {
            return;
        }

        Object msgObj = objectStack[objectStackSize - 1];
        if (!(msgObj instanceof String)) {
            return;
        }

        String message = (String) msgObj;
        if (shouldBlockMessage(message)) {
            intStack[intStackSize - 3] = 0; // Block message from appearing in chat
            blockedCount.incrementAndGet();
        }
    }
}
