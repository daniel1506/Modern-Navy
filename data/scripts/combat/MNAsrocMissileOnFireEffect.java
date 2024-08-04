package data.scripts.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class MNAsrocMissileOnFireEffect implements OnFireEffectPlugin {

	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {

		if (projectile instanceof MissileAPI) {
			MNAntiPhaseTorpedoPlugin antiPhaseTorpedoPlugin = new MNAntiPhaseTorpedoPlugin();
			engine.addLayeredRenderingPlugin(antiPhaseTorpedoPlugin);
			antiPhaseTorpedoPlugin.attachToProjectile(projectile);
		}

	}

}
