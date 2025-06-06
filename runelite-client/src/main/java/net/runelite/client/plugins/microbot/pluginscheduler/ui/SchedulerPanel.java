package net.runelite.client.plugins.microbot.pluginscheduler.ui;
import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerPlugin;
import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerState;
import net.runelite.client.plugins.microbot.pluginscheduler.model.PluginScheduleEntry;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public class SchedulerPanel extends PluginPanel {
    private final SchedulerPlugin plugin;

    // Current plugin section
    private final JLabel currentPluginLabel;
    private final JLabel runtimeLabel;

    // Previous plugin section
    private final JLabel prevPluginNameLabel;
    private final JLabel prevPluginDurationLabel;
    private final JLabel prevPluginStopReasonLabel;
    private final JLabel prevPluginStopTimeLabel;

    // Next plugin section
    private final JLabel nextPluginNameLabel;
    private final JLabel nextPluginTimeLabel;
    private final JLabel nextPluginScheduleLabel;

    // Scheduler status section
    private final JLabel schedulerStatusLabel;
    // Control buttons
    private final JButton configButton;
    private final JButton runButton;
    private final JButton stopButton;
    private final JButton antibanButton; // Added button for Antiban settings


    public SchedulerPanel(SchedulerPlugin plugin) {
        super(false);
        this.plugin = plugin;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        // Create main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Current plugin info panel
        JPanel infoPanel = createInfoPanel("Current Plugin");

        JLabel pluginLabel = new JLabel("Plugin:");
        pluginLabel.setForeground(Color.WHITE);
        pluginLabel.setFont(FontManager.getRunescapeFont());
        infoPanel.add(pluginLabel, createGbc(0, 0));

        currentPluginLabel = createValueLabel("None");
        infoPanel.add(currentPluginLabel, createGbc(1, 0));

        JLabel runtimeTitleLabel = new JLabel("Runtime:");
        runtimeTitleLabel.setForeground(Color.WHITE);
        runtimeTitleLabel.setFont(FontManager.getRunescapeFont());
        infoPanel.add(runtimeTitleLabel, createGbc(0, 1));

        runtimeLabel = createValueLabel("00:00:00");
        infoPanel.add(runtimeLabel, createGbc(1, 1));

        // Previous plugin info panel
        JPanel prevPluginPanel = createInfoPanel("Previous Plugin");
        JLabel prevPluginTitleLabel = new JLabel("Plugin:");
        prevPluginTitleLabel.setForeground(Color.WHITE);
        prevPluginTitleLabel.setFont(FontManager.getRunescapeFont());
        prevPluginPanel.add(prevPluginTitleLabel, createGbc(0, 0));

        prevPluginNameLabel = createValueLabel("None");
        prevPluginPanel.add(prevPluginNameLabel, createGbc(1, 0));

        JLabel prevDurationLabel = new JLabel("Duration:");
        prevDurationLabel.setForeground(Color.WHITE);
        prevDurationLabel.setFont(FontManager.getRunescapeFont());
        prevPluginPanel.add(prevDurationLabel, createGbc(0, 1));

        prevPluginDurationLabel = createValueLabel("00:00:00");
        prevPluginPanel.add(prevPluginDurationLabel, createGbc(1, 1));

        JLabel prevStopReasonLabel = new JLabel("Stop Reason:");
        prevStopReasonLabel.setForeground(Color.WHITE);
        prevStopReasonLabel.setFont(FontManager.getRunescapeFont());
        prevPluginPanel.add(prevStopReasonLabel, createGbc(0, 2));

        prevPluginStopReasonLabel = createValueLabel("None");
        prevPluginPanel.add(prevPluginStopReasonLabel, createGbc(1, 2));

        JLabel prevStopTimeLabel = new JLabel("Stop Time:");
        prevStopTimeLabel.setForeground(Color.WHITE);
        prevStopTimeLabel.setFont(FontManager.getRunescapeFont());
        prevPluginPanel.add(prevStopTimeLabel, createGbc(0, 3));

        prevPluginStopTimeLabel = createValueLabel("--:--");
        prevPluginPanel.add(prevPluginStopTimeLabel, createGbc(1, 3));

        // Next plugin info panel
        JPanel nextPluginPanel = createInfoPanel("Next Scheduled Plugin");
        JLabel nextPluginTitleLabel = new JLabel("Plugin:");
        nextPluginTitleLabel.setForeground(Color.WHITE);
        nextPluginTitleLabel.setFont(FontManager.getRunescapeFont());
        nextPluginPanel.add(nextPluginTitleLabel, createGbc(0, 0));

        nextPluginNameLabel = createValueLabel("None");
        nextPluginPanel.add(nextPluginNameLabel, createGbc(1, 0));

        JLabel nextRunLabel = new JLabel("Next Run:");
        nextRunLabel.setForeground(Color.WHITE);
        nextRunLabel.setFont(FontManager.getRunescapeFont());
        nextPluginPanel.add(nextRunLabel, createGbc(0, 1));

        nextPluginTimeLabel = createValueLabel("--:--");
        nextPluginPanel.add(nextPluginTimeLabel, createGbc(1, 1));

        JLabel scheduleLabel = new JLabel("Schedule:");
        scheduleLabel.setForeground(Color.WHITE);
        scheduleLabel.setFont(FontManager.getRunescapeFont());
        nextPluginPanel.add(scheduleLabel, createGbc(0, 2));

        nextPluginScheduleLabel = createValueLabel("None");
        nextPluginPanel.add(nextPluginScheduleLabel, createGbc(1, 2));

        // Scheduler status panel
        JPanel statusPanel = createInfoPanel("Scheduler Status");
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(FontManager.getRunescapeFont());
        statusPanel.add(statusLabel, createGbc(0, 0));

        schedulerStatusLabel = createValueLabel("Inactive");
        schedulerStatusLabel.setForeground(Color.YELLOW);
        statusPanel.add(schedulerStatusLabel, createGbc(1, 0));

        // Button panel - vertical layout (one button per row)
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 0, 5)); // Changed to 4 rows for the new button
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        
        // Add config button
        JButton configButton = createButton("Open Scheduler");
        configButton.addActionListener(this::onOpenConfigButtonClicked);
        this.configButton = configButton;
        
        // Control buttons
        Color greenColor = new Color(76, 175, 80);
        JButton runButton = createButton("Run Scheduler", greenColor);
        runButton.addActionListener(e -> {
            plugin.startScheduler();
            refresh();
        });
        this.runButton = runButton;

        Color redColor = new Color(244, 67, 54);
        JButton stopButton = createButton("Stop Scheduler", redColor);
        stopButton.addActionListener(e -> {
            plugin.stopScheduler();
            refresh();
        });
        this.stopButton = stopButton;

        // Add Antiban button - uses a distinct purple color
        Color purpleColor = new Color(156, 39, 176);
        JButton antibanButton = createButton("Antiban Settings", purpleColor);
        antibanButton.addActionListener(this::onAntibanButtonClicked);
        antibanButton.setToolTipText("Open Antiban settings in a separate window");
        this.antibanButton = antibanButton;

        // Add buttons in order
        buttonPanel.add(configButton);
        buttonPanel.add(runButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(antibanButton); // Add the new Antiban button

        // Add components to main panel
        mainPanel.add(infoPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(prevPluginPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(nextPluginPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(statusPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        add(mainPanel, BorderLayout.NORTH);
        refresh();
    }

    private GridBagConstraints createGbc(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
        gbc.fill = (x == 0) ? GridBagConstraints.BOTH
                : GridBagConstraints.HORIZONTAL;

        gbc.weightx = (x == 0) ? 0.1 : 1.0;
        gbc.weighty = 1.0;
        return gbc;
    }

    private JPanel createInfoPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.MEDIUM_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                FontManager.getRunescapeBoldFont(),
                Color.WHITE
        ));
        return panel;
    }

    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(FontManager.getRunescapeFont());
        return label;
    }

    private JButton createButton(String text) {
        return createButton(text, ColorScheme.BRAND_ORANGE);
    }

    private JButton createButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setFont(FontManager.getRunescapeSmallFont());
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(backgroundColor);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(backgroundColor.darker(), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));

        // Add hover effect that maintains the button's color theme
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });

        return button;
    }

    public void refresh() {
        updatePluginInfo();
        updateNextPluginInfo();
        updateButtonStates();
        
        // Update scheduler status
        SchedulerState state = plugin.getCurrentState();
        schedulerStatusLabel.setText(state.getDisplayName());
        schedulerStatusLabel.setForeground(state.getColor());
        schedulerStatusLabel.setToolTipText(state.getDescription());
    }
      
    /**
     * Updates button states based on plugin initialization status
     */
    private void updateButtonStates() {
        
        
        SchedulerState state = plugin.getCurrentState();
        boolean active = plugin.getCurrentState().isSchedulerActive();
        configButton.setEnabled(state != SchedulerState.UNINITIALIZED || state != SchedulerState.ERROR || state != SchedulerState.INITIALIZING);
        
        // Only enable run button if we're in READY or HOLD state
        runButton.setEnabled(!active && (state != SchedulerState.UNINITIALIZED || state != SchedulerState.ERROR || state != SchedulerState.INITIALIZING));
        
        // Only enable stop button in certain states
        stopButton.setEnabled(active);
        
        // Add tooltips
        if (state == SchedulerState.UNINITIALIZED || state == SchedulerState.ERROR || state == SchedulerState.INITIALIZING) {
            configButton.setToolTipText("Plugin not initialized yet");
            runButton.setToolTipText("Plugin not initialized yet");
            stopButton.setToolTipText("Plugin not initialized yet");
        } else {
            configButton.setToolTipText("Open scheduler configuration");
            runButton.setToolTipText(!runButton.isEnabled() ? 
                "Cannot start scheduler in " + state.getDisplayName() + " state" : 
                "Start the scheduler");
            stopButton.setToolTipText(!stopButton.isEnabled() ? 
                "Cannot stop scheduler: not running" : 
                "Stop the scheduler");
        }
    }

    void updatePluginInfo() {
        PluginScheduleEntry currentPlugin = plugin.getCurrentPlugin();

        if (currentPlugin != null) {
            // Get start time for runtime calculation
            ZonedDateTime startTimeZdt = currentPlugin.getLastRunStartTime();
            String pluginName = currentPlugin.getCleanName();
            
            // Add the stop reason indicator to the plugin name if available
            if (currentPlugin.getLastStopReasonType() != null && 
                currentPlugin.getLastStopReasonType() != PluginScheduleEntry.StopReason.NONE) {
                String stopReason = formatStopReason(currentPlugin.getLastStopReasonType());
                pluginName += " (" + stopReason + ")";
            }
            
            currentPluginLabel.setText(pluginName);
            
            // Show runtime - either current or last run duration
            if (currentPlugin.isRunning()) {
                // Calculate and display current runtime for active plugin
                if (startTimeZdt != null) {
                    long startTimeMillis = startTimeZdt.toInstant().toEpochMilli();
                    long runtimeMillis = System.currentTimeMillis() - startTimeMillis;
                    runtimeLabel.setText(formatDuration(runtimeMillis));
                } else {
                    runtimeLabel.setText("Running");
                }
            } else if (currentPlugin.getLastRunDuration() != null && !currentPlugin.getLastRunDuration().isZero()) {
                // Show the stored last run duration for completed plugins
                runtimeLabel.setText(formatDuration(currentPlugin.getLastRunDuration().toMillis()));
            } else {
                runtimeLabel.setText("Not started");
            }
            
            // Update previous plugin information if it has run at least once
            updatePreviousPluginInfo(currentPlugin);
        } else {
            currentPluginLabel.setText("None");
            runtimeLabel.setText("00:00:00");
            
            // Clear previous plugin info when there's no current plugin
            prevPluginNameLabel.setText("None");
            prevPluginDurationLabel.setText("00:00:00");
            prevPluginStopReasonLabel.setText("None");
            prevPluginStopTimeLabel.setText("--:--");
        }
    }
    
    /**
     * Updates information about the previously run plugin
     */
    private void updatePreviousPluginInfo(PluginScheduleEntry plugin) {
        if (plugin == null) return;
        
        // Only show previous plugin info if the plugin has been run at least once
        if (plugin.getLastRunEndTime() != null && plugin.getLastRunDuration() != null) {
            // Set name
            prevPluginNameLabel.setText(plugin.getCleanName());
            
            // Set duration
            long durationMillis = plugin.getLastRunDuration().toMillis();
            prevPluginDurationLabel.setText(formatDuration(durationMillis));
            
            // Set stop reason
            String stopReason = "None";
            if (plugin.getLastStopReasonType() != null && 
                plugin.getLastStopReasonType() != PluginScheduleEntry.StopReason.NONE) {
                stopReason = formatStopReason(plugin.getLastStopReasonType());
            }
            prevPluginStopReasonLabel.setText(stopReason);
            
            // Set stop time
            ZonedDateTime endTime = plugin.getLastRunEndTime();
            if (endTime != null) {
                prevPluginStopTimeLabel.setText(
                    endTime.format(PluginScheduleEntry.TIME_FORMATTER)
                );
            } else {
                prevPluginStopTimeLabel.setText("--:--");
            }
        }
    }
    
    /**
     * Formats a duration in milliseconds as HH:MM:SS
     */
    private String formatDuration(long durationMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * Returns a formatted stop reason
     */
    private String formatStopReason(PluginScheduleEntry.StopReason stopReason) {
        // Use the description from the enum if available
        if (stopReason != null) {
            switch (stopReason) {
                case MANUAL_STOP:
                    return "Stopped";
                case PLUGIN_FINISHED:
                    return "Completed";
                case ERROR:
                    return "Error";
                case SCHEDULED_STOP:
                    return "Timed out";
                case INTERRUPTED:
                    return "Interrupted";
                case HARD_STOP:
                    return "Force stopped";
                default:
                    return stopReason.getDescription();
            }
        }
        return "";
    }
    
    void updateNextPluginInfo() {
        PluginScheduleEntry nextPlugin = plugin.getNextScheduledPlugin();

        if (nextPlugin != null) {
            // Set the plugin name
            nextPluginNameLabel.setText(nextPlugin.getCleanName());
            
            // Set the next run time display (already handles various condition types)
            nextPluginTimeLabel.setText(nextPlugin.getNextRunDisplay());

            // Create an enhanced schedule description
            StringBuilder scheduleDesc = new StringBuilder(nextPlugin.getIntervalDisplay());
            
            // Add information about one-time schedules
            if (nextPlugin.hasAnyOneTimeStartConditions()) {
                if (nextPlugin.hasTriggeredOneTimeStartConditions() && !nextPlugin.canStartTriggerAgain()) {
                    scheduleDesc.append(" (Completed)");
                } else {
                    scheduleDesc.append(" (One-time)");
                }
            }
            
            // Add condition status information if available
            if (nextPlugin.hasAnyStartConditions()) {
                int total = nextPlugin.getStartConditionManager().getConditions().size();
                long satisfied = nextPlugin.getStartConditionManager().getConditions().stream()
                        .filter(condition -> condition.isSatisfied())
                        .count();
                
                if (total > 1) {
                    scheduleDesc.append(String.format(" [%d/%d conditions met]", satisfied, total));
                }
            }
            
            // Set the updated schedule description
            nextPluginScheduleLabel.setText(scheduleDesc.toString());
        } else {
            // No next plugin scheduled
            nextPluginNameLabel.setText("None");
            nextPluginTimeLabel.setText("--:--");
            nextPluginScheduleLabel.setText("None");
        }
    }

    private void onOpenConfigButtonClicked(ActionEvent e) {
        plugin.openSchedulerWindow();
    }

    private void onAntibanButtonClicked(ActionEvent e) {
        plugin.openAntibanSettings();
    }
}
