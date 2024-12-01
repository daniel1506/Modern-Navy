package data.scripts.combat;

import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

public class MNCHAMPOnHitEffect implements OnHitEffectPlugin {

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
            Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!(target instanceof ShipAPI))
            return;
        ShipAPI ship = (ShipAPI) target;
        ship.getFluxTracker().increaseFlux(projectile.getEmpAmount() * 2f, true);
        if (ship.getHullSize() != HullSize.FIGHTER) {
            // float pierceChance = ship.getHardFluxLevel() - 0.1f;
            // pierceChance *=
            // ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
            // boolean piercedShield = shieldHit && (float) Math.random() < pierceChance;

            float emp = projectile.getEmpAmount();
            engine.spawnEmpArcPierceShields(projectile.getSource(), point, target, target, DamageType.ENERGY, 0f,
                    emp, 500f, "tachyon_lance_emp_impact", 20f, new Color(205, 105, 255, 255),
                    new Color(255, 205, 255, 255));

        }

    }

}
