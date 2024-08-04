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
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.magiclib.util.MagicRender;

/**
 * IMPORTANT: will be multiple instances of this, as this doubles as the every
 * frame effect and the on fire effect (same instance)
 * But also as the visual for each individual shot (created via onFire, using
 * the non-default constructor)
 */
public class MNAntiPhaseTorpedoPlugin extends BaseCombatLayeredRenderingPlugin {

	private MissileAPI missile;
	private DamagingProjectileAPI proj;
	private boolean exploded = false;
	private final Color EXPLOSION_COLOR = new Color(255, 150, 50, 255);
	private final Color PARTICLE_COLOR = new Color(240, 200, 50, 255);

	public void attachToProjectile(DamagingProjectileAPI proj) {
		this.proj = proj;
		if (proj instanceof MissileAPI) {
			this.missile = (MissileAPI) proj;
		}
		return;
	}

	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused() || this.missile == null)
			return;

		if (missile.getMissileAI() instanceof GuidedMissileAI) {
			GuidedMissileAI missileAI = (GuidedMissileAI) missile.getMissileAI();
			if (missileAI.getTarget() instanceof ShipAPI) {
				ShipAPI target = (ShipAPI) missileAI.getTarget();
				if (target.isPhased()) {
					this.missile.setCollisionClass(CollisionClass.NONE);
					float collisionRadius = target.getCollisionRadius() * target.getCollisionRadius() * 0.75f;
					float dist = MathUtils.getDistanceSquared(this.missile.getLocation(), target.getLocation())
							- collisionRadius;
					if (dist < Math.pow(this.missile.getSpec().getExplosionRadius() / 2f,
							2)) {
						this.proximityFuse(target);
						return;
					}
				} else {
					this.missile.setCollisionClass(CollisionClass.MISSILE_NO_FF);
				}
			}
		}
	}

	void proximityFuse(ShipAPI target) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (target != null && !exploded) {
			// if (Math.random() * 11 < 1) {
			// target.getFluxTracker().increaseFlux(target.getMaxFlux(), true);
			// } else {
			target.getFluxTracker().increaseFlux(this.missile.getDamageAmount() * 2f, true);
			// }
			exploded = true;
		}
		if (MagicRender.screenCheck(0.1f, this.missile.getLocation())) {
			engine.addHitParticle(this.missile.getLocation(), new Vector2f(), 100.0f, 1.0f, 0.25f,
					this.EXPLOSION_COLOR);
			for (int i = 0; i < 20; ++i) {
				final float axis = (float) Math.random() * 360.0f;
				final float range = (float) Math.random() * 100.0f;
				engine.addHitParticle(MathUtils.getPointOnCircumference(this.missile.getLocation(), range / 5.0f, axis),
						MathUtils.getPointOnCircumference(new Vector2f(), range, axis),
						2.0f + (float) Math.random() * 2.0f, 1.0f, 1.0f + (float) Math.random(), this.PARTICLE_COLOR);
			}
			engine.applyDamage((CombatEntityAPI) this.missile, this.missile.getLocation(),
					this.missile.getHitpoints() * 2.0f, DamageType.FRAGMENTATION, 0.0f, false, false,
					(Object) this.missile);
		} else {
			engine.removeEntity((CombatEntityAPI) this.missile);
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
