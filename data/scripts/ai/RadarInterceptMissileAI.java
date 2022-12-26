package data.scripts.ai;

import java.lang.Math;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import data.scripts.util.MagicRender;
import data.scripts.util.MagicTargeting;

import org.apache.log4j.Logger;

public class RadarInterceptMissileAI implements MissileAIPlugin, GuidedMissileAI
{
    //public Logger log = Logger.getLogger(this.getClass());
	
	private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private Vector2f lead;
    private final float MAX_SPEED;
    private final float DAMPING = 0.05f;
    private final Color EXPLOSION_COLOR;
    private final Color PARTICLE_COLOR;
    private final int NUM_PARTICLES = 20;
    
    public RadarInterceptMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        this.lead = new Vector2f();
        this.EXPLOSION_COLOR = new Color(255, 0, 0, 255);
        this.PARTICLE_COLOR = new Color(240, 200, 50, 255);
        this.missile = missile;
        this.MAX_SPEED = missile.getMaxSpeed() * 1.25f;
    }
    
    public void advance(float amount) {
        if (this.engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        if (Global.getCombatEngine().isPaused() || this.missile.isFading()) {
            return;
        }
		if (this.missile.isFizzling()) {
			this.selfDestruct(this.missile, this.engine);
			return;
		}
		if (this.target == null) {
			this.missile.giveCommand(ShipCommand.ACCELERATE);
			if (this.engine.getCustomData().containsKey(this.missile.getWeapon().toString())) {
				findTargetLoop:
				for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData().get(this.missile.getWeapon().toString())) {					
					if (!this.engine.getCustomData().containsKey(m.toString()) && this.isMissileAlive(m)) {
						List<MissileAPI> birds = new ArrayList<MissileAPI>();
						this.setTarget((CombatEntityAPI) m);
						this.missile.setFacing(Misc.getAngleInDegrees(this.missile.getLocation(), this.target.getLocation()));
						//log.info("Kill Track1 " + m + " with " + this.missile);					
						birds.add(this.missile);
						this.engine.getCustomData().put(m.toString(), birds);
						this.nextLauncher();
						return;
					} else if (this.engine.getCustomData().containsKey(m.toString()) && this.isMissileAlive(m)) {
						for (MissileAPI b : (List<MissileAPI>) this.engine.getCustomData().get(m.toString())) {
							if (b.getWeaponSpec().getWeaponId() == m.getWeaponSpec().getWeaponId()) {
								//log.info("Filter " + m + " (" + this.missile + ")");
								continue findTargetLoop;
							}
						}
						List<MissileAPI> birds = new ArrayList<MissileAPI>();
						birds = (List<MissileAPI>) this.engine.getCustomData().get(m.toString());					
						this.setTarget((CombatEntityAPI) m);
						this.missile.setFacing(Misc.getAngleInDegrees(this.missile.getLocation(), this.target.getLocation()));
						//log.info("Kill Track2 " + m + " with " + this.missile);
						birds.add(this.missile);	
						this.nextLauncher();
						return;
					}
				}
				//log.info("No Missile Target Found by " + this.missile.getWeapon());
			}		
			if (this.target == null) {
				if (this.missile.getWeaponSpec().getAIHints().contains(AIHints.PD_ONLY)) {
					if (this.missile.getWeaponSpec().getAIHints().contains(AIHints.ANTI_FTR)) {						
						this.setTarget((CombatEntityAPI)MagicTargeting.pickMissileTarget(this.missile, MagicTargeting.targetSeeking.FULL_RANDOM , Integer.valueOf((int)(this.missile.getWeapon().getRange() * 1.5f * (this.missile.getMaxFlightTime() - this.missile.getFlightTime()) / this.missile.getMaxFlightTime())), Integer.valueOf(90), Integer.valueOf(100), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0)));						
						this.nextLauncher();
						if (this.target != null) {
							this.missile.setFacing(Misc.getAngleInDegrees(this.missile.getLocation(), this.target.getLocation()));
							//log.info("Kill Fighter " + this.target + " with " + this.missile);
							return;
						}
					}
					this.selfDestruct(this.missile, this.engine);
					return;
				}
				this.setTarget((CombatEntityAPI)MagicTargeting.pickMissileTarget(this.missile, MagicTargeting.targetSeeking.FULL_RANDOM , Integer.valueOf((int)(this.missile.getWeapon().getRange() * 1.5f * (this.missile.getMaxFlightTime() - this.missile.getFlightTime()) / this.missile.getMaxFlightTime())), Integer.valueOf(90), Integer.valueOf(25), Integer.valueOf(25), Integer.valueOf(25), Integer.valueOf(25), Integer.valueOf(25)));				
				//this.missile.setFacing(Misc.getAngleInDegrees(this.missile.getLocation(), m.getLocation()));
				//log.info("Kill Ship " + this.target + " with " + this.missile);
				this.nextLauncher();
				return;
			}
		}
		if (this.target != null && this.target instanceof MissileAPI) {
			if (!this.isMissileAlive((MissileAPI) this.target)) {
				//log.info("Lost Track " + this.target + " (" + this.missile + ")");
				this.selfDestruct(this.missile, this.engine);
				return;
			}
		}
        float dist = MathUtils.getDistanceSquared(this.missile.getLocation(), this.target.getLocation());
        if (dist < Math.pow(this.missile.getSpec().getExplosionRadius(), 2) && this.target instanceof MissileAPI && this.missile.getDamageType() != DamageType.KINETIC) {
            this.proximityFuse();
            return;
        }
        this.lead = AIUtils.getBestInterceptPoint(this.missile.getLocation(), this.MAX_SPEED, this.target.getLocation(), this.target.getVelocity());
        if (this.lead == null) {
            this.lead = this.target.getLocation();
        }
        float correctAngle = VectorUtils.getAngle(this.missile.getLocation(), this.lead);
        float correction = MathUtils.getShortestRotation(VectorUtils.getFacing(this.missile.getVelocity()), correctAngle);
        if (correction > 0.0f) {
            correction = -11.25f * ((float)Math.pow(FastTrig.cos((double)(3.1415927f * correction / 90.0f)) + 1.0, 2.0) - 4.0f);
        }
        else {
            correction = 11.25f * ((float)Math.pow(FastTrig.cos((double)(3.1415927f * correction / 90.0f)) + 1.0, 2.0) - 4.0f);
        }
        correctAngle += correction;
        float aimAngle = MathUtils.getShortestRotation(this.missile.getFacing(), correctAngle);
        if (aimAngle < 0.0f) {
            this.missile.giveCommand(ShipCommand.TURN_RIGHT);
        }
        else {
            this.missile.giveCommand(ShipCommand.TURN_LEFT);
        }
        if (Math.abs(aimAngle) < 45.0f) {
            this.missile.giveCommand(ShipCommand.ACCELERATE);
        }
        if (Math.abs(aimAngle) < Math.abs(this.missile.getAngularVelocity()) * 0.05f) {
            this.missile.setAngularVelocity(aimAngle / 0.05f);
        }
    }
	
	public void nextLauncher() {
		if (this.missile.getSource() == this.engine.getPlayerShip()) {
			if (this.missile.getSource().getWeaponGroupFor(this.missile.getWeapon()) == this.missile.getSource().getSelectedGroupAPI()) {
				return;
			}
		}
		if (this.missile.getWeapon() == (WeaponAPI) Global.getCombatEngine().getCustomData().get(this.missile.getSource().toString() + this.missile.getWeaponSpec().getWeaponId())) {	
			List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
			weapons = (List<WeaponAPI>) Global.getCombatEngine().getCustomData().get(this.missile.getSource().toString() + this.missile.getWeaponSpec().getWeaponId() + "list");
			int index = weapons.indexOf(this.missile.getWeapon()) + 1;
			while (true) {
				if (index >= weapons.size()) {
					index = 0;
				}
				if (this.missile.getSource().getWeaponGroupFor((WeaponAPI) weapons.get(index)).isAutofiring() && this.missile.getSource().getWeaponGroupFor((WeaponAPI) weapons.get(index)) != this.engine.getPlayerShip().getSelectedGroupAPI()) {
					break;
				}
				if (index == weapons.indexOf(this.missile.getWeapon())) {
					return;
				}				
				index++;
			}			
			//log.info("Next Launcher " + weapons.get(index) + " isAuto " + this.missile.getSource().getWeaponGroupFor(weapons.get(index)).isAutofiring());
			Global.getCombatEngine().getCustomData().put(this.missile.getSource().toString() + this.missile.getWeaponSpec().getWeaponId(), weapons.get(index));
		}
		return;
	}
	
	public boolean isMissileAlive(MissileAPI missile) {
		if (!this.engine.isEntityInPlay(missile) || missile.isFading() || missile.getCollisionClass() == CollisionClass.NONE) {
			return false;
		}
		else {
			return true;
		}
	}
	
	private void selfDestruct(final MissileAPI missile, final CombatEngineAPI engine) {
        engine.applyDamage((CombatEntityAPI)missile, missile.getLocation(), missile.getHitpoints() * 2.0f, DamageType.FRAGMENTATION, 0.0f, false, false, (Object)missile);
    }
    
    void proximityFuse() {
        this.engine.applyDamage(this.target, this.target.getLocation(), this.missile.getDamageAmount(), DamageType.FRAGMENTATION, 0.0f, false, false, (Object)this.missile.getSource());
        DamagingExplosionSpec boom = new DamagingExplosionSpec(0.1f, this.missile.getSpec().getExplosionRadius() * 1.5f, this.missile.getSpec().getExplosionRadius(), this.missile.getDamageAmount(), 50.0f, CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_FIGHTER, 2.0f, 5.0f, 5.0f, 25, new Color(225, 100, 0), new Color(200, 100, 25));
        boom.setDamageType(DamageType.FRAGMENTATION);
        boom.setShowGraphic(false);
        boom.setSoundSetId("explosion_flak");
        this.engine.spawnDamagingExplosion(boom, this.missile.getSource(), this.missile.getLocation());
        if (MagicRender.screenCheck(0.1f, this.missile.getLocation())) {
            this.engine.addHitParticle(this.missile.getLocation(), new Vector2f(), 100.0f, 1.0f, 0.25f, this.EXPLOSION_COLOR);
            for (int i = 0; i < 20; ++i) {
                final float axis = (float)Math.random() * 360.0f;
                final float range = (float)Math.random() * 100.0f;
                this.engine.addHitParticle(MathUtils.getPointOnCircumference(this.missile.getLocation(), range / 5.0f, axis), MathUtils.getPointOnCircumference(new Vector2f(), range, axis), 2.0f + (float)Math.random() * 2.0f, 1.0f, 1.0f + (float)Math.random(), this.PARTICLE_COLOR);
            }
            this.engine.applyDamage((CombatEntityAPI)this.missile, this.missile.getLocation(), this.missile.getHitpoints() * 2.0f, DamageType.FRAGMENTATION, 0.0f, false, false, (Object)this.missile);
        }
        else {
            this.engine.removeEntity((CombatEntityAPI)this.missile);
        }
    }
    
    public CombatEntityAPI getTarget() {
        return this.target;
    }
    
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }
    
    public void init(CombatEngineAPI engine) {
    }
}