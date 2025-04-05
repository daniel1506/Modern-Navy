package data.scripts.combat;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import org.apache.log4j.Logger;

public class MNAegisListener implements AdvanceableListener {

    public Logger log = Logger.getLogger(this.getClass());

    public static final String prefix = "acs";
    public static List<String> aegisMissileList = new ArrayList<String>();

    static {
        aegisMissileList.add("mn_sm6");
        aegisMissileList.add("mn_sm3");
        aegisMissileList.add("mn_essm");
        aegisMissileList.add("mn_essm_small");
        aegisMissileList.add("mn_rim7");
    }

    private CombatEngineAPI engine;
    private ShipAPI ship;

    public MNAegisListener(ShipAPI ship) {
        this.ship = ship;
        this.engine = Global.getCombatEngine();
    }

    public void advance(float amount) {
        if (this.engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        if (this.ship == null || !this.ship.isAlive() || this.engine.isPaused()) {
            return;
        }
        this.enableWeapons();
        this.updateTrackList();
    }

    private void updateTrackList() {
        if (!this.engine.getCustomData().containsKey(prefix + this.ship.getOwner() + "missileTrackList")) {
            return;
        }
        if (this.engine.getCustomData().get(prefix + this.ship.getOwner() + "missileTrackList") == null) {
            return;
        }
        List<MissileAPI> removeList = new ArrayList<MissileAPI>();
        for (MissileAPI m : (List<MissileAPI>) this.engine.getCustomData()
                .get(prefix + this.ship.getOwner() + "missileTrackList")) {
            if (!isMissileAlive(m)) {
                removeList.add(m);
            }
        }
        ((List<MissileAPI>) this.engine.getCustomData().get(prefix + this.ship.getOwner() + "missileTrackList"))
                .removeAll(removeList);
    }

    private void enableWeapons() {
        if (this.ship != this.engine.getPlayerShip() || this.engine.getCombatUI().isAutopilotOn()) {
            for (WeaponGroupAPI g : this.ship.getWeaponGroupsCopy()) {
                if (!g.isAutofiring()) {
                    for (WeaponAPI w : g.getWeaponsCopy()) {
                        if (aegisMissileList.contains(w.getId()) || w.getId().equals("mn_c_dome")) {
                            g.toggleOn();
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean isMissileAlive(MissileAPI missile) {
        return !(!this.engine.isEntityInPlay(missile) || missile.isFading() || missile.isFizzling());
    }

}
