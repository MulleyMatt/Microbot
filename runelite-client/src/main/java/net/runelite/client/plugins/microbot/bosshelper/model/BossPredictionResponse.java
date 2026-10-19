package net.runelite.client.plugins.microbot.bosshelper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BossPredictionResponse {
    private String recommendedPrayer;
    @Builder.Default
    private List<WorldPointDto> safeTiles = new ArrayList<>();
    @Builder.Default
    private List<WorldPointDto> hazardTiles = new ArrayList<>();
    private String predictedAttack;
    private Integer ticksUntilNextAttack;

    public Prayer toRecommendedPrayer() {
        if (recommendedPrayer == null || recommendedPrayer.trim().isEmpty()) {
            return null;
        }
        String clean = recommendedPrayer.trim().toUpperCase();
        if (clean.contains("MAGIC") || clean.contains("MAGE")) {
            return Prayer.PROTECT_FROM_MAGIC;
        }
        if (clean.contains("MISSILE") || clean.contains("RANGE") || clean.contains("RANGED")) {
            return Prayer.PROTECT_FROM_MISSILES;
        }
        if (clean.contains("MELEE")) {
            return Prayer.PROTECT_FROM_MELEE;
        }
        try {
            return Prayer.valueOf(clean);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public List<WorldPoint> toSafeWorldPoints() {
        if (safeTiles == null) {
            return new ArrayList<>();
        }
        return safeTiles.stream()
                .filter(dto -> dto != null)
                .map(WorldPointDto::toWorldPoint)
                .collect(Collectors.toList());
    }

    public List<WorldPoint> toHazardWorldPoints() {
        if (hazardTiles == null) {
            return new ArrayList<>();
        }
        return hazardTiles.stream()
                .filter(dto -> dto != null)
                .map(WorldPointDto::toWorldPoint)
                .collect(Collectors.toList());
    }
}
