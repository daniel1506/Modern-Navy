package data.scripts.combat;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
//import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

//import org.apache.log4j.Logger;

public class MNAESAListener implements AdvanceableListener {

    // public Logger log = Logger.getLogger(this.getClass());

    public static final String prefix = "AESA";
    private CombatEngineAPI engine;
    private ShipAPI ship;

    public static List<String> aamMissileList = new ArrayList<String>();

    static {
        aamMissileList.add("mn_aim120");
        aamMissileList.add("mn_aim174b");
    }

    public MNAESAListener(final ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount) {
        if (this.engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        this.enableWeapons();
        this.updateTrackList();
    }

    // private boolean isMissileAlive(MissileAPI missile) {
    // return !(!this.engine.isEntityInPlay(missile) || missile.isFading() ||
    // missile.isFizzling());
    // }

    private void updateTrackList() {
        if (this.engine.getCustomData().containsKey(prefix + this.ship.getOwner() + "fighterTrackList")) {
            List<ShipAPI> removeList = new ArrayList<ShipAPI>();
            for (ShipAPI f : (List<ShipAPI>) this.engine.getCustomData()
                    .get(prefix + this.ship.getOwner() + "fighterTrackList")) {
                if (!f.isAlive()) {
                    removeList.add(f);
                }
            }
            ((List<ShipAPI>) this.engine.getCustomData().get(prefix + this.ship.getOwner() + "fighterTrackList"))
                    .removeAll(removeList);
        }
    }

    private void enableWeapons() {
        if (this.ship != this.engine.getPlayerShip() || this.engine.getCombatUI().isAutopilotOn()) {
            for (WeaponGroupAPI g : this.ship.getWeaponGroupsCopy()) {
                if (!g.isAutofiring()) {
                    for (WeaponAPI w : g.getWeaponsCopy()) {
                        if (aamMissileList.contains(w.getId())) {
                            g.toggleOn();
                            break;
                        }
                    }
                }
            }
        }
    }

}
