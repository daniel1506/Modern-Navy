package data.scripts.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.ai.SeaRamMissileAI;

public class MNSeaRamMissileOnFireEffect implements OnFireEffectPlugin {

	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		MissileAPI missile = (MissileAPI) projectile;
		missile.setMissileAI(new SeaRamMissileAI(missile, weapon.getShip()));
	}

}
