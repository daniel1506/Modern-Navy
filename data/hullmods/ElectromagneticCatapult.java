package data.hullmods;

import java.util.ArrayList;
import java.util.List;
//import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.FighterWingAPI;

//import org.apache.log4j.Logger;

public class ElectromagneticCatapult extends BaseHullMod {

	//public Logger log = Logger.getLogger(this.getClass());
	
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, java.lang.String id)  {
		stats.getFighterRefitTimeMult().modifyMult(id, 0.85f) ;
	}
		
	public void advanceInCombat(ShipAPI ship, float amount) {
		
		for (FighterWingAPI w : ship.getAllWings()) {
			for (ShipAPI s : w.getWingMembers()) {
				if (s.isLiftingOff()) {
					if (!s.getTravelDrive().isOn()) {
						s.turnOnTravelDrive(1.5f);
					}
					s.setFacing(ship.getFacing());
				}
			}
		}
			
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {			
		if (index == 0) return "" + (int) 15 + "%";
		return null;
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && ship.hasLaunchBays() && ship.getHullSize() != HullSize.FRIGATE && ship.getHullSize() != HullSize.DESTROYER;
	}
	
	public String getUnapplicableReason(ShipAPI ship) {
		if (!ship.hasLaunchBays()) {
			return "Ship has no launch bay!";
		}
		else if (ship.getHullSize() == HullSize.FRIGATE || ship.getHullSize() == HullSize.DESTROYER) {
			return "Can not be installed on frigates or destroyers!";
		}
		return "Error";
	}
}
