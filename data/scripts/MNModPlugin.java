package data.scripts;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
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
import data.scripts.ai.RadarInterceptMissileAI;

public class MNModPlugin extends BaseModPlugin {

	public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		/*
		 * switch (missile.getProjectileSpecId()) {
		 * case "mn_sm3" :
		 * return new PluginPick<MissileAIPlugin>(new RadarInterceptMissileAI(missile,
		 * launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
		 * case "mn_sm6" :
		 * return new PluginPick<MissileAIPlugin>(new RadarInterceptMissileAI(missile,
		 * launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
		 * case "mn_essm":
		 * return new PluginPick<MissileAIPlugin>(new RadarInterceptMissileAI(missile,
		 * launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
		 * default:
		 * return null;
		 * }
		 */
		if (missile.getProjectileSpecId().equals("mn_sm3") || missile.getProjectileSpecId().equals("mn_sm6")
				|| missile.getProjectileSpecId().equals("mn_essm")) {
			return new PluginPick<MissileAIPlugin>(new RadarInterceptMissileAI(missile, launchingShip),
					CampaignPlugin.PickPriority.MOD_SPECIFIC);
		} else if (missile.getProjectileSpecId().equals("mn_rim7")) {
			return new PluginPick<MissileAIPlugin>(new RadarInterceptMissileAI(missile, launchingShip),
					CampaignPlugin.PickPriority.MOD_SPECIFIC);
		} else if (missile.getProjectileSpecId().equals("mn_aim120") || missile.getProjectileSpecId().equals("mn_aim174b")) {
			return new PluginPick<MissileAIPlugin>(new AAMMissileAI(missile, launchingShip),
					CampaignPlugin.PickPriority.MOD_SPECIFIC);
		} else {
			return null;
		}
	}

	public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
		if (weapon.getId().equals("mn_aim120") || weapon.getId().equals("mn_aim174b")) {
			return new PluginPick<AutofireAIPlugin>(new AAMWeaponAI(weapon, weapon.getShip()),
					CampaignPlugin.PickPriority.MOD_SPECIFIC);
		} else {
			return null;
		}
	}

}