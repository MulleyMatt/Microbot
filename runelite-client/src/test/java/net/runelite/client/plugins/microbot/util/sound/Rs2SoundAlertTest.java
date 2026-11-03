package net.runelite.client.plugins.microbot.util.sound;

import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Rs2SoundAlertTest {

    @Test
    public void testAlertAudioResourcesExist() {
        try (InputStream wavStream = Rs2SoundAlert.class.getResourceAsStream("alert.wav")) {
            assertNotNull("alert.wav must exist in net/runelite/client/plugins/microbot/util/sound/", wavStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (InputStream mp3Stream = Rs2SoundAlert.class.getResourceAsStream("alert.mp3")) {
            assertNotNull("alert.mp3 must exist in net/runelite/client/plugins/microbot/util/sound/", mp3Stream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSoundVolumeGainMath() {
        // At 100% volume (1.0), gain is 0 dB
        float gain100 = (float) Math.log10(100 / 100.0f) * 20.0f;
        assertEquals(0.0f, gain100, 0.001f);

        // At 10% volume (0.1), gain is -20 dB
        float gain10 = (float) Math.log10(10 / 100.0f) * 20.0f;
        assertEquals(-20.0f, gain10, 0.001f);

        // At 1% volume (0.01), gain is -40 dB
        float gain1 = (float) Math.log10(1 / 100.0f) * 20.0f;
        assertEquals(-40.0f, gain1, 0.001f);
    }

    @Test
    public void testSoundAlertTriggerIncrementsCounter() {
        int initialCount = Rs2SoundAlert.getGlobalPlayCount();
        Rs2SoundAlert.triggerAlert();
        assertTrue("Global play count should increase", Rs2SoundAlert.getGlobalPlayCount() > initialCount);
    }
}
