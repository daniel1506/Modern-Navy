package data.scripts.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;

import java.util.Iterator;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

public class MNVLSOnFireEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {
	public static final Vector2f SPRITE_SIZE = new Vector2f(12.0F, 12.0F);
	public static final Vector2f OFFSET_LARGE = new Vector2f(0.5F, 0.5F);
	//public static final Vector2f OFFSET_SMALL_1 = new Vector2f(-0.5F, -1.5F);
	public static final Vector2f OFFSET_SMALL_1 = new Vector2f(0.5F, -0.5F);
	public static final Vector2f OFFSET_SMALL_2 = new Vector2f(-0.5F, 0.5F);
	public static final int[] FRAMES_LARGE = new int[] { 1, 2, 3, 4, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 4, 3, 2, 1 };
	public static final int[] FRAMES_SMALL = new int[] { 1, 2, 3, 4, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 4, 3, 2, 1 };
	public static final float FRAMES_PER_SECOND = 15.0F;
	public static final float DELAY_BETWEEN_TUBES = 0.1F;
	private float animTimer = 0.0F;
	private int barrelNumber = 0;
	private int barrelSize = 0;
	private int burstSize = 0;
	private float burstTime = 0f;

	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine != null && weapon != null) {
			if (weapon.getSlot().isHidden()) {
				return;
			}
			if (weapon.hasAIHint(AIHints.GUIDED_POOR) && weapon.getSlot().isTurret()) {
				return;
			}
			if (this.barrelSize == 0) {
				this.barrelSize = weapon.getSpec().getTurretFireOffsets().size();
				this.burstSize = weapon.getSpec().getBurstSize();
				if (weapon.getSpec().getBurstSize() == 1) {
					this.burstTime = 0.5f;
				} else {
					if (weapon.getSpec() instanceof ProjectileWeaponSpecAPI) {
						float burstDelay = ((ProjectileWeaponSpecAPI) weapon.getSpec()).getBurstDelay();
						this.burstTime = weapon.getSpec().getBurstSize() * burstDelay;
					}
				}
			}
			if (engine.isPaused()) {
				amount = 0.0F;
			}

			float missileRofMult = weapon.getShip().getMutableStats().getMissileRoFMult().getModifiedValue();
			float chargeLevel = weapon.getChargeLevel();

			if (weapon.isFiring() || this.animTimer > 0.0F) {
				this.animTimer += amount * missileRofMult;
			}

			if (this.animTimer > 0.0F) {
				Vector2f offset = OFFSET_LARGE;
				// int[] frames = FRAMES_LARGE;
				int frameSixs = (int) Math.floor((weapon.getSpec().getChargeTime() + this.burstTime) * 15f);
				int[] frames = new int[frameSixs + 10];
				for (int i = 0; i < 5; i++) {
					frames[i] = i + 1;
					frames[frameSixs + 9 - i] = i + 1;
				}
				for (int j = 5; j < 5 + frameSixs; j++) {
					frames[j] = 6;
				}
				if (weapon.getSize().equals(WeaponSize.SMALL)) {
					// frames = FRAMES_SMALL;
					if (weapon.getId().contains("left")) {
						offset = OFFSET_SMALL_2;
					} else {
						offset = OFFSET_SMALL_1;
					}
				}
				for (int i = 0; i < weapon.getSpec().getBurstSize(); ++i) {
					int currentBarrel = (this.barrelNumber + i) % this.barrelSize;
					// int frame = (int) ((this.animTimer - 0.1F / missileRofMult * (float) i) *
					// 15.0F);
					int frame = (int) ((this.animTimer
							- ((ProjectileWeaponSpecAPI) weapon.getSpec()).getBurstDelay() / missileRofMult * (float) i)
							* 15.0F);
					if (frame < frames.length && frame >= 0) {
						Vector2f barrelLoc = new Vector2f(
								(ReadableVector2f) weapon.getSpec().getTurretFireOffsets().get(currentBarrel));
						Vector2f.add(barrelLoc, offset, barrelLoc);
						// Vector2f renderPoint = VectorUtils.rotate(barrelLoc, weapon.getCurrAngle());
						Vector2f renderPoint = VectorUtils.rotate(barrelLoc,
								weapon.getSlot().computeMidArcAngle(weapon.getShip()));
						Vector2f.add(renderPoint, weapon.getLocation(), renderPoint);
						SpriteAPI hatchFrame = Global.getSettings().getSprite("mn_vls", "vls_frame" + frames[frame]);
						// MagicRender.singleframe(hatchFrame, renderPoint, SPRITE_SIZE,
						// weapon.getCurrAngle() - 90.0F, hatchFrame.getColor(), false);
						MagicRender.singleframe(hatchFrame, renderPoint, SPRITE_SIZE,
								weapon.getSlot().computeMidArcAngle(weapon.getShip()) - 90.0F, hatchFrame.getColor(),
								false);
					}
				}
			}

			// float cutoff = weapon.getSize().equals(WeaponSize.SMALL) ? 3.0F : 4.0F;
			// float cutoff = weapon.getRefireDelay() * 0.9f;
			float cutoff = weapon.getSpec().getChargeTime() + this.burstTime + weapon.getCooldown();
			if (chargeLevel == 0.0F && this.animTimer >= cutoff * 0.9f / missileRofMult) {
				this.animTimer = 0.0F;
				this.barrelNumber = (this.barrelNumber + this.burstSize) % this.barrelSize;
			}
		}
	}

	public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
		if (!weapon.hasAIHint(AIHints.PD) && !weapon.hasAIHint(AIHints.PD_ONLY)) {
			GuidedMissileAI ai = (GuidedMissileAI) ((MissileAPI) proj).getUnwrappedMissileAI();
			if (ai.getTarget() != null) {
				this.rotateMissileToTarget(proj, ai.getTarget());
			}
		}
	}

	private void rotateMissileToTarget(DamagingProjectileAPI proj, CombatEntityAPI target) {
		float randFacing = VectorUtils.getAngle(proj.getLocation(), target.getLocation())
				+ MathUtils.getRandomNumberInRange(-60.0F, 60.0F);
		VectorUtils.rotate(proj.getVelocity(), MathUtils.getShortestRotation(proj.getFacing(), randFacing));
		proj.setFacing(randFacing);
		((GuidedMissileAI) ((MissileAPI) proj).getUnwrappedMissileAI()).setTarget(target);
	}

}
