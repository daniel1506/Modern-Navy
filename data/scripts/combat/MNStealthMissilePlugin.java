package data.scripts.combat;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.combat.CombatUtils;


/**
 * IMPORTANT: will be multiple instances of this, as this doubles as the every frame effect and the on fire effect (same instance)
 * But also as the visual for each individual shot (created via onFire, using the non-default constructor)
 */
public class MNStealthMissilePlugin extends BaseCombatLayeredRenderingPlugin implements OnHitEffectPlugin {
			
	private WeaponAPI weapon;
	private MissileAPI missile;
	private DamagingProjectileAPI proj;
	private List<ShipAPI> shipAround = new ArrayList<ShipAPI>();
	
	public MNStealthMissilePlugin(WeaponAPI weapon) {
		//super();
		this.weapon = weapon;
	}
	
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

	}
			
	public void attachToProjectile(DamagingProjectileAPI proj) {
		this.proj = proj;
		if (proj instanceof MissileAPI) {
			this.missile = (MissileAPI) proj;
		}
		return;
	}
	
	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused()) return;
		if (this.missile == null) {
			return;
		}
		if (this.missile.getCollisionClass() != CollisionClass.NONE) {
			return;
		}
		//super.advance(amount);
		
		shipAround = CombatUtils.getShipsWithinRange(this.missile.getLocation(), 150f);
		for (ShipAPI s : shipAround) {
			if (s.getOwner() != this.missile.getSource().getOwner()) {
				this.missile.setCollisionClass(CollisionClass.MISSILE_NO_FF);
				return;
			}
		}
			
		
	}
	
	@Override
	public boolean isExpired() {
		if (this.missile == null) {
			return true;
		}
		else if (!Global.getCombatEngine().isEntityInPlay(this.missile) || this.missile.isFading() || this.missile.isFizzling()) {
			return true;
		}
		else return false;
	}

	
	public float getRenderRadius() {
		return this.proj.getCollisionRadius();
	}
	
}






