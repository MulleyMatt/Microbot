package net.runelite.client.plugins.microbot.bosshelper;

import com.google.gson.Gson;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.bosshelper.model.BossPredictionResponse;
import net.runelite.client.plugins.microbot.bosshelper.model.BossState;
import net.runelite.client.plugins.microbot.bosshelper.model.TargetBoss;
import net.runelite.client.plugins.microbot.bosshelper.model.WorldPointDto;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BossHelperTest {

    @Test
    public void testPrayerMapping() {
        assertEquals(Rs2PrayerEnum.PROTECT_MAGIC, Rs2Prayer.fromRuneLitePrayer(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(Rs2PrayerEnum.PROTECT_RANGE, Rs2Prayer.fromRuneLitePrayer(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(Rs2PrayerEnum.PROTECT_MELEE, Rs2Prayer.fromRuneLitePrayer(Prayer.PROTECT_FROM_MELEE));
        assertNull(Rs2Prayer.fromRuneLitePrayer(null));
    }

    @Test
    public void testPredictionResponseParsing() {
        BossPredictionResponse resp = BossPredictionResponse.builder()
                .recommendedPrayer("PROTECT_FROM_MAGIC")
                .safeTiles(List.of(new WorldPointDto(3200, 3200, 0)))
                .hazardTiles(List.of(new WorldPointDto(3201, 3201, 0)))
                .predictedAttack("MAGIC")
                .ticksUntilNextAttack(2)
                .build();

        assertEquals(Prayer.PROTECT_FROM_MAGIC, resp.toRecommendedPrayer());
        List<WorldPoint> safe = resp.toSafeWorldPoints();
        assertEquals(1, safe.size());
        assertEquals(new WorldPoint(3200, 3200, 0), safe.get(0));

        List<WorldPoint> hazard = resp.toHazardWorldPoints();
        assertEquals(1, hazard.size());
        assertEquals(new WorldPoint(3201, 3201, 0), hazard.get(0));
    }

    @Test
    public void testBossStateSerialization() {
        Gson gson = new Gson();
        BossState state = BossState.builder()
                .targetBoss(TargetBoss.ZULRAH)
                .bossNpcId(2042)
                .bossNpcName("Zulrah")
                .bossAnimation(5071)
                .fightTick(12)
                .playerWorldLocation(new WorldPointDto(2268, 3072, 0))
                .playerAnimation(-1)
                .activeProtectionPrayer("PROTECT_FROM_MISSILES")
                .timestamp(123456789L)
                .build();

        String json = gson.toJson(state);
        assertNotNull(json);

        BossState deserialized = gson.fromJson(json, BossState.class);
        assertEquals(TargetBoss.ZULRAH, deserialized.getTargetBoss());
        assertEquals(2042, deserialized.getBossNpcId());
        assertEquals("Zulrah", deserialized.getBossNpcName());
        assertEquals(5071, deserialized.getBossAnimation());
        assertEquals(12, deserialized.getFightTick());
        assertEquals(2268, deserialized.getPlayerWorldLocation().getX());
    }

    @Test
    public void testFlexiblePrayerStrings() {
        BossPredictionResponse mage = BossPredictionResponse.builder().recommendedPrayer("magic").build();
        assertEquals(Prayer.PROTECT_FROM_MAGIC, mage.toRecommendedPrayer());

        BossPredictionResponse range = BossPredictionResponse.builder().recommendedPrayer("range").build();
        assertEquals(Prayer.PROTECT_FROM_MISSILES, range.toRecommendedPrayer());

        BossPredictionResponse melee = BossPredictionResponse.builder().recommendedPrayer("melee").build();
        assertEquals(Prayer.PROTECT_FROM_MELEE, melee.toRecommendedPrayer());

        BossPredictionResponse none = BossPredictionResponse.builder().recommendedPrayer("none").build();
        assertNull(none.toRecommendedPrayer());
    }
}
