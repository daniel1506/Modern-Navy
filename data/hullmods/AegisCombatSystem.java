package data.hullmods;

import java.lang.String;
import java.lang.Math;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.ai.AegisCombatSystemAI;

import org.lazywizard.lazylib.combat.CombatUtils;

//import org.apache.log4j.Logger;

public class AegisCombatSystem extends BaseHullMod {

	//public Logger log = Logger.getLogger(this.getClass());
	
	private CombatEngineAPI engine;				
	private static List<String> missileList = new ArrayList<String>();

	static {
		missileList.add("mn_sm6");
		missileList.add("mn_sm3");
		missileList.add("mn_essm");
		missileList.add("mn_rim7");
	}		
	
	public void applyEffectsAfterShipCreation(ShipAPI ship, java.lang.String id) {
		List<WeaponGroupAPI> weaponGroup = ship.getWeaponGroupsCopy();
		for (WeaponGroupAPI g : weaponGroup) {
			for (WeaponAPI w : g.getWeaponsCopy()) {
				if (missileList.contains(w.getId()) && g.isUsingDefaultAI(w)) {
					ship.getWeaponGroupFor(w).getAIPlugins().set(g.getAIPlugins().indexOf(g.getAutofirePlugin(w)), new AegisCombatSystemAI(w,ship));
				}
			}
		}		
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {			
		return null;
	}

	//public boolean isApplicableToShip(ShipAPI ship) {
	//	return ship != null && (ship.getShield() != null || (ship.getPhaseCloak() != null && deflectorShieldList.contains(ship.getPhaseCloak().getId())));
	//}
	
	//public String getUnapplicableReason(ShipAPI ship) {
	//	return "Ship has no shield";
	//}
}
