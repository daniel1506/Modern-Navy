package data.scripts.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.combat.MNStealthMissilePlugin;

/**
 * IMPORTANT: will be multiple instances of this, as this doubles as the every frame effect and the on fire effect (same instance)
 * But also as the visual for each individual shot (created via onFire, using the non-default constructor)
 */
public class MNAGM158Effect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

	//private CombatEntityAPI chargeGlowEntity;
	private MNStealthMissilePlugin stealthMissilePlugin;
	
	public MNAGM158Effect() {
		
	}
	
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		/* boolean charging = weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() <= 0;
		if (charging && chargeGlowEntity == null) {
			chargeGlowPlugin = new SWIonPulseGlow(weapon);
			chargeGlowEntity = Global.getCombatEngine().addLayeredRenderingPlugin(chargeGlowPlugin);	
		} else if (!charging && chargeGlowEntity != null) {
			chargeGlowEntity = null;
			chargeGlowPlugin = null;
		} */
	}
			
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {				
		/* if (chargeGlowPlugin != null) {
			chargeGlowPlugin.attachToProjectile(projectile);
			chargeGlowPlugin = null;
			chargeGlowEntity = null;
			
			MissileAPI missile = (MissileAPI) projectile;
			missile.setMine(true);
			missile.setNoMineFFConcerns(true);
			missile.setMineExplosionRange(SWIonPulseGlow.MAX_ARC_RANGE + 50f);
			missile.setMinePrimed(true);
			missile.setUntilMineExplosion(0f);
		} */
		stealthMissilePlugin = new MNStealthMissilePlugin(weapon);
		engine.addLayeredRenderingPlugin(stealthMissilePlugin);
		stealthMissilePlugin.attachToProjectile(projectile);
		stealthMissilePlugin = null;
	}
	
	
}




