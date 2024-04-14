import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.input.Keyboard;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.randoms.RandomEvent;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.interactive.Entity;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.widgets.Menu;
import org.dreambot.api.wrappers.widgets.message.Message;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

/**
 * This class holds all of my anti-ban ideas.
 *
 * @author Zenarchist
 */

@SuppressWarnings("MagicConstant")
public class ZenAntiBan {
    public int ANTIBAN_RATE = 50; // This is the frequency rate for anti-ban actions (in % terms - 100% = frequent, 0% = never
    public int MIN_WAIT_NO_ACTION = 50; // This is the minimum time to wait if no action was taken
    public int MAX_WAIT_NO_ACTION = 100; // This is the maximum time to wait if no action was taken
    private AbstractScript s; // Script
    private String STATUS = "Idling"; // Current anti-ban status
    private Skill[] STATS_TO_CHECK = { Skill.HITPOINTS, Skill.STRENGTH, Skill.ATTACK, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.PRAYER }; // This is used for determining which stats to randomly check
    public int MIN_WAIT_BETWEEN_EVENTS = 300; // In seconds
    private long LAST_EVENT = 0L; // Last time an antiban event was triggered
    private long LAST_IDLE; // Last time we idled for a while
    private boolean DO_RANDOM = false; // This is a generic flag for randomly doing something early in a script for anti-patterning
    private int MAX_RUNTIME_MINUTES = -1; // This is the maximum amount of time the script should run for (used for calculating progressive lag multiplier + max duration)
    private long START_TIME = 0L; // Time the script was started
    // Stat widget coordinates (in order of DB API listing for Skill array)
    public final Point[] STAT_WIDGET = {
            new Point(550, 210), // Attack
            new Point(550, 270), // Defence
            new Point(550, 240), // Strength
            new Point(612, 210), // Hits
            new Point(550, 304), // Ranged
            new Point(550, 336), // Prayer
            new Point(350, 370), // Magic
            new Point(367, 304), // Cooking
            new Point(676, 368), // Woodcut
            new Point(613, 369), // Fletching
            new Point(677, 273), // Fishing
            new Point(676, 336), // Firemaking
            new Point(614, 337), // Crafting
            new Point(677, 240), // Smithing
            new Point(677, 209), // Mining
            new Point(613, 271), // Herblore
            new Point(614, 240), // Agility
            new Point(614, 304), // Thieving
            new Point(614, 401), // Slayer
            new Point(676, 400), // Farming
            new Point(550, 400), // Runecrafting
            new Point(613, 432), // Hunter
            new Point(550, 432), // Construction
    };
    private final Point STATS_WIDGET = new Point(577, 186); // Stats menu
    private final Point INVENTORY_WIDGET = new Point(643, 185); // Inventory menu
    private final Point COMBAT_WIDGET = new Point(543, 186); // Combat style menu
    private final Point MAGIC_WIDGET = new Point(742, 186); // Magic menu

    // Constructs a new Anti-Ban class with the given script
    public ZenAntiBan(AbstractScript script) {
        this.s = script;
        // Set last idle to now to avoid idling early into the script
        LAST_IDLE = System.currentTimeMillis();
        START_TIME = System.currentTimeMillis();
    }

    // Returns the wait time for when the antiban system does nothing
    private int doNothing() {
        return rh(MIN_WAIT_NO_ACTION, MAX_WAIT_NO_ACTION);
    }

    // Sets the stats to check during random antiban events
    public void setStatsToCheck(Skill... skills) {
        STATS_TO_CHECK = skills;
    }

    // Returns the sleep time after performing an anti-ban check
    public int antiBan() {
        setStatus("");
        if(ANTIBAN_RATE == 0 || System.currentTimeMillis() - LAST_EVENT <= r(MIN_WAIT_BETWEEN_EVENTS * 1000, MIN_WAIT_BETWEEN_EVENTS * 2000))
            return doNothing();

        // If we have moved the mouse outside of the screen, wait a moment before performing the ban action
        if(Mouse.getX() == -1 && Mouse.getY() == -1)
            s.sleep(1000, 2000);

        // Calculate overall random anti-ban intervention rate (%)
        int rp = r(0, 100);
        if(rp < ANTIBAN_RATE) {
            // Calculate event-specific activation rate (%)
            rp = r(0, 100);
            // Calculate event ID
            int event = r(0, 14);
            // Handle specified event
            switch(event) {

                case 0: { // Examine random entity
                    if(rp < 25) { // 25% chance
                        setStatus("Checking random entity");
                        int r = r(1, 3);
                        Entity e = GameObjects.closest(o -> o != null && !o.getName().equals("null") && r(1, 2) != 1);
                        if (e == null || r == 2) {
                            e = NPCs.closest(n -> n != null && !n.getName().equals("null"));
                            if (e == null || r == 3) {
                                e = GroundItems.closest(i -> i != null && !i.getName().equals("null"));
                                if (e == null)
                                    return doNothing();
                            }
                        }

                        setStatus("Examining entity (" + e.getName() + ")");
                        Mouse.move(e);

                        if(r(0, 100) < 99) { // 99% chance of clicking examine
                            // Open right-click menu and find Examine option
                            rh(1, 100);
                            Menu.open();
                            s.sleep(rh(250, 1000));
                            if (Menu.contains("Examine"))
                                Menu.clickAction("Examine", e);
                            else
                            if(Menu.contains("Cancel"))
                                Menu.clickAction("Cancel");
                        }

                        LAST_EVENT = System.currentTimeMillis();
                        return rh(250, 3000);
                    }
                }
                case 1: { // Check random stat
                    if(rp < 10) { // 10% chance
                        setStatus("Checking random stat");
                        if (Tabs.getOpen() != Tab.SKILLS)
                            openStats();
                        int x = r(0, 25);
                        int y = r(0, 15);
                        int skill = -1;
                        long t = System.currentTimeMillis();
                        while (skill == -1 && System.currentTimeMillis() - t <= 500) {
                            int r = r(0, Skill.values().length - 1);
                            for (Skill s : STATS_TO_CHECK) {
                                if (s.getName().equals(Skill.values()[r].getName()))
                                    skill = r;
                            }
                        }

                        setStatus("Checking EXP (" + Skill.values()[skill].getName() + ")");
                        Point p = STAT_WIDGET[skill];
                        p.setLocation(p.getX() + x, p.getY() + y);
                        Mouse.move(p);

                        LAST_EVENT = System.currentTimeMillis();
                        return rh(2000, 5000);
                    }
                }
                case 3: { // Move mouse to random location (and sometimes click)
                    if(rp < 10) { // 10% chance
                        int r = r(0, 100);
                        int x = r(0, 760);
                        int y = r(0,500);
                        setStatus("Moving mouse (" + x + "," + y + ")");
                        Mouse.move(new Point(x, y));
                        if(r < 5) // 10% chance of right-clicking
                            Mouse.click(true);
                        else if(r < 5) // 5% chance of left-clicking
                            Mouse.click();

                        LAST_EVENT = System.currentTimeMillis();
                        return rh(500, 3000);
                    }
                }
                case 4: { // Walk to random location
                    if(rp < 1) { // 1% chance
                        int x = Players.getLocal().getX() - 15;
                        int y = Players.getLocal().getY() - 15;
                        int x2 = r(0, 30);
                        int y2 = r(0, 30);
                        Area a = new Area(x, y, x2, y2);
                        Tile t = a.getRandomTile();
                        setStatus("Walking to random tile (" + t.getX() + "," + t.getY() + ")");
                        Walking.walk(t);
                        LAST_EVENT = System.currentTimeMillis();
                        return rh(500, 3000);
                    }
                }

                case 6: { // Click random entity
                    if(rp < 1) { // 1% chance
                        setStatus("clicking random shit");
                        Entity e = GameObjects.closest(o -> o != null && !o.getName().equals("null")  && r(1, 2) != 1 && Players.getLocal().distance(o) < 5);
                        int r = r(1, 3);
                        if(e == null || r == 2) {
                            e = NPCs.closest(n -> n != null && !n.getName().equals("null"));
                            if(e == null || r == 3) {
                                e = GroundItems.closest(i -> i != null && !i.getName().equals("null"));
                                if(e == null)
                                    return doNothing();
                            }
                        }

                        if(e instanceof NPC && Combat.isInMultiCombat())
                            break;

                        setStatus("Clicking random entity (" + e.getName() + ")");
                        Mouse.move(e);
                        s.sleep(rh(0, 50));
                        if(r(0, 100) < 25)
                            Mouse.click(true);
                        else
                            Mouse.click();

                        LAST_EVENT = System.currentTimeMillis();
                        return rh(500, 3000);
                    }
                }
                case 7: { // Just idle for a while
                    if(rp < 5) { // 5% chance
                        if (System.currentTimeMillis() - LAST_IDLE >= 300000) { // Only allow idling to occur every 5+ minutes
                            int idle = r(60000, 120000);
                            setStatus("Idling for " + (idle / 1000) + " seconds");
                            if (r(0, 100) < 99)
                                Mouse.moveOutsideScreen();
                            // Disable dismiss & autologin solvers temporarily
                            ScriptManager.getScriptManager().getCurrentScript().getRandomManager().disableSolver(RandomEvent.LOGIN);
                            ScriptManager.getScriptManager().getCurrentScript().getRandomManager().disableSolver(RandomEvent.DISMISS);
                            // Sleep for the calculated time
                            s.sleep(idle);
                            // Enable dismiss & autologin solvers and resume script as normal
                            ScriptManager.getScriptManager().getCurrentScript().getRandomManager().disableSolver(RandomEvent.LOGIN);
                            ScriptManager.getScriptManager().getCurrentScript().getRandomManager().disableSolver(RandomEvent.DISMISS);
                            LAST_IDLE = System.currentTimeMillis();
                            return 1;
                        }
                    }
                }
                case 8: { // Open inventory or stats
                    if(rp < 25) { // 25% chance
                        setStatus("Opening inventory or stats");
                        if(Tabs.getOpen() != Tab.INVENTORY && Inventory.getEmptySlots() > 0)
                            setStatus("Opening inventory");
                        if (openInventory()) {
                            LAST_EVENT = System.currentTimeMillis();
                            s.sleep(50, 100);
                            Mouse.moveOutsideScreen();
                            return rh(500, 1000);
                        }
                    } else
                    if(rp > 75) { // 25% chance
                        if(Tabs.getOpen() != Tab.SKILLS)
                            setStatus("Opening stats");
                        if(openStats()) {
                            LAST_EVENT = System.currentTimeMillis();
                            s.sleep(50, 100);
                            Mouse.moveOutsideScreen();
                            return rh(500, 1000);
                        }
                    }
                }

                case 10: { // Moving mouse off-screen for a moment
                    if(rp < 50) { // 50% chance
                        if(Mouse.getX() == -1 && Mouse.getY() == -1)
                            return doNothing();

                        setStatus("Moving mouse off-screen");
                        Mouse.moveOutsideScreen();
                        LAST_EVENT = System.currentTimeMillis();
                        return rh(5000, 8000);
                    }
                }

                case 11: { // Open magic menu
                    if(rp < 1) { // 1% chance
                        setStatus("Opening magic menu");
                        if(Tabs.getOpen() != Tab.MAGIC) {
                            openMagic();
                            s.sleep(50, 100);
                            Mouse.moveOutsideScreen();
                        }
                    }
                }

                case 12: { // Examine random inventory item
                    if(rp < 1) { // 1% chance
                        setStatus("Examining random inventory item");
                        if (openInventory())
                            s.sleep(10, 250);
                        for(Item i : Inventory.all(Objects::nonNull)) {
                            if(i != null && r(1, 3) == 2) {
                                setStatus("Examining item (" + i.getName() + ")");
                                Mouse.move(i.getDestination());
                                s.sleep(rh(0, 50));
                                // Open right-click menu and find Examine option
                                Menu.open();
                                s.sleep(rh(250, 1000));
                                if (Menu.contains("Examine"))
                                    Menu.clickAction("Examine");
                                else
                                if(Menu.contains("Cancel"))
                                    Menu.clickAction("Cancel");

                                break;
                            }
                        }
                    }
                }

                case 13: { // Move camera randomly
                    if(rp < 30) { // 30% chance
                        setStatus("rotating camera randomly");
                        rotateCamera();
                        return doNothing();
                    }
                }

                case 14: { // Do nothing
                    setStatus("doing nothing");
                    if(rp < 5) { // 5% chance
                        return doNothing();
                    }
                }

                default:
                    setStatus("doing default of nothing");
                    return doNothing();
            }

        }

        return doNothing();
    }

    // Returns whether or not the DO_RANDOM flag has been triggerd
    public boolean doRandom() {
        if(DO_RANDOM) {
            DO_RANDOM = false;
            return true;
        }

        return false;
    }

    // Get antiban status
    public String getStatus() {
        return STATUS;
    }

    // Print to the console if debug is enabled
    private void print(Object o) {
        s.log("[AntiBan] " + o.toString());
    }

    // Allows an external class to set the anti-ban status
    public void setStatus(String status) {
        STATUS = status;
        if(!status.equals(""))
            Logger.log(status);
    }

    // Returns a random number
    public int r(int x, int y) {
        return Calculations.random(x, y+1);
    }

    // Returns a random number with the human lag element added to the minimum wait time
    public int rh(int x, int y) {
        //return r(x + getHumanLag(), y + RAND + getHumanLag());
        return r(x + (int)(x*getLagMultiplier()), y + (int)(y*getLagMultiplier()));
    }

    // Returns the lag multiplier (increases as script runs longer)
    public double getLagMultiplier() {
        double minutesRunning = (double) ((System.currentTimeMillis() - START_TIME) / 60000);
        double percent = (minutesRunning / (double)(MAX_RUNTIME_MINUTES == -1 ? 480 : MAX_RUNTIME_MINUTES));

        if(percent > 1.0)
            percent = 1.0D;

        return percent;
    }

    public boolean rotateCamera(){
        setStatus("Moving camera");
        Area a = new Area(Players.getLocal().getX() - 10, Players.getLocal().getY() - 10, Players.getLocal().getX() + 10, Players.getLocal().getY() + 10);
        Camera.rotateToTile(a.getRandomTile());
        return true;
    }

    // This method opens the stats menu
    public boolean openStats() {
        if (Tabs.getOpen() != Tab.SKILLS) {
            // Sometimes use hot keys, sometimes use mouse
            if (Calculations.random(1, 3) == 2)
                Skills.open();
            else {
                int x = (int) STATS_WIDGET.getX() + r(0, 10);
                int y = (int) STATS_WIDGET.getY() + r(0, 10);
                Mouse.move(new Point(x, y));
                s.sleep(0, 50);
                Mouse.click();
            }

            s.sleep(50, 250);
        }

        return Tabs.getOpen() == Tab.SKILLS;
    }

    // Opens the  combat menu then waits for a second
    public boolean openCombat() {
        if (Tabs.getOpen() != Tab.COMBAT) {
            // Sometimes use hot keys, sometimes use mouse
            if (Calculations.random(1, 3) == 2)
                Tabs.open(Tab.COMBAT);
            else {
                int x = (int) COMBAT_WIDGET.getX() + Calculations.random(0, 10);
                int y = (int) COMBAT_WIDGET.getY() + Calculations.random(0, 10);
                Mouse.move(new Point(x, y));
                s.sleep(0, 50);
                Mouse.click();
            }

            s.sleep(50, 250);
        }

        return Tabs.getOpen() == Tab.COMBAT;
    }

    // This method opens the magic menu
    public boolean openMagic() {
        if (Tabs.getOpen() != Tab.MAGIC) {
            // Sometimes use hot keys, sometimes use mouse
            if (Calculations.random(1, 3) == 2)
                Tabs.open(Tab.MAGIC);
            else {
                int x = (int) MAGIC_WIDGET.getX() + r(0, 10);
                int y = (int) MAGIC_WIDGET.getY() + r(0, 10);
                Mouse.move(new Point(x, y));
                Mouse.click();
            }

            s.sleep(50, 250);
        }

        return Tabs.getOpen() == Tab.MAGIC;
    }

    // This method opens the inventory
    public boolean openInventory() {
        if (Tabs.getOpen() != Tab.INVENTORY) {
            // Sometimes use hot keys, sometimes use mouse
            if (Calculations.random(1, 3) == 2)
                Tabs.open(Tab.INVENTORY);
            else {
                int x = (int) INVENTORY_WIDGET.getX() + r(0, 10);
                int y = (int) INVENTORY_WIDGET.getY() + r(0, 10);
                Mouse.move(new Point(x, y));
                Mouse.click();
            }

            s.sleep(50, 250);
        }

        return Tabs.getOpen() == Tab.INVENTORY;
    }
}