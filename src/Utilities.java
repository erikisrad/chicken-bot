import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Character;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;

import java.util.Objects;

public class Utilities {

    //run to selected area, sprinting when above min_stamina
    //will return true if at desired area within timeout, false if timeout elapses
    //if timeout < 0, no timeout is used
    public boolean run_to_location(int min_stamina, Area location, int timeout_ms){

        long run_start = System.currentTimeMillis();
        long current_time;
        long last_move = 0;
        int move_interval;

        while(!location.contains(Players.getLocal().getTile())) {

            current_time = System.currentTimeMillis();
            move_interval = check_stamina_and_run(min_stamina);

            //if method has run longer than timeout
            if (timeout_ms > 0 && (current_time - run_start) > timeout_ms){
                Logger.warn("run method timed out");
                return false;

                //else if has been longer than move interval or player isn't moving
            }else if ((current_time - last_move) > (move_interval + Calculations.random(100, 300))
                    || !Players.getLocal().isMoving()) {

                last_move = current_time;
                Walking.walk(location.getRandomTile());
                Logger.debug("clicked to move");

            } else {
                Logger.debug("too early to move");
                Sleep.sleep(50);
            }
        }

        //double check, probably unneeded
        if(location.contains(Players.getLocal().getTile())){
            Logger.info("arrived at location");
            return true;
        }else{
            Logger.warn("not in desired location");
            return false;
        }

    }

    //attacks specified creep in specified area, returns true if engaged
    public boolean attack_creep_in_area(String creep_name, Area area){

        NPC creep = NPCs.closest(c -> creep_name.equals(c.getName())
                && !c.isInCombat()
                && c.canReach()
                && area.contains(c.getTile()));

        if (creep != null && creep.hasAction("Attack")) {

            Logger.info("starting attack on " + creep_name);
            creep.interact("Attack");

            if (Sleep.sleepUntil(() -> (Players.getLocal().isInCombat()), 4000)){
                Logger.info("fighting " + creep_name);
                return true;

            }else{
                Logger.warn("timed out before fighting " + creep_name);
                return false;
            }

        }else{
            Logger.warn("failed to find " + creep_name + "in area");
            return false;
        }
    }

    public Target check_target(String creep_name){
        Character aggressor = Players.getLocal().getCharacterInteractingWithMe();

        if(aggressor != null && Players.getLocal().isInCombat() && aggressor.isInCombat()) {
            String aggressor_name = aggressor.getName();

            if (Objects.equals(aggressor_name, creep_name)) {
                Logger.debug("in combat with " + creep_name);

                if (aggressor.canReach()){
                    return Target.DESIRED;

                }else{
                    Logger.warn(creep_name + " is unreachable");
                    return Target.UNREACHABLE;
                }

            } else {
                Logger.warn("in combat with an undesired " + aggressor_name);
                return Target.UNDESIRED;
            }

        }else{
            Logger.warn("can't define aggressor");
            return Target.NO_TARGET;
        }
    }

    public boolean collect_item_if_near(String item_name, int radius, Area area){

        GroundItem near_item = GroundItems.closest(
                item -> item.isOnScreen()
                        && item.getName().equals(item_name)
                        && item.distance(Players.getLocal().getTile()) <= radius
                        && item.canReach()
                        && area.contains(item.getTile()));

        if (near_item != null && near_item.hasAction("Take")) {
            near_item.interact("Take");
            if(Sleep.sleepUntil(() -> (!near_item.exists()), 4000)){
                Logger.info("grabbed " + item_name);
            }else{
                Logger.warn("couldn't grab " + item_name);
            }

            return true;

        }else{
            Logger.info("didn't find any " + item_name + " within " + radius + " tiles");
            return false;
        }
    }

    //turns sprint on if its off and youre above stamina minimum
    //returns a good click interval for movement methods based on if were sprinting
    private int check_stamina_and_run(int min_stamina){
        boolean is_sprinting = Walking.isRunEnabled();

        if ((Walking.getRunEnergy() > min_stamina) && !is_sprinting) {
            is_sprinting = Walking.toggleRun();
        }

        if(is_sprinting){
            return 2000;
        }else{
            return 3000;
        }
    }
}
