package net.runelite.client.plugins.microbot;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.ResetResistantAttachment;

public class GameChatAppender extends AppenderBase<ILoggingEvent> {
    private final PatternLayout layout = new PatternLayout();
    
    // Cache the current configuration to avoid config lookups during filtering
    private static volatile boolean loggingEnabled = true;
    private static volatile Level minimumLevel = Level.WARN;
    private static volatile boolean onlyMicrobotLogging = true;
    private static final Set<Predicate<ILoggingEvent>> customFilters = new CopyOnWriteArraySet<>();

    public GameChatAppender(String pattern) {
        // Order matters! Level filter should run first to deny based on log level
        addFilter(new GameChatLevelFilter());
        addFilter(new OnlyMicrobotLoggingFilter());
        addFilter(new CustomLoggingFilter());
        layout.setPattern(pattern);
    }

    public GameChatAppender() {
        this("[%d{HH:mm:ss}] %msg%ex{0}%n"); // Default simple pattern
    }

    public ResetResistantAttachment install(LoggerContext context, BooleanSupplier enabled) {
        setContext(context);
        Runnable startIfEnabled = () -> {
            if (enabled.getAsBoolean()) start();
        };
        startIfEnabled.run();
        return ResetResistantAttachment.attach(context, this, startIfEnabled);
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        layout.setContext(context);
    }

    @Override
    public void start() {
        layout.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        layout.stop();
    }

    public void setPattern(String pattern) {
        final boolean started = layout.isStarted();
        if (started) layout.stop();
        layout.setPattern(pattern);
        if (started) layout.start();
    }
    
    /**
     * Updates the cached configuration for filtering
     */
    public static void updateConfiguration(boolean enabled, Level level, boolean microbotOnly) {
        loggingEnabled = enabled;
        minimumLevel = level;
        onlyMicrobotLogging = microbotOnly;
    }

    /**
     * Registers a custom predicate filter for game chat log events.
     * If the predicate returns true, the event is denied and dropped.
     */
    public static void addCustomFilter(Predicate<ILoggingEvent> filter) {
        if (filter != null) {
            customFilters.add(filter);
        }
    }

    /**
     * Removes a previously registered custom predicate filter.
     */
    public static void removeCustomFilter(Predicate<ILoggingEvent> filter) {
        if (filter != null) {
            customFilters.remove(filter);
        }
    }

    /**
     * Clears all registered custom filters.
     */
    public static void clearCustomFilters() {
        customFilters.clear();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!Microbot.isLoggedIn()) return;

        final String formatted = layout.doLayout(event);
        // use invoke so we don't stall the calling thread
        Microbot.getClientThread().invoke(() -> {
            Microbot.getClient().addChatMessage(ChatMessageType.ENGINE, "", formatted, "", false);
        });
    }

    /**
     * Filter to control which log levels appear in game chat based on configuration
     */
    private static class GameChatLevelFilter extends Filter<ILoggingEvent> {
        @Override
        public FilterReply decide(ILoggingEvent event) {
            // Check if logging is enabled
            if (!loggingEnabled) {
                return FilterReply.DENY;
            }
            
            // In debug mode, show all levels (overrides configuration)
            if (Microbot.isDebug()) {
                return FilterReply.NEUTRAL;
            }
            
            // Use cached minimum level to filter (includes DEBUG if configured)
            return event.getLevel().isGreaterOrEqual(minimumLevel) ? FilterReply.NEUTRAL : FilterReply.DENY;
        }
    }

    private static class OnlyMicrobotLoggingFilter extends Filter<ILoggingEvent> {
        @Override
        public FilterReply decide(ILoggingEvent event) {
            // If only microbot logging is disabled, accept all logs
            if (!onlyMicrobotLogging) {
                return FilterReply.NEUTRAL;
            }
            
            // Otherwise, only accept microbot logs
            return event.getLoggerName().startsWith("net.runelite.client.plugins.microbot") ? FilterReply.NEUTRAL : FilterReply.DENY;
        }
    }

    private static class CustomLoggingFilter extends Filter<ILoggingEvent> {
        @Override
        public FilterReply decide(ILoggingEvent event) {
            for (Predicate<ILoggingEvent> filter : customFilters) {
                if (filter.test(event)) {
                    return FilterReply.DENY;
                }
            }
            return FilterReply.NEUTRAL;
        }
    }
}
