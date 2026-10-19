package net.runelite.client.plugins.microbot.bosshelper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BossState {
    private TargetBoss targetBoss;
    private int bossNpcId;
    private String bossNpcName;
    private int bossAnimation;
    private int fightTick;
    private WorldPointDto playerWorldLocation;
    private int playerAnimation;
    @Builder.Default
    private List<ProjectileDto> activeProjectiles = new ArrayList<>();
    private String activeProtectionPrayer;
    private long timestamp;
}
