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
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.combat.CombatUtils;
import data.scripts.util.MagicTargeting;

import org.apache.log4j.Logger;

public class AegisCombatSystemAI implements AutofireAIPlugin {
	
	//public Logger log = Logger.getLogger(this.getClass());
	
	private CombatEngineAPI engine;
	private WeaponAPI weapon;
	private ShipAPI ship;
	private MissileAPI targetMissile;
	private ShipAPI targetShip;
	private boolean isForceOff = false;	
	private MissileAPI tempTargetMissile;
	private int weaponArc = 360;
	
	private IntervalUtil fireTimer = new IntervalUtil(1f, 1.5f);
	private List<MissileAPI> tracks = new ArrayList<MissileAPI>();

	public AegisCombatSystemAI(final WeaponAPI weapon, final ShipAPI ship) {
		this.weapon = weapon;
		this.ship = ship;
    }
	
	public void advance(float amount) {

		if (Global.getCombatEngine().isPaused()) {
            return;
        }
		if (this.engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
		if (!this.engine.getCustomData().containsKey(this.weapon.toString())) {
			this.engine.getCustomData().put(this.weapon.toString(), this.tracks);
		}
		if (!this.engine.getCustomData().containsKey(this.ship.toString() + this.weapon.getId())) {
			this.engine.getCustomData().put(this.ship.toString() + this.weapon.getId(), this.weapon);
		}
		if (!this.engine.getCustomData().containsKey(this.ship.toString() + this.weapon.getId() + "list") ) {
			List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
			for (WeaponAPI w: this.ship.getAllWeapons()) {
				if (w.getId() == this.weapon.getId()) {
					weapons.add(w);
				}
			}
			this.engine.getCustomData().put(this.ship.toString() + this.weapon.getId() + "list", weapons);			
		}
		
		this.checkIsAutoFire();
		this.updateTracks();		
		
		if (this.targetShip != null && this.weapon == (WeaponAPI) this.engine.getCustomData().get(this.ship.toString() + this.weapon.getId())) {
			this.fireTimer.advance(amount);
		}
		
		if (this.targetMissile == null) {
			this.tempTargetMissile = MagicTargeting.randomMissile((CombatEntityAPI)this.ship, MagicTargeting.missilePriority.DAMAGE_PRIORITY, this.weapon.getLocation(), this.weapon.getArcFacing(), Integer.valueOf(weaponArc), Integer.valueOf((int) this.weapon.getRange()));
			if (this.isTargetMissileValid(this.tempTargetMissile)) {
				this.targetMissile = this.tempTargetMissile;
				return;
			}
			if (!this.isTargetMissileValid(this.tempTargetMissile)) {
				this.tempTargetMissile = MagicTargeting.randomMissile((CombatEntityAPI)this.ship, MagicTargeting.missilePriority.RANDOM, this.weapon.getLocation(), this.weapon.getArcFacing(), Integer.valueOf(weaponArc), Integer.valueOf((int) this.weapon.getRange()));
				if (this.isTargetMissileValid(this.tempTargetMissile)) {
					this.targetMissile = this.tempTargetMissile;
					return;
				}
			}		
		}
		else if (this.targetMissile != null) {
			if (!this.isTargetMissileValid(this.targetMissile)) {
				this.targetMissile = null;
				return;
			}
		}
		
		if (this.targetShip == null) {
			if (this.weapon.hasAIHint(AIHints.ANTI_FTR)) {
				this.targetShip = MagicTargeting.pickShipTarget(this.ship, MagicTargeting.targetSeeking.FULL_RANDOM , Integer.valueOf((int) this.weapon.getRange()), Integer.valueOf(weaponArc), Integer.valueOf(100), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));				
				if (this.targetShip != null) {
					return;
				};
			}
			if (this.weapon.hasAIHint(AIHints.PD_ONLY)) {
				this.targetShip = null;
				return;
			}
			this.targetShip = MagicTargeting.pickShipTarget(this.ship, MagicTargeting.targetSeeking.LOCAL_RANDOM , Integer.valueOf((int) this.weapon.getRange()), Integer.valueOf(weaponArc), Integer.valueOf(25), Integer.valueOf(25), Integer.valueOf(25), Integer.valueOf(25), Integer.valueOf(25));
		}
		if (this.targetShip != null) {
			if (!this.targetShip.isAlive() || Misc.getDistance(this.weapon.getLocation(), this.targetShip.getLocation()) > this.weapon.getRange()) {
				this.targetShip = null;
				return;
			}
		}
		return;
	}
	
	public void forceOff() {
		this.isForceOff = true;
		return;
	}
	
	public Vector2f getTarget() {
		if (this.targetMissile != null) {
			return this.engine.getAimPointWithLeadForAutofire((CombatEntityAPI) this.ship, 1f, (CombatEntityAPI) this.targetMissile, this.weapon.getProjectileSpeed());
		} else if (this.targetShip != null) {
			return this.engine.getAimPointWithLeadForAutofire((CombatEntityAPI) this.ship, 1f, (CombatEntityAPI) this.targetShip, this.weapon.getProjectileSpeed());
		} else {
			return null;
		}
	}
	
	public MissileAPI getTargetMissile() {
		return this.targetMissile;
	}
	
	public ShipAPI getTargetShip() {
		return this.targetShip;
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
		if ((this.targetMissile != null || this.targetShip != null) && this.weapon == this.engine.getCustomData().get(this.ship.toString() + this.weapon.getId())) {
			if (this.targetMissile != null && isTargetMissileValid(this.targetMissile)) {
				if (!tracks.contains(this.targetMissile)) {
					this.tracks.add(this.targetMissile);
					//log.info("Targetting " + Misc.getDistance(this.weapon.getLocation(), this.targetMissile.getLocation()) + " by " + this.weapon.getId() + " " + this.getMinLaunchDist());
				}				
				return true;
			} else if (this.targetShip != null && this.fireTimer.intervalElapsed()) {
				return true;
			}				
		}
		return false;
	}
	
	public boolean isTargetMissileValid(MissileAPI missile) {
		if (missile == null) {
			return false;
		}
		if (this.ship.getOwner() == missile.getOwner()) {
			return false;
		}
		if (missile.getCollisionClass() == CollisionClass.NONE) {
			return false;
		}
		if (Misc.getDistance(this.weapon.getLocation(), missile.getLocation()) <= this.getMinLaunchDist()) {			
			return false;
		}
		if (!this.isMissileAlive(missile)) {
			return false;
		}
		if (this.isInterceptMissile(missile)) {
			return false;
		}
		if (this.engine.getCustomData().containsKey(missile.toString())) {
			for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData().get(missile.toString())) {
				if (m.getWeaponSpec().getWeaponId() == this.weapon.getId()) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean isInterceptMissile(MissileAPI missile) {
		if (missile.getMissileAI() instanceof GuidedMissileAI) {
			GuidedMissileAI missileAI = (GuidedMissileAI) missile.getMissileAI();
			if (missileAI.getTarget() instanceof MissileAPI || missileAI.getTarget()== null) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	public boolean isMissileAlive(MissileAPI missile) {
		if (!this.engine.isEntityInPlay(missile) || missile.isFading() || missile.isFizzling()) {
			return false;
		}
		else {
			return true;
		}
	}
	
	public void checkIsAutoFire() {		
		if (this.weapon.isDisabled()) {
			return;
		}
		if (this.weapon.usesAmmo() ) {
			if (this.weapon.getAmmo() == 0) {
				return;
			}
		}
		//if (this.ship != this.engine.getPlayerShip()) {
		//	return;
		//}
		WeaponAPI currentWeapon = (WeaponAPI) this.engine.getCustomData().get(this.ship.toString() + this.weapon.getId());
		if (this.ship.getWeaponGroupFor(currentWeapon) != this.ship.getSelectedGroupAPI() && !currentWeapon.isDisabled()) {
			if (currentWeapon.usesAmmo()) {
				if (this.weapon.getAmmo() > 0) {
					return;
				}
			}
			return;
		}		
		this.engine.getCustomData().put(this.ship.toString() + this.weapon.getId(), this.weapon);
		return;
	}
	
	public void updateTracks() {		
		if (!this.tracks.isEmpty()) {
			List<MissileAPI> newTracks = new ArrayList<MissileAPI>();
			for (MissileAPI m : this.tracks) {
				if (this.isMissileAlive(m)) {
					newTracks.add(m);			
				} else {
					//log.info("Track " + m + " is history!");
				}
				if (!this.isMissileAlive(m) && this.engine.getCustomData().containsKey(m.toString())) {
					this.engine.getCustomData().remove(m.toString());						
				}
			}
			this.tracks.clear();
			this.tracks.addAll(newTracks);
			for (MissileAPI m : this.tracks) {
				if (this.engine.getCustomData().containsKey(m.toString())) {					
					List<MissileAPI> birds = new ArrayList<MissileAPI>();
					for (MissileAPI b : (List<MissileAPI>) this.engine.getCustomData().get(m.toString())) {
						if (this.isMissileAlive(b)) {
							if (b.getMissileAI() instanceof GuidedMissileAI) {
								GuidedMissileAI missileAI = (GuidedMissileAI) b.getMissileAI();
								if (m == (MissileAPI) missileAI.getTarget()) {
									birds.add(b);
								}
							}																				
						}
					}
					this.engine.getCustomData().put(m.toString(), birds);
				}
			}
		}
		return;
	}
	
	public float getMinLaunchDist() {
		if (this.weapon.getId().equals("mn_sm3")) {
			return 1300f;
		}
		if (this.weapon.getId().equals("mn_sm6")) {
			return 600f;
		}
		if (this.weapon.getId().equals("mn_essm")) {
			return 100f;
		}
		return 0f;
	}

}
