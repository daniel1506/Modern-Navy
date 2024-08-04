package data.scripts.combat;

//import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.ai.CDomeMissileAI;

/**
 * IMPORTANT: will be multiple instances of this, as this doubles as the every frame effect and the on fire effect (same instance)
 * But also as the visual for each individual shot (created via onFire, using the non-default constructor)
 */
public class MNCDomeMissileOnFireEffect implements OnFireEffectPlugin {

	
	//public MNCDomeMissileOnFireEffect() {	
	//}
	
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {				
		MissileAPI missile = (MissileAPI) projectile;
		missile.setMissileAI(new CDomeMissileAI(missile, weapon.getShip()));	
	}
	
}




