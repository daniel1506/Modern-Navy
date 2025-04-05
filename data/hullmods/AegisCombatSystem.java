package data.hullmods;

import java.lang.String;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.combat.BaseHullMod;

public class AegisCombatSystem extends BaseHullMod {

	public static float EW_PENALTY_MULT = 0.5f;
	public static float EW_PENALTY_REDUCTION = 5f;

	public static float ECCM_CHANCE = 0.5f;
	public static float GUIDANCE_IMPROVEMENT = 1f;

	public static float SMOD_ECCM_CHANCE = 1f;
	public static float SMOD_EW = 0f;

	public static float DAMAGE_BONUS = 50f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		// boolean sMod = isSMod(stats);
		stats.getEccmChance().modifyFlat(id, ECCM_CHANCE);
		stats.getMissileGuidance().modifyFlat(id, GUIDANCE_IMPROVEMENT);

		stats.getDynamic().getMod(Stats.PD_IGNORES_FLARES).modifyFlat(id, 1f);
		stats.getDynamic().getMod(Stats.PD_BEST_TARGET_LEADING).modifyFlat(id, 1f);
		stats.getDamageToFighters().modifyPercent(id, DAMAGE_BONUS);

		// if (sMod) {
		// stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD).modifyMult(id,
		// SMOD_EW);
		// } else {
		// stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD).modifyMult(id,
		// EW_PENALTY_MULT);
		// }
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0)
			return "" + (int) (ECCM_CHANCE * 100f) + "%";
		if (index == 1)
			return "" + (int) Math.round(DAMAGE_BONUS) + "%";
		if (index == 2)
			return "" + (int) ((1f - EW_PENALTY_MULT) * 100f) + "%";
		return null;
	}

}
