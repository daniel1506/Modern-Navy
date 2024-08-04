package data.scripts.combat;

//import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.ai.CruiseMissileAI;

/**
 * IMPORTANT: will be multiple instances of this, as this doubles as the every frame effect and the on fire effect (same instance)
 * But also as the visual for each individual shot (created via onFire, using the non-default constructor)
 */
public class MNCruiseMissileOnFireEffect implements OnFireEffectPlugin {

	
	//public MNCruiseMissileOnFireEffect() {	
	//}
	
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {				

		if (weapon.getShip() == engine.getPlayerShip() && !engine.getCombatUI().isAutopilotOn()) {
			MissileAPI missile = (MissileAPI) projectile;
			//engine.addPlugin(new CruiseMissileAI(proj, weapon));
			missile.setMissileAI(new CruiseMissileAI(missile, weapon.getShip()));
		}
		
	}
	
}




