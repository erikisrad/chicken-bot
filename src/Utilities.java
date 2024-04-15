import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Character;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.GroundItem;
import java.awt.Point;

import java.util.Objects;

public class Utilities {

    public void mouse_jiggle(int diff){
        int half_diff = diff/2;
        int x = Mouse.getX();
        int y = Mouse.getY();

        int new_x = Calculations.random(x-half_diff, x+half_diff);
        int new_y = Calculations.random(y-half_diff, y+half_diff);

        Mouse.move(new Point(new_x, new_y));
    }

    public void inventory_open(){
        if(!Inventory.isOpen()){
            Inventory.open();
        }
    }

    //returns true is some dingus is in our area
    public boolean others_in_area(Area area){
        Player dingus = Players.closest(p ->
                !p.equals(Players.getLocal())
                && area.contains(p.getTile()));

        if(dingus != null){
            Logger.debug("some dingus named " + dingus.getName()
                + " is in our area");
            return true;
        }else{
            Logger.debug("area clear of other players");
            return false;
        }

    }

    //returns true is some dingus is in our area and not moving
    public boolean others_static_in_area(Area area){
        Player dingus = Players.closest(p ->
                !p.equals(Players.getLocal())
                && !p.isMoving()
                && area.contains(p.getTile()));

        if(dingus != null){
            Logger.debug("some dingus named " + dingus.getName()
                    + " is in our area");
            return true;
        }else{
            Logger.debug("area clear of other players");
            return false;
        }

    }

    //finds target in area and does action on it
    //returns true if player starts animating
    public boolean find_and_gather(String target, String action, Area area){
        GameObject resource = GameObjects.closest(r ->
                r.getName().equals(target)
                && area.contains(r.getTile())
                && r.canReach()
                && r.hasAction(action));

        if(resource != null){
            if(Calculations.random(0,2) == 1) {
                mouse_jiggle(5);
            }
            resource.interact(action);

            if(Sleep.sleepUntil(() -> Players.getLocal().isAnimating(), 4000)){
                Logger.debug(target + " is being " + action);
                return true;

            }else{
                Logger.warn("failed to " + action + " " + target);
                return false;
            }
        }else{
            Logger.debug("no valid " + target + " to " + action);
            return false;
        }
    }

    //deposits items, assumes you are near a bank
    //argument can be 'all', 'equipment' or any item name
    //empties inventory of that item
    public boolean bank_deposit_all(String item){
        if (item.equalsIgnoreCase("all")) {
            if(Bank.depositAllItems()){
                Logger.info("deposited all items");
                return true;
            }else{
                Logger.warn("failed to deposit all items");
                return false;
            }

        }else if(item.equalsIgnoreCase("equipment")) {
            if(Bank.depositAllEquipment()){
                Logger.info("deposited all equipment");
                return true;
            }else{
                Logger.warn("failed to deposit all equipment");
                return false;
            }

        }else{
            if(Bank.depositAll(item)){
                Logger.info("deposited all " + item);
                return true;

            }else{
                Logger.warn("failed to deposit all " + item);
                return false;
            }
        }
    }

    //opens bank menu using a bank booth in given area
    public boolean open_bank(Area area){
        GameObject bankBooth = GameObjects.closest(g ->
                g.getName().equals("Bank booth")
                && area.contains(g.getTile())
                && g.canReach()
                && g.hasAction("Bank"));

        if(bankBooth != null) {
            bankBooth.interact("Bank");

            if(Sleep.sleepUntil(Bank::isOpen, 4000)){
                Logger.info("bank open");
                return true;

            }else{
                Logger.warn("found bank but failed to open");
                return false;
            }

        }else {
            Logger.warn("can't find bank booth");
            return false;
        }
    }

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

    //makes sure we are currently in combat with an expected enemy
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

    //picks up item_name if in given radius & area
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
