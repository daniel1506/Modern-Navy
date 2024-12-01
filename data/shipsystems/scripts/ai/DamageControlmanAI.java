package data.shipsystems.scripts.ai;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
//import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.util.IntervalUtil;

//import org.apache.log4j.Logger;

public class DamageControlmanAI implements ShipSystemAIScript {

	private ShipAPI ship;
	// private CombatEngineAPI engine;
	// private ShipwideAIFlags flags;
	// private ShipSystemAPI system;
	// private ShipSystemAPI shield;

	// public Logger log = Logger.getLogger(this.getClass());

	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		// this.flags = flags;
		// this.engine = engine;
		// this.system = system;
		// this.shield = ship.getPhaseCloak();
	}

	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (ship.getHullLevel() < 0.9f) {
			ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, ship.getLocation(), 0);
		}
	}

}