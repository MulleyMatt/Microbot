package net.runelite.client.plugins.microbot.ui;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.AlwaysOnPlugins;
import net.runelite.client.plugins.microbot.agentserver.AgentServerPlugin;
import net.runelite.client.plugins.microbot.cachewarningfilter.CacheWarningFilterPlugin;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import org.junit.Test;

import javax.swing.JLabel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MicrobotPluginListPanelModeTest {

    @PluginDescriptor(
            name = "[Syn] Zulrah Overlay Helper",
            tags = {"zulrah", "overlay", "helper", "pvm"},
            isExternal = true
    )
    private static class ZulrahOverlayHelperFixture extends Plugin {
    }

    @PluginDescriptor(
            name = "Custom User Helper",
            tags = {"user", "helper"}
    )
    private static class CustomUserHelperFixture extends Plugin {
    }

    @Test
    public void testAlwaysOnUserPluginsAreLocked() {
        PluginDescriptor agentDesc = AgentServerPlugin.class.getAnnotation(PluginDescriptor.class);
        PluginDescriptor cacheDesc = CacheWarningFilterPlugin.class.getAnnotation(PluginDescriptor.class);

        assertTrue(AlwaysOnPlugins.isLocked(agentDesc));
        assertTrue(AlwaysOnPlugins.isLocked(cacheDesc));
        assertTrue(MicrobotPluginListPanel.isUserBuiltPlugin(new AgentServerPlugin()));
        assertTrue(MicrobotPluginListPanel.isUserBuiltPlugin(new CacheWarningFilterPlugin()));
    }

    @Test
    public void testHelperTagAloneOrHubPluginIsNotUserBuilt() {
        Plugin zulrah = new ZulrahOverlayHelperFixture();
        assertFalse(MicrobotPluginListPanel.isUserBuiltPlugin(zulrah));

        MicrobotPluginConfigurationDescriptor hubZulrahDesc = new MicrobotPluginConfigurationDescriptor(
                "[Syn] Zulrah Overlay Helper",
                "Zulrah helper",
                new String[]{"zulrah", "helper"},
                zulrah,
                null,
                null,
                null,
                "zulrah-overlay-helper"
        );
        assertFalse(MicrobotPluginListPanel.isUserBuiltPlugin(hubZulrahDesc));

        Plugin customUser = new CustomUserHelperFixture();
        assertTrue(MicrobotPluginListPanel.isUserBuiltPlugin(customUser));

        MicrobotPluginConfigurationDescriptor hubTaggedUserDesc = new MicrobotPluginConfigurationDescriptor(
                "Some Hub Plugin",
                "desc",
                new String[]{"user"},
                customUser,
                null,
                null,
                null,
                "some-hub-plugin"
        );
        assertFalse(MicrobotPluginListPanel.isUserBuiltPlugin(hubTaggedUserDesc));
    }

    @Test
    public void testIsUserBuiltPluginName() {
        assertTrue(MicrobotPluginListPanel.isUserBuiltPluginName("Agent Server"));
        assertTrue(MicrobotPluginListPanel.isUserBuiltPluginName("Herb Run Helper"));
        assertTrue(MicrobotPluginListPanel.isUserBuiltPluginName("<html>[<font color=#b8f704M>M</font>] Agent Server"));
        assertTrue(MicrobotPluginListPanel.isUserBuiltPluginName("<html>[<font color=#b8f704M>M</font>] Herb Run Helper"));
        assertTrue(MicrobotPluginListPanel.isUserBuiltPluginName("[M] Agent Server"));
        assertTrue(MicrobotPluginListPanel.isUserBuiltPluginName("[M] Herb Run Helper"));

        assertFalse(MicrobotPluginListPanel.isUserBuiltPluginName("Boss Helper"));
        assertFalse(MicrobotPluginListPanel.isUserBuiltPluginName("[M] Boss Helper"));
        assertFalse(MicrobotPluginListPanel.isUserBuiltPluginName("Auto Mining"));
        assertFalse(MicrobotPluginListPanel.isUserBuiltPluginName("Museum Cleaner"));
        assertFalse(MicrobotPluginListPanel.isUserBuiltPluginName("Microbot"));
        assertFalse(MicrobotPluginListPanel.isUserBuiltPluginName("Quest Helper"));
        assertFalse(MicrobotPluginListPanel.isUserBuiltPluginName(null));
    }

    @Test
    public void testMaterialTabGroupAddRemoveContains() {
        MaterialTabGroup group = new MaterialTabGroup();
        MaterialTab tab1 = new MaterialTab("Tab 1", group, new JLabel("Content 1"));
        MaterialTab tab2 = new MaterialTab("Tab 2", group, new JLabel("Content 2"));
        MaterialTab tab3 = new MaterialTab("Tab 3", group, new JLabel("Content 3"));

        group.addTab(tab1);
        group.addTab(tab3);

        assertTrue(group.containsTab(tab1));
        assertFalse(group.containsTab(tab2));
        assertTrue(group.containsTab(tab3));

        // Insert tab2 at index 1
        group.addTab(1, tab2);
        assertTrue(group.containsTab(tab2));
        assertEquals(tab1, group.getTab(0));
        assertEquals(tab2, group.getTab(1));
        assertEquals(tab3, group.getTab(2));

        // Remove tab2
        group.removeTab(tab2);
        assertFalse(group.containsTab(tab2));
        assertEquals(tab1, group.getTab(0));
        assertEquals(tab3, group.getTab(1));
    }
}
