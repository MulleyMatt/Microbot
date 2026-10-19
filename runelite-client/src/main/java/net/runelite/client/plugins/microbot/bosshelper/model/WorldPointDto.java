package net.runelite.client.plugins.microbot.bosshelper.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.runelite.api.coords.WorldPoint;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorldPointDto {
    private int x;
    private int y;
    private int plane;

    public static WorldPointDto fromWorldPoint(WorldPoint wp) {
        if (wp == null) {
            return null;
        }
        return new WorldPointDto(wp.getX(), wp.getY(), wp.getPlane());
    }

    public WorldPoint toWorldPoint() {
        return new WorldPoint(x, y, plane);
    }
}
