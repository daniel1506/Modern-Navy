package data.scripts.ai;

import java.lang.String;
import java.lang.Integer;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatUIAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.combat.CombatUtils;
import org.magiclib.util.MagicTargeting;

//import org.apache.log4j.Logger;

public class CounterBatterySystemAI implements AutofireAIPlugin {
	
	//public Logger log = Logger.getLogger(this.getClass());
	
	private CombatEngineAPI engine;
	private WeaponAPI weapon;
	private ShipAPI ship;
	private DamagingProjectileAPI targetProjectile;
	private ShipAPI targetShip;
	private boolean isForceOff = false;	
	private DamagingProjectileAPI tempTargetProjectile;
	private int weaponArc = 360;
	private static final String prefix = "CBS";
	
	private IntervalUtil fireTimer = new IntervalUtil(1f, 1.5f);
	private List<DamagingProjectileAPI> tracks = new ArrayList<DamagingProjectileAPI>();

	public CounterBatterySystemAI(final WeaponAPI weapon, final ShipAPI ship) {
		this.weapon = weapon;
		this.ship = ship;		
    }
	
	public void advance(float amount) {
		
		if (this.engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
		if (this.engine.isPaused()) {
            return;
        }
		
		//initalize data storing the target list of this weapon
		if (!this.engine.getCustomData().containsKey(this.prefix + this.weapon.toString())) {
			this.engine.getCustomData().put(this.prefix + this.weapon.toString(), this.tracks);
		}
		//initalize data storing the current active weapon among the same weapons of the ship
		if (!this.engine.getCustomData().containsKey(this.prefix + this.ship.toString() + this.weapon.getId())) {
			this.engine.getCustomData().put(this.prefix + this.ship.toString() + this.weapon.getId(), this.weapon);
		}
		//initalize data storing the all weapons of the same kind on the ship
		if (!this.engine.getCustomData().containsKey(this.prefix + this.ship.toString() + this.weapon.getId() + "list")) {
			List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
			for (WeaponAPI w: this.ship.getAllWeapons()) {
				if (w.getId() == this.weapon.getId()) {
					weapons.add(w);
				}
			}
			this.engine.getCustomData().put(this.prefix + this.ship.toString() + this.weapon.getId() + "list", weapons);			
		}
		
		this.checkIsAutoFire();
		this.updateTracks();		
		
		if (this.targetShip != null && this.weapon == (WeaponAPI) this.engine.getCustomData().get(this.prefix + this.ship.toString() + this.weapon.getId())) {
			this.fireTimer.advance(amount);
		}
		
		if (this.targetProjectile == null) {
			this.targetProjectile = this.selectTargetProjectile();
			//this.tempTargetProjectile = MagicTargeting.randomMissile((CombatEntityAPI)this.ship, MagicTargeting.missilePriority.DAMAGE_PRIORITY, this.weapon.getLocation(), this.weapon.getArcFacing(), Integer.valueOf(weaponArc), Integer.valueOf((int) this.weapon.getRange() + 200));
			//if (this.isTargetMissileValid(this.tempTargetMissile)) {
			//	this.targetMissile = this.tempTargetMissile;
			//	return;
			//}
			//if (!this.isTargetMissileValid(this.tempTargetMissile)) {
			//	this.tempTargetMissile = MagicTargeting.randomMissile((CombatEntityAPI)this.ship, MagicTargeting.missilePriority.RANDOM, this.weapon.getLocation(), this.weapon.getArcFacing(), Integer.valueOf(weaponArc), Integer.valueOf((int) this.weapon.getRange()));
			//	if (this.isTargetMissileValid(this.tempTargetMissile)) {
			//		this.targetMissile = this.tempTargetMissile;
			//		return;
			//	}
			//}		
		} else if (this.targetProjectile != null) {
			if (!this.isTargetProjectileValid(this.targetProjectile)) {
				this.targetProjectile = null;
				return;
			}
		}
		
		//if (this.targetShip == null) {
		//	if (this.weapon.hasAIHint(AIHints.ANTI_FTR)) {
		//		this.targetShip = MagicTargeting.pickShipTarget(this.ship, MagicTargeting.targetSeeking.FULL_RANDOM , Integer.valueOf((int) this.weapon.getRange()), Integer.valueOf(weaponArc), Integer.valueOf(100), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));				
		//		if (this.targetShip != null) {
		//			return;
		//		};
		//	}
		//	if (this.weapon.hasAIHint(AIHints.PD_ONLY)) {
		//		this.targetShip = null;
		//		return;
		//	}
		//	this.targetShip = MagicTargeting.pickShipTarget(this.ship, MagicTargeting.targetSeeking.LOCAL_RANDOM , Integer.valueOf((int) this.weapon.getRange()), Integer.valueOf(weaponArc), Integer.valueOf(25), Integer.valueOf(25), Integer.valueOf(25), Integer.valueOf(25), Integer.valueOf(25));
		//}
		//if (this.targetShip != null) {
		//	if (!this.targetShip.isAlive() || Misc.getDistance(this.weapon.getLocation(), this.targetShip.getLocation()) > this.weapon.getRange()) {
		//		this.targetShip = null;
		//		return;
		//	}
		//}
		return;
	}
	
	public void forceOff() {
		this.isForceOff = true;
		return;
	}
	
	public Vector2f getTarget() {
		if (this.targetProjectile != null) {
			return this.engine.getAimPointWithLeadForAutofire((CombatEntityAPI) this.ship, 1f, (CombatEntityAPI) this.targetProjectile, this.weapon.getProjectileSpeed());
		} else if (this.targetShip != null) {
			return this.engine.getAimPointWithLeadForAutofire((CombatEntityAPI) this.ship, 1f, (CombatEntityAPI) this.targetShip, this.weapon.getProjectileSpeed());
		} else {
			return null;
		}
	}
	
	public DamagingProjectileAPI getTargetProjectile() {
		return this.targetProjectile;
	}
	
	public DamagingProjectileAPI selectTargetProjectile() {
		DamagingProjectileAPI selectedTarget = null;
		List<DamagingProjectileAPI> projectiles = CombatUtils.getProjectilesWithinRange(this.weapon.getLocation(), this.weapon.getRange() * 1.75f);
		List<DamagingProjectileAPI> validTargets = new ArrayList<DamagingProjectileAPI>();
		for (DamagingProjectileAPI p : projectiles) {
			if (!this.isTargetProjectileValid(p)) {
				continue;
			}
			if (p.getDamage().getDamage() < 150f) {
				continue;
			}
			if (Misc.getAngleDiff(p.getFacing(), Misc.getAngleInDegrees(p.getLocation(), this.ship.getLocation()) ) > 90f) {
				continue;
			}
			float m = (float) Math.tan(Math.toRadians(p.getFacing()));
			float n = p.getLocation().getY() - p.getLocation().getX() * (float) Math.tan(Math.toRadians(p.getFacing()));
			float h = this.ship.getLocation().getX();
			float k = this.ship.getLocation().getY();
			float r = this.ship.getCollisionRadius() + 50f;
			if ((Math.pow((2 * m * n - 2 * k - 2 * n), 2) - 4 * (m * m + 1) * (n * n - 2 * n * k - r * r + h * h + k * k)) < 0) {
				continue;
			}
			//if (this.weapon.getSlot().isTurret()) {
			//	if (Misc.getAngleDiff(this.weapon.getArcFacing(), Misc.getAngleInDegrees(this.weapon.getLocation(), p.getLocation()) ) > 30f) {
			//		continue;
			//	}
			//}
			validTargets.add(p);
		}
		for (DamagingProjectileAPI p : validTargets) {
			if (selectedTarget == null) {
				selectedTarget = p;
			} else if (selectedTarget.getDamage().getDamage() < p.getDamage().getDamage()) {
				selectedTarget = p;
			} else if (selectedTarget.getDamage().getDamage() == p.getDamage().getDamage()) {
				if (Misc.getDistance(selectedTarget.getLocation(), this.weapon.getLocation()) > Misc.getDistance(p.getLocation(), this.weapon.getLocation())) {
					selectedTarget = p;
				}
			}
		}		
		return selectedTarget;
	}
	
	public ShipAPI getTargetShip() {
		return this.targetShip;
	}
	
	public MissileAPI getTargetMissile() {
		return null;
	}
	
	public WeaponAPI getWeapon() {
		return this.weapon;
	}
	
	public boolean shouldFire() {
		if (Global.getCombatEngine().isPaused()) {
            return false;
        }
		if (this.isForceOff) {
			this.isForceOff = false;
			return false;
		}
		if (this.weapon.isDisabled() || this.weapon.getCooldownRemaining() > 0f) {
			return false;
		}
		if (this.targetProjectile != null && this.weapon == this.engine.getCustomData().get(this.prefix + this.ship.toString() + this.weapon.getId())) {
			if (isTargetProjectileValid(this.targetProjectile)) {
				if (!tracks.contains(this.targetProjectile)) {
					this.tracks.add(this.targetProjectile);
					//log.info("Targetting " + Misc.getDistance(this.weapon.getLocation(), this.targetMissile.getLocation()) + " by " + this.weapon.getId() + " " + this.getMinLaunchDist());
				}				
				return true;
			}				
		}
		return false;
	}
	
	public boolean isTargetProjectileValid(DamagingProjectileAPI projectile) {
		if (projectile == null) {
			return false;
		}
		if (!isProjectileAlive(projectile)) {
			return false;
		}
		if (this.ship.getOwner() == projectile.getOwner()) {
			return false;
		}
		if (projectile.getProjectileSpec() != null) {
			if (projectile.getProjectileSpec().isPassThroughMissiles() || projectile.getCollisionClass() == CollisionClass.NONE) {
				return false;
			}			
		}
		if (Misc.getDistance(this.weapon.getLocation(), projectile.getLocation()) <= this.getMinLaunchDist()) {			
			return false;
		}	
		if (this.engine.getCustomData().containsKey(this.prefix + projectile.toString())) {
			for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData().get(this.prefix + projectile.toString())) {
				if (this.isMissileAlive(m) && m.getSourceAPI() == this.ship) {
					return false;
				}
				if (m.getWeaponSpec().getWeaponId() == this.weapon.getId()) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean isProjectileAlive(DamagingProjectileAPI projectile) {
		if (!this.engine.isEntityInPlay(projectile) || projectile.isFading() || projectile.didDamage()) {
			return false;
		}
		return true;
	}
	
	public boolean isMissileAlive(MissileAPI missile) {
		if (!this.engine.isEntityInPlay(missile) || missile.isFading() || missile.isFizzling()) {
			return false;
		}
		return true;
	}
	
	public void checkIsAutoFire() {
		//check if this weapon is operational
		if (this.weapon.isDisabled()) {
			return;
		}
		if (this.weapon.usesAmmo()) {
			if (this.weapon.getAmmo() == 0) {
				return;
			}
		}
		
		//check if the current active weapon is operational and in auto mode
		WeaponAPI currentWeapon = (WeaponAPI) this.engine.getCustomData().get(this.prefix + this.ship.toString() + this.weapon.getId());
		if (this.ship != this.engine.getPlayerShip() || this.ship.getWeaponGroupFor(currentWeapon) != this.ship.getSelectedGroupAPI()) {
			if (!currentWeapon.isDisabled()) {
				if (currentWeapon.usesAmmo()) {
					if (currentWeapon.getAmmo() > 0) {
						return;
					}
				} else return;
			}		
		}
		
		//replace current active weapon with this weapon if needed
		this.engine.getCustomData().put(this.prefix + this.ship.toString() + this.weapon.getId(), this.weapon);
		return;
	}
	
	public void updateTracks() {
		if (!this.tracks.isEmpty()) {
			//filter out the dead track
			List<DamagingProjectileAPI> newTracks = new ArrayList<DamagingProjectileAPI>();
			for (DamagingProjectileAPI d : this.tracks) {
				if (this.isProjectileAlive(d)) {
					newTracks.add(d);			
				} else {
					if (this.engine.getCustomData().containsKey(this.prefix + d.toString())) {
						this.engine.getCustomData().remove(this.prefix + d.toString());	
					}
					//log.info("Track " + m + " is history!");
				}
			}
			this.tracks.clear();
			this.tracks.addAll(newTracks);
			//update the list of intercept missiles that are targeting the projectiles
			for (DamagingProjectileAPI d : this.tracks) {
				if (this.engine.getCustomData().containsKey(this.prefix + d.toString())) {					
					List<DamagingProjectileAPI> birds = new ArrayList<DamagingProjectileAPI>();
					for (MissileAPI b : (List<MissileAPI>) this.engine.getCustomData().get(this.prefix + d.toString())) {
						if (this.isMissileAlive(b)) {
							if (b.getMissileAI() instanceof GuidedMissileAI) {
								GuidedMissileAI missileAI = (GuidedMissileAI) b.getMissileAI();
								if (d == (DamagingProjectileAPI) missileAI.getTarget()) {
									birds.add(b);
								}
							}																				
						}
					}
					this.engine.getCustomData().put(this.prefix + d.toString(), birds);
				}
			}
		}
		return;
	}
	
	public float getMinLaunchDist() {
		if (this.weapon.getId().equals("mn_iron_dome")) {	
			return 100f;
		}		
		return 0f;
	}

}
