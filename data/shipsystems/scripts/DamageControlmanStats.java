package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

//import org.apache.log4j.Logger;

public class DamageControlmanStats extends BaseShipSystemScript {

	// public Logger log = Logger.getLogger(this.getClass());
	public static final String DCDATAKEY = "mn_dcRepair";
	public static float REGEN_RATE = 0.005f;
	private boolean listenerAdded;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else
			return;
		if (!listenerAdded) {
			DCRepairListenerScript listenerScript = new DCRepairListenerScript(ship);
			ship.addListener(listenerScript);
			listenerAdded = true;
		}
		if (effectLevel == 1 && !ship.hasTag(DCDATAKEY)) {
			ship.addTag(DCDATAKEY);
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else
			return;
		if (ship.hasTag(DCDATAKEY)) {
			ship.getTags().remove(DCDATAKEY);
		}
	}

	public StatusData getStatusData(final int index, final State state, final float effectLevel) {
		if (index == 0) {
			return new StatusData("DC teams repairing ship", false);
		}
		return null;
	}

	public class DCRepairListenerScript implements AdvanceableListener {
		protected ShipAPI ship;

		public DCRepairListenerScript(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (this.ship.hasTag(DCDATAKEY)) {
				if (ship.getHullLevel() < 1f) {
					float maxHull = ship.getMaxHitpoints();
					float currHull = ship.getHitpoints();
					float repairAmount = maxHull * REGEN_RATE * amount;
					if (repairAmount > maxHull - currHull)
						repairAmount = maxHull - currHull;
					if (repairAmount > 0) {
						ship.setHitpoints(ship.getHitpoints() + repairAmount);
					}
				}
			}
		}

	}

}
