package net.runelite.client.plugins.microbot.bosshelper.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetBoss {
    AUTO_DETECT("Auto Detect"),
    ZULRAH("Zulrah"),
    HUNLLEF("Hunllef"),
    VORKATH("Vorkath"),
    GENERIC_HAZARDS("Generic Hazards");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
