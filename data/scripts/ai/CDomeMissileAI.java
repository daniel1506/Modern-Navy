package data.scripts.ai;

import java.lang.Math;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicRender;

//import org.apache.log4j.Logger;

public class CDomeMissileAI implements MissileAIPlugin, GuidedMissileAI {
	// public Logger log = Logger.getLogger(this.getClass());

	private CombatEngineAPI engine;
	private final MissileAPI missile;
	private CombatEntityAPI target;
	private Vector2f lead;
	private final float MAX_SPEED;
	private final float DAMPING = 0.05f;
	private final Color EXPLOSION_COLOR;
	private final Color PARTICLE_COLOR;
	private final int NUM_PARTICLES = 20;

	private static final String prefix = "CBS";

	public CDomeMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		this.lead = new Vector2f();
		this.EXPLOSION_COLOR = new Color(255, 0, 0, 255);
		this.PARTICLE_COLOR = new Color(240, 200, 50, 255);
		this.missile = missile;
		this.MAX_SPEED = missile.getMaxSpeed() * 1.25f;
	}

	public void advance(float amount) {
		if (this.engine != Global.getCombatEngine()) {
			this.engine = Global.getCombatEngine();
		}
		if (Global.getCombatEngine().isPaused() || this.missile.isFading()) {
			return;
		}
		if (this.missile.isFizzling()) {
			this.selfDestruct(this.missile, this.engine);
			return;
		}
		if (this.target == null) {
			this.missile.giveCommand(ShipCommand.ACCELERATE);
			// Aquire a target from track list of its weapon
			if (this.engine.getCustomData().containsKey(this.prefix + this.missile.getWeapon().toString())) {
				findTargetLoop: for (DamagingProjectileAPI d : (List<DamagingProjectileAPI>) this.engine.getCustomData()
						.get(this.prefix + this.missile.getWeapon().toString())) {
					// If the track hasn't been targeted by any bird before, lock on it and create a
					// custom data to store the birds that are targetting this track
					if (!this.engine.getCustomData().containsKey(this.prefix + d.toString())
							&& this.isProjectileAlive(d)) {
						List<MissileAPI> birds = new ArrayList<MissileAPI>();
						this.setTarget((CombatEntityAPI) d);
						this.missile.setFacing(
								Misc.getAngleInDegrees(this.missile.getLocation(), this.target.getLocation()));
						// log.info("Kill Track1 " + m + " with " + this.missile);
						birds.add(this.missile);
						this.engine.getCustomData().put(this.prefix + d.toString(), birds);
						this.nextLauncher();
						return;
					} else if (this.engine.getCustomData().containsKey(this.prefix + d.toString())
							&& this.isProjectileAlive(d)) {
						// If the track already has custom data, check if there is already a same type
						// of bird engaging this track, if yes continue to next track
						// If not, lock onto this track, add this bird to the list, use "next launcher"
						// function to cycle to next weapon
						for (MissileAPI b : (List<MissileAPI>) this.engine.getCustomData()
								.get(this.prefix + d.toString())) {
							if (b != null && this.isMissileAlive(b)
									&& b.getWeaponSpec().getWeaponId() == this.missile.getWeaponSpec().getWeaponId()) {
								// if (b.getWeaponSpec().getWeaponId() == m.getWeaponSpec().getWeaponId()) {
								// log.info("Filter " + m + " (" + this.missile + ")");
								continue findTargetLoop;
							}
						}
						List<MissileAPI> birds = new ArrayList<MissileAPI>();
						birds = (List<MissileAPI>) this.engine.getCustomData().get(this.prefix + d.toString());
						this.setTarget((CombatEntityAPI) d);
						this.missile.setFacing(
								Misc.getAngleInDegrees(this.missile.getLocation(), this.target.getLocation()));
						// log.info("Kill Track2 " + m + " with " + this.missile + " " +
						// this.missile.getWeaponSpec().getWeaponId());
						birds.add(this.missile);
						this.nextLauncher();
						return;
					}
				}
				// log.info("No Missile Target Found by " + this.missile.getWeapon());
			}
		}
		// If still no valid target, self destruct
		if (this.target != null && this.target instanceof DamagingProjectileAPI) {
			if (!this.isProjectileAlive((DamagingProjectileAPI) this.target)) {
				// log.info("Lost Track " + this.target + " (" + this.missile + ")");
				this.selfDestruct(this.missile, this.engine);
				return;
			}
		}
		if (this.target == null) {
			this.selfDestruct(this.missile, this.engine);
			return;
		}
		// Guidence and movement code from Tartiflette's Diable Avionics mod
		// float dist = MathUtils.getDistanceSquared(this.missile.getLocation(),
		// this.target.getLocation());
		// if (dist < Math.pow(this.missile.getSpec().getExplosionRadius(), 2) &&
		// this.target instanceof MissileAPI) {
		// this.proximityFuse();
		// return;
		// }
		this.lead = AIUtils.getBestInterceptPoint(this.missile.getLocation(), this.MAX_SPEED, this.target.getLocation(),
				this.target.getVelocity());
		if (this.lead == null) {
			this.lead = this.target.getLocation();
		}
		float correctAngle = VectorUtils.getAngle(this.missile.getLocation(), this.lead);
		float correction = MathUtils.getShortestRotation(VectorUtils.getFacing(this.missile.getVelocity()),
				correctAngle);
		if (correction > 0.0f) {
			correction = -11.25f
					* ((float) Math.pow(FastTrig.cos((double) (3.1415927f * correction / 90.0f)) + 1.0, 2.0) - 4.0f);
		} else {
			correction = 11.25f
					* ((float) Math.pow(FastTrig.cos((double) (3.1415927f * correction / 90.0f)) + 1.0, 2.0) - 4.0f);
		}
		correctAngle += correction;
		float aimAngle = MathUtils.getShortestRotation(this.missile.getFacing(), correctAngle);
		if (aimAngle < 0.0f) {
			this.missile.giveCommand(ShipCommand.TURN_RIGHT);
		} else {
			this.missile.giveCommand(ShipCommand.TURN_LEFT);
		}
		if (Math.abs(aimAngle) < 45.0f) {
			this.missile.giveCommand(ShipCommand.ACCELERATE);
		}
		// if (this.missile.getDamageType() == DamageType.KINETIC &&
		// MathUtils.getDistanceSquared(this.missile.getLocation(),
		// this.target.getLocation()) < 40000) {
		// this.missile.setFacing(Misc.getAngleInDegrees(this.missile.getLocation(),
		// this.target.getLocation()));
		// this.missile.setFacing(this.missile.getFacing() + aimAngle);
		// this.missile.getVelocity().set(this.missile.getVelocity().length() * (float)
		// Math.sin(Math.toRadians(this.missile.getFacing())),
		// this.missile.getVelocity().length() * (float)
		// Math.cos(Math.toRadians(this.missile.getFacing())));
		// }
		if (Math.abs(aimAngle) < Math.abs(this.missile.getAngularVelocity()) * 0.05f) {
			this.missile.setAngularVelocity(aimAngle / 0.05f);
		}
	}

	public void nextLauncher() {
		// If this missile is fired from launcher manually by player, skip
		if (this.missile.getSource() == this.engine.getPlayerShip()) {
			if (this.missile.getSource().getWeaponGroupFor(this.missile.getWeapon()) == this.missile.getSource()
					.getSelectedGroupAPI()) {
				return;
			}
		}
		// Cycle to next weapon in the weapon list
		if (this.missile.getWeapon() == (WeaponAPI) Global.getCombatEngine().getCustomData()
				.get(this.prefix + this.missile.getSource().toString() + this.missile.getWeaponSpec().getWeaponId())) {
			List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
			weapons = (List<WeaponAPI>) Global.getCombatEngine().getCustomData().get(this.prefix
					+ this.missile.getSource().toString() + this.missile.getWeaponSpec().getWeaponId() + "list");
			int index = weapons.indexOf(this.missile.getWeapon()) + 1;
			while (true) {
				if (index >= weapons.size()) {
					index = 0;
				}
				if (this.missile.getSource().getWeaponGroupFor((WeaponAPI) weapons.get(index)).isAutofiring()
						&& this.missile.getSource().getWeaponGroupFor((WeaponAPI) weapons.get(index)) != this.engine
								.getPlayerShip().getSelectedGroupAPI()) {
					break;
				}
				if (index == weapons.indexOf(this.missile.getWeapon())) {
					return;
				}
				index++;
			}
			// log.info("Next Launcher " + weapons.get(index) + " isAuto " +
			// this.missile.getSource().getWeaponGroupFor(weapons.get(index)).isAutofiring());
			Global.getCombatEngine().getCustomData().put(
					this.prefix + this.missile.getSource().toString() + this.missile.getWeaponSpec().getWeaponId(),
					weapons.get(index));
		}
		return;
	}

	public boolean isProjectileAlive(DamagingProjectileAPI proj) {
		if (!this.engine.isEntityInPlay(proj) || proj.isFading() || proj.getCollisionClass() == CollisionClass.NONE) {
			return false;
		} else {
			return true;
		}
	}

	public boolean isMissileAlive(MissileAPI missile) {
		if (!this.engine.isEntityInPlay(missile) || missile.isFading()
				|| missile.getCollisionClass() == CollisionClass.NONE) {
			return false;
		} else {
			return true;
		}
	}

	private void selfDestruct(final MissileAPI missile, final CombatEngineAPI engine) {
		engine.applyDamage((CombatEntityAPI) missile, missile.getLocation(), missile.getHitpoints() * 2.0f,
				DamageType.FRAGMENTATION, 0.0f, false, false, (Object) missile);
	}

	void proximityFuse() {
		this.engine.applyDamage(this.target, this.target.getLocation(), this.missile.getDamageAmount(),
				DamageType.FRAGMENTATION, 0.0f, false, false, (Object) this.missile.getSource());
		DamagingExplosionSpec boom = new DamagingExplosionSpec(0.1f, this.missile.getSpec().getExplosionRadius() * 1.5f,
				this.missile.getSpec().getExplosionRadius(), this.missile.getDamageAmount(), 50.0f,
				CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_FIGHTER, 2.0f, 5.0f, 5.0f, 25,
				new Color(225, 100, 0), new Color(200, 100, 25));
		boom.setDamageType(DamageType.FRAGMENTATION);
		boom.setShowGraphic(false);
		boom.setSoundSetId("explosion_flak");
		this.engine.spawnDamagingExplosion(boom, this.missile.getSource(), this.missile.getLocation());
		if (MagicRender.screenCheck(0.1f, this.missile.getLocation())) {
			this.engine.addHitParticle(this.missile.getLocation(), new Vector2f(), 100.0f, 1.0f, 0.25f,
					this.EXPLOSION_COLOR);
			for (int i = 0; i < 20; ++i) {
				final float axis = (float) Math.random() * 360.0f;
				final float range = (float) Math.random() * 100.0f;
				this.engine.addHitParticle(
						MathUtils.getPointOnCircumference(this.missile.getLocation(), range / 5.0f, axis),
						MathUtils.getPointOnCircumference(new Vector2f(), range, axis),
						2.0f + (float) Math.random() * 2.0f, 1.0f, 1.0f + (float) Math.random(), this.PARTICLE_COLOR);
			}
			this.engine.applyDamage((CombatEntityAPI) this.missile, this.missile.getLocation(),
					this.missile.getHitpoints() * 2.0f, DamageType.FRAGMENTATION, 0.0f, false, false,
					(Object) this.missile);
		} else {
			this.engine.removeEntity((CombatEntityAPI) this.missile);
		}
	}

	public CombatEntityAPI getTarget() {
		return this.target;
	}

	public void setTarget(CombatEntityAPI target) {
		this.target = target;
	}

	public void init(CombatEngineAPI engine) {
	}
}