package data.scripts.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import org.lazywizard.lazylib.combat.CombatUtils;

public class MNStealthMissilePlugin extends BaseCombatLayeredRenderingPlugin {

	private MissileAPI missile;
	private DamagingProjectileAPI proj;

	public void attachToProjectile(DamagingProjectileAPI proj) {
		this.proj = proj;
		if (proj instanceof MissileAPI) {
			this.missile = (MissileAPI) proj;
		}
		return;
	}

	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused())
			return;
		if (this.missile == null) {
			return;
		}
		if (this.missile.getCollisionClass() != CollisionClass.NONE) {
			return;
		}

		for (ShipAPI s : CombatUtils.getShipsWithinRange(this.missile.getLocation(), 175f)) {
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
		} else if (!Global.getCombatEngine().isEntityInPlay(this.missile) || this.missile.isFading()
				|| this.missile.isFizzling()) {
			return true;
		} else
			return false;
	}

	public float getRenderRadius() {
		return this.proj.getCollisionRadius();
	}

}
