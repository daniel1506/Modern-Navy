package data.scripts.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class MNStealthMissileOnFireEffect implements OnFireEffectPlugin {

	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {

		if (projectile instanceof MissileAPI) {
			MNStealthMissilePlugin stealthMissilePlugin = new MNStealthMissilePlugin();
			engine.addLayeredRenderingPlugin(stealthMissilePlugin);
			stealthMissilePlugin.attachToProjectile(projectile);
		}

	}

}
