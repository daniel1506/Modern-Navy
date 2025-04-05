package data.scripts;

import java.util.ArrayList;
import java.util.List;

//import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.MissileAPI;

import data.scripts.ai.AAMMissileAI;
import data.scripts.ai.AAMWeaponAI;
import data.scripts.ai.AegisCombatSystemAI;
import data.scripts.ai.AntiShellSystemAI;
import data.scripts.ai.RadarInterceptMissileAI;

public class MNModPlugin extends BaseModPlugin {

	public static List<String> aamMissileList = new ArrayList<String>();
	public static List<String> aegisMissileList = new ArrayList<String>();

	static {
		aamMissileList.add("mn_aim120");
		aamMissileList.add("mn_aim174b");
		aegisMissileList.add("mn_sm6");
		aegisMissileList.add("mn_sm3");
		aegisMissileList.add("mn_essm");
		aegisMissileList.add("mn_essm_small");
		aegisMissileList.add("mn_rim7");
	}

	public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		if (aegisMissileList.contains(missile.getProjectileSpecId())) {
			return new PluginPick<MissileAIPlugin>(new RadarInterceptMissileAI(missile, launchingShip),
					CampaignPlugin.PickPriority.MOD_SPECIFIC);
		} else if (aamMissileList.contains(missile.getProjectileSpecId())) {
			return new PluginPick<MissileAIPlugin>(new AAMMissileAI(missile, launchingShip),
					CampaignPlugin.PickPriority.MOD_SPECIFIC);
		} else {
			return null;
		}
	}

	public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
		if (aegisMissileList.contains(weapon.getId())) {
			return new PluginPick<AutofireAIPlugin>(new AegisCombatSystemAI(weapon, weapon.getShip()),
					CampaignPlugin.PickPriority.MOD_SPECIFIC);
		} else if (aamMissileList.contains(weapon.getId())) {
			return new PluginPick<AutofireAIPlugin>(new AAMWeaponAI(weapon, weapon.getShip()),
					CampaignPlugin.PickPriority.MOD_SPECIFIC);
		} else if (weapon.getId().equals("mn_c_dome")) {
			return new PluginPick<AutofireAIPlugin>(new AntiShellSystemAI(weapon, weapon.getShip()),
					CampaignPlugin.PickPriority.MOD_SPECIFIC);
		} else {
			return null;
		}
	}

}