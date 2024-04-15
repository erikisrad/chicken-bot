import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.dialogues.Dialogues;

@ScriptManifest(name = "chicken-bot", description = "kills them birds", author = "longarm",
        version = 1.0, category = Category.MISC, image = "")

public class Chickens extends AbstractScript {

    ZenAntiBan z;
    Utilities u;
    private final String creep_name = "Chicken";
    private State state;
    private Target target_type;

    Area pen_area_small = new Area(3171, 3301, 3184, 3291, 0);
    Area pen_area_large = new Area(3169, 3307, 3185, 3288, 0);


    public void retreat(){

        Area safety = new Area(3208, 3249, 3214,3244, 0);

        if(u.run_to_location(1, safety, -1)){
            Logger.info("successfully retreated");
        }else{
            //if we hit this something is really fucked
            Logger.error("failed to retreat!");
        }
    }

    @Override
    public void onStart() {

        Logger.setDisplayDebugMessages(false);
        z = new ZenAntiBan(this);
        u= new Utilities();
        Logger.log("booting script...");
        if(!Walking.isRunEnabled()){
            Walking.toggleRun();
        }

    }

    @Override
    public int onLoop(){

        switch (getState()) {

            case CLOSING_DIALOGUE:
                Sleep.sleep(200, 400);
                Dialogues.spaceToContinue();
                break;

            case RETREATING:
                retreat();
                break;

            case IN_COMBAT:
                switch(u.check_target(creep_name)){
                    case DESIRED:

                    case NO_TARGET:
                        break;

                    case UNDESIRED:
                        Logger.warn("wtf is attacking us?");
                        retreat();
                        break;

                    case UNREACHABLE:
                        u.run_to_location(33, pen_area_small, 10000);
                        u.attack_creep_in_area(creep_name, pen_area_large);
                        break;
                }
                break;

            case WALKING_TO_PEN:

                if(u.run_to_location(33, pen_area_small, 10000)){
                    Logger.info("inside pen");
                }else{
                    Logger.warn("still not in pen");
                }
                break;

            case COLLECTING_AND_TARGETING:
                Sleep.sleep(z.antiBan());
                if (!u.collect_item_if_near("Feather", 4, pen_area_large)){
                    if(u.attack_creep_in_area(creep_name, pen_area_large)){
                        Sleep.sleep(50, 120);
                    }
                }
                break;

            case UNKNOWN:
                break;
        }


        return 100;
    }

    private State getState(){

        if(Dialogues.canContinue() || Dialogues.inDialogue()){
            Logger.log("closing dialogue");
            state = State.CLOSING_DIALOGUE;

        }else if(Players.getLocal().getHealthPercent() < 33){
            Logger.log("retreating");
            state = State.RETREATING;

        }else if (Players.getLocal().isInCombat() && (Players.getLocal().getCharacterInteractingWithMe() != null)){
            if(state != State.IN_COMBAT) {
                Logger.log("in combat");
            }
            state = State.IN_COMBAT;

        }else if(!pen_area_large.contains(Players.getLocal().getTile())){
            Logger.log("walking to pen");
            state = State.WALKING_TO_PEN;

        }else if (!Players.getLocal().isInCombat() && !Players.getLocal().isMoving()){
            Logger.log("collecting & targeting");
            state = State.COLLECTING_AND_TARGETING;

        }else{
            if(state != State.UNKNOWN) {
                Logger.info("waiting...");
            }
            state = State.UNKNOWN;
        }

        return state;
    }
}