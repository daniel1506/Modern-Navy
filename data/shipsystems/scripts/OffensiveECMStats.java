package data.shipsystems.scripts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import org.lazywizard.lazylib.combat.CombatUtils;

//import org.apache.log4j.Logger;

public class OffensiveECMStats extends BaseShipSystemScript {

	// public Logger log = Logger.getLogger(this.getClass());

	public static final String JAMMING = "mn_jamming";
	public static final int MAX_JAM = 4;
	public static final float ACCURACY_DEBUFF = -8f;
	public static final float MISSILE_DAMAGE_DEBUFF = -75f;
	public static final float FIGHTER_DAMAGE_DEBUFF = -50f;
	public static final Color TEXT_COLOR = new Color(0, 105, 255, 150);

	private List<ShipAPI> jamList = new ArrayList<ShipAPI>();
	private List<ShipAPI> fighterJamList = new ArrayList<ShipAPI>();

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else
			return;
		if (effectLevel == 1) {
			for (ShipAPI s : CombatUtils.getShipsWithinRange(ship.getLocation(), 1000f)) {
				if (s.isAlive() && s.getOwner() != ship.getWing().getSourceShip().getOwner()
						&& !s.hasTag(JAMMING)) {
					if (s.getHullSize() != HullSize.FIGHTER && this.jamList.size() < MAX_JAM) {
						s.getMutableStats().getAutofireAimAccuracy().modifyFlat(JAMMING,
								ACCURACY_DEBUFF);
						s.getMutableStats().getDamageToMissiles().modifyPercent(JAMMING,
								MISSILE_DAMAGE_DEBUFF);
						s.getMutableStats().getDamageToFighters().modifyPercent(JAMMING,
								FIGHTER_DAMAGE_DEBUFF);
						s.addTag(JAMMING);
						this.jamList.add(s);
						s.getFluxTracker().showOverloadFloatyIfNeeded("Jammed!", TEXT_COLOR, 4f, true);
						// log.info("Owner: " + ship.isAlive());
					} else if (s.getHullSize() == HullSize.FIGHTER) {
						s.getMutableStats().getAutofireAimAccuracy().modifyFlat(JAMMING, ACCURACY_DEBUFF);
						s.getMutableStats().getDamageToMissiles().modifyPercent(JAMMING,
								MISSILE_DAMAGE_DEBUFF);
						s.getMutableStats().getDamageToFighters().modifyPercent(JAMMING,
								FIGHTER_DAMAGE_DEBUFF);
						s.addTag(JAMMING);
						this.fighterJamList.add(s);
						// log.info("Owner: " + ship.isAlive());
						s.getFluxTracker().showOverloadFloatyIfNeeded("Jammed!", TEXT_COLOR, 4f, true);
					}
				}
			}
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		if (!this.jamList.isEmpty() || !this.fighterJamList.isEmpty()) {
			for (ShipAPI s : this.jamList) {
				s.getMutableStats().getAutofireAimAccuracy().unmodifyFlat(JAMMING);
				s.getMutableStats().getDamageToMissiles().unmodifyPercent(JAMMING);
				s.getMutableStats().getDamageToFighters().unmodifyPercent(JAMMING);
				s.getTags().remove(JAMMING);
			}
			for (ShipAPI s : this.fighterJamList) {
				s.getMutableStats().getAutofireAimAccuracy().unmodifyFlat(JAMMING);
				s.getMutableStats().getDamageToMissiles().unmodifyPercent(JAMMING);
				s.getMutableStats().getDamageToFighters().unmodifyPercent(JAMMING);
				s.getTags().remove(JAMMING);
			}
			this.jamList.clear();
			this.fighterJamList.clear();
			// log.info("Unjammed: ");
		}
	}

	// public StatusData getStatusData(final int index, final State state, final
	// float effectLevel) {
	// if (index == 0) {
	// return new StatusData("DC teams repairing ship", false);
	// }
	// return null;
	// }

}