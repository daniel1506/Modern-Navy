package data.hullmods;

import java.lang.String;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import data.scripts.ai.AegisCombatSystemAI;
import data.scripts.ai.CounterBatterySystemAI;

import org.apache.log4j.Logger;

public class AegisCombatSystem extends BaseHullMod {

	public Logger log = Logger.getLogger(this.getClass());

	public static final String prefix = "acs";
	public static List<String> aegisMissileList = new ArrayList<String>();

	static {
		aegisMissileList.add("mn_sm6");
		aegisMissileList.add("mn_sm3");
		aegisMissileList.add("mn_essm");
		aegisMissileList.add("mn_essm_small");
		aegisMissileList.add("mn_rim7");
	}

	private CombatEngineAPI engine;
	private ShipAPI ship;
	private List<String> weaponList = new ArrayList<String>();

	public void applyEffectsAfterShipCreation(ShipAPI ship, java.lang.String id) {
		for (WeaponGroupAPI g : ship.getWeaponGroupsCopy()) {
			for (WeaponAPI w : g.getWeaponsCopy()) {
				if (aegisMissileList.contains(w.getId()) && g.isUsingDefaultAI(w)) {
					if (!this.weaponList.contains(w.getId())) {
						this.weaponList.add(w.getId());
					}
					g.getAIPlugins().set(g.getAIPlugins().indexOf(g.getAutofirePlugin(w)),
							new AegisCombatSystemAI(w, ship));
				} else if (w.getId().equals("mn_c_dome") && g.isUsingDefaultAI(w)) {
					g.getAIPlugins().set(g.getAIPlugins().indexOf(g.getAutofirePlugin(w)),
							new CounterBatterySystemAI(w, ship));
				}
			}
		}
	}

	public void advanceInCombat(ShipAPI ship, float amount) {
		if (weaponList.isEmpty()) {
			return;
		}
		if (this.engine != Global.getCombatEngine()) {
			this.initSystem(ship);
		}
		this.enableWeapons();
		this.updateTrackList();
	}

	private boolean isMissileAlive(MissileAPI missile) {
		return !(!this.engine.isEntityInPlay(missile) || missile.isFading() || missile.isFizzling());
	}

	private void updateTrackList() {
		if (!this.engine.getCustomData().containsKey(prefix + this.ship.getOwner() + "missileTrackList")) {
			return;
		}
		if (this.engine.getCustomData().get(prefix + this.ship.getOwner() + "missileTrackList") == null) {
			return;
		}
		List<MissileAPI> removeList = new ArrayList<MissileAPI>();
		for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData()
				.get(prefix + this.ship.getOwner() + "missileTrackList")) {
			if (!isMissileAlive(m)) {
				removeList.add(m);
			}
		}
		((List<MissileAPI>) this.engine.getCustomData().get(prefix + this.ship.getOwner() + "missileTrackList"))
				.removeAll(removeList);
	}

	private void enableWeapons() {
		if (this.ship != this.engine.getPlayerShip() || this.engine.getCombatUI().isAutopilotOn()) {
			for (WeaponGroupAPI g : this.ship.getWeaponGroupsCopy()) {
				if (!g.isAutofiring()) {
					for (WeaponAPI w : g.getWeaponsCopy()) {
						if (aegisMissileList.contains(w.getId()) || w.getId().equals("mn_c_dome")) {
							g.toggleOn();
							break;
						}
					}
				}
			}
		}
	}

	private void initSystem(ShipAPI ship) {
		this.engine = Global.getCombatEngine();
		this.ship = ship;
		// this.shipUid = ship.toString();

		// if (!this.engine.getCustomData().containsKey(prefix + this.ship.getOwner() +
		// "missileTrackList")) {
		// List<MissileAPI> missileTrackList = new ArrayList<MissileAPI>();
		// this.engine.getCustomData().put(prefix + this.ship.getOwner() +
		// "missileTrackList", missileTrackList);
		// }

		// for (String s : this.weaponList) {
		// if (!this.engine.getCustomData().containsKey(prefix + this.shipUid + s +
		// "list")) {
		// List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
		// for (WeaponAPI w : this.ship.getAllWeapons()) {
		// if (w.getId().equals(s)) {
		// weapons.add(w);
		// }
		// }
		// this.engine.getCustomData().put(prefix + this.shipUid + s + "list", weapons);
		// }
		// }
	}

}
