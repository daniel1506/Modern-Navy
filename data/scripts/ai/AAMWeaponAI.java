package data.scripts.ai;

import java.lang.String;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.magiclib.util.MagicTargeting;

import data.scripts.combat.MNAESAListener;

import org.apache.log4j.Logger;

public class AAMWeaponAI implements AutofireAIPlugin {

	public Logger log = Logger.getLogger(this.getClass());

	public static final String prefix = "AESA";

	private CombatEngineAPI engine;
	private WeaponAPI weapon;
	private String weaponId;
	private String weaponUid;
	private ShipAPI ship;
	private String shipUid;

	private MissileAPI targetMissile;
	private ShipAPI targetShip;
	private boolean isForceOff = false;

	public List<MissileAPI> missileTrackList = new ArrayList<MissileAPI>();
	public List<ShipAPI> fighterTrackList = new ArrayList<ShipAPI>();

	public AAMWeaponAI(final WeaponAPI weapon, final ShipAPI ship) {
		this.weapon = weapon;
		this.weaponUid = weapon.toString();
		this.weaponId = weapon.getId();

		this.ship = ship;
		this.shipUid = ship.toString();

		if (!ship.hasListenerOfClass(MNAESAListener.class)) {
			MNAESAListener aesa = new MNAESAListener(ship);
			ship.addListener(aesa);
		}
	}

	public void advance(float amount) {
		if (this.engine != Global.getCombatEngine()) {
			this.engine = Global.getCombatEngine();
			// this.initSystem();
			return;
		}
		if (this.engine.isPaused() || this.weapon == null) {
			return;
		}
		this.initSystem();

		// Update tracks of this weapon, check the function for more detail
		this.updateTracks();

		if (this.targetMissile != null) {
			if (!this.isTargetMissileValid(this.targetMissile, true)) {
				this.targetMissile = null;
			}
		}

		if (this.targetShip != null) {
			if (!this.isTargetShipValid(targetShip, false)) {
				this.targetShip = null;
			}
		}

		if (this.engine.getCustomData().containsKey(prefix + this.shipUid + this.weaponId)) {
			if (!this.ship.getAllWeapons()
					.contains((WeaponAPI) this.engine.getCustomData().get(prefix + this.shipUid + this.weaponId))) {
				this.refreshWeaponList();
			}
			if (this.weapon == (WeaponAPI) this.engine.getCustomData().get(prefix + this.shipUid + this.weaponId)) {
				if (this.isWeaponAvailable(this.weapon)) {
					if (this.targetMissile == null && this.targetShip == null) {
						this.searchTarget();
					}
				} else {
					this.updateActiveWeapon();
				}
			}
		}
	}

	public void forceOff() {
		this.isForceOff = true;
		return;
	}

	public Vector2f getTarget() {
		if (this.targetMissile != null) {
			return this.engine.getAimPointWithLeadForAutofire((CombatEntityAPI) this.ship, 1f,
					(CombatEntityAPI) this.targetMissile, this.weapon.getProjectileSpeed());
		} else if (this.targetShip != null) {
			return this.engine.getAimPointWithLeadForAutofire((CombatEntityAPI) this.ship, 1f,
					(CombatEntityAPI) this.targetShip, this.weapon.getProjectileSpeed());
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

	// Fire or not
	public boolean shouldFire() {
		if (this.isForceOff) {
			if (this.targetMissile != null || this.targetShip != null) {
				this.isForceOff = false;
			}
			return false;
		}
		if (!this.isWeaponAvailable(this.weapon)) {
			return false;
		}
		if (this.weapon == this.engine.getCustomData().get(prefix + this.shipUid + this.weaponId)) {
			if (this.targetShip != null && this.targetShip.isAlive()
					&& !this.fighterTrackList.contains(this.targetShip)) {
				((List<ShipAPI>) this.engine.getCustomData().get(prefix + this.ship.getOwner() + "fighterTrackList"))
						.add(this.targetShip);
				this.fighterTrackList.add(this.targetShip);
				// this.engine.getCustomData().put(prefix + this.weaponUid + "fighter",
				// this.fighterTrackList);
				// log.info("Kill Track " + this.targetShip.toString() + " with " +
				// this.weapon.toString());
				return true;
			}
			if (this.targetMissile != null && this.isTargetMissileValid(this.targetMissile, true)) {
				((List<MissileAPI>) this.engine.getCustomData().get(prefix + this.ship.getOwner() + "missileTrackList"))
						.add(this.targetMissile);
				this.missileTrackList.add(this.targetMissile);
				// this.engine.getCustomData().put(prefix + this.weaponUid + "missile",
				// this.missileTrackList);
				return true;
			}
		}
		return false;
	}

	private void searchTarget() {
		// Select Fighter
		// if (this.weapon.hasAIHint(AIHints.ANTI_FTR)) {
		// for (ShipAPI s : CombatUtils.getShipsWithinRange(this.weapon.getLocation(),
		// this.weapon.getRange() * 0.9f)) {
		// if (WeaponUtils.isWithinArc((CombatEntityAPI) s, this.weapon)) {
		// if (isTargetShipValid(s, true) && !this.fighterTrackList.contains(s)) {
		// this.targetShip = s;
		// return;
		// }
		// }
		// }
		// }
		if (this.weapon.hasAIHint(AIHints.ANTI_FTR)) {
			ShipAPI tempTargetShip = MagicTargeting.pickShipTarget(this.ship,
					MagicTargeting.targetSeeking.IGNORE_SOURCE,
					Integer.valueOf((int) (this.weapon.getRange() * 0.8f)), Integer.valueOf((int) this.weapon.getArc()),
					1, 0, 0, 0, 0);
			if (isTargetShipValid(tempTargetShip, true) && !this.fighterTrackList.contains(tempTargetShip)) {
				this.targetShip = tempTargetShip;
				log.info("ValidTarget: " + tempTargetShip.getId());
				return;
			}
		}
		// Select Missiles
		if (!this.weapon.getId().equals("mn_aim174b")) {
			return;
		}
		for (MissileAPI m : CombatUtils.getMissilesWithinRange(this.ship.getLocation(),
				this.weapon.getRange() * 1.2f)) {
			if (this.isTargetMissileValid(m, true) && !this.missileTrackList.contains(m)) {
				if (this.weapon.hasAIHint(AIHints.GUIDED_POOR) && this.weapon.getSlot().isTurret()) {
					if (!WeaponUtils.isWithinArc((CombatEntityAPI) m, this.weapon)) {
						for (WeaponAPI w : (List<WeaponAPI>) this.engine.getCustomData()
								.get(prefix + this.shipUid + this.weaponId + "list")) {
							if (this.isWeaponAvailable(w)) {
								if (WeaponUtils.isWithinArc((CombatEntityAPI) m, w) || !w.getSlot().isTurret()) {
									this.engine.getCustomData().put(prefix + this.shipUid + this.weaponId, w);
									return;
								}
							}
						}
					}
				} else {
					this.targetMissile = m;
					return;
				}
			}
		}
	}

	private void updateTracks() {
		if (!this.missileTrackList.isEmpty()) {
			List<MissileAPI> removeList = new ArrayList<MissileAPI>();
			for (MissileAPI m : this.missileTrackList) {
				if (!this.isMissileAlive(m)) {
					if (this.engine.getCustomData().containsKey(prefix + m.toString())) {
						this.engine.getCustomData().remove(prefix + m.toString());
					}
					removeList.add(m);
				}
				if (!this.isTargetMissileValid(m, false)) {
					removeList.add(m);
				}
			}
			this.missileTrackList.removeAll(removeList);
		}
		if (!this.fighterTrackList.isEmpty()) {
			List<ShipAPI> removeList = new ArrayList<ShipAPI>();
			for (ShipAPI f : this.fighterTrackList) {
				if (!f.isAlive()) {
					if (this.engine.getCustomData().containsKey(prefix + f.toString())) {
						this.engine.getCustomData().remove(prefix + f.toString());
					}
					removeList.add(f);
				}
			}
			this.fighterTrackList.removeAll(removeList);
		}
	}

	private void updateActiveWeapon() {
		List<WeaponAPI> weapons = (List<WeaponAPI>) this.engine.getCustomData()
				.get(prefix + this.shipUid + this.weaponId + "list");
		int index = weapons.indexOf(this.weapon) + 1;
		while (true) {
			if (index >= weapons.size()) {
				index = 0;
			} else if (this.isWeaponAvailable((WeaponAPI) weapons.get(index))) {
				this.engine.getCustomData().put(prefix + this.shipUid + this.weaponId, weapons.get(index));
				break;
			} else if (index == weapons.indexOf(this.weapon)) {
				break;
			} else
				index++;
		}
		this.engine.getCustomData().put(prefix + this.shipUid + this.weaponId, weapons.get(index));
	}

	private void refreshWeaponList() {
		List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
		for (WeaponAPI w : this.ship.getAllWeapons()) {
			if (w.getId().equals(this.weaponId)) {
				weapons.add(w);
			}
		}
		if (weapons.indexOf(this.weapon) != 0) {
			return;
		}
		this.engine.getCustomData().put(prefix + this.shipUid + this.weaponId + "list", weapons);
		this.engine.getCustomData().put(prefix + this.shipUid + this.weaponId, this.weapon);
	}

	private boolean isWeaponAvailable(WeaponAPI weapon) {
		if (weapon == null)
			return false;
		if (weapon.isDisabled() || weapon.isFiring() || weapon.getCooldownRemaining() > 0f) {
			return false;
		}
		if (this.ship == this.engine.getPlayerShip() && !this.engine.getCombatUI().isAutopilotOn()
				&& this.ship.getWeaponGroupFor(weapon) == this.ship.getSelectedGroupAPI()) {
			return false;
		}
		if (weapon.usesAmmo()) {
			if (weapon.getAmmo() == 0) {
				return false;
			}
		}
		return true;
	}

	private boolean isInterceptMissile(MissileAPI missile) {
		if (missile.getMissileAI() instanceof GuidedMissileAI) {
			GuidedMissileAI missileAI = (GuidedMissileAI) missile.getMissileAI();
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

	private boolean isMissileAlive(MissileAPI missile) {
		return !(!this.engine.isEntityInPlay(missile) || missile.isFading() || missile.isFizzling());
	}

	private boolean isTargetMissileValid(MissileAPI missile, boolean targeting) {
		if (missile == null) {
			return false;
		}
		if (this.ship.getOwner() == missile.getOwner() || missile.isFlare()) {
			return false;
		}
		if (missile.getCollisionClass() == CollisionClass.NONE) {
			return false;
		}
		if (!this.isMissileAlive(missile) || this.isInterceptMissile(missile)) {
			return false;
		}
		if (targeting) {
			// If not targeting this ship and faced > 60 degree away from this ship, skip
			float angle = Math.abs(Misc.getAngleDiff(missile.getFacing(),
					Misc.getAngleInDegrees(missile.getLocation(), this.ship.getLocation())));
			if (angle > this.weapon.getArc()) {
				return false;
			}
			//
			if (((List<MissileAPI>) this.engine.getCustomData().get(prefix + this.ship.getOwner() + "missileTrackList"))
					.contains(missile)) {
				return false;
			}
			float distance = Misc.getDistance(this.weapon.getLocation(), missile.getLocation());
			if (distance > this.weapon.getRange() * 1.2f) {
				return false;
			}
		}
		if (this.engine.getCustomData().containsKey(prefix + missile.toString())) {
			for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData().get(prefix + missile.toString())) {
				if (this.isMissileAlive(m)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isTargetShipValid(ShipAPI target, boolean targeting) {
		if (target == null) {
			return false;
		}
		if (!target.isFighter() || target.getOwner() == this.ship.getOwner()) {
			return false;
		}
		if (!target.isAlive() || target.getCollisionClass() == CollisionClass.NONE) {
			return false;
		}
		if (targeting) {
			// float distance = Misc.getDistance(this.ship.getLocation(),
			// target.getLocation());
			// if (distance > this.weapon.getRange()) {
			// return false;
			// }
			// Get estimate number of missile to take out the target
			// int maxMissiles = (int) Math.ceil(target.getHitpoints() /
			// this.weapon.getDamage().getDamage());
			// if (target.getShield() != null) {
			// maxMissiles++;
			// }
			int frequency = Collections.frequency(
					(List<ShipAPI>) this.engine.getCustomData()
							.get(prefix + this.ship.getOwner() + "fighterTrackList"),
					target);
			if (frequency >= 2) {
				return false;
			}
			// if (((List<ShipAPI>) this.engine.getCustomData().get(prefix +
			// this.ship.getOwner() + "fighterTrackList"))
			// .contains(target)) {
			// if (!this.engine.getCustomData().containsKey(prefix + target.toString())) {
			// return false;
			// }
			// }
		}
		if (this.engine.getCustomData().containsKey(prefix + target.toString())) {
			this.updateTargetStatus(target);
			if (((List<MissileAPI>) this.engine.getCustomData().get(prefix + target.toString())).size() > 2) {
				return false;
			}
			// float damageAmount = 0f;
			// for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData().get(prefix
			// + target.toString())) {
			// if (this.isMissileAlive(m)) {
			// damageAmount = damageAmount + m.getDamageAmount();
			// }
			// }
			// float targetHP = target.getShield() != null ? target.getHitpoints() +
			// target.getMaxFlux()
			// : target.getHitpoints();
			// if (damageAmount > targetHP) {
			// return false;
			// }
		}
		return true;
	}

	private void updateTargetStatus(ShipAPI target) {
		if (this.engine.getCustomData().containsKey(prefix + target.toString())) {
			List<MissileAPI> removeList = new ArrayList<MissileAPI>();
			for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData().get(prefix + target.toString())) {
				if (!this.isMissileAlive(m)) {
					removeList.add(m);
				}
			}
			((List<MissileAPI>) this.engine.getCustomData().get(prefix + target.toString())).removeAll(removeList);
		}
	}

	private void initSystem() {

		// Create a custom data for this weapon, the data is to store the tracks this
		// weapon is engaging, if not created already
		// Class<AegisCombatSystemController> c = AegisCombatSystemController.class;
		if (!this.engine.getCustomData().containsKey(prefix + this.ship.getOwner() + "fighterTrackList")) {
			List<ShipAPI> AESAfighterTrackList = new ArrayList<ShipAPI>();
			this.engine.getCustomData().put(prefix + this.ship.getOwner() + "fighterTrackList", AESAfighterTrackList);
		}

		if (!this.engine.getCustomData().containsKey(prefix + this.shipUid + this.weaponId)) {
			this.engine.getCustomData().put(prefix + this.shipUid + this.weaponId, this.weapon);
		}

		if (!this.engine.getCustomData().containsKey(prefix + this.shipUid + this.weaponId + "list")) {
			List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
			for (WeaponAPI w : this.ship.getAllWeapons()) {
				if (w.getId().equals(this.weaponId)) {
					weapons.add(w);
				}
			}
			this.engine.getCustomData().put(prefix + this.shipUid + this.weaponId + "list", weapons);
		}

		if (!this.engine.getCustomData().containsKey(prefix + this.weaponUid + "fighter")) {
			this.engine.getCustomData().put(prefix + this.weaponUid + "fighter", this.fighterTrackList);
		}

		if (this.weapon.getId().equals("mn_aim174b")) {
			if (!this.engine.getCustomData().containsKey(prefix + this.ship.getOwner() + "missileTrackList")) {
				List<MissileAPI> missileTrackList = new ArrayList<MissileAPI>();
				this.engine.getCustomData().put(prefix + this.ship.getOwner() + "missileTrackList", missileTrackList);
			}
			if (!this.engine.getCustomData().containsKey(prefix + this.weaponUid + "missile")) {
				this.engine.getCustomData().put(prefix + this.weaponUid + "missile", this.missileTrackList);
			}
		}
	}

}
