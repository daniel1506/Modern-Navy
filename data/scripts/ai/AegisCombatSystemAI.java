package data.scripts.ai;

import java.lang.String;
import java.util.ArrayList;
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

import data.scripts.combat.MNAegisListener;
//import org.apache.log4j.Logger;

public class AegisCombatSystemAI implements AutofireAIPlugin {

	// public Logger log = Logger.getLogger(this.getClass());

	public static final String prefix = "acs";

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

	public AegisCombatSystemAI(final WeaponAPI weapon, final ShipAPI ship) {
		this.weapon = weapon;
		this.weaponUid = weapon.toString();
		this.weaponId = weapon.getId();

		this.ship = ship;
		this.shipUid = ship.toString();

		if (!ship.hasListenerOfClass(MNAegisListener.class)) {
			MNAegisListener aegisListener = new MNAegisListener(ship);
			ship.addListener(aegisListener);
		}
	}

	public void advance(float amount) {
		if (this.engine != Global.getCombatEngine()) {
			this.initSystem();
			return;
		}

		if (this.engine.isPaused() || this.weapon == null) {
			return;
		}

		// Update tracks of this weapon, check the function for more detail
		this.updateTracks();

		if (this.targetMissile != null) {
			if (!this.isTargetMissileValid(this.targetMissile, true)) {
				this.targetMissile = null;
			}
		}

		if (this.targetShip != null) {
			if (!this.isTargetShipValid(targetShip)) {
				this.targetShip = null;
			}
		}

		if (this.engine.getCustomData().containsKey(prefix + this.shipUid + this.weaponId)) {
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
			this.isForceOff = false;
			return false;
		}
		if (!this.isWeaponAvailable(this.weapon)) {
			return false;
		}
		if (this.weapon == this.engine.getCustomData().get(prefix + this.shipUid + this.weaponId)) {
			if (this.targetMissile != null && this.isTargetMissileValid(this.targetMissile, true)) {
				((List<MissileAPI>) this.engine.getCustomData().get(prefix + this.ship.getOwner() + "missileTrackList"))
						.add(this.targetMissile);
				this.missileTrackList.add(this.targetMissile);
				// this.engine.getCustomData().put(prefix + this.weaponUid + "missile",
				// this.missileTrackList);
				return true;
			}
			if (this.targetShip != null && !this.fighterTrackList.contains(this.targetShip)) {
				this.fighterTrackList.add(this.targetShip);
				// this.engine.getCustomData().put(prefix + this.weaponUid + "fighter",
				// this.fighterTrackList);
				return true;
			}
		}
		return false;
	}

	private void searchTarget() {
		// Select Missiles
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
		// Select Fighter
		if (this.weapon.hasAIHint(AIHints.ANTI_FTR)) {
			for (ShipAPI s : CombatUtils.getShipsWithinRange(this.ship.getLocation(), this.weapon.getRange())) {
				if (isTargetShipValid(s) && !this.fighterTrackList.contains(s)) {
					this.targetShip = s;
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
			// ((List<MissileAPI>) this.engine.getCustomData().get(prefix + this.weaponUid +
			// "missile"))
			// .removeAll(removeList);
			// this.engine.getCustomData().put(prefix + this.weaponUid + "missile",
			// this.missileTrackList);
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
			if (missile.getMissileAI() instanceof GuidedMissileAI) {
				GuidedMissileAI missileAI = (GuidedMissileAI) missile.getMissileAI();
				if (missileAI.getTarget() != null && missileAI.getTarget() instanceof ShipAPI) {
					ShipAPI target = (ShipAPI) missileAI.getTarget();
					if (target != this.ship) {
						float angle = Math.abs(Misc.getAngleDiff(missile.getFacing(),
								Misc.getAngleInDegrees(missile.getLocation(), this.ship.getLocation())));
						if (angle > 90f) {
							return false;
						}
					}
					float distA = Misc.getDistance(missile.getLocation(), this.ship.getLocation());
					float distB = Misc.getDistance(missile.getLocation(), target.getLocation());
					if (distA > 1250f && distB < 750f) {
						return false;
					}
				}
			}
			//
			if (!missile.isGuided() && !missile.isMine()) {
				float angle = Math.abs(Misc.getAngleDiff(missile.getFacing(),
						Misc.getAngleInDegrees(missile.getLocation(), this.ship.getLocation())));
				if (angle > 90f) {
					return false;
				}
			}
			if (((List<MissileAPI>) this.engine.getCustomData().get(prefix + this.ship.getOwner() + "missileTrackList"))
					.contains(missile)) {
				return false;
			}
			float distance = Misc.getDistance(this.weapon.getLocation(), missile.getLocation());
			if (distance > this.weapon.getRange() * 1.2f || distance < this.getMinLaunchDist()) {
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

	private boolean isTargetShipValid(ShipAPI target) {
		if (target == null) {
			return false;
		}
		if (!target.isFighter() || target.getOwner() == this.ship.getOwner()) {
			return false;
		}
		if (!target.isAlive() || target.getCollisionClass() == CollisionClass.NONE) {
			return false;
		}
		float distance = Misc.getDistance(this.ship.getLocation(), target.getLocation());
		if (distance > this.weapon.getRange() || distance < this.getMinLaunchDist()) {
			return false;
		}
		return true;
	}

	private void initSystem() {
		this.engine = Global.getCombatEngine();

		// Create a custom data for this weapon, the data is to store the tracks this
		// weapon is engaging, if not created already

		if (!this.engine.getCustomData().containsKey(prefix + this.ship.getOwner() + "missileTrackList")) {
			List<MissileAPI> missileTrackList = new ArrayList<MissileAPI>();
			this.engine.getCustomData().put(prefix + this.ship.getOwner() + "missileTrackList", missileTrackList);
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

		if (!this.engine.getCustomData().containsKey(prefix + this.weaponUid + "missile")) {
			this.engine.getCustomData().put(prefix + this.weaponUid + "missile", this.missileTrackList);
		}
		if (this.weapon.hasAIHint(AIHints.ANTI_FTR)) {
			if (!this.engine.getCustomData().containsKey(prefix + this.weaponUid + "fighter")) {
				this.engine.getCustomData().put(prefix + this.weaponUid + "fighter", this.fighterTrackList);
			}
		}
	}

	// Get the minimum launch distance for this weapon, used for layered area
	// defense
	// E.g. SM3 engage track at at least 1300 distance away, SM6 600, ESSM 150, if
	// no SM6 onboard, SM3 engage track at 600 distance, ESSM 150, etc
	private float getMinLaunchDist() {
		if (this.weaponId.equals("mn_sm3")) {
			if (!this.engine.getCustomData().containsKey(prefix + this.shipUid + "mn_sm6")) {
				return 650f;
			}
			return 2000f;
		}
		if (this.weaponId.equals("mn_sm6")) {
			if (!this.engine.getCustomData().containsKey(prefix + this.shipUid + "mn_essm")
					&& !this.engine.getCustomData().containsKey(prefix + this.shipUid + "mn_essm_small")) {
				return 150f;
			}
			return 650f;
		}
		if (this.weaponId.equals("mn_essm") || this.weaponId.equals("mn_essm_small")) {
			return 150f;
		}
		if (this.weaponId.equals("mn_rim7")) {
			return 100f;
		}
		return 0f;
	}

}
