package data.scripts.ai;

import java.lang.Math;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
//import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.combat.CombatEngineAPI;
//import com.fs.starfarer.api.combat.CollisionClass;
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
//import org.magiclib.util.MagicRender;
//import org.magiclib.util.MagicTargeting;

//import org.apache.log4j.Logger;

public class CruiseMissileAI implements MissileAIPlugin, GuidedMissileAI
{
    //public Logger log = Logger.getLogger(this.getClass());
	
	private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private Vector2f lead;
    private final float MAX_SPEED;
    //private final float DAMPING = 0.05f;
    //private final Color EXPLOSION_COLOR;
    //private final Color PARTICLE_COLOR;
    //private final int NUM_PARTICLES = 20;
	private ShipAPI ship;
    
    public CruiseMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        this.lead = new Vector2f();
        //this.EXPLOSION_COLOR = new Color(255, 0, 0, 255);
        //this.PARTICLE_COLOR = new Color(240, 200, 50, 255);
        this.missile = missile;
        this.MAX_SPEED = missile.getMaxSpeed() * 1.25f;
		this.ship = launchingShip;
    }
    
    public void advance(float amount) {
        if (this.engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        if (this.engine.isPaused() || !this.isMissileAlive(this.missile)) {
            return;
        }
		if (ship == null) {
			return;
		}
		
		
		if (ship == this.engine.getPlayerShip()) {
			//this.lead = AIUtils.getBestInterceptPoint(this.missile.getLocation(), this.MAX_SPEED, this.ship.getMouseTarget(), new Vector2f(0f, 0f));
			this.lead = this.ship.getMouseTarget();
		} else if (this.ship.getShipTarget() != null) {
			this.setTarget(this.ship.getShipTarget());
			this.lead = this.target.getLocation();
		} else return;
        
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
	
	public boolean isMissileAlive(MissileAPI missile) {
		if (!this.engine.isEntityInPlay(missile) || missile.isFading()) {
			return false;
		}
		else {
			return true;
		}
	}
	
	private void selfDestruct(final MissileAPI missile, final CombatEngineAPI engine) {
        engine.applyDamage((CombatEntityAPI)missile, missile.getLocation(), missile.getHitpoints() * 2.0f, DamageType.FRAGMENTATION, 0.0f, false, false, (Object)missile);
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