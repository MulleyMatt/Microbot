package net.runelite.client.plugins.microbot.cachewarningfilter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.lang.reflect.Field;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.GameChatAppender;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CacheWarningFilterTest {

    private static final String EXACT_WARNING =
            "[14:24:45] Mismatch in overlaid cache archive hash for 12/84:\n" +
            "DC9A98F40E8B21508F3E0516C82B70737ECA6B6B6EBCC38535D1F7E1D2C23A26 !=\n" +
            "F7B4EC765ACB19D9625CDCA567E0EA355452AA12212791FE75F97C879290B4E8";

    private static final String SINGLE_LINE_WARNING =
            "Mismatch in overlaid cache archive hash for 12/84: DC9A98F40E8B21508F3E0516C82B70737ECA6B6B6EBCC38535D1F7E1D2C23A26 != F7B4EC765ACB19D9625CDCA567E0EA355452AA12212791FE75F97C879290B4E8";

    @Mock
    private Client client;

    @Mock
    private ClientThread clientThread;

    @Mock
    private OverlayManager overlayManager;

    @Mock
    private CacheWarningFilterConfig config;

    @Mock
    private CacheWarningFilterOverlay overlay;

    private CacheWarningFilterPlugin plugin;

    private Object previousClient;
    private Object previousClientThread;

    @Before
    public void setUp() throws Exception {
        plugin = new CacheWarningFilterPlugin();

        injectField(plugin, "client", client);
        injectField(plugin, "clientThread", clientThread);
        injectField(plugin, "overlayManager", overlayManager);
        injectField(plugin, "config", config);
        injectField(plugin, "overlay", overlay);

        when(config.hideHashMismatch()).thenReturn(true);
        when(config.hideAllOverlaidCacheWarnings()).thenReturn(true);
        when(config.customFilterWords()).thenReturn("");

        previousClient = swapStatic(GameChatAppender.class, "client", client);
        previousClientThread = swapStatic(GameChatAppender.class, "clientThread", clientThread);
        GameChatAppender.clearCustomFilters();
    }

    @After
    public void tearDown() throws Exception {
        GameChatAppender.clearCustomFilters();
        swapStatic(GameChatAppender.class, "client", previousClient);
        swapStatic(GameChatAppender.class, "clientThread", previousClientThread);
    }

    @Test
    public void testBlocksMismatchMessage() {
        assertTrue(plugin.shouldBlockMessage(EXACT_WARNING));
        assertTrue(plugin.shouldBlockMessage(SINGLE_LINE_WARNING));
        assertTrue(plugin.shouldBlockMessage("Mismatch in overlaid cache archive hash for 12/84: abc != def"));
    }

    @Test
    public void testAllowsNormalMessages() {
        assertFalse(plugin.shouldBlockMessage("You get some redwood logs."));
        assertFalse(plugin.shouldBlockMessage("You swing your axe at the tree."));
        assertFalse(plugin.shouldBlockMessage("Welcome to Old School RuneScape."));
        assertFalse(plugin.shouldBlockMessage(null));
        assertFalse(plugin.shouldBlockMessage(""));
    }

    @Test
    public void testHideAllOverlaidCacheWarnings() {
        String genericCacheWarning = "Warning: Failed to load overlaid cache index 5";
        assertTrue(plugin.shouldBlockMessage(genericCacheWarning));

        when(config.hideAllOverlaidCacheWarnings()).thenReturn(false);
        when(config.hideHashMismatch()).thenReturn(false);
        assertFalse(plugin.shouldBlockMessage(genericCacheWarning));
    }

    @Test
    public void testCustomKeywordsFilter() throws Exception {
        when(config.customFilterWords()).thenReturn("unwanted bot alert, spammy warning");
        // trigger startUp to parse keywords
        plugin.startUp();

        assertTrue(plugin.shouldBlockMessage("This contains an unwanted bot alert message"));
        assertTrue(plugin.shouldBlockMessage("A spammy warning just appeared"));
        assertFalse(plugin.shouldBlockMessage("Normal player message"));
    }

    @Test
    public void testChatFilterCheckCallbackBlocksWarning() {
        int[] simulatedIntStack = new int[]{1, ChatMessageType.ENGINE.getType(), 100}; // [allow, type, id]
        Object[] simulatedObjectStack = new Object[]{EXACT_WARNING};

        when(client.getIntStack()).thenReturn(simulatedIntStack);
        when(client.getIntStackSize()).thenReturn(simulatedIntStack.length);
        when(client.getObjectStack()).thenReturn(simulatedObjectStack);
        when(client.getObjectStackSize()).thenReturn(simulatedObjectStack.length);

        ScriptCallbackEvent event = new ScriptCallbackEvent();
        event.setEventName("chatFilterCheck");

        plugin.onScriptCallbackEvent(event);

        // intStack[intStackSize - 3] should be mutated to 0 to block rendering
        assertEquals(0, simulatedIntStack[0]);
        assertEquals(1, plugin.getBlockedCount().get());
    }

    @Test
    public void testChatFilterCheckCallbackAllowsNormalChat() {
        int[] simulatedIntStack = new int[]{1, ChatMessageType.GAMEMESSAGE.getType(), 101}; // [allow, type, id]
        Object[] simulatedObjectStack = new Object[]{"You get some redwood logs."};

        when(client.getIntStack()).thenReturn(simulatedIntStack);
        when(client.getIntStackSize()).thenReturn(simulatedIntStack.length);
        when(client.getObjectStack()).thenReturn(simulatedObjectStack);
        when(client.getObjectStackSize()).thenReturn(simulatedObjectStack.length);

        ScriptCallbackEvent event = new ScriptCallbackEvent();
        event.setEventName("chatFilterCheck");

        plugin.onScriptCallbackEvent(event);

        // intStack[0] should remain 1 (allowed)
        assertEquals(1, simulatedIntStack[0]);
        assertEquals(0, plugin.getBlockedCount().get());
    }

    @Test
    public void testGameChatAppenderIntegration() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        GameChatAppender.updateConfiguration(true, Level.WARN, false); // allow all loggers to test filter

        GameChatAppender appender = new GameChatAppender();
        appender.setContext(new LoggerContext());
        appender.start();

        // Start plugin - registers custom filter
        plugin.startUp();

        LoggerContext context = new LoggerContext();
        LoggingEvent warningEvent = new LoggingEvent(
                "test",
                context.getLogger("injected-client"),
                Level.WARN,
                SINGLE_LINE_WARNING,
                null,
                null);

        // Append warning event
        appender.doAppend(warningEvent);

        // Verify that clientThread was NEVER invoked because the custom filter denied the warning
        verify(clientThread, never()).invoke(any(Runnable.class));
        assertTrue(plugin.getBlockedCount().get() >= 1);

        // Append a normal event to ensure normal logs still pass through
        LoggingEvent normalEvent = new LoggingEvent(
                "test",
                context.getLogger("net.runelite.client.plugins.microbot.test"),
                Level.WARN,
                "Normal warning message",
                null,
                null);
        appender.doAppend(normalEvent);
        verify(clientThread, org.mockito.Mockito.times(1)).invoke(any(Runnable.class));

        // Shut down plugin - filter is removed
        plugin.shutDown();
        appender.stop();
    }

    @Test
    public void testStartUpAndShutDownChatRefresh() throws Exception {
        plugin.startUp();
        verify(clientThread).invokeLater(any(Runnable.class));
        verify(overlayManager).add(overlay);

        plugin.shutDown();
        // invokeLater called again on shutdown
        verify(clientThread, org.mockito.Mockito.times(2)).invokeLater(any(Runnable.class));
        verify(overlayManager).remove(overlay);
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object swapStatic(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = net.runelite.client.plugins.microbot.Microbot.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object old = field.get(null);
            field.set(null, value);
            return old;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
