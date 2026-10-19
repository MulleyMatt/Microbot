package net.runelite.client.plugins.microbot.bosshelper.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectileDto {
    private int id;
    private int targetX;
    private int targetY;
    private int remainingCycles;
    private int endCycle;

    public static ProjectileDto fromProjectile(Projectile projectile, WorldPoint targetWorldPoint) {
        if (projectile == null) {
            return null;
        }
        int tx = targetWorldPoint != null ? targetWorldPoint.getX() : (int) Math.round(projectile.getX());
        int ty = targetWorldPoint != null ? targetWorldPoint.getY() : (int) Math.round(projectile.getY());
        return new ProjectileDto(
                projectile.getId(),
                tx,
                ty,
                projectile.getRemainingCycles(),
                projectile.getEndCycle()
        );
    }
}
