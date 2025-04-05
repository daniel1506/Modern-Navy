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
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.magiclib.util.MagicRender;
//import org.magiclib.util.MagicTargeting;

//import org.apache.log4j.Logger;

public class AAMMissileAI implements MissileAIPlugin, GuidedMissileAI {

	// public Logger log = Logger.getLogger(this.getClass());

	public static final String prefix = "AESA";
	public static final Color EXPLOSION_COLOR = new Color(255, 0, 0, 255);
	public static final Color PARTICLE_COLOR = new Color(240, 200, 50, 255);

	private Vector2f lead;
	private final float MAX_SPEED;
	private CombatEngineAPI engine;
	private MissileAPI missile;
	private ShipAPI ship;
	// private String shipUid;
	private String weaponUid;
	private CombatEntityAPI target;

	public AAMMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		// this.EXPLOSION_COLOR = new Color(255, 0, 0, 255);
		// this.PARTICLE_COLOR = new Color(240, 200, 50, 255);
		this.lead = new Vector2f();
		this.MAX_SPEED = missile.getMaxSpeed() * 1.25f;
		this.missile = missile;
		this.weaponUid = missile.getWeapon().toString();
		this.ship = missile.getSource();
		// this.shipUid = missile.getSource().toString();
	}

	public void advance(float amount) {
		if (this.missile == null) {
			return;
		}
		if (this.engine != Global.getCombatEngine()) {
			this.engine = Global.getCombatEngine();
		}
		if (this.engine.isPaused() || this.missile.isFading()) {
			return;
		}
		if (this.missile.isFizzling()) {
			this.selfDestruct(this.missile, this.engine);
			return;
		}

		if (this.target != null) {
			if (this.target instanceof MissileAPI) {
				if (!this.isMissileAlive((MissileAPI) this.target)) {
					this.aquireNewTarget();
				}
			}
			if (this.target instanceof ShipAPI) {
				if (!((ShipAPI) this.target).isAlive()) {
					this.aquireNewTarget();
				}
			}
		}

		if (this.target == null) {
			this.missile.giveCommand(ShipCommand.ACCELERATE);
			// Aquire a missile target from track list of its source weapon
			this.getFighterTarget();
			if (this.target == null) {
				if (this.missile.getWeapon().getId().equals("mn_aim174b")) {
					this.getMissileTarget();
				} else {
					this.aquireNewTarget();
				}
			}
			// if (this.missile.getDamageType() == DamageType.KINETIC) {
			// this.missile.setFacing(Misc.getAngleInDegrees(this.missile.getLocation(),
			// this.target.getLocation()));
			// }
		}

		if (this.target == null) {
			return;
		}

		// Guidence and movement code from Tartiflette's Diable Avionics mod
		float dist = MathUtils.getDistanceSquared(this.missile.getLocation(), this.target.getLocation());
		// if (this.missile.getDamageType() != DamageType.KINETIC) {
		if (dist < Math.pow(this.missile.getSpec().getExplosionRadius(), 2) * 0.85f) {
			this.proximityFuse();
			return;
		}
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
			// if (this.missile.getDamageType() == DamageType.KINETIC) {
			// if (this.missile.getVelocity().length() < 2041f
			// || Misc.getDistance(this.missile.getLocation(), this.target.getLocation()) <=
			// 750f) {
			// this.missile.giveCommand(ShipCommand.ACCELERATE);
			// }
			// } else {
			this.missile.giveCommand(ShipCommand.ACCELERATE);
			// }
		}
		if (Math.abs(aimAngle) < Math.abs(this.missile.getAngularVelocity()) * 0.05f) {
			this.missile.setAngularVelocity(aimAngle / 0.05f);
		}
	}

	private void getFighterTarget() {
		if (this.engine.getCustomData().containsKey(prefix + this.weaponUid + "fighter")) {
			for (ShipAPI s : (List<ShipAPI>) this.engine.getCustomData()
					.get(prefix + this.weaponUid + "fighter")) {
				if (this.isTargetShipValid(s)) {
					if (this.engine.getCustomData().containsKey(prefix + s.toString())) {
						((List<MissileAPI>) this.engine.getCustomData().get(prefix + s.toString())).add(this.missile);
					} else {
						List<MissileAPI> birds = new ArrayList<MissileAPI>();
						birds.add(this.missile);
						this.engine.getCustomData().put(prefix + s.toString(), birds);
					}
					((List<ShipAPI>) this.engine.getCustomData()
							.get(prefix + this.ship.getOwner() + "fighterTrackList"))
							.remove(s);
					this.setTarget((CombatEntityAPI) s);
					// this.updateActiveWeapon(this.missile.getWeapon());
					((List<ShipAPI>) this.engine.getCustomData().get(prefix + this.weaponUid + "fighter")).remove(s);
					return;
				}
			}
		}
	}

	private void getMissileTarget() {
		if (this.engine.getCustomData().containsKey(prefix + this.weaponUid + "missile")) {
			for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData()
					.get(prefix + this.weaponUid + "missile")) {
				if (this.isTargetMissileValid(m)) {
					if (this.engine.getCustomData().containsKey(prefix + m.toString())) {
						((List<MissileAPI>) this.engine.getCustomData().get(prefix + m.toString())).add(this.missile);
					} else {
						List<MissileAPI> birds = new ArrayList<MissileAPI>();
						birds.add(this.missile);
						this.engine.getCustomData().put(prefix + m.toString(), birds);
					}
					((List<MissileAPI>) this.engine.getCustomData()
							.get(prefix + this.ship.getOwner() + "missileTrackList"))
							.remove(m);
					this.setTarget((CombatEntityAPI) m);
					// this.updateActiveWeapon(this.missile.getWeapon());
					return;
				}
			}
		}
	}

	private void aquireNewTarget() {
		// float range = this.missile.getMaxSpeed() * 0.9f
		// * (this.missile.getMaxFlightTime() - this.missile.getFlightTime());
		float range = this.missile.getMaxSpeed() * this.missile.getFlightTime() * 0.9f;
		for (ShipAPI s : CombatUtils.getShipsWithinRange(this.missile.getLocation(), range)) {
			if (this.isTargetShipValid(s)) {
				if (this.engine.getCustomData().containsKey(prefix + s.toString())) {
					((List<MissileAPI>) this.engine.getCustomData().get(prefix + s.toString()))
							.add(this.missile);
				} else {
					List<MissileAPI> birds = new ArrayList<MissileAPI>();
					birds.add(this.missile);
					this.engine.getCustomData().put(prefix + s.toString(), birds);
				}
				this.setTarget((CombatEntityAPI) s);
				return;
			}
		}
		if (this.missile.getWeapon().getId().equals("mn_aim174b")) {
			for (MissileAPI m : CombatUtils.getMissilesWithinRange(this.missile.getLocation(), range)) {
				if (this.isTargetMissileValid(m)) {
					if (Misc.getAngleDiff(this.missile.getFacing(),
							Misc.getAngleInDegrees(this.missile.getLocation(), m.getLocation())) <= 60f) {
						if (this.engine.getCustomData().containsKey(prefix + m.toString())) {
							((List<MissileAPI>) this.engine.getCustomData().get(prefix + m.toString()))
									.add(this.missile);
						} else {
							List<MissileAPI> birds = new ArrayList<MissileAPI>();
							birds.add(this.missile);
							this.engine.getCustomData().put(prefix + m.toString(), birds);
						}
						this.setTarget((CombatEntityAPI) m);
						return;
					}
				}
			}
		}
		// this.selfDestruct(this.missile, this.engine);
	}

	// private boolean isWeaponAvailable(WeaponAPI weapon) {
	// if (weapon == null)
	// return false;
	// if (weapon.isDisabled() || weapon.getCooldownRemaining() > 0f) {
	// return false;
	// }
	// if (this.ship == this.engine.getPlayerShip() &&
	// !this.engine.getCombatUI().isAutopilotOn()
	// && this.ship.getWeaponGroupFor(weapon) == this.ship.getSelectedGroupAPI()) {
	// return false;
	// }
	// if (weapon.usesAmmo()) {
	// if (weapon.getAmmo() == 0) {
	// return false;
	// }
	// }
	// return true;
	// }

	// private void updateActiveWeapon(WeaponAPI weapon) {
	// if (this.ship == this.engine.getPlayerShip() &&
	// !this.engine.getCombatUI().isAutopilotOn()) {
	// if (this.ship.getWeaponGroupFor(this.missile.getWeapon()) ==
	// this.ship.getSelectedGroupAPI()) {
	// return;
	// }
	// }
	// List<WeaponAPI> weapons = (List<WeaponAPI>) this.engine.getCustomData()
	// .get(prefix + this.shipUid + weapon.getId() + "list");
	// int index = weapons.indexOf(weapon) + 1;
	// while (true) {
	// if (index >= weapons.size()) {
	// index = 0;
	// } else if (this.isWeaponAvailable((WeaponAPI) weapons.get(index))) {
	// this.engine.getCustomData().put(prefix + this.shipUid + weapon.getId(),
	// weapons.get(index));
	// break;
	// } else if (index == weapons.indexOf(weapon)) {
	// break;
	// } else
	// index++;
	// }
	// }

	public boolean isInterceptMissile(MissileAPI targetMissile) {
		if (targetMissile.getMissileAI() instanceof GuidedMissileAI) {
			GuidedMissileAI missileAI = (GuidedMissileAI) targetMissile.getMissileAI();
			if (missileAI.getTarget() instanceof MissileAPI || missileAI.getTarget() == null) {
				return true;
			} else if (missileAI.getTarget() instanceof ShipAPI) {
				if (((ShipAPI) missileAI.getTarget()).isFighter()) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isMissileAlive(MissileAPI missile) {
		if (!this.engine.isEntityInPlay(missile) || missile.isFading()
				|| missile.getCollisionClass() == CollisionClass.NONE) {
			return false;
		} else {
			return true;
		}
	}

	public boolean isTargetMissileValid(MissileAPI targetMissile) {
		if (targetMissile == null) {
			return false;
		}
		if (this.missile.getSourceAPI().getOwner() == targetMissile.getOwner()) {
			return false;
		}
		if (!this.isMissileAlive(targetMissile) || this.isInterceptMissile(targetMissile)) {
			return false;
		}
		if (targetMissile.getCollisionClass() == CollisionClass.NONE) {
			return false;
		}
		if (this.engine.getCustomData().containsKey(prefix + targetMissile.toString())) {
			for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData().get(prefix + targetMissile.toString())) {
				if (this.isMissileAlive(m)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isTargetShipValid(ShipAPI s) {
		if (s == null) {
			return false;
		}
		if (!s.isFighter() || s.getOwner() == this.ship.getOwner()) {
			return false;
		}
		if (!s.isAlive() || s.getCollisionClass() == CollisionClass.NONE) {
			return false;
		}
		return true;
	}

	private void selfDestruct(final MissileAPI missile, final CombatEngineAPI engine) {
		engine.applyDamage((CombatEntityAPI) missile, missile.getLocation(), missile.getHitpoints() * 2.0f,
				DamageType.FRAGMENTATION, 0.0f, false, false, (Object) missile);
	}

	private void proximityFuse() {
		this.engine.applyDamage(this.target, this.target.getLocation(), this.missile.getDamageAmount() * 0.5f,
				DamageType.HIGH_EXPLOSIVE, 0.0f, false, false, (Object) this.missile.getSource());
		DamagingExplosionSpec boom = new DamagingExplosionSpec(0.1f, this.missile.getSpec().getExplosionRadius() * 1.2f,
				this.missile.getSpec().getExplosionRadius(), this.missile.getDamageAmount(), 50.0f,
				CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_FIGHTER, 2.0f, 5.0f, 5.0f, 25,
				new Color(225, 100, 0), new Color(200, 100, 25));
		boom.setDamageType(DamageType.FRAGMENTATION);
		boom.setShowGraphic(false);
		boom.setSoundSetId("explosion_flak");
		this.engine.spawnDamagingExplosion(boom, this.missile.getSource(), this.missile.getLocation());
		if (MagicRender.screenCheck(0.1f, this.missile.getLocation())) {
			this.engine.addHitParticle(this.missile.getLocation(), new Vector2f(), 100.0f, 1.0f, 0.25f,
					EXPLOSION_COLOR);
			for (int i = 0; i < 20; ++i) {
				final float axis = (float) Math.random() * 360.0f;
				final float range = (float) Math.random() * 100.0f;
				this.engine.addHitParticle(
						MathUtils.getPointOnCircumference(this.missile.getLocation(), range / 5.0f, axis),
						MathUtils.getPointOnCircumference(new Vector2f(), range, axis),
						2.0f + (float) Math.random() * 2.0f, 1.0f, 1.0f + (float) Math.random(), PARTICLE_COLOR);
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