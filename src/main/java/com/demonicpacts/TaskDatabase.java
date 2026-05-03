package com.demonicpacts;

import java.util.*;
import java.util.stream.Collectors;

public class TaskDatabase
{
    private static final List<DemonicPactsTask> ALL_TASKS = new ArrayList<>();

    // Reverse lookup maps built at init
    private static final Map<String, List<DemonicPactsTask>> NPC_TASKS = new HashMap<>();
    private static final Map<String, List<DemonicPactsTask>> ITEM_TASKS = new HashMap<>();
    private static final Map<String, List<DemonicPactsTask>> OBJECT_TASKS = new HashMap<>();

    /**
     * Tools that appear as ingredients for many different tasks but aren't the
     * actual subject of any of them. Highlighting them creates visual noise
     * since a chisel, knife, or mould is used across dozens of craft tasks.
     * Items in this list will not produce highlights or tooltips.
     */
    private static final Set<String> ITEM_BLOCKLIST = new HashSet<>(Arrays.asList(
        "chisel",
        "knife",
        "ammo mould",
        "ring mould",
        "amulet mould",
        "vial of water"
    ));

    /**
     * Maps world object names (rocks, trees, fishing spots, patches) to their
     * corresponding item-based task keywords so we can highlight the source objects.
     */
    private static final Map<String, String[]> OBJECT_TO_TASK_KEYWORDS = new HashMap<>();
    static
    {
        // Mining rocks -> ore item keywords
        OBJECT_TO_TASK_KEYWORDS.put("clay rocks", new String[]{"clay"});
        OBJECT_TO_TASK_KEYWORDS.put("tin rocks", new String[]{"tin ore"});
        OBJECT_TO_TASK_KEYWORDS.put("copper rocks", new String[]{"copper ore"});
        OBJECT_TO_TASK_KEYWORDS.put("iron rocks", new String[]{"iron ore"});
        OBJECT_TO_TASK_KEYWORDS.put("silver rocks", new String[]{"silver ore"});
        OBJECT_TO_TASK_KEYWORDS.put("coal rocks", new String[]{"coal"});
        OBJECT_TO_TASK_KEYWORDS.put("gold rocks", new String[]{"gold ore"});
        OBJECT_TO_TASK_KEYWORDS.put("mithril rocks", new String[]{"mithril ore"});
        OBJECT_TO_TASK_KEYWORDS.put("adamantite rocks", new String[]{"adamantite ore"});
        OBJECT_TO_TASK_KEYWORDS.put("runite rocks", new String[]{"runite ore"});

        // Trees -> log item keywords
        OBJECT_TO_TASK_KEYWORDS.put("tree", new String[]{"logs"});
        OBJECT_TO_TASK_KEYWORDS.put("oak tree", new String[]{"oak logs"});
        OBJECT_TO_TASK_KEYWORDS.put("oak", new String[]{"oak logs"});
        OBJECT_TO_TASK_KEYWORDS.put("willow tree", new String[]{"willow logs"});
        OBJECT_TO_TASK_KEYWORDS.put("willow", new String[]{"willow logs"});
        OBJECT_TO_TASK_KEYWORDS.put("maple tree", new String[]{"maple logs"});
        OBJECT_TO_TASK_KEYWORDS.put("yew tree", new String[]{"yew logs"});
        OBJECT_TO_TASK_KEYWORDS.put("yew", new String[]{"yew logs"});
        OBJECT_TO_TASK_KEYWORDS.put("magic tree", new String[]{"magic logs"});
        OBJECT_TO_TASK_KEYWORDS.put("teak tree", new String[]{"teak logs"});
        OBJECT_TO_TASK_KEYWORDS.put("mahogany tree", new String[]{"mahogany logs"});
        OBJECT_TO_TASK_KEYWORDS.put("redwood tree", new String[]{"redwood logs"});
        OBJECT_TO_TASK_KEYWORDS.put("blisterwood tree", new String[]{"blisterwood logs"});
        OBJECT_TO_TASK_KEYWORDS.put("padri tree", new String[]{"padri logs"});
        OBJECT_TO_TASK_KEYWORDS.put("juniper tree", new String[]{"juniper logs"});
        OBJECT_TO_TASK_KEYWORDS.put("sulliusceps", new String[]{"mushroom"});
        OBJECT_TO_TASK_KEYWORDS.put("mushroom", new String[]{"mushroom"});

        // Fishing spots -> raw fish item keywords. Each sub-type is narrow;
        // the plain "fishing spot" entry covers only the net/bait category
        // (what you get with "Small Net" / "Bait" actions) since that's what
        // non-type-prefixed Fishing spot NPCs in OSRS typically use.
        OBJECT_TO_TASK_KEYWORDS.put("fishing spot", new String[]{
            "raw shrimps", "raw anchovies", "raw sardine", "raw herring"});
        OBJECT_TO_TASK_KEYWORDS.put("rod fishing spot", new String[]{
            "raw herring", "raw trout", "raw salmon", "raw pike", "raw sardine"});
        OBJECT_TO_TASK_KEYWORDS.put("net fishing spot", new String[]{
            "raw shrimps", "raw anchovies", "raw sardine", "raw herring", "minnow"});
        OBJECT_TO_TASK_KEYWORDS.put("small net fishing spot", new String[]{
            "raw shrimps", "raw anchovies", "raw sardine", "raw herring"});
        OBJECT_TO_TASK_KEYWORDS.put("bait fishing spot", new String[]{
            "raw sardine", "raw herring", "raw pike"});
        OBJECT_TO_TASK_KEYWORDS.put("harpoon fishing spot", new String[]{
            "raw tuna", "raw swordfish", "raw shark"});
        OBJECT_TO_TASK_KEYWORDS.put("cage fishing spot", new String[]{"raw lobster"});
        OBJECT_TO_TASK_KEYWORDS.put("karambwan fishing spot", new String[]{"raw karambwan"});
        OBJECT_TO_TASK_KEYWORDS.put("lure fishing spot", new String[]{
            "raw trout", "raw salmon"});

        // Farming patches
        OBJECT_TO_TASK_KEYWORDS.put("allotment", new String[]{"allotment"});
        OBJECT_TO_TASK_KEYWORDS.put("allotment patch", new String[]{"allotment"});
        OBJECT_TO_TASK_KEYWORDS.put("flower patch", new String[]{"flower patch"});
        OBJECT_TO_TASK_KEYWORDS.put("herb patch", new String[]{
            "grimy guam leaf", "grimy tarromin", "grimy harralander", "grimy ranarr weed",
            "grimy toadflax", "grimy irit leaf", "grimy avantoe", "grimy kwuarm",
            "grimy snapdragon", "grimy cadantine", "grimy lantadyme", "grimy dwarf weed",
            "grimy torstol"});
        OBJECT_TO_TASK_KEYWORDS.put("fruit tree patch", new String[]{
            "papaya tree", "palm tree", "calquat tree"});
        OBJECT_TO_TASK_KEYWORDS.put("tree patch", new String[]{"oak logs", "willow logs", "maple logs", "yew logs", "magic logs"});
        OBJECT_TO_TASK_KEYWORDS.put("hops patch", new String[]{"barley", "hammerstone hop", "asgarnian hop", "yanillian hop", "krandorian hop", "wildblood hop"});
        OBJECT_TO_TASK_KEYWORDS.put("bush patch", new String[]{"redberries", "cadavaberries", "dwellberries", "jangerberries", "whiteberries", "poison ivy berries"});
        OBJECT_TO_TASK_KEYWORDS.put("spirit tree patch", new String[]{"spirit seed"});
        OBJECT_TO_TASK_KEYWORDS.put("crystal tree patch", new String[]{"crystal shard"});
    }

    static
    {
        // =====================================================================
        // EASY TASKS (10 pts)
        // =====================================================================
        // -- Defeat NPCs --
        defeatNpc("Defeat a Troll in Asgarnia", "Defeat a Troll in Asgarnia. Earns one Demonic Pact.", "Asgarnia", TaskDifficulty.EASY, "", "Troll");
        defeatNpc("Defeat a Hill Giant", "Defeat a Hill Giant. Earns one Demonic Pact.", "General", TaskDifficulty.EASY, "", "Hill Giant");
        defeatNpc("Defeat a Chicken", "Defeat a Chicken.", "General", TaskDifficulty.EASY, "", "Chicken");
        defeatNpc("Defeat a Frog", "Defeat a Frog.", "General", TaskDifficulty.EASY, "", "Frog");
        defeatNpc("Defeat a Rat", "Defeat a Rat.", "General", TaskDifficulty.EASY, "", "Rat");
        defeatNpc("Defeat a scorpion", "Defeat a Scorpion.", "General", TaskDifficulty.EASY, "", "Scorpion");
        defeatNpc("Defeat an Imp with an earth spell", "Defeat an Imp with an earth spell.", "General", TaskDifficulty.EASY, "Magic 9", "Imp");
        defeatNpc("Defeat a Cockatrice in the Fremennik Province", "Defeat a Cockatrice in the Fremennik Province. Earns one Demonic Pact.", "Fremennik", TaskDifficulty.EASY, "Slayer 25", "Cockatrice");
        defeatNpc("Defeat a Werewolf in Morytania", "Defeat a Werewolf in Morytania. Earns one Demonic Pact.", "Morytania", TaskDifficulty.EASY, "", "Werewolf");
        defeatNpc("Defeat a Chaos Dwarf in the Wilderness", "Defeat a Chaos Dwarf in the Wilderness. Earns one Demonic Pact.", "Wilderness", TaskDifficulty.EASY, "", "Chaos Dwarf");
        defeatNpc("Set a Mummy ablaze", "Set a Mummy on Fire with fire damage. Earns one Demonic Pact.", "Desert", TaskDifficulty.EASY, "Magic 13 or Tirannwn for oily cloth", "Mummy");

        // -- Equip Items --
        equipItem("Equip an Elemental Staff", "Equip a basic elemental staff.", "General", TaskDifficulty.EASY, "",
            "Staff of water", "Staff of fire", "Staff of earth", "Staff of air");
        equipItem("Equip an Iron dagger", "Equip an Iron dagger.", "General", TaskDifficulty.EASY, "", "Iron dagger");
        equipItem("Equip a Tyras helm", "Equip a Tyras helm.", "General", TaskDifficulty.EASY, "Defence 5", "Tyras helm");

        // -- Skilling --
        addTask("Achieve Your First Level Up", "Level up any of your skills for the first time.", "General", TaskDifficulty.EASY, TaskType.SKILL_LEVEL, "", 1);
        addTask("Achieve Your First Level 5", "Reach level 5 in any skill.", "General", TaskDifficulty.EASY, TaskType.SKILL_LEVEL, "", 1);
        addTask("Achieve Your First Level 10", "Reach level 10 in any skill.", "General", TaskDifficulty.EASY, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Combat Level 25", "Reach Combat Level 25.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);

        // -- Fishing --
        catchItem("Catch a Shrimp", "Catch Raw Shrimp while Fishing.", "General", TaskDifficulty.EASY, "", "Raw shrimps");
        catchItem("Catch a Herring", "Catch a Raw Herring whilst Fishing.", "General", TaskDifficulty.EASY, "Fishing 10", "Raw herring");
        catchItem("Catch an Anchovy", "Catch a Raw Anchovy whilst Fishing.", "General", TaskDifficulty.EASY, "Fishing 15", "Raw anchovies");

        // -- Mining --
        mineItem("Mine 5 Tin Ore", "Mine 5 Tin Ore.", "General", TaskDifficulty.EASY, "", 5, "Tin ore");
        mineItem("Mine some Clay", "Mine some Clay.", "General", TaskDifficulty.EASY, "", 1, "Clay");

        // -- Woodcutting --
        chopItem("Chop Some Logs", "Chop any kind of logs.", "General", TaskDifficulty.EASY, "", 1, "Logs", "Oak logs", "Willow logs", "Maple logs", "Yew logs", "Magic logs");

        // -- Cooking --
        cookItem("Cook Shrimp", "Cook Raw Shrimp.", "General", TaskDifficulty.EASY, "", 1, "Raw shrimps");
        addTask("Burn Some Food", "Burn any kind of food while trying to cook it.", "General", TaskDifficulty.EASY, TaskType.COOK_ITEM, "", 1);

        // -- Firemaking --
        burnItem("Burn Some Normal Logs", "Burn some Normal Logs.", "General", TaskDifficulty.EASY, "", 1, "Logs");
        burnItem("Burn Some Oak Logs", "Burn some Oak Logs.", "General", TaskDifficulty.EASY, "Firemaking 15", 1, "Oak logs");

        // -- Herblore --
        cleanItem("Clean a Grimy Guam", "Clean a Grimy Guam.", "General", TaskDifficulty.EASY, "", 1, "Grimy guam leaf");
        cleanItem("Clean 25 Grimy Guam Leafs", "Clean 25 Grimy Guam Leafs.", "General", TaskDifficulty.EASY, "", 25, "Grimy guam leaf");
        cleanItem("Clean 15 Grimy Tarromin", "Clean 15 Grimy Tarromin.", "General", TaskDifficulty.EASY, "Herblore 11", 15, "Grimy tarromin");

        // -- Crafting --
        craftItem("Craft Leather chaps", "Craft Leather chaps.", "General", TaskDifficulty.EASY, "Crafting 18",
            "Leather", "Needle", "Thread");
        craftItem("Cut a Ruby", "Cut a Ruby.", "General", TaskDifficulty.EASY, "Crafting 34",
            "Uncut ruby", "Chisel");
        craftItem("Successfully Cut a Red Topaz", "Successfully Cut a Red Topaz.", "General", TaskDifficulty.EASY, "Crafting 16",
            "Uncut red topaz", "Chisel");

        // -- Fletching --
        craftItem("Fletch Some Arrow Shafts", "Fletch some Arrow Shafts.", "General", TaskDifficulty.EASY, "",
            "Logs", "Oak logs", "Willow logs", "Maple logs", "Yew logs", "Magic logs", "Knife");
        craftItem("Fletch an Oak Shortbow", "Fletch an Oak Shortbow.", "General", TaskDifficulty.EASY, "Fletching 20",
            "Oak shortbow (u)", "Bow string", "Knife");

        // -- Herblore potions --
        craftItem("Make an Attack Potion", "Make an Attack Potion.", "General", TaskDifficulty.EASY, "",
            "Guam potion (unf)", "Eye of newt", "Guam leaf", "Vial of water");
        craftItem("Create an Antipoison", "Create an Antipoison.", "General", TaskDifficulty.EASY, "Herblore 5",
            "Marrentill potion (unf)", "Unicorn horn dust", "Marrentill", "Vial of water");

        // -- Smithing --
        craftItem("Smelt a Bronze Bar", "Use a Furnace to smelt a Bronze Bar.", "General", TaskDifficulty.EASY, "",
            "Copper ore", "Tin ore");
        craftItem("Smelt an Iron Bar", "Use a Furnace to smelt an Iron Bar.", "General", TaskDifficulty.EASY, "Smithing 15",
            "Iron ore");

        // -- Thieving --
        addTask("Pickpocket a Citizen", "Pickpocket a Man or a Woman.", "General", TaskDifficulty.EASY, TaskType.DEFEAT_NPC, "", 1, "Man", "Woman");

        // -- Hunter --
        catchItem("Catch a Baby Impling", "Catch a Baby Impling.", "General", TaskDifficulty.EASY, "Hunter 17", "Baby impling");
        catchItem("Snare a Bird", "Catch any bird with a Bird Snare.", "General", TaskDifficulty.EASY, "", "Crimson swift", "Golden warbler", "Copper longtail", "Cerulean twitch", "Tropical wagtail");
        catchItem("Snare 5 Crimson Swifts", "Snare 5 Crimson Swifts.", "General", TaskDifficulty.EASY, "", 5, "Crimson swift");
        catchItem("Snare 15 Tropical Wagtails", "Snare 15 Tropical Wagtails.", "General", TaskDifficulty.EASY, "Hunter 19", 15, "Tropical wagtail");

        // -- Prayer --
        addTask("Bury 6 bones", "Bury 6 bones of any kind.", "General", TaskDifficulty.EASY, TaskType.PRAYER, "", 6, "Bones", "Big bones", "Dragon bones");

        // -- Misc --
        addTask("Complete the Leagues Tutorial", "Complete the Leagues Tutorial and begin your adventure.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Open the Leagues Menu", "Open the Leagues Menu found within the Journal Panel.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Cast Home Teleport", "Cast the Home Teleport spell.", "General", TaskDifficulty.EASY, TaskType.SPELL, "", 1);
        addTask("Perform a Special Attack", "Perform any special attack.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Enter your Player Owned House", "Enter your Player Owned House.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Purchase a Player Owned House", "Purchase a Player Owned House.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Spin a Ball of Wool", "Use a Spinning Wheel to spin a Ball of Wool.", "General", TaskDifficulty.EASY, TaskType.CRAFT_ITEM, "", 1, "Ball of wool");
        addTask("Drink a Strength Potion", "Drink a Strength Potion.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Strength potion");
        addTask("Eat a Rabbit", "Eat a cooked rabbit.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Cooked rabbit");
        addTask("Eat an Onion", "Eat an Onion, raw.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Onion");
        addTask("Light a Torch", "Light a Torch.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Torch");
        addTask("Dye a cape Purple", "Dye a cape Purple.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Purple dye");
        addTask("Cook something with an apron", "Cook something with an apron equipped.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Apron");
        addTask("Feed a dog some bones", "Feed a dog some bones.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Bones");
        addTask("Sell some silk to a silk trader", "Sell some silk to a silk trader.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Silk");
        addTask("Get a haircut", "Go and get a haircut.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Turn off your run", "Turn off your run.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Activate a prayer near an altar", "Activate a prayer near an altar.", "General", TaskDifficulty.EASY, TaskType.PRAYER, "", 1);
        addTask("Attack a dummy", "Attack a dummy.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Dummy");
        addTask("Plant Seeds in an Allotment Patch", "Plant some seeds in an Allotment patch.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Protect Your Crops", "Pay a farmer to protect any of your crops.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Rake a Flower Patch", "Rake a Flower Patch.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Obtain a Bird Nest", "Obtain a Bird Nest whilst cutting down trees.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Bird nest");
        addTask("Obtain a Casket from Fishing", "Obtain a Casket from Fishing.", "General", TaskDifficulty.EASY, TaskType.MISC, "Fishing 16", 1, "Casket");
        addTask("Shoot 6 iron arrows", "Shoot 6 iron arrows.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 6, "Iron arrow");
        addTask("Pick 6 flax", "Pick 6 flax.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 6, "Flax");
        addTask("Turn any Logs Into a Plank", "Use a Sawmill to turn Logs into a Plank.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Logs", "Oak logs", "Teak logs", "Mahogany logs");
        addTask("Talk to any Port master", "Talk to any Port master.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Steal some bread", "Steal some bread from a Bakery Stall.", "General", TaskDifficulty.EASY, TaskType.MISC, "Thieving 5", 1);

        // Kourend easy
        addTask("Open 1 Grubby Chest", "Open the grubby chest in the Forthos Dungeon. Earns one Demonic Pact.", "Kourend", TaskDifficulty.EASY, TaskType.MISC, "Thieving 57", 1, "Grubby chest");

        // =====================================================================
        // EASY TASKS — missing General
        // =====================================================================
        addTask("Chop Some Logs With a Steel Axe", "Chop any kind of logs using a Steel Axe.", "General", TaskDifficulty.EASY, TaskType.MISC, "Woodcutting 6", 1, "Steel axe");
        addTask("Mine some Ore With a Steel Pickaxe", "Mine any ore using a Steel Pickaxe.", "General", TaskDifficulty.EASY, TaskType.MISC, "Mining 6", 1, "Steel pickaxe");
        addTask("Pick 6 wheat, 6 cabbages and 6 potatoes", "Pick 6 wheat, 6 cabbage and 6 potatoes.", "General", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Wheat", "Cabbage", "Potato");

        // =====================================================================
        // EASY TASKS — Karamja
        // =====================================================================
        catchItem("Catch a Karambwanji", "Catch a Karambwanji on Karamja.", "Karamja", TaskDifficulty.EASY, "Fishing 5", "Raw karambwanji");
        defeatNpc("Defeat a Snake in Karamja", "Defeat a Snake in Karamja.", "Karamja", TaskDifficulty.EASY, "", "Snake");
        addTask("Fill a Crate With Bananas", "Fill a crate with Bananas for Luthas at Musa Point.", "Karamja", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Banana");
        addTask("Pick a Pineapple on Karamja", "Pick a Pineapple on Karamja.", "Karamja", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Pineapple");
        addTask("Receive an Agility Arena Ticket", "Receive an Agility Arena Ticket from the Brimhaven Agility Arena.", "Karamja", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Agility arena ticket");

        // =====================================================================
        // EASY TASKS — Varlamore
        // =====================================================================
        addTask("Admire some beautiful scenery", "Admire some beautiful scenery in Auburnvale or the Auburn Valley.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Bow near a quetzal", "Bow near a quetzal.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Quetzal");
        addTask("Charter a Ship From Sunset Coast to Civitas", "Take a Charter Ship from Sunset Coast to Civitas illa Fortis.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Chop a tree in the Tlati Rainforest", "Chop a tree in the Tlati Rainforest.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Complete the Vale totems miniquest", "Complete the Vale totems miniquest.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "Fletching 20", 1);
        addTask("Cry near a child", "Cry near a child.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Child");
        addTask("Dance near a bard", "Dance near a bard.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Bard");
        defeatNpc("Defeat a Buffalo", "Defeat a Buffalo.", "Varlamore", TaskDifficulty.EASY, "", "Buffalo");
        defeatNpc("Defeat a Guard in Varlamore underground", "Defeat a Guard in Varlamore underground.", "Varlamore", TaskDifficulty.EASY, "", "Guard");
        defeatNpc("Defeat a Seagull", "Defeat a Seagull.", "Varlamore", TaskDifficulty.EASY, "", "Seagull");
        defeatNpc("Defeat a thief", "Defeat a thief.", "Varlamore", TaskDifficulty.EASY, "", "Thief");
        defeatNpc("Defeat an Icefiend in Varlamore", "Defeat an Icefiend in Varlamore.", "Varlamore", TaskDifficulty.EASY, "", "Icefiend");
        defeatNpc("Defeat an Imp in a basement", "Defeat an Imp in the Kastori basement in Varlamore.", "Varlamore", TaskDifficulty.EASY, "", "Imp");
        addTask("Drink a cup of tea in auburn valley", "Drink a cup of tea in auburn valley.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Cup of tea");
        addTask("Drink from a bird bath", "Drink from a bird bath.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Bird bath");
        addTask("Drink some moon-lite", "Drink some moon-lite.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Moon-lite");
        addTask("Exit civitas via the secret passage", "Exit civitas via the secret passage.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Fill a bucket with sand at the Sunset coast", "Fill a bucket with sand at the Sunset coast.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Bucket");
        addTask("Fill something up from a water pump", "Fill something up from a water pump.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Water pump");
        addTask("Give Oli some Stew", "Give Oli some Stew in Civitas illa Fortis.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Stew");
        addTask("Inspect a green flame", "Inspect a green flame near Salvagers outlook.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        defeatNpc("Milk a Buffalo", "Milk a Buffalo.", "Varlamore", TaskDifficulty.EASY, "", "Dairy Buffalo");
        mineItem("Mine some Coal from Stonecutter Outpost", "Mine some Coal from Stonecutter Outpost.", "Varlamore", TaskDifficulty.EASY, "Mining 30", 1, "coal");
        defeatNpc("Pet Renu", "Pet Renu.", "Varlamore", TaskDifficulty.EASY, "", "Renu");
        defeatNpc("Pet Xolo in Civitas", "Pet Xolo in Civitas illa Fortis.", "Varlamore", TaskDifficulty.EASY, "", "Xolo");
        defeatNpc("Pet a Caique", "Pet a Caique near the statue of Ates in Kastori or in the north of the Tlati Rainforest.", "Varlamore", TaskDifficulty.EASY, "", "Caique");
        addTask("Pick some Sweetcorn from a Field", "Pick some Sweetcorn from a Field.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Sweetcorn");
        addTask("Salute next to a statue of Quoatlos", "Salute next to a statue of Quoatlos.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Scatter some Ashes in Yama's lair", "Scatter some Ashes in Yama's lair.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Ashes");
        defeatNpc("Shear an Alpaca", "Shear an Alpaca.", "Varlamore", TaskDifficulty.EASY, "", "Alpaca");
        addTask("Sit near a stolen cabbage", "Sit near a stolen cabbage.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Stolen cabbage");
        addTask("Step onto an Ent trail", "Step onto an Ent trail.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        defeatNpc("Talk to a Gladiator", "Talk to a Gladiator.", "Varlamore", TaskDifficulty.EASY, "", "Gladiator");
        addTask("Travel in Achilka's boat", "Travel in Achilka's boat at one of her destinations along the River Varla.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1, "Achilka");
        addTask("Travel to Aldarin via Fairy ring", "Travel to Aldarin via Fairy ring.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Travel using the Quetzal Transport System", "Travel using the Quetzal Transport System.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        addTask("Trim your beard in Cam torum", "Trim your beard in Cam Torum.", "Varlamore", TaskDifficulty.EASY, TaskType.MISC, "", 1);
        defeatNpc("Witness the gemstone Crab burrow away", "Witness the gemstone crab burrow away.", "Varlamore", TaskDifficulty.EASY, "", "Gemstone Crab");

        // =====================================================================
        // MEDIUM TASKS (30 pts)
        // =====================================================================
        addTask("Achieve Your First Level 20", "Reach level 20 in any skill.", "General", TaskDifficulty.MEDIUM, TaskType.SKILL_LEVEL, "", 1);
        addTask("Achieve Your First Level 30", "Reach level 30 in any skill.", "General", TaskDifficulty.MEDIUM, TaskType.SKILL_LEVEL, "", 1);
        addTask("Achieve Your First Level 40", "Reach level 40 in any skill.", "General", TaskDifficulty.MEDIUM, TaskType.SKILL_LEVEL, "", 1);
        addTask("Achieve Your First Level 50", "Reach level 50 in any skill.", "General", TaskDifficulty.MEDIUM, TaskType.SKILL_LEVEL, "", 1);
        addTask("Achieve Your First Level 60", "Reach level 60 in any skill.", "General", TaskDifficulty.MEDIUM, TaskType.SKILL_LEVEL, "", 1);

        // -- Prayers --
        addTask("Use the Protect from Melee Prayer", "Use the Protect from Melee Prayer.", "General", TaskDifficulty.MEDIUM, TaskType.PRAYER, "Prayer 43", 1);
        addTask("Activate Smite", "Activate Smite in your prayer book.", "General", TaskDifficulty.MEDIUM, TaskType.PRAYER, "Prayer 52", 1);

        // -- Clue scrolls --
        addTask("1 Easy Clue Scroll", "Open a Reward casket for completing an Easy clue scroll.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("1 Medium Clue Scroll", "Open a Reward casket for completing a Medium clue scroll.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("1 Hard Clue Scroll", "Open a Reward casket for completing a Hard clue scroll.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("1 Elite Clue Scroll", "Open a Reward casket for completing an Elite clue scroll.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("25 Easy Clue Scrolls", "Open 25 Reward caskets for completing Easy clue scrolls.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 25);
        addTask("25 Medium Clue Scrolls", "Open 25 Reward caskets for completing Medium clue scrolls.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 25);
        addTask("25 Hard Clue Scrolls", "Open 25 Reward caskets for completing Hard clue scrolls.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 25);
        addTask("25 Elite Clue Scrolls", "Open 25 Reward caskets for completing Elite clue scrolls.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 25);
        addTask("75 Easy Clue Scrolls", "Open 75 Reward caskets for completing Easy clue scrolls.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 75);
        addTask("75 Medium Clue Scrolls", "Open 75 Reward caskets for completing Medium clue scrolls.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 75);

        // -- Collection log --
        addTask("5 Collection log slots", "Obtain 5 unique Collection Log slots.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 5);
        addTask("15 Collection log slots", "Obtain 15 unique Collection Log slots.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 15);
        addTask("30 Collection log slots", "Obtain 30 unique Collection Log slots.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 30);
        addTask("50 Collection log slots", "Obtain 50 unique Collection Log slots.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 50);

        // -- Slayer --
        addTask("Complete 1 Slayer Task", "Complete 1 Slayer Task.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        defeatNpc("Defeat a Superior slayer creature", "Defeat a Superior slayer creature.", "General", TaskDifficulty.MEDIUM, "Slayer 5 + Bigger and Badder or Tier 3 Relic", "Superior");
        defeatNpc("Defeat 25 Superior slayer creatures", "Defeat 25 Superior slayer monsters.", "General", TaskDifficulty.MEDIUM, "Slayer 5 + Bigger and Badder or Tier 3 Relic", "Superior");
        defeatNpc("Defeat 75 Superior slayer creatures", "Defeat 75 Superior slayer monsters.", "General", TaskDifficulty.MEDIUM, "Slayer 5 + Bigger and Badder or Tier 3 Relic", "Superior");

        // -- Defeat NPCs --
        defeatNpc("Defeat a Steel Dragon on Karamja", "Defeat a Steel Dragon on Karamja.", "Karamja", TaskDifficulty.MEDIUM, "", "Steel dragon");
        defeatNpc("Defeat 150 Lizardmen Shaman", "Help the Shayzien House by killing 150 Lizardmen shamans.", "General", TaskDifficulty.MEDIUM, "", "Lizardman shaman");
        defeatNpc("Defeat a Black Dragon in Tirannwn", "Defeat a Black Dragon in Tirannwn.", "Tirannwn", TaskDifficulty.MEDIUM, "", "Black dragon");

        // -- Fishing --
        catchItem("Catch 10 Cod", "Catch 10 Cod.", "General", TaskDifficulty.MEDIUM, "Fishing 23", 10, "Raw cod");
        catchItem("Catch 20 mackerel", "Catch 20 Raw mackerel whilst Fishing.", "General", TaskDifficulty.MEDIUM, "Fishing 16", 20, "Raw mackerel");
        catchItem("Catch 50 Tuna", "Catch 50 Tuna.", "General", TaskDifficulty.MEDIUM, "Fishing 35", 50, "Raw tuna");
        catchItem("Catch 75 Trout", "Catch 75 Raw Trout whilst Fishing.", "General", TaskDifficulty.MEDIUM, "Fishing 20", 75, "Raw trout");
        catchItem("Catch 75 Lobsters", "Catch 75 Lobsters.", "General", TaskDifficulty.MEDIUM, "Fishing 40", 75, "Raw lobster");
        catchItem("Catch 100 Tuna", "Catch 100 Tuna.", "General", TaskDifficulty.MEDIUM, "Fishing 35", 100, "Raw tuna");
        catchItem("Catch 100 Swordfish", "Catch 100 Swordfish.", "General", TaskDifficulty.MEDIUM, "Fishing 50", 100, "Raw swordfish");

        // -- Cooking --
        cookItem("Cook 50 Tuna", "Cook 50 Raw Tuna.", "General", TaskDifficulty.MEDIUM, "Cooking 35", 50, "Raw tuna");
        cookItem("Cook 100 Swordfish", "Cook 100 Swordfish.", "General", TaskDifficulty.MEDIUM, "Cooking 45", 100, "Raw swordfish");
        addTask("Butter a potato", "Make a potato with butter.", "General", TaskDifficulty.MEDIUM, TaskType.COOK_ITEM, "Cooking 39", 1, "Potato with butter");

        // -- Woodcutting --
        chopItem("Chop 100 Willow Logs", "Chop 100 Willow Logs from Willow Trees.", "General", TaskDifficulty.MEDIUM, "Woodcutting 30", 100, "Willow logs");
        chopItem("Chop 50 Maple Logs", "Chop 50 Maple Logs.", "General", TaskDifficulty.MEDIUM, "Woodcutting 45", 50, "Maple logs");
        addTask("Chop some Rising Roots", "Chop some Rising Roots spawned via Forestry.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);

        // -- Firemaking --
        burnItem("Burn 100 Willow Logs", "Burn 100 Willow Logs.", "General", TaskDifficulty.MEDIUM, "Firemaking 30", 100, "Willow logs");
        burnItem("Burn 25 Maple Logs", "Burn 25 Maple Logs.", "General", TaskDifficulty.MEDIUM, "Firemaking 45", 25, "Maple logs");
        addTask("Burn Some Coloured Logs", "Burn some logs that have been coloured with a firelighter.", "General", TaskDifficulty.MEDIUM, TaskType.BURN_ITEM, "", 1, "Firelighter");

        // -- Herblore --
        cleanItem("Clean 50 Grimy Ranarr Weed", "Clean 50 Grimy Ranarr Weed.", "General", TaskDifficulty.MEDIUM, "Herblore 25", 50, "Grimy ranarr weed");
        cleanItem("Clean 50 Grimy Cadantine", "Clean 50 Grimy Cadantine.", "General", TaskDifficulty.MEDIUM, "Herblore 65", 50, "Grimy cadantine");
        cleanItem("Clean a Grimy Avantoe", "Clean a Grimy Avantoe.", "General", TaskDifficulty.MEDIUM, "Herblore 48", 1, "Grimy avantoe");

        // -- Magic --
        addTask("Cast an Earth Blast Spell", "Cast an Earth Blast Spell.", "General", TaskDifficulty.MEDIUM, TaskType.SPELL, "Magic 53", 1);
        addTask("Cast Low Level Alchemy", "Cast the Low Level Alchemy spell.", "General", TaskDifficulty.MEDIUM, TaskType.SPELL, "Magic 21", 1);
        addTask("Convert an item into at least 500 coins", "Cast High Level Alchemy to convert an item into 500+ coins.", "General", TaskDifficulty.MEDIUM, TaskType.SPELL, "Magic 55", 1);

        // -- Crafting --
        craftItem("Craft 200 Essence Into Runes", "Use Runecrafting Altars to craft 200 essence into runes.", "General", TaskDifficulty.MEDIUM, "", "Rune essence", "Pure essence");
        craftItem("Craft a Sapphire Amulet", "Craft a Sapphire amulet.", "General", TaskDifficulty.MEDIUM, "Crafting 24",
            "Sapphire", "Gold bar", "Amulet mould");
        craftItem("Craft an Emerald Ring", "Craft an Emerald Ring.", "General", TaskDifficulty.MEDIUM, "Crafting 27",
            "Emerald", "Gold bar", "Ring mould");
        addTask("Craft 20 Silver items", "Craft 20 Silver items using Silver bars.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "", 20, "Silver bar");
        addTask("Craft Any Combination Rune", "Use a Runecrafting Altar to craft any type of combination rune.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Runecraft 6+", 1, "Pure essence", "Rune essence");

        // -- Equip sets --
        equipItem("Equip a Full Bronze Set", "Equip a Bronze Platebody, Bronze Full Helm and Bronze Platelegs/Plateskirt.", "General", TaskDifficulty.MEDIUM, "",
            "Bronze platebody", "Bronze full helm", "Bronze platelegs", "Bronze plateskirt");
        equipItem("Equip a Full Adamant Set", "Equip an Adamant Platebody, Adamant Full Helm and Adamant Platelegs/Plateskirt.", "General", TaskDifficulty.MEDIUM, "Defence 30",
            "Adamant platebody", "Adamant full helm", "Adamant platelegs", "Adamant plateskirt");
        equipItem("Equip a Full Blue Dragonhide Set", "Equip Blue d'hide body, chaps, and vambraces.", "General", TaskDifficulty.MEDIUM, "Ranged 50, Defence 40",
            "Blue d'hide body", "Blue d'hide chaps", "Blue d'hide vambs");
        equipItem("Equip a Full Red Dragonhide Set", "Equip Red d'hide body, chaps, and vambraces.", "General", TaskDifficulty.MEDIUM, "Ranged 60, Defence 40",
            "Red d'hide body", "Red d'hide chaps", "Red d'hide vambs");
        equipItem("Equip a Mithril Weapon", "Equip any Mithril weapon.", "General", TaskDifficulty.MEDIUM, "Attack 20",
            "Mithril scimitar", "Mithril sword", "Mithril longsword", "Mithril battleaxe", "Mithril mace", "Mithril dagger", "Mithril warhammer");

        // -- Hunter --
        catchItem("Catch a Butterfly", "Catch any butterfly.", "General", TaskDifficulty.MEDIUM, "Hunter 15", "Ruby harvest", "Sapphire glacialis", "Snowy knight", "Black warlock");
        catchItem("Catch a Swamp Lizard or Salamander", "Catch either a Swamp Lizard or any kind of Salamander.", "General", TaskDifficulty.MEDIUM, "Hunter 29+", "Swamp lizard", "Orange salamander", "Red salamander", "Black salamander");
        catchItem("Catch 50 Implings in Puro-Puro", "Catch 50 Implings in Puro-Puro.", "General", TaskDifficulty.MEDIUM, "Hunter 17", 50, "Baby impling", "Young impling", "Gourmet impling", "Earth impling", "Essence impling", "Eclectic impling");

        // -- Farming --
        addTask("Check a grown Tree", "Check the health of any regular Tree you've grown.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 15", 1);
        addTask("Check a grown Fruit Tree", "Check the health of any Fruit Tree you've grown.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 27", 1);
        addTask("Churn some butter", "Use a churn to make some butter.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Cooking 38", 1, "Butter");

        // -- Construction --
        addTask("Build a Room in Your Player Owned House", "Build a room in your Player Owned House.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Build an Oak Larder", "Build an Oak Larder in a Kitchen.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Construction 33", 1);
        addTask("Build a Mahogany Portal", "Build a Mahogany Portal in a Portal Chamber.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Construction 65", 1);

        // -- Prayer --
        addTask("Bury Some Wyvern or Dragon Bones", "Bury either some Wyvern Bones or some Dragon Bones.", "General", TaskDifficulty.MEDIUM, TaskType.PRAYER, "", 1, "Wyvern bones", "Dragon bones");
        addTask("Eat some Purple Sweets", "Eat some Purple Sweets.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Purple sweets");

        // -- Random events / Forestry --
        addTask("Complete the Evil Bob random event", "Complete the Evil Bob random event.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Maze random event", "Complete the Maze random event.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Pillory random event", "Complete the Pillory random event.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Pinball random event", "Complete the Pinball random event.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Postie Pete random event", "Complete the Postie Pete random event.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Prison Pete random event", "Complete the Prison Pete random event.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Surprise Exam random event", "Complete the Surprise Exam random event.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Flowering Bush event", "Complete the Flowering Bush event spawned via Forestry.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Pheasant Forestry Event", "Complete the Pheasant event spawned via Forestry.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Ritual Forestry Event", "Complete the Ritual event spawned via Forestry.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Struggling Sapling event", "Complete the Struggling Sapling event spawned via Forestry.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);

        // =====================================================================
        // MEDIUM TASKS — additional General combat & skilling
        // =====================================================================
        defeatNpc("Defeat 3 chickens in 6 seconds", "Defeat 3 chickens in 6 seconds.", "General", TaskDifficulty.MEDIUM, "", "Chicken");
        defeatNpc("Defeat 5 Bunnies", "Defeat 5 Bunnies.", "General", TaskDifficulty.MEDIUM, "", "Rabbit", "Bunny");
        defeatNpc("Defeat 5 creatures with a mace", "Defeat 5 creatures with a mace as the final hit.", "General", TaskDifficulty.MEDIUM, "", "Mace");
        defeatNpc("Defeat a Scorpion with a Mithril Spear", "Defeat a Scorpion with a mithril spear as the final hit.", "General", TaskDifficulty.MEDIUM, "Attack 20", "Scorpion");
        defeatNpc("Defeat 10 Superior slayer creatures", "Defeat 10 Superior slayer creatures.", "General", TaskDifficulty.MEDIUM, "Slayer 5 + Bigger and Badder or Tier 3 Relic", "Superior");
        addTask("Slay 250 Creatures", "Slay 250 creatures whilst on a Slayer Task.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 250);

        // Level / base level
        addTask("Reach Base Level 5", "Reach level 5 in every skill.", "General", TaskDifficulty.MEDIUM, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Base Level 10", "Reach level 10 in every skill.", "General", TaskDifficulty.MEDIUM, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Base Level 20", "Reach level 20 in every skill.", "General", TaskDifficulty.MEDIUM, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Base Level 30", "Reach level 30 in every skill.", "General", TaskDifficulty.MEDIUM, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Combat Level 50", "Reach Combat Level 50. Earns one Demonic Pact.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Reach Combat Level 75", "Reach Combat Level 75.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Reach Total Level 100", "Reach a Total Level of 100.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Reach Total Level 250", "Reach a Total Level of 250.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Reach Total Level 666", "Reach Total Level 666.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Reach Total Level 750", "Reach a Total Level of 750.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Reach a Prayer Bonus of 15", "Equip enough items to reach a Prayer bonus of 15 or more.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);

        // Clue log fills / uniques
        addTask("Fill 5 Easy Clue Collection Log Slots", "Fill 5 slots in the Easy Clue section of the Collection Log.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 5);
        addTask("Fill 20 Easy Clue Collection Log Slots", "Fill 20 slots in the Easy Clue section of the Collection Log.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 20);
        addTask("Fill 5 Medium Clue Collection Log Slots", "Fill 5 slots in the Medium Clue section of the Collection Log.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 5);
        addTask("Fill 20 Medium Clue Collection Log Slots", "Fill 20 slots in the Medium Clue section of the Collection Log.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 20);
        addTask("Fill 3 Hard Clue Collection Log Slots", "Fill 3 slots in the Hard Clue section of the Collection Log.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 3);
        addTask("Fill 15 Hard Clue Collection Log Slots", "Fill 15 slots in the Hard Clue section of the Collection Log.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 15);
        addTask("Fill 3 Elite Clue Collection Log Slots", "Fill 3 slots in the Elite Clue section of the Collection Log.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 3);
        addTask("Gain a Unique Item From an Easy Clue", "Gain a unique item from an Easy Clue Scroll Reward Casket.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Gain 10 Unique Items From Easy Clues", "Gain 10 unique items from Easy Clue Scroll Reward Caskets.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 10);
        addTask("Gain 35 Unique Items From Easy Clues", "Gain 35 unique items from Easy Clue Scroll Reward Caskets.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 35);
        addTask("Gain a Unique Item From a Medium Clue", "Gain a unique item from a Medium Clue Scroll Reward Casket.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Gain 10 Unique Items From Medium Clues", "Gain 10 unique items from Medium Clue Scroll Reward Caskets.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 10);
        addTask("Gain a Unique Item From a Hard Clue", "Gain a unique item from a Hard Clue Scroll Reward Casket.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Gain 5 Unique Items From Hard Clues", "Gain 5 unique items from Hard Clue Scroll Reward Caskets.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 5);
        addTask("Gain 20 Unique Items From Hard Clues", "Gain 20 unique items from Hard Clue Scroll Reward Caskets.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 20);
        addTask("Gain a Unique Item From a Master Clue", "Gain a unique item from a Master Clue Scroll Reward Casket.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);

        // Hunter / Forestry / Farming
        catchItem("Catch a Sabre-toothed Kebbit", "Catch a Sabre-toothed Kebbit.", "General", TaskDifficulty.MEDIUM, "Hunter 51", "Sabre-toothed kebbit");
        catchItem("Catch a Snowy Knight", "Catch a Snowy Knight.", "General", TaskDifficulty.MEDIUM, "Hunter 35", "Snowy knight");
        addTask("Snare a Bird 20 times", "Snare a Bird 20 times.", "General", TaskDifficulty.MEDIUM, TaskType.CATCH_ITEM, "", 20, "Crimson swift", "Golden warbler", "Copper longtail", "Cerulean twitch", "Tropical wagtail");
        addTask("Harvest a Ranarr Weed", "Harvest a Ranarr Weed from any Herb patch.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 32", 1, "Ranarr seed", "Grimy ranarr weed");
        addTask("Have a Leprechaun send something to the bank", "Have the Forestry leprechaun send something to your bank.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Give an Entling a haircut", "Give an entling a haircut via Forestry.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Entling");

        // Cooking / Herblore
        addTask("Successfully Cook 5 Pieces of Food", "Cook 5 pieces of food in a row without burning them.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 5);
        addTask("Eat a piece of food that restores at least 6 hitpoints", "Eat a piece of food that restores at least 6 hitpoints.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Make a Meat Pizza", "Make a meat pizza.", "General", TaskDifficulty.MEDIUM, TaskType.COOK_ITEM, "Cooking 45", 1, "Incomplete pizza", "Cooked meat");
        addTask("Make some Flour", "Make some Flour in a windmill.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Grain");
        addTask("Make 30 Prayer Potions", "Make 30 Prayer Potions.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Herblore 38", 30, "Ranarr potion (unf)", "Snape grass");
        addTask("Make 20 Stamina Potions", "Make 20 Stamina Potions.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Herblore 77", 20, "Super energy(3)", "Amylase crystal");
        addTask("Make a 4 dose potion", "Make any 4 dose potion using an Amulet of Chemistry.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Crafting 34, Magic 27", 1, "Amulet of chemistry");
        addTask("Trade a herb with Jekyll", "Trade a herb with Jekyll for a potion.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Upset the Sandwich lady", "Make the Sandwich lady quite cross with you.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Sandwich lady");

        // Smithing / Mining
        craftItem("Smelt a Steel Bar", "Use a Furnace to smelt a Steel Bar.", "General", TaskDifficulty.MEDIUM, "Smithing 30", "Iron ore", "Coal");
        addTask("Smith 150 Iron Arrowtips", "Use an Anvil to smith 150 Iron Arrowtips.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Smithing 20", 150, "Iron bar");
        addTask("Smith 10 Steel bolts (unf)", "Use an Anvil to smith 10 Steel bolts (unf).", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Smithing 33", 10, "Steel bar");
        addTask("Smith 250 Mithril bolts (unf)", "Use an Anvil to smith 250 Mithril bolts (unf).", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Smithing 53", 250, "Mithril bar");
        addTask("Smith a Steel 2h sword", "Use an Anvil to smith a Steel 2h sword.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Smithing 44", 1, "Steel bar");
        mineItem("Mine 10 Silver", "Mine 10 silver ore.", "General", TaskDifficulty.MEDIUM, "Mining 20", 10, "Silver ore");
        mineItem("Mine 50 Iron Ore", "Mine 50 iron ore.", "General", TaskDifficulty.MEDIUM, "Mining 15", 50, "Iron ore");
        mineItem("Mine 50 Mithril Ore", "Mine 50 mithril ore.", "General", TaskDifficulty.MEDIUM, "Mining 55", 50, "Mithril ore");
        addTask("Mine some Ore With a Rune Pickaxe", "Mine any ore using a Rune Pickaxe.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Mining 41", 1, "Rune pickaxe");
        addTask("Obtain a Clue Geode While Mining", "Obtain any kind of clue geode whilst Mining a rock.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Clue geode");
        addTask("Obtain a Gem While Mining", "Obtain any kind of gem whilst Mining a rock.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Fill a Bucket With Supercompost", "Fill a Bucket with Supercompost from a Compost Bin.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Supercompost");

        // Fletching
        addTask("Fletch 1000 arrow shafts", "Fletch 1000 arrow shafts.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "", 1000, "Logs", "Knife");
        addTask("Fletch 150 Iron Arrows", "Fletch 150 Iron Arrows.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Fletching 15", 150, "Headless arrow", "Iron arrowtips");
        addTask("Fletch 25 Oak Stocks", "Fletch 25 Oak Stocks.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Fletching 24", 25, "Oak logs");
        addTask("Fletch a Willow Shortbow (u)", "Fletch a Willow Shortbow (u).", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Fletching 35", 1, "Willow logs", "Knife");
        addTask("Fletch 50 Willow longbow (u)", "Fletch 50 Willow longbow (u).", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Fletching 40", 50, "Willow logs");
        addTask("Fletch 25 Maple longbow (u)", "Fletch 25 Maple longbow (u).", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Fletching 55", 25, "Maple logs");
        addTask("Fletch some Broad Arrows or Bolts", "Fletch either some Broad Arrows or some Broad Bolts.", "General", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Fletching 52 + Broader Fletching", 1, "Broad arrowheads", "Unfinished broad bolts");

        // Runecraft / Magic / Thieving
        addTask("Teleport Using Law Runes", "Cast any teleport spell that uses Law Runes.", "General", TaskDifficulty.MEDIUM, TaskType.SPELL, "Magic 40", 1, "Law rune");
        addTask("Pickpocket a Master Farmer", "Successfully pickpocket from a Master farmer.", "General", TaskDifficulty.MEDIUM, TaskType.DEFEAT_NPC, "Thieving 38", 1, "Master farmer");
        addTask("Obtain 800 Coins From Coin Pouches At Once", "Open a stack of Coin Pouches and obtain at least 800 Coins.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 40", 1, "Coin pouch");
        addTask("Open 28 Coin Pouches At Once", "Open 28 Coin Pouches at once.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 28, "Coin pouch");

        // Equipment sets
        equipItem("Equip a Rune Weapon", "Equip any Rune weapon.", "General", TaskDifficulty.MEDIUM, "Attack 40",
            "Rune scimitar", "Rune sword", "Rune longsword", "Rune battleaxe", "Rune mace", "Rune dagger", "Rune warhammer", "Rune 2h sword", "Rune spear", "Rune halberd", "Rune pickaxe", "Rune axe", "Rune claws");
        equipItem("Equip an Adamant Weapon", "Equip any Adamant weapon.", "General", TaskDifficulty.MEDIUM, "Attack 30",
            "Adamant scimitar", "Adamant sword", "Adamant longsword", "Adamant battleaxe", "Adamant mace", "Adamant dagger", "Adamant warhammer", "Adamant 2h sword");
        equipItem("Equip some Black armour", "Equip either a Black Platebody, Black Platelegs or a Black Full Helm.", "General", TaskDifficulty.MEDIUM, "Defence 10", "Black platebody", "Black platelegs", "Black full helm");
        equipItem("Equip some Steel armour", "Equip either a Steel Platebody, Steel Platelegs or a Steel Full Helm.", "General", TaskDifficulty.MEDIUM, "Defence 5", "Steel platebody", "Steel platelegs", "Steel full helm");
        equipItem("Equip a Piece of a Mystic Set", "Equip any piece of any Mystic robe set.", "General", TaskDifficulty.MEDIUM, "Magic 40, Defence 20",
            "Mystic hat", "Mystic robe top", "Mystic robe bottom", "Mystic gloves", "Mystic boots");
        equipItem("Equip an Elemental Battlestaff or Mystic Staff", "Equip either an elemental battlestaff or an elemental mystic staff.", "General", TaskDifficulty.MEDIUM, "Magic 30, Attack 30",
            "Air battlestaff", "Water battlestaff", "Earth battlestaff", "Fire battlestaff",
            "Mystic air staff", "Mystic water staff", "Mystic earth staff", "Mystic fire staff");
        equipItem("Equip a Trimmed Amulet", "Equip a Trimmed Amulet.", "General", TaskDifficulty.MEDIUM, "",
            "Amulet of power (t)", "Amulet of glory (t)", "Amulet of magic (t)", "Amulet of defence (t)", "Amulet of strength (t)", "Amulet of accuracy (t)");
        equipItem("Equip a Willow Shield", "Equip a Willow Shield.", "General", TaskDifficulty.MEDIUM, "Defence 30, Fletching 42", "Willow shield");
        equipItem("Equip a Yew Shortbow", "Equip a Yew Shortbow.", "General", TaskDifficulty.MEDIUM, "Ranged 40", "Yew shortbow");
        equipItem("Equip a piece of Beekeeper's Outfit", "Equip a piece of Beekeeper's Outfit.", "General", TaskDifficulty.MEDIUM, "",
            "Beekeeper's hat", "Beekeeper's top", "Beekeeper's legs", "Beekeeper's gloves");
        equipItem("Equip a piece of Camouflage outfit", "Equip a piece of Camouflage outfit.", "General", TaskDifficulty.MEDIUM, "",
            "Camo helmet", "Camo top", "Camo bottoms");
        equipItem("Equip a piece of Mime Outfit", "Equip a piece of Mime Outfit.", "General", TaskDifficulty.MEDIUM, "",
            "Mime mask", "Mime top", "Mime legs", "Mime gloves", "Mime boots");
        equipItem("Equip a piece of Zombie Outfit", "Equip a piece of Zombie Outfit.", "General", TaskDifficulty.MEDIUM, "",
            "Zombie mask", "Zombie shirt", "Zombie trousers", "Zombie gloves", "Zombie boots");
        equipItem("Equip the Forestry Basket", "Equip the Forestry Basket.", "General", TaskDifficulty.MEDIUM, "Woodcutting 75, Smithing 75", "Forestry basket");

        // Misc general
        addTask("Redecorate your player-owned house", "Redecorate your player-owned house.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Construction 10", 1);
        addTask("Sacrifice something to Death's Coffer", "Sacrifice something to Death's Coffer.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Scrape some Blue Dragonhide", "Scrape some blue dragonhide.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Scaly blue dragonhide");
        addTask("Get a Gem from a Gorak", "Obtain a gem drop from a Gorak.", "General", TaskDifficulty.MEDIUM, TaskType.DEFEAT_NPC, "", 1, "Gorak");
        addTask("Obtain a Kebab from a random event", "Obtain a Kebab from any random event.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Kebab");
        addTask("Obtain an old boot from a fishing spot", "Obtain an old boot from a fishing spot.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Old boot");
        addTask("Open the Mystery Box", "Open the mystery box.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Mystery box");
        addTask("Land a hoop on a stick", "Successfully land a hoop on a stick in the PoH minigame.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Construction 30", 1, "Hoop and stick");
        addTask("Fill a Large Pouch", "Fill a Large Pouch with Essence.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Runecraft 50", 1, "Large pouch");
        addTask("Fill a Medium STASH Unit", "Build a Medium STASH unit and fill it with the relevant items.", "General", TaskDifficulty.MEDIUM, TaskType.MISC, "Construction 42", 1);
        addTask("Successfully pickpocket a Citizen 10 times in a row", "Successfully pickpocket a Citizen 10 times in a row without failing.", "General", TaskDifficulty.MEDIUM, TaskType.DEFEAT_NPC, "", 10, "Citizen");

        // =====================================================================
        // MEDIUM TASKS — Asgarnia
        // =====================================================================
        defeatNpc("Defeat a Black Demon in Asgarnia", "Defeat a Black Demon in Asgarnia.", "Asgarnia", TaskDifficulty.MEDIUM, "", "Black demon");
        defeatNpc("Defeat a Blue Dragon in Asgarnia", "Defeat a Blue Dragon in Asgarnia.", "Asgarnia", TaskDifficulty.MEDIUM, "", "Blue dragon");
        defeatNpc("Defeat a Skeletal Wyvern", "Defeat a Skeletal Wyvern in the Asgarnian Ice Dungeon.", "Asgarnia", TaskDifficulty.MEDIUM, "Slayer 72", "Skeletal wyvern");
        defeatNpc("Defeat the Giant Mole", "Defeat the Giant Mole beneath Falador.", "Asgarnia", TaskDifficulty.MEDIUM, "", "Giant Mole");
        defeatNpc("Defeat the Giant Mole 50 Times", "Defeat the Giant Mole beneath Falador 50 times.", "Asgarnia", TaskDifficulty.MEDIUM, "", "Giant Mole");
        defeatNpc("Defeat the Giant Mole 150 Times", "Defeat the Giant Mole beneath Falador 150 times.", "Asgarnia", TaskDifficulty.MEDIUM, "", "Giant Mole");
        defeatNpc("Defeat the Royal Titans", "Defeat the Royal Titans.", "Asgarnia", TaskDifficulty.MEDIUM, "", "Royal Titans");
        defeatNpc("Defeat the Royal Titans 50 times", "Defeat the Royal Titans 50 times.", "Asgarnia", TaskDifficulty.MEDIUM, "", "Royal Titans");
        defeatNpc("Defeat 5 Spinners", "Defeat 5 Spinners in Pest Control.", "Asgarnia", TaskDifficulty.MEDIUM, "Combat level 40", "Spinner");
        defeatNpc("Defeat Some Animated Rune Armour", "Defeat some Animated Rune Armour in the Warriors' Guild.", "Asgarnia", TaskDifficulty.MEDIUM, "", "Animated rune armour");
        equipItem("Equip Amy's Saw", "Equip Amy's Saw from Mahogany Homes.", "Asgarnia", TaskDifficulty.MEDIUM, "", "Amy's saw");
        equipItem("Equip an Imcando Hammer", "Equip an Imcando hammer.", "Asgarnia", TaskDifficulty.MEDIUM, "Mining 14", "Imcando hammer");
        addTask("Charge an Amulet of Glory in the Heroes' Guild", "Charge an Amulet of Glory at the Fountain of Heroes in the Heroes' Guild.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Heroes' Quest", 1, "Amulet of glory");
        addTask("Complete A Porcine of Interest", "Complete A Porcine of Interest quest.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete Witch's Potion", "Complete the Witch's Potion quest.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete a Game of Intermediate Pest Control", "Complete a game of Intermediate Pest Control or higher.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Combat 70", 1);
        addTask("Complete a Game of Veteran Pest Control", "Complete a game of Veteran Pest Control.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Combat 100", 1);
        addTask("Complete the Easy Falador Diary", "Complete all of the Easy tasks in the Falador Achievement Diary.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Medium Falador Diary", "Complete all of the Medium tasks in the Falador Achievement Diary.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Falador Agility Course", "Complete a lap of the Falador Rooftop Agility Course.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Agility 50", 1);
        addTask("Consume a Saradomin's Light", "Consume a Saradomin's Light.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Agility 70", 1, "Saradomin's light");
        addTask("Craft a Body Rune", "Craft a Body Rune from Essence at the Body Altar.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Runecraft 20", 1, "Rune essence", "Pure essence");
        addTask("Enter the Crafting Guild", "Enter the Crafting Guild.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Crafting 40", 1);
        addTask("Hang a Painting of a Watermill", "Hang a Painting of Lumbridge Watermill in your PoH.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Construction 44", 1);
        addTask("Harvest Any Herb at the Troll Stronghold", "Harvest any herb you've grown at the Troll Stronghold.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 9", 1);
        addTask("Make 50 Ancient Brews", "Make 50 Ancient Brews.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Herblore 85", 50, "Dwarf weed potion (unf)");
        addTask("Obtain 20 Golden Nuggets", "Obtain 20 Golden Nuggets from the Motherlode Mine beneath Falador.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Mining 30", 20, "Golden nugget");
        addTask("Obtain the Plank Sack", "Obtain the Plank Sack from Mahogany Homes.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Plank sack");
        addTask("Open an Ornate Lockbox", "Open an Ornate Lockbox from the Camdozaal Vault.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Ornate lockbox");
        addTask("Open the Crystal Chest", "Open the Crystal Chest in Taverley.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Crystal chest");
        addTask("Set Up a Dwarf Cannon", "Set Up a Dwarf Cannon.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Cannon base");
        addTask("Teleport with a Giantsoul amulet", "Use the Giantsoul amulet to teleport to the Royal Titans.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Giantsoul amulet");
        addTask("Throw a Shot Put 12 yards", "Throw a Shot Put 12 yards in the Warriors Guild.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "Attack 99 or Strength 99 or combined 130", 1);
        addTask("Turn in 100 Mole Claws to Wyson the Gardener", "Turn in 100 Mole Claws to Wyson the Gardener in Falador.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 100, "Mole claw");
        addTask("Unlock a Gate in Taverley Dungeon", "Unlock a gate in Taverley Dungeon using the Dusty Key.", "Asgarnia", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Dusty key");

        // =====================================================================
        // MEDIUM TASKS — Kharidian Desert
        // =====================================================================
        addTask("Cast Ice Burst", "Cast the Ice Burst spell.", "Desert", TaskDifficulty.MEDIUM, TaskType.SPELL, "Magic 70", 1);
        addTask("Cast Ice Rush", "Cast the Ice Rush spell.", "Desert", TaskDifficulty.MEDIUM, TaskType.SPELL, "Magic 58", 1);
        catchItem("Catch 30 Orange Salamanders", "Catch 30 Orange Salamanders at Uzer.", "Desert", TaskDifficulty.MEDIUM, "Hunter 47", 30, "Orange salamander");
        addTask("Commune a Pharoah's Sceptre to the Necropolis", "Commune a Pharaoh's sceptre to the Necropolis.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 21 + Beneath Cursed Sands", 1, "Pharaoh's sceptre");
        addTask("Complete Shadow of the Storm", "Complete the Shadow of the Storm quest.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Crafting 30", 1);
        addTask("Complete Sleeping Giants", "Complete Sleeping Giants quest.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Smithing 15", 1);
        addTask("Complete Spirits of the Elid", "Complete the Spirits of the Elid quest.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Magic 33, Ranged 37, Mining 37, Thieving 37", 1);
        addTask("Complete The Golem", "Complete The Golem quest.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Crafting 20, Thieving 25", 1);
        addTask("Complete the Easy Desert Diary", "Complete all of the Easy tasks in the Desert Achievement Diary.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Medium Desert Diary", "Complete all of the Medium tasks in the Desert Achievement Diary.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Craft a Lava Rune at the Fire Altar", "Craft a Lava Rune at the Fire Altar.", "Desert", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Runecraft 23", 1, "Pure essence", "Rune essence", "Earth rune");
        addTask("Craft some Pottery In Sophanem", "Craft some Pottery In Sophanem.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Soft clay");
        addTask("Create the Divine Rune pouch", "Create the Divine Rune pouch.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Crafting 75 + Beneath Cursed Sands", 1, "Rune pouch");
        defeatNpc("Defeat 30 Bandits", "Defeat 30 Bandits.", "Desert", TaskDifficulty.MEDIUM, "", "Bandit");
        defeatNpc("Defeat Tempoross 10 times", "Help the Spirit Anglers defeat Tempoross 10 times.", "Desert", TaskDifficulty.MEDIUM, "Fishing 35", "Tempoross");
        defeatNpc("Defeat a Kalphite Guardian", "Defeat a Kalphite Guardian in the Kharidian Desert.", "Desert", TaskDifficulty.MEDIUM, "", "Kalphite guardian");
        defeatNpc("Defeat a Kalphite with the Keris Partisan", "Defeat a Kalphite with the Keris Partisan.", "Desert", TaskDifficulty.MEDIUM, "Attack 65 + Beneath Cursed Sands", "Kalphite", "Kalphite worker", "Kalphite soldier");
        defeatNpc("Defeat a Scarab Mage", "Defeat a Scarab Mage.", "Desert", TaskDifficulty.MEDIUM, "Light source + Contact!/Beneath Cursed Sands", "Scarab mage");
        addTask("Drink Kovac's grog", "Drink some of Kovac's grog, purchased from the Giants' Foundry Reward Shop.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Sleeping Giants + 300 Foundry Rep", 1, "Kovac's grog");
        equipItem("Equip the Colossal Blade", "Equip the Colossal Blade from Giant's Foundry.", "Desert", TaskDifficulty.MEDIUM, "Smithing 15, Attack 60", "Colossal blade");
        equipItem("Equip the Tome of Water", "Equip the Tome of Water from Tempoross.", "Desert", TaskDifficulty.MEDIUM, "Fishing 35, Magic 50", "Tome of water");
        addTask("Giants' Foundry 10 handins", "Hand in 10 successful swords to Kovac.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Sleeping Giants", 10);
        addTask("Giants' Foundry 50 quality sword", "Hand in a sword with at least 50 quality in Giants' Foundry.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Sleeping Giants", 1);
        addTask("Guardians of the Rift 1 Rift closed", "Close the Rift in the Temple of the Eye.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Runecraft 27", 1);
        addTask("Guardians of the Rift 10 Rifts closed", "Close the Rift in the Temple of the Eye 10 times.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Runecraft 27", 10);
        addTask("Make a Combat Potion", "Make a Combat Potion.", "Desert", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Herblore 36", 1, "Harralander potion (unf)", "Goat horn dust");
        addTask("Mine 15 Granite in the Necropolis", "Mine 15 Granite in the Necropolis.", "Desert", TaskDifficulty.MEDIUM, TaskType.MINE_ITEM, "Mining 45 + Beneath Cursed Sands", 15, "Granite (500g)", "Granite (2kg)", "Granite (5kg)");
        mineItem("Mine 30 Chunks of Granite", "Mine 30 chunks of Granite at the Kharidian Desert Quarry.", "Desert", TaskDifficulty.MEDIUM, "Mining 45", 30, "Granite (500g)", "Granite (2kg)", "Granite (5kg)");
        addTask("Obtain the Big Harpoonfish", "Obtain the Big Harpoonfish from Tempoross.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Fishing 35", 1, "Big harpoonfish");
        addTask("Offend some bandits", "Offend some bandits in the Desert Bandit Camp.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Bandit");
        addTask("Pick a Autumn Sq'irk", "Pick a Autumn Sq'irk in the Sorceress's Garden.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 45", 1, "Autumn sq'irk");
        addTask("Pick a Spring Sq'irk", "Pick a Spring Sq'irk in the Sorceress's Garden.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 25", 1, "Spring sq'irk");
        addTask("Pickpocket a Bandit in the Bandit Camp", "Pickpocket a Bandit in the Kharidian Desert's Bandit Camp.", "Desert", TaskDifficulty.MEDIUM, TaskType.DEFEAT_NPC, "Thieving 53", 1, "Bandit");
        addTask("Pray at the Elidinis Statuette", "Pray at the Elidinis Statuette in Nardah.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Spirits of the Elid", 1);
        addTask("Room 4 of Pyramid Plunder", "Search the Golden Chest in Room 4 of Pyramid Plunder in Sophanem.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 51", 1);
        addTask("Room 5 of Pyramid Plunder", "Search the Golden Chest in Room 5 of Pyramid Plunder in Sophanem.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 61", 1);
        addTask("Room 6 of Pyramid Plunder", "Search the Golden Chest in Room 6 of Pyramid Plunder in Sophanem.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 71", 1);
        addTask("Turn in 10 Spring Sq'irkjuices to Osman", "Turn in 10 Spring Sq'irkjuices to Osman in Al Kharid in one go.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 25", 10, "Spring sq'irkjuice");
        addTask("Turn in a Pyramid Top to Simon Templeton", "Turn in a Pyramid Top to Simon Templeton at the Agility Pyramid.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "Agility 30", 1, "Pyramid top");
        addTask("Turn in a Winter Sq'irkjuice to Osman", "Turn in a Winter Sq'irkjuice to Osman in Al Kharid.", "Desert", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Winter sq'irkjuice");

        // =====================================================================
        // MEDIUM TASKS — Fremennik
        // =====================================================================
        defeatNpc("Defeat a Brine Rat", "Defeat a brine rat.", "Fremennik", TaskDifficulty.MEDIUM, "Slayer 47", "Brine rat");
        defeatNpc("Defeat a Dagannoth in the Fremennik Province", "Defeat a dagannoth in the Fremennik Province.", "Fremennik", TaskDifficulty.MEDIUM, "", "Dagannoth");
        defeatNpc("Defeat a Jelly in the Fremennik Province", "Defeat a jelly in the Fremennik Province.", "Fremennik", TaskDifficulty.MEDIUM, "Slayer 52", "Jelly");
        defeatNpc("Defeat a Kurask in the Fremennik Province", "Defeat a kurask in the Fremennik Province.", "Fremennik", TaskDifficulty.MEDIUM, "Slayer 70", "Kurask");
        defeatNpc("Defeat a Suqah", "Defeat a suqah.", "Fremennik", TaskDifficulty.MEDIUM, "", "Suqah");
        defeatNpc("Defeat a Troll in the Fremennik Province", "Defeat a troll in the Fremennik Province.", "Fremennik", TaskDifficulty.MEDIUM, "", "Troll");
        defeatNpc("Defeat a Turoth in the Fremennik Province", "Defeat a turoth in the Fremennik Province.", "Fremennik", TaskDifficulty.MEDIUM, "Slayer 55", "Turoth");
        defeatNpc("Defeat a Wallasalki", "Defeat a wallasaki.", "Fremennik", TaskDifficulty.MEDIUM, "", "Wallasalki");
        addTask("Complete Royal Trouble", "Complete the Royal Trouble quest.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "Agility 40, Slayer 40", 1);
        addTask("Complete Throne of Miscellania", "Complete the Throne of Miscellania quest.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Easy Fremennik Diary", "Complete all of the Easy tasks in the Fremennik Achievement Diary.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Medium Fremennik Diary", "Complete all of the Medium tasks in the Fremennik Achievement Diary.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Penguin Agility Course", "Complete a lap of the Penguin Agility Course.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "Agility 30, Construction 34", 1);
        addTask("Craft 50 Astral Runes", "Craft 50 Astral Runes from Essence at the Astral Altar.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Runecraft 40", 50, "Pure essence", "Rune essence");
        defeatNpc("Defeat 8 penguins within 5 seconds", "Defeat 8 penguins within 5 seconds.", "Fremennik", TaskDifficulty.MEDIUM, "", "Penguin");
        equipItem("Equip a Damaged God book", "Equip a damaged god book.", "Fremennik", TaskDifficulty.MEDIUM, "", "Damaged book");
        equipItem("Equip a Granite Shield in the Fremennik Province", "Equip a granite shield while in the Fremennik Province.", "Fremennik", TaskDifficulty.MEDIUM, "Defence 50, Strength 50", "Granite shield");
        equipItem("Equip a Helm of Neitiznot", "Equip a Helm of Neitiznot.", "Fremennik", TaskDifficulty.MEDIUM, "Defence 55", "Helm of neitiznot");
        equipItem("Equip a full set of Yakhide Armour", "Equip a full set of Yakhide Armour.", "Fremennik", TaskDifficulty.MEDIUM, "Crafting 46, Defence 20", "Yak-hide top", "Yak-hide legs");
        addTask("Fill up 20 buckets of sand in Rellekka", "Fill up 20 buckets of sand in Rellekka.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "", 20, "Bucket of sand");
        addTask("Loot a Lyre", "Defeat someone and obtain their lyre.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Lyre");
        addTask("Open a Frozen Cache", "Open a Frozen Cache.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Frozen cache");
        addTask("Steal a Chisel", "Steal a Chisel from a crafting stall in Keldagrim.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 5", 1, "Chisel");
        addTask("Steal a Cow bell in Rellekka", "Steal a Cow bell in Rellekka.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 15", 1, "Cow bell");
        addTask("Steal a Fish", "Steal some fish from a Fish stall.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 42", 1);
        addTask("Steal a Wooden Stock", "Steal a Wooden Stock from a Crossbow Stall.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 49", 1, "Wooden stock");
        addTask("Unlock Free Use of the Blast Furnace", "Unlock free use of the Keldagrim Blast Furnace by speaking with the Foreman.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "Smithing 60", 1);
        addTask("Use Some Icy Basalt to Teleport to Weiss", "Use some Icy Basalt to teleport to Weiss.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "Mining 72", 1, "Icy basalt");
        addTask("Use the Special Attack of a Dragon Axe", "Use the special attack of a Dragon Axe.", "Fremennik", TaskDifficulty.MEDIUM, TaskType.MISC, "Attack 60", 1, "Dragon axe");

        // =====================================================================
        // MEDIUM TASKS — Kandarin
        // =====================================================================
        catchItem("Catch a Monkfish", "Catch a Monkfish at the Piscatoris Fishing Colony.", "Kandarin", TaskDifficulty.MEDIUM, "Fishing 62", "Raw monkfish");
        catchItem("Catch a Red Salamander", "Catch a Red Salamander outside the Ourania Altar.", "Kandarin", TaskDifficulty.MEDIUM, "Hunter 59", "Red salamander");
        addTask("Complete Fishing Contest", "Complete the Fishing Contest Quest.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Fishing 10", 1);
        addTask("Complete Monk's Friend", "Complete the Monk's Friend quest.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete Sea Slug", "Complete the Sea Slug quest.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Firemaking 30", 1);
        addTask("Complete Tower of Life", "Complete the Tower of Life quest.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Construction 10", 1);
        addTask("Complete a Fishing Trawler Game", "Complete a Fishing Trawler game at Port Khazard.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Fishing 15", 1);
        addTask("Complete the Barbarian Outpost Agility Course", "Complete a lap of the Barbarian Outpost Agility Course.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Agility 35", 1);
        addTask("Complete the Easy Ardougne Diary", "Complete all of the Easy tasks in the Ardougne Achievement Diary.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Easy Kandarin Diary", "Complete all of the Easy tasks in the Kandarin Achievement Diary.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Medium Ardougne Diary", "Complete all of the Medium tasks in the Ardougne Achievement Diary.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Medium Kandarin Diary", "Complete all of the Medium tasks in the Kandarin Achievement Diary.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Observatory Quest", "Complete the Observatory Quest.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        defeatNpc("Defeat a Bloodveld in Kandarin", "Defeat a Bloodveld in Kandarin.", "Kandarin", TaskDifficulty.MEDIUM, "Slayer 50", "Bloodveld");
        defeatNpc("Defeat a Frogeel", "Defeat a Frogeel underneath the Tower of Life.", "Kandarin", TaskDifficulty.MEDIUM, "Tower of Life", "Frogeel");
        defeatNpc("Defeat a Newtroost", "Defeat a Newtroost underneath the Tower of Life.", "Kandarin", TaskDifficulty.MEDIUM, "Tower of Life", "Newtroost");
        defeatNpc("Defeat a Spidine", "Defeat a Spidine underneath the Tower of Life.", "Kandarin", TaskDifficulty.MEDIUM, "Construction 10 + Tower of Life", "Spidine");
        defeatNpc("Defeat a Swordchick", "Defeat a Swordchick underneath the Tower of Life.", "Kandarin", TaskDifficulty.MEDIUM, "Tower of Life", "Swordchick");
        defeatNpc("Defeat a Tortoise With Riders in Kandarin", "Defeat a Tortoise with riders in Kandarin.", "Kandarin", TaskDifficulty.MEDIUM, "", "Tortoise");
        addTask("Enter the Fishing Guild", "Enter the Fishing Guild.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Fishing 68", 1);
        addTask("Enter the Myths' Guild", "Enter the Myths' Guild.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Enter the Ranging Guild", "Enter the Ranging Guild.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Ranged 40", 1);
        addTask("Enter the Wizards' Guild", "Enter the Wizards' Guild in Yanille.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Magic 66", 1);
        equipItem("Equip a Comp Ogre Bow", "Equip a Comp ogre bow.", "Kandarin", TaskDifficulty.MEDIUM, "Fletching 30, Woodcutting 25", "Comp ogre bow");
        equipItem("Equip a Dragon Scimitar", "Equip a Dragon Scimitar.", "Kandarin", TaskDifficulty.MEDIUM, "Attack 60", "Dragon scimitar");
        equipItem("Equip a Marksman Chompy Hat", "Equip a Marksman Chompy Hat.", "Kandarin", TaskDifficulty.MEDIUM, "Fletching 5, Cooking 30, Ranged 30, Crafting 5", "Chompy bird hat (marksman)");
        equipItem("Equip a Monkey Backpack", "Equip a Monkey Backpack.", "Kandarin", TaskDifficulty.MEDIUM, "", "Monkey backpack");
        equipItem("Equip a Spottier Cape", "Equip a Spottier Cape.", "Kandarin", TaskDifficulty.MEDIUM, "Hunter 69", "Spottier cape");
        equipItem("Equip an Ogre Forester Chompy Hat", "Equip an Ogre Forester Chompy Hat.", "Kandarin", TaskDifficulty.MEDIUM, "Fletching 5, Cooking 30, Ranged 30, Crafting 5", "Chompy bird hat (ogre forester)");
        addTask("Get 250 Target points", "Achieve 250 points in the Target minigame in the Ranging Guild.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Ranged 40", 250);
        addTask("Light a Pyre Ship", "Light a Barbarian Pyre Ship.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Firemaking 11, Crafting 11", 1);
        addTask("Move Your House to Yanille", "Move your Player Owned House to Yanille.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Construction 50", 1);
        addTask("Pickpocket a Knight of Ardougne 50 Times", "Pickpocket a Knight of Ardougne 50 times.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.DEFEAT_NPC, "Thieving 55", 50, "Knight of Ardougne");
        addTask("Score a Goal in Gnomeball", "Score a Goal in Gnomeball.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Sell 20 Iron Sheets", "Sell 20 Iron Sheets to Franklin Caranos.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Smithing 45", 20, "Iron sheet");
        addTask("Steal from a Fur Stall", "Steal from a Fur Stall in Kandarin.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 35", 1);
        addTask("Steal from a Silver Stall", "Steal from a Silver Stall in Kandarin.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 50", 1);
        addTask("Steal from a Spice Stall", "Steal from a Spice Stall in Kandarin.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 65", 1);
        addTask("Trap a Spined Larupia in the Feldip Hills", "Trap a Spined Larupia in the Feldip Hills.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "Hunter 31", 1, "Larupia fur");
        addTask("Use a Herring on a mighty tree", "Use a herring on the mightiest of trees.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Herring");
        addTask("Win a Game of Castle Wars", "Win a game of Castle Wars.", "Kandarin", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);

        // =====================================================================
        // MEDIUM TASKS — Karamja
        // =====================================================================
        addTask("Buy a Snapdragon From Pirate Jackie the Fruit", "Buy a Snapdragon from Pirate Jackie the Fruit in Brimhaven.", "Karamja", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Snapdragon");
        catchItem("Catch 50 Karambwan", "Catch 50 Karambwan on Karamja.", "Karamja", TaskDifficulty.MEDIUM, "Fishing 65 + Tai Bwo Wannai Trio", 50, "Raw karambwan");
        catchItem("Catch a Salmon on Karamja", "Catch a Salmon on Karamja.", "Karamja", TaskDifficulty.MEDIUM, "Fishing 30", "Raw salmon");
        addTask("Chop a dense jungle", "Fully clear a dense jungle in the Tai Bwo Wannai Cleanup.", "Karamja", TaskDifficulty.MEDIUM, TaskType.MISC, "Woodcutting 35", 1);
        addTask("Complete the Easy Karamja Diary", "Complete all of the Easy tasks in the Karamja Achievement Diary.", "Karamja", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Complete the Medium Karamja Diary", "Complete all of the Medium tasks in the Karamja Achievement Diary.", "Karamja", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Craft 50 Nature Runes", "Craft 50 Nature Runes from Essence at the Nature Altar.", "Karamja", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Runecraft 44", 50, "Pure essence", "Rune essence");
        defeatNpc("Defeat a Black Demon on Karamja", "Defeat a Black Demon on Karamja.", "Karamja", TaskDifficulty.MEDIUM, "", "Black demon");
        defeatNpc("Defeat a TzHaar", "Defeat any TzHaar in Mor Ul Rek.", "Karamja", TaskDifficulty.MEDIUM, "", "TzHaar-Hur", "TzHaar-Mej", "TzHaar-Xil", "TzHaar-Ket");
        addTask("Enter the Brimhaven Dungeon", "Enter the Brimhaven Dungeon.", "Karamja", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Enter the Tai Bwo Wannai Hardwood Grove", "Enter the Hardwood Grove in Tai Bwo Wannai.", "Karamja", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        equipItem("Equip a Full Tai Bwo Wannai Villager Set", "Equip a full set of Tai Bwo Wannai villager clothing.", "Karamja", TaskDifficulty.MEDIUM, "",
            "Villager armband", "Villager sandals", "Villager hat", "Villager robe");
        equipItem("Equip a Toktz-Ket-Xil", "Equip a Toktz-Ket-Xil.", "Karamja", TaskDifficulty.MEDIUM, "Defence 60", "Toktz-ket-xil");
        equipItem("Equip a Toktz-Xil-Ak", "Equip a Toktz-Xil-Ak.", "Karamja", TaskDifficulty.MEDIUM, "Attack 60", "Toktz-xil-ak");
        equipItem("Equip a Toktz-Xil-Ek", "Equip a Toktz-Xil-Ek.", "Karamja", TaskDifficulty.MEDIUM, "Attack 60", "Toktz-xil-ek");
        equipItem("Equip an Obsidian Cape", "Equip an Obsidian cape.", "Karamja", TaskDifficulty.MEDIUM, "", "Obsidian cape");
        equipItem("Equip any spear (kp)", "Equip any spear poisoned by a karambwan.", "Karamja", TaskDifficulty.MEDIUM, "",
            "Dragon spear (kp)", "Rune spear (kp)", "Adamant spear (kp)", "Mithril spear (kp)", "Steel spear (kp)", "Iron spear (kp)", "Bronze spear (kp)");
        addTask("Sleep in Paramaya Inn", "Pay the barkeep to sleep in Paramaya Inn, in Shilo Village.", "Karamja", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Take a Shortcut Across the Shilo Village River", "Use the Stepping Stones Agility Shortcut in Shilo Village.", "Karamja", TaskDifficulty.MEDIUM, TaskType.MISC, "Agility 32", 1);

        // =====================================================================
        // MEDIUM TASKS — Kourend
        // =====================================================================
        addTask("Cast Degrime Spell Full Clean", "Clean 26 herbs at the same time using the Degrime spell.", "Kourend", TaskDifficulty.MEDIUM, TaskType.SPELL, "Magic 70", 1);
        addTask("Cast Kourend Castle Teleport", "Cast the spell Kourend Castle Teleport after unlocking it.", "Kourend", TaskDifficulty.MEDIUM, TaskType.SPELL, "Magic 48", 1);
        chopItem("Chop 15 Yew Logs in Shayzien", "Chop 15 Yew Logs in Shayzien.", "Kourend", TaskDifficulty.MEDIUM, "Woodcutting 60", 15, "Yew logs");
        chopItem("Chop 25 Juniper Logs", "Chop 25 Juniper Logs.", "Kourend", TaskDifficulty.MEDIUM, "Woodcutting 42", 25, "Juniper logs");
        addTask("Complete 10 Farming Contracts", "Complete 10 Farming Contracts for Guildmaster Jane in the Farming Guild.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 45", 10);
        addTask("Complete the Garden of Death", "Complete the Garden of Death.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 20", 1);
        addTask("Create 100 Juniper Charcoal", "Create 100 Juniper Charcoal.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Woodcutting 42", 100, "Juniper logs");
        defeatNpc("Defeat 50 Lizardmen Shaman", "Help the Shayzien House by dealing with 50 Lizardmen shamans.", "Kourend", TaskDifficulty.MEDIUM, "", "Lizardman shaman");
        defeatNpc("Defeat Sarachnis 1 Time", "Defeat Sarachnis in the Forthos Dungeon.", "Kourend", TaskDifficulty.MEDIUM, "", "Sarachnis");
        defeatNpc("Defeat Sarachnis 50 Times", "Defeat Sarachnis in the Forthos Dungeon 50 times.", "Kourend", TaskDifficulty.MEDIUM, "", "Sarachnis");
        defeatNpc("Defeat a Drake", "Defeat a Drake in the Mount Karuulm Slayer Dungeon.", "Kourend", TaskDifficulty.MEDIUM, "Slayer 84", "Drake");
        defeatNpc("Defeat a King Sand Crab", "Defeat a King Sand Crab.", "Kourend", TaskDifficulty.MEDIUM, "", "King sand crab");
        defeatNpc("Defeat a Moss, Fire & Hill Giant in Kourend", "Defeat a Moss, Fire & Hill Giant in Kourend.", "Kourend", TaskDifficulty.MEDIUM, "", "Moss giant", "Fire giant", "Hill Giant");
        defeatNpc("Defeat the Wintertodt 10 times", "Help the pyromancers defeat the Wintertodt 10 times.", "Kourend", TaskDifficulty.MEDIUM, "Firemaking 50", "Wintertodt");
        defeatNpc("Defeat the Wintertodt 25 times", "Help the pyromancers defeat the Wintertodt 25 times.", "Kourend", TaskDifficulty.MEDIUM, "Firemaking 50", "Wintertodt");
        addTask("Dig 25 Saltpetre", "Dig 25 Saltpetre.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "", 25, "Saltpetre");
        addTask("Enter the Farming Guild", "Enter the Farming Guild in the Kebos Lowlands.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 45", 1);
        addTask("Enter the Woodcutting Guild", "Enter the Woodcutting Guild in Hosidius.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Woodcutting 60", 1);
        equipItem("Equip a Pearl Fishing Rod", "Equip a Pearl fishing rod.", "Kourend", TaskDifficulty.MEDIUM, "Hunter 35, Fishing 43", "Pearl fishing rod");
        equipItem("Equip a Xeric's Talisman", "Equip a Xeric's talisman.", "Kourend", TaskDifficulty.MEDIUM, "", "Xeric's talisman");
        equipItem("Equip the Cursed Amulet of Magic", "Equip the Cursed amulet of magic.", "Kourend", TaskDifficulty.MEDIUM, "", "Cursed amulet of magic");
        addTask("Headbang with Ket'Sal K'uk", "Headbang with Ket'Sal K'uk.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Inferior Demonslaying", "Defeat a demon using the Inferior Demonbane spell.", "Kourend", TaskDifficulty.MEDIUM, TaskType.SPELL, "Magic 44", 1);
        addTask("Kourend and Kebos Easy Diary Tasks", "Complete all of the Kourend & Kebos Diary easy tasks.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        addTask("Kourend and Kebos Medium Diary Tasks", "Complete all of the Kourend & Kebos Diary medium tasks.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1);
        mineItem("Mine 50 Volcanic Sulphur", "Mine 50 Volcanic Sulphur.", "Kourend", TaskDifficulty.MEDIUM, "Mining 42", 50, "Volcanic sulphur");
        addTask("Obtain the Temple Key", "Obtain the Temple key in the Forthos Dungeon.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Temple key");
        addTask("Offer an egg to a shrine", "Offer an egg to a shrine in the woodcutting guild.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Woodcutting 60", 1, "Bird's egg");
        addTask("Open 10 Grubby Chests", "Open the Grubby Chest in the Forthos Dungeon 10 times.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "", 10, "Grubby chest");
        addTask("Open 25 Grubby Chests", "Open the Grubby Chest in the Forthos Dungeon 25 times.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "", 25, "Grubby chest");
        addTask("Plant 100 Golovanova Seeds", "Plant 100 Golovanova seeds in the Tithe Farm minigame.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 34", 100, "Golovanova seed");
        addTask("Plant an Anima Seed", "Plant an Anima Seed in the Farming Guild.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 76", 1);
        addTask("Purchase a Seed Box", "Purchase a Seed box from the Tithe Farm minigame.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Farming 34", 1, "Seed box");
        addTask("Restore 14 Prayer points in Hosidius", "Restore exactly 14 Prayer Points in Hosidius.", "Kourend", TaskDifficulty.MEDIUM, TaskType.PRAYER, "Prayer 14", 1);
        addTask("Smelt a mithril bar in a volcanic fissure", "Smelt a Mithril bar in a volcanic Fissure.", "Kourend", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Smithing 50", 1, "Mithril ore", "Coal");
        addTask("Smith Shayzien (1)", "Smith any piece of Shayzien (1).", "Kourend", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Smithing 45, Mining 65", 1, "Lovakite bar");
        addTask("Smith Steel in Kourend Castle", "Smith Steel in Kourend Castle.", "Kourend", TaskDifficulty.MEDIUM, TaskType.CRAFT_ITEM, "Smithing 30", 1, "Steel bar");
        addTask("Steal 1 Artefact", "Steal an artefact for Captain Khaled in Piscarilius.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 49", 1);
        addTask("Steal 15 Artefacts", "Steal 15 artefacts for Captain Khaled in Piscarilius.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 49", 15);
        addTask("Steal a Golovanova Fruit Top", "Steal a Golovanova fruit top from a fruit stall in Hosidius.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "Thieving 25", 1, "Golovanova fruit top");
        addTask("Teleport to Xeric's Honour", "Teleport to Xeric's Honour using Xeric's talisman.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "", 1, "Xeric's talisman");
        addTask("Turn in 10 Library Books", "Find and turn in 10 books in the Arceuus Library.", "Kourend", TaskDifficulty.MEDIUM, TaskType.MISC, "", 10);

        // =====================================================================
        // MEDIUM TASKS — Morytania
        // =====================================================================
        addTask("Achieve 100% Shades of Mort'Ton Sanctity", "Achieve 100% Sanctity during the Shades of Mort'ton minigame.", "Morytania", TaskDifficulty.MEDIUM, TaskType.MISC, "Shades of Mort'ton", 1);

        // =====================================================================
        // MEDIUM TASKS — Varlamore
        // =====================================================================
        catchItem("Catch 25 Sunlight Antelopes", "Catch 25 Sunlight Antelopes.", "Varlamore", TaskDifficulty.HARD, "Hunter 72", 25, "Sunlight antelope", "Sunlight antelope fur");
        catchItem("Catch 50 Moonlight Antelopes", "Catch 50 Moonlight Antelopes.", "Varlamore", TaskDifficulty.ELITE, "Hunter 91", 50, "Moonlight antelope", "Moonlight antelope fur");
        catchItem("Catch a Pyre Fox", "Catch a Pyre Fox.", "Varlamore", TaskDifficulty.MEDIUM, "Hunter 36", "Pyre fox fur");
        addTask("Complete a Hunter Rumour", "Complete a Hunter Rumour.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.MISC, "Hunter 46", 1);

        // =====================================================================
        // HARD TASKS — missing additions
        // =====================================================================
        // General — Prayers
        addTask("Activate Deadeye", "Activate the Deadeye prayer.", "General", TaskDifficulty.HARD, TaskType.PRAYER, "Prayer 62", 1, "Deadeye prayer scroll");
        addTask("Activate Mystic Vigour", "Activate the Mystic Vigour prayer.", "General", TaskDifficulty.HARD, TaskType.PRAYER, "Prayer 63", 1, "Mystic vigour prayer scroll");
        addTask("Use the Chivalry Prayer", "Use the Chivalry Prayer.", "General", TaskDifficulty.HARD, TaskType.PRAYER, "Prayer 60, Defence 65", 1);
        addTask("Sacrifice a Dagannoth bone on a Gilded altar", "Sacrifice a Dagannoth bone on a Gilded altar in a POH with both burners lit.", "General", TaskDifficulty.HARD, TaskType.PRAYER, "Construction 75", 1, "Dagannoth bones");
        addTask("Build a Gilded Altar", "Build a Gilded Altar in a Chapel in your POH.", "General", TaskDifficulty.HARD, TaskType.MISC, "Construction 75", 1);

        // Runecraft
        addTask("Craft 50 Law Runes", "Craft 50 Law Runes from Essence at the Law Altar.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Runecraft 54", 50, "Pure essence", "Rune essence");
        addTask("Craft 50 Death Runes", "Craft 50 Death Runes from Essence at the Death Altar.", "Tirannwn", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Runecraft 65", 50, "Pure essence", "Rune essence");
        addTask("Craft 100 Blood runes", "Craft 100 Blood runes at the Kourend Blood Altar.", "Kourend", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Runecraft 77", 100, "Pure essence", "Rune essence");
        addTask("Guardians of the Rift 25 Rifts closed", "Close the Rift in the Temple of the Eye 25 times.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Runecraft 27", 25);
        addTask("Create the Colossal Rune Pouch", "Create the Colossal Rune Pouch from Guardians of the Rift.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Runecraft 27, Crafting 56", 1, "Giant pouch", "Abyssal needle");
        equipItem("Equip the Abyssal Lantern", "Equip the Abyssal Lantern.", "Desert", TaskDifficulty.HARD, "Runecraft 27", "Abyssal lantern");
        addTask("Complete the Hard Falador Diary", "Complete all of the Hard tasks in the Falador Achievement Diary.", "Asgarnia", TaskDifficulty.HARD, TaskType.MISC, "", 1);

        // God Wars Dungeon
        defeatNpc("Defeat Commander Zilyana", "Defeat Commander Zilyana in the God Wars Dungeon.", "General", TaskDifficulty.HARD, "Agility 70", "Commander Zilyana");
        defeatNpc("Defeat General Graardor", "Defeat General Graardor in the God Wars Dungeon.", "General", TaskDifficulty.HARD, "Strength 70", "General Graardor");
        defeatNpc("Defeat K'ril Tsutsaroth", "Defeat K'ril Tsutsaroth in the God Wars Dungeon.", "General", TaskDifficulty.HARD, "Hitpoints 70", "K'ril Tsutsaroth");
        defeatNpc("Defeat Kree'arra", "Defeat Kree'arra in the God Wars Dungeon.", "General", TaskDifficulty.HARD, "Ranged 70", "Kree'arra");
        defeatNpc("Defeat Nex", "Defeat Nex in the God Wars Dungeon.", "General", TaskDifficulty.HARD, "The Frozen Door", "Nex");
        defeatNpc("Defeat Nex 50 Times", "Defeat Nex in the God Wars Dungeon 50 times.", "General", TaskDifficulty.HARD, "The Frozen Door", "Nex");
        defeatNpc("Defeat Nex 100 Times", "Defeat Nex in the God Wars Dungeon 100 times.", "General", TaskDifficulty.HARD, "The Frozen Door", "Nex");
        defeatNpc("Defeat Any God Wars Dungeon Boss 100 Times", "Defeat any one of the God Wars Dungeon bosses 100 times.", "General", TaskDifficulty.HARD, "", "Commander Zilyana", "General Graardor", "K'ril Tsutsaroth", "Kree'arra", "Nex");
        defeatNpc("Defeat Any God Wars Dungeon Boss 250 Times", "Defeat any one of the God Wars Dungeon bosses 250 times.", "General", TaskDifficulty.HARD, "", "Commander Zilyana", "General Graardor", "K'ril Tsutsaroth", "Kree'arra", "Nex");

        // Cerberus / Giant Mole additions
        defeatNpc("Defeat Cerberus", "Defeat Cerberus in the Taverley Dungeon.", "Asgarnia", TaskDifficulty.HARD, "Slayer 91", "Cerberus");
        defeatNpc("Defeat Cerberus 50 times", "Defeat Cerberus 50 times.", "Asgarnia", TaskDifficulty.HARD, "Slayer 91", "Cerberus");
        defeatNpc("Defeat Cerberus 150 times", "Defeat Cerberus 150 times.", "Asgarnia", TaskDifficulty.HARD, "Slayer 91", "Cerberus");
        addTask("Mole Combat Achievements", "Complete all the Giant Mole Combat Achievements.", "Asgarnia", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("Royal Titans Combat Achievements", "Complete all of the Combat Achievements for the Royal Titans.", "Asgarnia", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);

        // Whisperer / Leviathan / Duke / Vardorvis non-awakened
        defeatNpc("Defeat Whisperer", "Defeat Whisperer.", "General", TaskDifficulty.HARD, "", "Whisperer");
        defeatNpc("Defeat Whisperer 50 times", "Defeat Whisperer 50 times.", "General", TaskDifficulty.HARD, "", "Whisperer");
        defeatNpc("Defeat Whisperer 150 times", "Defeat Whisperer 150 times.", "General", TaskDifficulty.HARD, "", "Whisperer");
        defeatNpc("Defeat Leviathan", "Defeat Leviathan.", "Desert", TaskDifficulty.HARD, "", "Leviathan");
        defeatNpc("Defeat Leviathan 50 times", "Defeat Leviathan 50 times.", "Desert", TaskDifficulty.HARD, "", "Leviathan");
        defeatNpc("Defeat Leviathan 150 times", "Defeat Leviathan 150 times.", "Desert", TaskDifficulty.HARD, "", "Leviathan");
        defeatNpc("Defeat Duke Sucellus", "Defeat Duke Sucellus.", "Fremennik", TaskDifficulty.HARD, "", "Duke Sucellus");
        defeatNpc("Defeat Duke Sucellus 50 times", "Defeat Duke Sucellus 50 times.", "Fremennik", TaskDifficulty.HARD, "", "Duke Sucellus");
        defeatNpc("Defeat Duke Sucellus 150 times", "Defeat Duke Sucellus 150 times.", "Fremennik", TaskDifficulty.HARD, "", "Duke Sucellus");

        // Phantom Muspah / Vorkath additions
        defeatNpc("Defeat Phantom Muspah 50 times", "Defeat Phantom Muspah 50 times.", "Fremennik", TaskDifficulty.HARD, "", "Phantom Muspah");
        defeatNpc("Defeat Phantom Muspah 150 times", "Defeat Phantom Muspah 150 times.", "Fremennik", TaskDifficulty.HARD, "", "Phantom Muspah");
        defeatNpc("Defeat Vorkath", "Defeat Vorkath on Ungael.", "Fremennik", TaskDifficulty.HARD, "", "Vorkath");
        defeatNpc("Defeat Vorkath 50 Times", "Defeat Vorkath on Ungael 50 times.", "Fremennik", TaskDifficulty.HARD, "", "Vorkath");
        defeatNpc("Defeat Vorkath 150 Times", "Defeat Vorkath on Ungael 150 times.", "Fremennik", TaskDifficulty.HARD, "", "Vorkath");

        // Dagannoth Kings
        defeatNpc("Defeat Each Dagannoth King 50 Times", "Defeat all three Dagannoth Kings 50 times.", "Fremennik", TaskDifficulty.HARD, "", "Dagannoth Supreme", "Dagannoth Prime", "Dagannoth Rex");
        defeatNpc("Defeat Each Dagannoth King 150 Times", "Defeat all three Dagannoth Kings 150 times.", "Fremennik", TaskDifficulty.HARD, "", "Dagannoth Supreme", "Dagannoth Prime", "Dagannoth Rex");
        defeatNpc("Defeat the Dagannoth Kings Without Leaving", "Defeat all three of the Dagannoth Kings without leaving their area.", "Fremennik", TaskDifficulty.HARD, "", "Dagannoth Supreme", "Dagannoth Prime", "Dagannoth Rex");

        // Kalphite Queen / misc desert
        defeatNpc("Defeat the Kalphite Queen", "Defeat the Kalphite Queen in the Kalphite Lair.", "Desert", TaskDifficulty.HARD, "", "Kalphite Queen");
        defeatNpc("Defeat the Kalphite Queen 50 Times", "Defeat the Kalphite Queen 50 times.", "Desert", TaskDifficulty.HARD, "", "Kalphite Queen");
        defeatNpc("Defeat the Kalphite Queen 150 Times", "Defeat the Kalphite Queen 150 times.", "Desert", TaskDifficulty.HARD, "", "Kalphite Queen");
        addTask("Kalphite Queen Combat Achievements", "Complete all of the Combat Achievements for the Kalphite Queen.", "Desert", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        defeatNpc("Defeat a Dust Devil in the Kharidian Desert", "Defeat a dust devil in the desert Smoke Dungeon.", "Desert", TaskDifficulty.HARD, "Slayer 65", "Dust devil");
        defeatNpc("Defeat Tempoross 25 times", "Help the Spirit Anglers defeat Tempoross 25 times.", "Desert", TaskDifficulty.HARD, "Fishing 35", "Tempoross");
        addTask("Complete Beneath Cursed Sands", "Complete Beneath Cursed Sands quest.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Agility 62, Crafting 55, Firemaking 55", 1);
        addTask("Complete Tombs of Amascut", "Complete Tombs of Amascut on normal or Expert.", "Desert", TaskDifficulty.HARD, TaskType.ACTIVITY, "Beneath Cursed Sands", 1);
        addTask("Complete Tombs of Amascut 25 times", "Complete Tombs of Amascut on normal or Expert 25 times.", "Desert", TaskDifficulty.HARD, TaskType.ACTIVITY, "Beneath Cursed Sands", 25);
        addTask("Complete the Hard Desert Diary", "Complete all of the Hard tasks in the Desert Achievement Diary.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Check a grown Cactus", "Check the health of a cactus you've grown in Al Kharid.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Farming 55", 1);
        addTask("Make 20 Magic Potions", "Make 20 Magic Potions.", "Desert", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Herblore 76", 20, "Lantadyme potion (unf)");
        addTask("Make 50 Menaphite Remedies", "Make 50 Menaphite Remedies.", "Desert", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Herblore 88", 50, "Cactus spine", "Gold leaf", "4-dose 2nd potion");
        addTask("Obtain the Fish Barrel", "Obtain the Fish Barrel from Tempoross.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Fishing 35", 1, "Fish barrel");
        addTask("Pick a Summer Sq'irk", "Pick a Summer Sq'irk in the Sorceress's Garden.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Thieving 65", 1, "Summer sq'irk");
        addTask("Pickpocket a Menaphite Thug 50 Times", "Knock out and then pickpocket a Menaphite Thug 50 times.", "Desert", TaskDifficulty.HARD, TaskType.DEFEAT_NPC, "Thieving 65", 50, "Menaphite Thug");
        addTask("Room 7 of Pyramid Plunder", "Search the Golden Chest in Room 7 of Pyramid Plunder in Sophanem.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Thieving 81", 1);
        addTask("Room 8 of Pyramid Plunder", "Search the Golden Chest in Room 8 of Pyramid Plunder in Sophanem.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Thieving 91", 1);
        addTask("Room 8 of Pyramid Plunder 25 Times", "Search the Golden Chest in Room 8 25 times.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Thieving 91", 25);
        addTask("Smith 1,000 Adamant Dart Tips", "Use an Anvil to smith 1,000 Adamant Dart Tips.", "Desert", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Smithing 74", 1000, "Adamantite bar");
        addTask("Turn in 25 Autumn Sq'irkjuices to Osman", "Turn in 25 Autumn Sq'irkjuices to Osman in one go.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Thieving 45", 25, "Autumn sq'irkjuice");
        addTask("Blast Furnace 100 Adamantite Bars", "Smelt 100 adamantite bars at the Blast Furnace.", "Fremennik", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Smithing 70", 100, "Adamantite ore");
        addTask("Blast Furnace 100 Mithril Bars", "Smelt 100 mithril bars at the Blast Furnace.", "Fremennik", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Smithing 50", 100, "Mithril ore");
        addTask("Have Drew create 500 buckets", "Create 500 buckets of sand at Drew's sandstorm machine.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Mining 35", 500, "Bucket of sand");
        addTask("Giants' Foundry 125 quality sword", "Hand in a sword with at least 125 quality in Giants' Foundry.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Sleeping Giants", 1);
        addTask("Giants' Foundry 150 quality sword", "Hand in a sword with at least 150 quality in Giants' Foundry.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Sleeping Giants", 1);
        addTask("Giants' Foundry 25 handins", "Hand in 25 successful swords to Kovac.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Sleeping Giants", 25);
        addTask("Giants' Foundry 50 handins", "Hand in 50 successful swords to Kovac.", "Desert", TaskDifficulty.HARD, TaskType.MISC, "Sleeping Giants", 50);
        equipItem("Equip the Lightbearer", "Equip the Lightbearer.", "Desert", TaskDifficulty.HARD, "Beneath Cursed Sands", "Lightbearer");
        equipItem("Equip a Dragon Pickaxe in the Desert", "Equip a dragon pickaxe in the Kharidian Desert.", "Desert", TaskDifficulty.HARD, "Attack 60", "Dragon pickaxe");
        equipItem("Equip a Dragon 2-Handed Sword", "Equip a dragon 2h sword in the Kharidian Desert.", "Desert", TaskDifficulty.HARD, "Attack 60", "Dragon 2h sword");
        equipItem("Equip a full set of the Smith's outfit", "Equip a full set of the Smith's outfit.", "Desert", TaskDifficulty.HARD, "Smithing 15 + Sleeping Giants",
            "Smiths helmet", "Smiths tunic", "Smiths trousers", "Smiths boots", "Smiths gloves");

        // Fremennik / Prifddinas
        defeatNpc("Defeat a Rock Crab in one hit", "Defeat a Rock Crab in one hit (50 damage).", "Fremennik", TaskDifficulty.HARD, "", "Rock Crab");
        defeatNpc("Defeat a Rock Lobster", "Defeat a Rock Lobster.", "Fremennik", TaskDifficulty.HARD, "", "Rock lobster");
        addTask("Cast Fertile Soil", "Cast the Fertile Soil spell.", "Fremennik", TaskDifficulty.HARD, TaskType.SPELL, "Magic 83", 1);
        addTask("Cast Moonclan Teleport", "Cast the Moonclan Teleport spell.", "Fremennik", TaskDifficulty.HARD, TaskType.SPELL, "Magic 69", 1);
        addTask("Cast Spellbook Swap", "Cast the Spellbook Swap spell.", "Fremennik", TaskDifficulty.HARD, TaskType.SPELL, "Magic 96", 1);
        addTask("Collect Miscellania Resources at Full Approval", "Collect resources on Miscellania with an approval rating of 100%.", "Fremennik", TaskDifficulty.HARD, TaskType.MISC, "Throne of Miscellania", 1);
        addTask("Complete 25 laps of the Rellekka Agility Course", "Complete 25 laps of the Rellekka Agility Course.", "Fremennik", TaskDifficulty.HARD, TaskType.MISC, "Agility 80", 25);
        addTask("Complete the Hard Fremennik Diary", "Complete all of the Hard tasks in the Fremennik Achievement Diary.", "Fremennik", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Complete the Rellekka Agility Course", "Complete a lap of the Rellekka Rooftop Agility Course.", "Fremennik", TaskDifficulty.HARD, TaskType.MISC, "Agility 80", 1);
        addTask("Create a Catherby Teleport Tablet", "Create a Catherby Teleport Tablet.", "Fremennik", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Magic 87", 1, "Soft clay", "Law rune", "Water rune");
        addTask("Harvest a Snapdragon in Weiss", "Harvest a Snapdragon you've grown in Weiss.", "Fremennik", TaskDifficulty.HARD, TaskType.MISC, "Farming 62", 1, "Grimy snapdragon", "Snapdragon");
        addTask("Obtain a Frozen Cache from a Cache", "Obtain a Frozen Cache from another Frozen Cache.", "Fremennik", TaskDifficulty.HARD, TaskType.MISC, "", 1, "Frozen cache");
        addTask("Trade with Bardur", "Trade with Bardur in Waterbirth island dungeon.", "Fremennik", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Trap a Sabre-toothed Kyatt", "Trap a Sabre-toothed Kyatt.", "Fremennik", TaskDifficulty.HARD, TaskType.CATCH_ITEM, "Hunter 55", 1, "Kyatt fur");
        addTask("Use Your Portal Nexus to Teleport to Weiss", "Use a Portal Nexus to teleport to Weiss.", "Fremennik", TaskDifficulty.HARD, TaskType.MISC, "Mining 72, Construction 72", 1);
        equipItem("Equip a Berserker Ring", "Equip a Berserker Ring.", "Fremennik", TaskDifficulty.HARD, "", "Berserker ring");
        equipItem("Equip a Brine Sabre", "Equip a Brine Sabre.", "Fremennik", TaskDifficulty.HARD, "Attack 40, Slayer 47", "Brine sabre");
        equipItem("Equip a Full Rockshell Armour Set", "Equip a full set of Rockshell armour.", "Fremennik", TaskDifficulty.HARD, "Defence 40",
            "Rockshell helm", "Rockshell plate", "Rockshell legs");
        equipItem("Equip a Full Skeletal Armour Set", "Equip a full set of Skeletal armour.", "Fremennik", TaskDifficulty.HARD, "Defence 40, Magic 40",
            "Skeletal helm", "Skeletal top", "Skeletal bottoms");
        equipItem("Equip a Full Spined Armour Set", "Equip a full set of Spined armour.", "Fremennik", TaskDifficulty.HARD, "Defence 40, Ranged 40",
            "Spined helm", "Spined body", "Spined chaps");
        equipItem("Equip a Leaf-Bladed Battleaxe", "Equip a Leaf-Bladed Battleaxe in the Fremennik Provinces.", "Fremennik", TaskDifficulty.HARD, "Attack 65, Slayer 70", "Leaf-bladed battleaxe");
        equipItem("Equip a Mud Battlestaff", "Equip a Mud Battlestaff.", "Fremennik", TaskDifficulty.HARD, "Magic 30, Attack 30", "Mud battlestaff");
        equipItem("Equip a Seer's Ring", "Equip a Seer's Ring.", "Fremennik", TaskDifficulty.HARD, "", "Seers ring");
        equipItem("Equip a Seercull", "Equip a Seercull.", "Fremennik", TaskDifficulty.HARD, "Ranged 50", "Seercull");
        equipItem("Equip a Warrior Ring", "Equip a Warrior Ring.", "Fremennik", TaskDifficulty.HARD, "", "Warrior ring");
        equipItem("Equip an Archer's Ring", "Equip an Archer's Ring.", "Fremennik", TaskDifficulty.HARD, "", "Archers ring");
        equipItem("Equip an Ava's Assembler", "Equip an Ava's Assembler.", "Fremennik", TaskDifficulty.HARD, "Ranged 70", "Ava's assembler");
        equipItem("Equip the Ancient Sceptre", "Equip the Ancient Sceptre.", "Fremennik", TaskDifficulty.HARD, "Magic 70, Attack 50, Strength 60", "Ancient sceptre");

        // Kandarin
        addTask("Bury an Ourg bone", "Bury an ourg bone.", "Kandarin", TaskDifficulty.HARD, TaskType.PRAYER, "", 1, "Ourg bones");
        addTask("Cast a water surge spell at a Black dragon in Kandarin", "Cast a water surge spell at a black dragon in Kandarin.", "Kandarin", TaskDifficulty.HARD, TaskType.SPELL, "Magic 85", 1);
        addTask("Check a grown Papaya Tree in the Gnome Stronghold", "Check the health of a Papaya Tree you've grown in the Tree Gnome Stronghold.", "Kandarin", TaskDifficulty.HARD, TaskType.MISC, "Farming 57", 1);
        addTask("Complete Elemental Workshop II", "Complete the Elemental Workshop II quest.", "Kandarin", TaskDifficulty.HARD, TaskType.MISC, "Magic 20, Smithing 30", 1);
        addTask("Complete Path of Glouphrie", "Complete the Path of Glouphrie quest.", "Kandarin", TaskDifficulty.HARD, TaskType.MISC, "Strength 60, Slayer 56", 1);
        addTask("Complete the Hard Ardougne Diary", "Complete all of the Hard tasks in the Ardougne Achievement Diary.", "Kandarin", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Complete the Hard Kandarin Diary", "Complete all of the Hard tasks in the Kandarin Achievement Diary.", "Kandarin", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Complete the Seers' Village Agility Course", "Complete a lap of the Seers' Village Rooftop Agility Course.", "Kandarin", TaskDifficulty.HARD, TaskType.MISC, "Agility 60", 1);
        addTask("Deal 66 damage with an elemental spell", "Deal at least 66 damage in a single hit using elemental spells.", "Kandarin", TaskDifficulty.HARD, TaskType.SPELL, "", 1);
        defeatNpc("Defeat 150 Demonic Gorillas", "Defeat 150 Demonic Gorillas in the Crash Site Cavern.", "Kandarin", TaskDifficulty.HARD, "", "Demonic gorilla");
        defeatNpc("Defeat a Cave Kraken", "Defeat a Cave Kraken in Kraken Cove.", "Kandarin", TaskDifficulty.HARD, "Slayer 87", "Cave kraken");
        defeatNpc("Defeat a Jubster while dressed like it", "Defeat a Jubster while wearing a beret.", "Kandarin", TaskDifficulty.HARD, "Tower of Life", "Jubster");
        defeatNpc("Defeat a Smoke Devil", "Defeat a Smoke Devil in the Smoke Devil Dungeon.", "Kandarin", TaskDifficulty.HARD, "Slayer 93", "Smoke devil");
        defeatNpc("Defeat the Kraken Boss 150 Times", "Defeat the Kraken boss 150 times.", "Kandarin", TaskDifficulty.HARD, "Slayer 87", "Kraken");
        defeatNpc("Defeat the Penance Queen", "Defeat the Penance Queen in Barbarian Assault.", "Kandarin", TaskDifficulty.HARD, "", "Penance Queen");
        defeatNpc("Defeat the Thermonuclear Smoke Devil", "Defeat the Thermonuclear Smoke Devil in the Smoke Devil Dungeon.", "Kandarin", TaskDifficulty.HARD, "Slayer 93", "Thermonuclear smoke devil");
        equipItem("Equip a Full Angler's Outfit", "Equip a full set of Angler gear.", "Kandarin", TaskDifficulty.HARD, "Fishing 34",
            "Angler hat", "Angler top", "Angler waders", "Angler boots");
        equipItem("Equip a Karamja Monkey Backpack", "Equip a Karamja Monkey Backpack.", "Kandarin", TaskDifficulty.HARD, "Agility 48", "Karamja monkey backpack");
        equipItem("Equip a Warped Sceptre", "Equip a Warped Sceptre.", "Kandarin", TaskDifficulty.HARD, "Magic 62, Slayer 56", "Warped sceptre");
        equipItem("Equip an Ogre Expert Chompy Hat", "Equip an Ogre Expert Chompy Hat.", "Kandarin", TaskDifficulty.HARD, "Fletching 5, Cooking 30, Ranged 30, Crafting 5", "Chompy bird hat (ogre expert)");
        addTask("Feed Longramble", "Deliver some Tangled Toads Legs to Longramble.", "Kandarin", TaskDifficulty.HARD, TaskType.MISC, "Cooking 40", 1, "Tangled toads legs");
        addTask("Fletch 100 Maple Longbow (u) in Kandarin", "Fletch 100 Maple Longbow (u) in Kandarin.", "Kandarin", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Fletching 55", 100, "Maple logs");
        addTask("Get 750 Target points", "Achieve 750 points in the Target minigame.", "Kandarin", TaskDifficulty.HARD, TaskType.MISC, "Ranged 40", 750);
        addTask("Kraken Combat Achievements", "Complete all the Kraken Combat Achievements.", "Kandarin", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("Pickpocket a King Worm", "Pickpocket a gnome for a King Worm.", "Kandarin", TaskDifficulty.HARD, TaskType.DEFEAT_NPC, "Thieving 75", 1, "Gnome");
        addTask("Pickpocket an Elite clue from a hero", "Pickpocket an elite clue scroll from a hero.", "Kandarin", TaskDifficulty.HARD, TaskType.DEFEAT_NPC, "Thieving 80", 1, "Hero");
        addTask("Thermonuclear Smoke Devil Combat Achievements", "Complete all of the Combat Achievements for the Thermonuclear smoke devil.", "Kandarin", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("Trade in 1000 Minnows", "Trade in 1000 Minnows in the Fishing guild.", "Kandarin", TaskDifficulty.HARD, TaskType.MISC, "Fishing 75", 1000, "Minnow");

        // Karamja additions
        addTask("Check a grown Calquat Tree", "Check the health of a Calquat Tree you've grown on Karamja.", "Karamja", TaskDifficulty.HARD, TaskType.MISC, "Farming 72", 1);
        addTask("Complete Tzhaar-Ket-Rak's first challenge", "Complete Tzhaar-Ket-Rak's first challenge.", "Karamja", TaskDifficulty.HARD, TaskType.ACTIVITY, "Fire cape", 1);
        addTask("Complete Tzhaar-Ket-Rak's second challenge", "Complete Tzhaar-Ket-Rak's second challenge.", "Karamja", TaskDifficulty.HARD, TaskType.ACTIVITY, "Fire cape", 1);
        addTask("Complete the Hard Karamja Diary", "Complete all of the Hard tasks in the Karamja Achievement Diary.", "Karamja", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        cookItem("Cook 100 Karambwans", "Cook 100 Karambwan.", "Karamja", TaskDifficulty.HARD, "Fishing 30 + Tai Bwo Wannai Trio", 100, "Raw karambwan");
        cookItem("Cook 20 Karambwans in a row", "Cook 20 Karambwans in a row without burning them.", "Karamja", TaskDifficulty.HARD, "Cooking 30 + Tai Bwo Wannai Trio", 20, "Raw karambwan");
        equipItem("Equip Some Dragon Platelegs or a Dragon Plateskirt", "Equip either some Dragon Platelegs or a Dragon Plateskirt.", "Karamja", TaskDifficulty.HARD, "Defence 60", "Dragon platelegs", "Dragon plateskirt");
        equipItem("Equip a Full Obsidian Armour Set", "Equip a full set of Obsidian armour.", "Karamja", TaskDifficulty.HARD, "Defence 60 + Fight Cave",
            "Obsidian helmet", "Obsidian platebody", "Obsidian platelegs");
        equipItem("Equip a Red Topaz Machete", "Equip a Red Topaz Machete.", "Karamja", TaskDifficulty.HARD, "", "Red topaz machete");
        addTask("Find a Gout Tuber", "Find a Gout Tuber in Tai Bwo Wannai.", "Karamja", TaskDifficulty.HARD, TaskType.MISC, "Woodcutting 35", 1, "Gout tuber");
        addTask("Receive 30 Agility Arena Tickets With No Mistakes", "Receive 30 Agility Arena Tickets without missing any pillars.", "Karamja", TaskDifficulty.HARD, TaskType.MISC, "", 30, "Agility arena ticket");

        // Kourend
        addTask("1 Chambers of Xeric", "Complete the Chambers of Xeric.", "Kourend", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("50 Chambers of Xeric", "Complete Chambers of Xeric 50 times.", "Kourend", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 50);
        addTask("Activate an Arcane or Dexterous Prayer Scroll", "Activate an Arcane or Dexterous Prayer scroll.", "Kourend", TaskDifficulty.HARD, TaskType.PRAYER, "", 1, "Arcane prayer scroll", "Dexterous prayer scroll");
        addTask("Blow the Soulflame Horn", "Use the special attack of the soulflame horn.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "Magic 80", 1, "Soulflame horn");
        burnItem("Burn 200 Redwood Logs", "Burn 200 Redwood Logs.", "Kourend", TaskDifficulty.HARD, "Firemaking 90, Woodcutting 90", 200, "Redwood logs");
        addTask("Chop a Magic Log at the Forsaken Tower", "Chop a log from the magic trees around the Forsaken Tower.", "Kourend", TaskDifficulty.HARD, TaskType.CHOP_ITEM, "Woodcutting 75", 1, "Magic logs");
        addTask("Complete 50 Farming Contracts", "Complete 50 Farming Contracts for Guildmaster Jane.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "Farming 45", 50);
        addTask("Complete the Shayzien Advanced Agility Course", "Complete a lap of the Shayzien Advanced Agility Course.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "Attack 45", 1);
        addTask("Create 10 Dynamite", "Create 10 Dynamite.", "Kourend", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Mining 42, Woodcutting 42", 10, "Dynamite pot", "Gunpowder");
        addTask("Create the Silklined herb sack", "Create the silklined herb sack.", "Kourend", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Herblore 58", 1, "Herb sack", "Silk");
        defeatNpc("Defeat 300 Lizardmen Shaman", "Help the Shayzien House by dealing with 300 Lizardmen shamans.", "Kourend", TaskDifficulty.HARD, "", "Lizardman shaman");
        defeatNpc("Defeat Sarachnis 150 Times", "Defeat Sarachnis 150 times.", "Kourend", TaskDifficulty.HARD, "", "Sarachnis");
        defeatNpc("Defeat Sarachnis 300 Times", "Defeat Sarachnis 300 times.", "Kourend", TaskDifficulty.HARD, "", "Sarachnis");
        defeatNpc("Defeat Yama 1 time", "Defeat Yama 1 time.", "Kourend", TaskDifficulty.HARD, "", "Yama");
        defeatNpc("Defeat Yama 50 times", "Defeat Yama 50 times.", "Kourend", TaskDifficulty.HARD, "", "Yama");
        defeatNpc("Defeat a Hydra", "Defeat a regular hydra in the Karuulm Slayer Dungeon.", "Kourend", TaskDifficulty.HARD, "Slayer 95", "Hydra");
        defeatNpc("Defeat the Alchemical Hydra 1 Time", "Defeat the Alchemical Hydra in Mount Karuulm.", "Kourend", TaskDifficulty.HARD, "Slayer 95", "Alchemical Hydra");
        defeatNpc("Defeat the Alchemical Hydra 50 Times", "Defeat the Alchemical hydra 50 times.", "Kourend", TaskDifficulty.HARD, "Slayer 95", "Alchemical Hydra");
        defeatNpc("Defeat the Alchemical Hydra 150 Times", "Defeat the Alchemical hydra 150 times.", "Kourend", TaskDifficulty.HARD, "Slayer 95", "Alchemical Hydra");
        defeatNpc("Defeat the Mimic 1 Time", "Defeat the Mimic as part of a Treasure Trail.", "Kourend", TaskDifficulty.HARD, "", "Mimic");
        defeatNpc("Defeat the Mimic 5 Times", "Defeat the Mimic 5 times.", "Kourend", TaskDifficulty.HARD, "", "Mimic");
        defeatNpc("Defeat the Wintertodt 50 times", "Help the pyromancers defeat the Wintertodt 50 times.", "Kourend", TaskDifficulty.HARD, "Firemaking 50", "Wintertodt");
        addTask("Enter the Farming Guild's High Tier", "Enter the high tier of the Farming Guild.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "Farming 85", 1);
        equipItem("Equip Boots of Brimstone", "Equip a pair of Boots of brimstone.", "Kourend", TaskDifficulty.HARD, "Slayer 84, Defence 70, Magic 70, Ranged 70", "Boots of brimstone");
        equipItem("Equip Tier 5 Shayzien Armour", "Equip a full set of tier 5 Shayzien armour.", "Kourend", TaskDifficulty.HARD, "Defence 20",
            "Shayzien helm (5)", "Shayzien body (5)", "Shayzien greaves (5)", "Shayzien boots (5)", "Shayzien gloves (5)");
        equipItem("Equip the Farmer's Outfit", "Equip a full set of the Farmer's outfit.", "Kourend", TaskDifficulty.HARD, "Farming 34",
            "Farmer's strawhat", "Farmer's jacket", "Farmer's boro trousers", "Farmer's boots");
        equipItem("Equip the Pyromancer's Garb", "Equip a full set of Pyromancer's garb.", "Kourend", TaskDifficulty.HARD, "Firemaking 50",
            "Pyromancer hood", "Pyromancer garb", "Pyromancer robe", "Pyromancer boots");
        addTask("Harvest 25 Zamorak's Grapes", "Harvest 25 Zamorak's Grapes.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "Farming 36, Prayer 50, Cooking 65", 25, "Zamorak's grapes");
        addTask("Kourend and Kebos Hard Diary Tasks", "Complete all of the Kourend & Kebos Diary hard tasks.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Obtain Runite Ore at the Blast Mine", "Obtain Runite ore at the Blast Mine.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "Mining 75", 1, "Runite ore");
        addTask("Plant 100 Bologano Seeds", "Plant 100 Bologano seeds in Tithe Farm.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "Farming 54", 100, "Bologano seed");
        addTask("Plant 100 Logavano Seeds", "Plant 100 Logavano seeds in Tithe Farm.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "Farming 74", 100, "Logavano seed");
        addTask("Read the Rite of Vile Transference", "Read the rite of vile transference.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "", 1, "Rite of vile transference");
        addTask("Turn in 50 Library Books", "Find and turn in 50 books in the Arceuus Library.", "Kourend", TaskDifficulty.HARD, TaskType.MISC, "", 50);

        // Morytania
        addTask("Burn 20 Blisterwood Logs", "Burn 20 Blisterwood Logs.", "Morytania", TaskDifficulty.HARD, TaskType.BURN_ITEM, "Firemaking 62, Woodcutting 62 + Sins of the Father", 20, "Blisterwood logs");
        addTask("Complete Sins of the Father", "Complete Sins of the Father quest.", "Morytania", TaskDifficulty.HARD, TaskType.MISC, "Woodcutting 62, Fletching 60, Crafting 56, Agility 52", 1);
        addTask("Complete the Hard Morytania Diary", "Complete all of the Hard tasks in the Morytania Achievement Diary.", "Morytania", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        cookItem("Cook 20 Sharks in Darkmeyer", "Cook 20 Sharks in Darkmeyer.", "Morytania", TaskDifficulty.HARD, "Cooking 80", 20, "Raw shark");
        defeatNpc("Defeat Araxxor 50 Times", "Defeat Araxxor 50 times.", "Morytania", TaskDifficulty.HARD, "Slayer 92", "Araxxor");
        defeatNpc("Defeat Araxxor 150 Times", "Defeat Araxxor 150 times.", "Morytania", TaskDifficulty.HARD, "Slayer 92", "Araxxor");
        defeatNpc("Defeat The Nightmare", "Defeat The Nightmare in the Sisterhood Sanctuary.", "Morytania", TaskDifficulty.HARD, "", "The Nightmare", "Nightmare");
        defeatNpc("Defeat The Nightmare 25 times", "Defeat The Nightmare or Phosani's Nightmare 25 times combined.", "Morytania", TaskDifficulty.HARD, "", "The Nightmare", "Phosani's Nightmare");
        defeatNpc("Defeat a Urium Shade", "Defeat a Urium Shade.", "Morytania", TaskDifficulty.HARD, "Firemaking 80", "Urium shade");
        defeatNpc("Defeat an Abyssal Demon in Morytania", "Defeat an Abyssal Demon in Morytania.", "Morytania", TaskDifficulty.HARD, "Slayer 85", "Abyssal demon");
        defeatNpc("Defeat the Grotesque Guardians", "Defeat the Grotesque Guardians at the Slayer Tower.", "Morytania", TaskDifficulty.HARD, "Slayer 75", "Dusk", "Dawn");
        defeatNpc("Defeat the Grotesque Guardians 50 Times", "Defeat the Grotesque Guardians 50 times.", "Morytania", TaskDifficulty.HARD, "Slayer 75", "Dusk", "Dawn");
        defeatNpc("Defeat the Grotesque Guardians 150 Times", "Defeat the Grotesque Guardians 150 times.", "Morytania", TaskDifficulty.HARD, "Slayer 75", "Dusk", "Dawn");
        equipItem("Equip Aranea Boots", "Equip Aranea Boots.", "Morytania", TaskDifficulty.HARD, "Slayer 92", "Aranea boots");
        equipItem("Equip Full Ahrims Armour Set", "Equip a full set of Ahrims items.", "Morytania", TaskDifficulty.HARD, "Magic 70, Defence 70, Attack 70",
            "Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Ahrim's staff");
        equipItem("Equip Full Dharoks Armour Set", "Equip a full set of Dharoks items.", "Morytania", TaskDifficulty.HARD, "Strength 70, Defence 70, Attack 70",
            "Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe");
        equipItem("Equip Full Guthans Armour Set", "Equip a full set of Guthans items.", "Morytania", TaskDifficulty.HARD, "Defence 70, Attack 70",
            "Guthan's helm", "Guthan's platebody", "Guthan's chainskirt", "Guthan's warspear");
        equipItem("Equip Full Karils Armour Set", "Equip a full set of Karils items.", "Morytania", TaskDifficulty.HARD, "Ranged 70, Defence 70",
            "Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Karil's crossbow");
        equipItem("Equip Full Torags Armour Set", "Equip a full set of Torags items.", "Morytania", TaskDifficulty.HARD, "Strength 70, Defence 70, Attack 70",
            "Torag's helm", "Torag's platebody", "Torag's platelegs", "Torag's hammers");
        equipItem("Equip Full Veracs Armour Set", "Equip a full set of Veracs items.", "Morytania", TaskDifficulty.HARD, "Defence 70, Attack 70",
            "Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");
        equipItem("Equip a Granite Hammer or Granite Ring", "Equip either a Granite Hammer or a Granite Ring.", "Morytania", TaskDifficulty.HARD, "Slayer 75, Strength 50", "Granite hammer", "Granite ring");
        equipItem("Equip a Nightmare Staff", "Equip a normal Nightmare Staff.", "Morytania", TaskDifficulty.HARD, "Magic 72, Hitpoints 50", "Nightmare staff");
        equipItem("Equip a Piece of Zealot's Robes", "Equip a piece of Zealot's Robes.", "Morytania", TaskDifficulty.HARD, "Firemaking 80",
            "Zealot's helm", "Zealot's robe top", "Zealot's robe bottom", "Zealot's boots");
        addTask("Floor 4 of the Hallowed Sepulchre", "Complete floor 4 of the Hallowed Sepulchre in Darkmeyer.", "Morytania", TaskDifficulty.HARD, TaskType.MISC, "Agility 82", 1);
        addTask("Learn how to make Bloodbark", "Learn how to make bloodbark armour.", "Morytania", TaskDifficulty.HARD, TaskType.MISC, "Firemaking 5", 1, "Runescroll of bloodbark");
        addTask("Learn how to make Swampbark", "Learn how to make swampbark armour.", "Morytania", TaskDifficulty.HARD, TaskType.MISC, "Firemaking 5", 1, "Runescroll of swampbark");
        addTask("Obtain Every Hallowed Tool", "Obtain all four Hallowed Tools from the Hallowed Sepulchre.", "Morytania", TaskDifficulty.HARD, TaskType.MISC, "Agility 52", 1);
        addTask("Open a Gold Chest", "Open a Gold Chest from Shades of Mort'ton.", "Morytania", TaskDifficulty.HARD, TaskType.MISC, "Firemaking 80", 1, "Gold chest");
        addTask("Pickpocket a Vyre 50 Times", "Pickpocket a Vyre 50 times.", "Morytania", TaskDifficulty.HARD, TaskType.DEFEAT_NPC, "Thieving 82", 50, "Vyre");
        addTask("Unlock permanent boat travel with Andras", "Unlock permanent boat travel with Andras in Morytania.", "Morytania", TaskDifficulty.HARD, TaskType.MISC, "", 1, "Andras");

        // Tirannwn / Prifddinas
        addTask("Check a grown Crystal Tree", "Check the health of a Crystal Tree you've grown.", "Tirannwn", TaskDifficulty.HARD, TaskType.MISC, "Farming 74", 1);
        chopItem("Chop 100 Teak Logs in Prifddinas", "Chop 100 Teak Logs in Prifddinas.", "Tirannwn", TaskDifficulty.HARD, "Woodcutting 35", 100, "Teak logs");
        chopItem("Chop 50 Magic Logs in Tirannwn", "Chop 50 Magic Logs in Tirannwn.", "Tirannwn", TaskDifficulty.HARD, "Woodcutting 75", 50, "Magic logs");
        addTask("Complete 50 laps of the Prifddinas Agility Course", "Complete 50 laps of the Prifddinas Agility Course.", "Tirannwn", TaskDifficulty.HARD, TaskType.MISC, "Agility 75", 50);
        addTask("Complete the Gauntlet", "Complete the Gauntlet in Prifddinas.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("Complete the Prifddinas Agility Course", "Complete a lap of the Prifddinas Agility Course.", "Tirannwn", TaskDifficulty.HARD, TaskType.MISC, "Agility 75", 1);
        addTask("Complete the Prifddinas Agility Course in 1:10", "Complete a lap of the Prifddinas Agility Course in 1:10 or less.", "Tirannwn", TaskDifficulty.HARD, TaskType.MISC, "Agility 75", 1);
        addTask("Craft a Piece of Crystal Armour", "Use a Singing Bowl to craft any piece of Crystal armour.", "Tirannwn", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Smithing 70, Crafting 70", 1,
            "Crystal armour seed", "Crystal shard");
        addTask("Craft an Eternal Teleport Crystal", "Use a Singing Bowl to craft an Eternal Teleport Crystal.", "Tirannwn", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Smithing 80, Crafting 80", 1, "Enhanced crystal teleport seed");
        addTask("Create 100 Anti-venom Potions", "Make an anti-venom potion 100 times.", "Tirannwn", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Herblore 87", 100, "Antidote++", "Zulrah's scales");
        addTask("Create 25 Divine Super Attack Potions", "Create 25 Divine Super Attack Potions.", "Tirannwn", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Herblore 85", 25, "Super attack(4)", "Crystal dust");

        // Combat achievements / Level / Stats / Clues
        addTask("Achieve Your First Level 70", "Reach level 70 in any skill.", "General", TaskDifficulty.HARD, TaskType.SKILL_LEVEL, "", 1);
        addTask("Achieve Your First Level 80", "Reach level 80 in any skill.", "General", TaskDifficulty.HARD, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Combat Level 100", "Reach Combat Level 100.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Reach Combat Level 110", "Reach Combat Level 110.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Reach Total Level 1000", "Reach a Total Level of 1000.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Reach Total Level 1250", "Reach a Total Level of 1250.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Reach Total Level 1500", "Reach a Total Level of 1500.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Reach Total Level 1750", "Reach a Total Level of 1750.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Reach Base Level 40", "Reach level 40 in every skill.", "General", TaskDifficulty.HARD, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Base Level 50", "Reach level 50 in every skill.", "General", TaskDifficulty.HARD, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Base Level 60", "Reach level 60 in every skill.", "General", TaskDifficulty.HARD, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Base Level 70", "Reach level 70 in every skill.", "General", TaskDifficulty.HARD, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach a Prayer Bonus of 30", "Equip enough items to reach a Prayer Bonus of 30 or more.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Restore 75 Prayer Points at an Altar", "Restore 75 or more Prayer Points at any altar.", "General", TaskDifficulty.HARD, TaskType.PRAYER, "Prayer 75", 1);
        addTask("1 Master Clue Scroll", "Open a Reward casket for completing a Master clue scroll.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("25 Master Clue Scrolls", "Open 25 Reward caskets for completing Master clue scrolls.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 25);
        addTask("75 Elite Clue Scrolls", "Open 75 Reward caskets for completing Elite clue scrolls.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 75);
        addTask("75 Hard Clue Scrolls", "Open 75 Reward caskets for completing hard clue scrolls.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 75);
        addTask("100 Collection log slots", "Obtain 100 unique Collection Log slots.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 100);
        addTask("200 Collection log slots", "Obtain 200 unique Collection Log slots.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 200);

        // Combat Achievements / Speed / Echo
        addTask("Combat Achievements Easy Tier", "Obtain enough points to unlock the easy tier of Combat Achievements.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("Complete 1 Speed Task", "Complete a Combat Achievement Speed task.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("Complete 5 Speed Tasks", "Complete 5 Combat Achievement Speed tasks.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 5);
        addTask("Complete all tasks for 1 boss", "Complete all Combat Achievements for 1 boss.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("Complete all tasks for 3 bosses", "Complete all Combat Achievements for 3 bosses.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 3);
        addTask("Defeat 1 unique Echo Boss", "Defeat 1 unique Echo Boss.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("Defeat 2 unique Echo Bosses", "Defeat 2 unique Echo Bosses.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 2);
        addTask("Defeat 3 unique Echo Bosses", "Defeat 3 unique Echo Bosses.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 3);
        addTask("Defeat 25 Echo Bosses", "Defeat 25 Echo Bosses.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 25);
        addTask("Equip one unique Echo Item", "Equip one unique echo item.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Equip 2 unique Echo Items", "Equip 2 unique Echo Items.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 2);
        addTask("Equip 3 unique Echo Items", "Equip 3 unique Echo Items.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 3);
        defeatNpc("Defeat 20 Superior slayer creatures", "Defeat 20 Superior slayer creatures.", "General", TaskDifficulty.HARD, "Slayer 5 + Bigger and Badder or T3 Relic", "Superior");
        defeatNpc("Defeat 100 Superior slayer creatures", "Defeat 100 Superior slayer creatures.", "General", TaskDifficulty.HARD, "Slayer 5 + Bigger and Badder or T3 Relic", "Superior");
        defeatNpc("Defeat the Abyssal Sire", "Defeat the Abyssal Sire in the Abyssal Nexus.", "General", TaskDifficulty.HARD, "Slayer 85", "Abyssal Sire");
        addTask("Offer an Unsired to the Font of Consumption", "Offer an Unsired to the Font of Consumption.", "General", TaskDifficulty.HARD, TaskType.MISC, "Slayer 85", 1, "Unsired");

        // Fletching / Crafting / Smithing / Mining / Cooking / Firemaking / Herblore / Woodcutting
        burnItem("Burn 100 Yew Logs", "Burn 100 Yew Logs.", "General", TaskDifficulty.HARD, "Firemaking 60", 100, "Yew logs");
        burnItem("Burn Some Magic Logs", "Burn some Magic Logs.", "General", TaskDifficulty.HARD, "Firemaking 75", 1, "Magic logs");
        addTask("Cast a Wave Spell", "Cast any wave spell.", "General", TaskDifficulty.HARD, TaskType.SPELL, "Magic 62", 1);
        catchItem("Catch 100 Shark", "Catch 100 Raw Shark whilst Fishing.", "General", TaskDifficulty.HARD, "Fishing 76", 100, "Raw shark");
        catchItem("Catch 200 Implings in Puro-Puro", "Catch 200 Implings in Puro-Puro.", "General", TaskDifficulty.HARD, "Hunter 17", 200, "Baby impling", "Young impling", "Gourmet impling", "Earth impling", "Essence impling", "Eclectic impling");
        catchItem("Catch a Dragon Impling", "Catch a Dragon Impling.", "General", TaskDifficulty.HARD, "Hunter 83", "Dragon impling");
        chopItem("Chop 100 Yew Logs", "Chop 100 Yew Logs.", "General", TaskDifficulty.HARD, "Woodcutting 60", 100, "Yew logs");
        chopItem("Chop 75 Magic Logs", "Chop 75 Magic Logs from Magic Trees.", "General", TaskDifficulty.HARD, "Woodcutting 75", 75, "Magic logs");
        cleanItem("Clean 100 Grimy Avantoe", "Clean 100 Grimy Avantoe.", "General", TaskDifficulty.HARD, "Herblore 48", 100, "Grimy avantoe");
        cleanItem("Clean 50 Grimy Lantadyme", "Clean 50 Grimy Lantadyme.", "General", TaskDifficulty.HARD, "Herblore 67", 50, "Grimy lantadyme");
        cookItem("Cook 100 Sharks", "Cook 100 Raw Sharks.", "General", TaskDifficulty.HARD, "Cooking 80", 100, "Raw shark");
        cookItem("Cook 100 pies", "Cook 100 pies.", "General", TaskDifficulty.HARD, "Cooking 10", 100, "Uncooked pie");
        cookItem("Cook 25 Meat Pies", "Cook 25 Meat pies.", "General", TaskDifficulty.HARD, "Cooking 20", 25, "Uncooked meat pie");
        addTask("Ferment 100 Jugs of Wine", "Successfully ferment 100 Jugs of Wine.", "General", TaskDifficulty.HARD, TaskType.COOK_ITEM, "Cooking 35", 100, "Jug of wine");
        addTask("Craft 100 Unpowered Orbs", "Craft 100 Unpowered Orbs.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Crafting 46", 100, "Molten glass");
        addTask("Craft 2500 Essence Into Runes", "Craft 2500 essence into runes.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "", 2500, "Rune essence", "Pure essence");
        addTask("Craft 30 Blue Dragonhide Bodies", "Craft 30 Blue Dragonhide Bodies.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Crafting 71", 30, "Blue dragon leather");
        addTask("Craft a Dragonstone Amulet", "Craft a Dragonstone Amulet.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Crafting 80", 1, "Dragonstone", "Gold bar", "Amulet mould");
        addTask("Create a Mithril Grapple", "Create a Mithril Grapple.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Fletching 59, Smithing 59", 1, "Mith grapple tip", "Mithril bolts (unf)");
        addTask("Create a Red D'hide Shield", "Create a Red d'hide shield.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Crafting 76", 1, "Red dragon leather");
        addTask("Fletch 200 Magic Longbow (u)", "Fletch 200 Magic Longbow (u).", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Fletching 85", 200, "Magic logs");
        addTask("Fletch 50 Yew longbow (u)", "Fletch 50 Yew longbow (u).", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Fletching 70", 50, "Yew logs");
        addTask("Fletch a Magic Shield", "Fletch a Magic Shield.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Fletching 87", 1, "Magic logs");
        addTask("Fletch a Rune Crossbow", "Fletch a Rune Crossbow.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Fletching 69", 1, "Runite limbs", "Runite crossbow (u)");
        addTask("Make 20 Ranging Potions", "Make 20 Ranging Potions.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Herblore 72", 20, "Dwarf weed potion (unf)", "Wine of zamorak");
        addTask("Make a Saradomin Brew", "Make a Saradomin Brew.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Herblore 81", 1, "Toadflax potion (unf)", "Crushed nest");
        mineItem("Mine 50 Adamantite Ore", "Mine 50 Adamantite Ore.", "General", TaskDifficulty.HARD, "Mining 70", 50, "Adamantite ore");
        addTask("Mine a shooting star", "Mine a shooting star.", "General", TaskDifficulty.HARD, TaskType.MISC, "Mining 10", 1, "Stardust");
        addTask("Obtain a High level tree seed from a birds nest", "Obtain a Magic, Dragonfruit, Redwood or Spirit seed from a birds nest.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1, "Magic seed", "Dragonfruit tree seed", "Redwood tree seed", "Spirit seed");
        craftItem("Smelt a Runite Bar", "Use a Furnace to smelt a Runite Bar.", "General", TaskDifficulty.HARD, "Smithing 85", "Runite ore", "Coal");
        addTask("Smith a Rune Item", "Smith a Runite Item.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Smithing 85", 1, "Runite bar");
        addTask("Steal From a Gem Stall", "Steal from a Gem Stall.", "General", TaskDifficulty.HARD, TaskType.MISC, "Thieving 75", 1);
        addTask("Blow 100 Light Orbs", "Blow 100 Empty light orbs.", "General", TaskDifficulty.HARD, TaskType.CRAFT_ITEM, "Crafting 87", 100, "Empty light orb");
        addTask("Beat Jacky Jester", "Successfully copy Jacky the Jester to win.", "General", TaskDifficulty.HARD, TaskType.MISC, "Construction 39", 1);

        // Equipment
        equipItem("Equip a Combination Battlestaff or Mystic Staff", "Equip a Combination Battlestaff or Combination Mystic Staff.", "General", TaskDifficulty.HARD, "Magic 30, Attack 30",
            "Mud battlestaff", "Lava battlestaff", "Steam battlestaff", "Smoke battlestaff", "Mist battlestaff", "Dust battlestaff",
            "Mystic mud staff", "Mystic lava staff", "Mystic steam staff", "Mystic smoke staff", "Mystic mist staff", "Mystic dust staff");
        equipItem("Equip a Dragon Weapon", "Equip any Dragon weapon.", "General", TaskDifficulty.HARD, "Attack 60",
            "Dragon scimitar", "Dragon longsword", "Dragon dagger", "Dragon mace", "Dragon battleaxe", "Dragon 2h sword", "Dragon spear", "Dragon halberd", "Dragon hasta", "Dragon sword", "Dragon claws", "Dragon warhammer");
        equipItem("Equip a Full Black Dragonhide Set", "Equip Black d'hide body, chaps, and vambraces.", "General", TaskDifficulty.HARD, "Ranged 70, Defence 40",
            "Black d'hide body", "Black d'hide chaps", "Black d'hide vambs");
        equipItem("Equip a Full God Dragonhide Set", "Equip Blessed Coif, Body and Chaps aligned to the same god.", "General", TaskDifficulty.HARD, "Ranged 70, Defence 40",
            "Guthix coif", "Guthix body", "Guthix chaps",
            "Saradomin coif", "Saradomin body", "Saradomin chaps",
            "Zamorak coif", "Zamorak body", "Zamorak chaps");
        equipItem("Equip a Full God Rune Set", "Equip a full set of God Rune Armour aligned to the same god.", "General", TaskDifficulty.HARD, "Defence 40",
            "Ancient platebody", "Ancient plateskirt", "Ancient platelegs", "Ancient full helm",
            "Armadyl platebody", "Armadyl plateskirt", "Armadyl platelegs", "Armadyl full helm",
            "Bandos platebody", "Bandos plateskirt", "Bandos platelegs", "Bandos full helm",
            "Guthix platebody", "Guthix plateskirt", "Guthix platelegs", "Guthix full helm",
            "Saradomin platebody", "Saradomin plateskirt", "Saradomin platelegs", "Saradomin full helm",
            "Zamorak platebody", "Zamorak plateskirt", "Zamorak platelegs", "Zamorak full helm");
        equipItem("Equip a Full Rune Set", "Equip Rune Platebody, Full Helm and Platelegs/Plateskirt.", "General", TaskDifficulty.HARD, "Defence 40",
            "Rune platebody", "Rune full helm", "Rune platelegs", "Rune plateskirt");
        equipItem("Equip a Full Vestment Set", "Equip a full set of Vestment robes aligned to the same god.", "General", TaskDifficulty.HARD, "Prayer 60, Magic 40",
            "Ancient mitre", "Ancient robe top", "Ancient robe legs", "Ancient cloak", "Ancient stole", "Ancient crozier",
            "Armadyl mitre", "Armadyl robe top", "Armadyl robe legs", "Armadyl cloak", "Armadyl stole", "Armadyl crozier",
            "Bandos mitre", "Bandos robe top", "Bandos robe legs", "Bandos cloak", "Bandos stole", "Bandos crozier",
            "Guthix mitre", "Guthix robe top", "Guthix robe legs", "Guthix cloak", "Guthix stole", "Guthix crozier",
            "Saradomin mitre", "Saradomin robe top", "Saradomin robe legs", "Saradomin cloak", "Saradomin stole", "Saradomin crozier",
            "Zamorak mitre", "Zamorak robe top", "Zamorak robe legs", "Zamorak cloak", "Zamorak stole", "Zamorak crozier");
        equipItem("Equip a Gilded or Trimmed Wizard Item", "Equip a Gilded armour item or trimmed wizard item.", "General", TaskDifficulty.HARD, "",
            "Gilded platebody", "Gilded plateskirt", "Gilded platelegs", "Gilded full helm", "Gilded kiteshield",
            "Wizard robe (t)", "Wizard hat (t)");
        equipItem("Equip a Magic Shortbow", "Equip a Magic Shortbow.", "General", TaskDifficulty.HARD, "Ranged 50", "Magic shortbow");
        equipItem("Equip a Mist Battlestaff", "Equip a Mist battlestaff.", "General", TaskDifficulty.HARD, "Slayer 5, Attack 30, Magic 30", "Mist battlestaff");
        equipItem("Equip a Rune Crossbow", "Equip a Rune Crossbow.", "General", TaskDifficulty.HARD, "Ranged 61", "Rune crossbow");
        equipItem("Equip a Saradomin Sword", "Equip a Saradomin Sword.", "General", TaskDifficulty.HARD, "Attack 70, Agility 70", "Saradomin sword");
        equipItem("Equip a Twinflame staff", "Equip a Twinflame staff.", "General", TaskDifficulty.HARD, "Magic 60", "Twinflame staff");
        equipItem("Equip a Zamorakian Spear", "Equip a Zamorakian Spear.", "General", TaskDifficulty.HARD, "Attack 70, Hitpoints 70", "Zamorakian spear");
        equipItem("Equip a two-handed Axe", "Equip a Felling axe.", "General", TaskDifficulty.HARD, "",
            "Bronze felling axe", "Iron felling axe", "Steel felling axe", "Black felling axe", "Mithril felling axe", "Adamant felling axe", "Rune felling axe", "Dragon felling axe", "Crystal felling axe", "3rd age felling axe");
        equipItem("Equip a Full Prospector Outfit", "Equip a full set of Prospector gear.", "General", TaskDifficulty.HARD, "Mining 30",
            "Prospector helmet", "Prospector jacket", "Prospector legs", "Prospector boots");
        equipItem("Equip a Full Rogue Outfit", "Equip a full set of Rogue gear.", "General", TaskDifficulty.HARD, "Thieving 50, Agility 50",
            "Rogue mask", "Rogue top", "Rogue trousers", "Rogue gloves", "Rogue boots");
        equipItem("Equip a Full Void Knight Set", "Equip a full set of Void Knight equipment.", "General", TaskDifficulty.HARD, "Combat stats 42",
            "Void knight helm", "Void knight top", "Void knight robe", "Void knight gloves");
        equipItem("Equip a Pair of Dragon Boots in Asgarnia", "Equip a pair of dragon boots in Asgarnia.", "Asgarnia", TaskDifficulty.HARD, "Defence 60, Slayer 83", "Dragon boots");
        equipItem("Equip some Holy Moleys", "Equip some Holy moleys.", "Asgarnia", TaskDifficulty.HARD, "Prayer 31", "Holy moleys");
        equipItem("Equip a set of recoloured Graceful", "Equip a full set of recoloured Graceful.", "General", TaskDifficulty.HARD, "",
            "Graceful hood", "Graceful top", "Graceful legs", "Graceful gloves", "Graceful boots", "Graceful cape");
        equipItem("Equip an Ornament Kit Item", "Equip any item with an Ornament Kit attached.", "General", TaskDifficulty.HARD, "",
            "Dragon scimitar (or)", "Dragon defender (or)", "Dragon platebody (or)", "Dragon chainbody (or)", "Dragon 2h sword (or)",
            "Dragon mace (or)", "Dragon dagger (or)", "Abyssal whip (or)", "Amulet of fury (or)", "Fire cape (or)", "Kiteshield (or)",
            "Granite maul (or)");
        equipItem("Equip some Ranger Boots", "Equip a pair of Ranger Boots.", "General", TaskDifficulty.HARD, "Ranged 40", "Ranger boots");
        addTask("Purchase a Celestial ring", "Purchase a Celestial ring from Dusuri's Star Shop.", "Asgarnia", TaskDifficulty.HARD, TaskType.MISC, "Mining 10", 1, "Celestial ring");

        // STASH / Clue fills
        addTask("Fill 10 Elite Clue Collection Log Slots", "Fill 10 slots in the Elite Clue section.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 10);
        addTask("Fill 25 Elite Clue Collection Log Slots", "Fill 25 slots in the Elite Clue section.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 25);
        addTask("Fill 30 Hard Clue Collection Log Slots", "Fill 30 slots in the Hard Clue section.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 30);
        addTask("Fill 40 Medium Clue Collection Log Slots", "Fill 40 slots in the Medium Clue section.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 40);
        addTask("Fill 5 Master Clue Collection Log Slots", "Fill 5 slots in the Master Clue section.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 5);
        addTask("Fill 50 Easy Clue Collection Log Slots", "Fill 50 slots in the Easy Clue section.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 50);
        addTask("Fill a Giant Pouch", "Fill a Giant Pouch with Essence.", "General", TaskDifficulty.HARD, TaskType.MISC, "Runecraft 75", 1, "Giant pouch");
        addTask("Fill a Hard STASH Unit", "Build a Hard Stash unit and fill it.", "General", TaskDifficulty.HARD, TaskType.MISC, "Construction 55", 1);
        addTask("Gain 10 Unique Items From Elite Clues", "Gain 10 unique items from Elite Clue Scroll Reward Caskets.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 10);
        addTask("Gain 10 Unique Items From Master Clues", "Gain 10 unique items from Master Clue Scroll Reward Caskets.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 10);
        addTask("Gain 25 Unique Items From Medium Clues", "Gain 25 unique items from Medium Clue Scroll Reward Caskets.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 25);
        addTask("Gain 50 Unique Items From Hard Clues", "Gain 50 unique items from Hard Clue Scroll Reward Caskets.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 50);
        addTask("Gain a Unique Item From an Elite Clue", "Gain a unique item from an Elite Clue Scroll Reward Casket.", "General", TaskDifficulty.HARD, TaskType.MISC, "", 1);
        addTask("Cast Paddewwa Teleport", "Cast the Paddewwa Teleport spell.", "General", TaskDifficulty.HARD, TaskType.SPELL, "Magic 54", 1);
        addTask("Cast Ice Blitz", "Cast the Ice Blitz spell.", "General", TaskDifficulty.HARD, TaskType.SPELL, "Magic 82", 1);

        // =====================================================================
        // HARD TASKS (80 pts)
        // =====================================================================
        defeatNpc("Defeat a Demonic Gorilla", "Defeat a Demonic Gorilla in the Crash Site Cavern.", "General", TaskDifficulty.HARD, "", "Demonic gorilla");
        defeatNpc("Defeat a Mithril Dragon", "Defeat a Mithril Dragon in the Ancient Cavern.", "General", TaskDifficulty.HARD, "", "Mithril dragon");
        defeatNpc("Defeat a Basilisk Knight", "Defeat a Basilisk Knight in Jormungand's Prison.", "Fremennik", TaskDifficulty.HARD, "Slayer 60 + The Fremennik Exiles", "Basilisk Knight");
        defeatNpc("Defeat Phantom Muspah", "Defeat Phantom Muspah.", "General", TaskDifficulty.HARD, "", "Phantom Muspah");
        defeatNpc("Defeat the Kraken Boss 50 Times", "Defeat the Kraken boss in Kraken Cove 50 times.", "General", TaskDifficulty.HARD, "Slayer 87", "Kraken");
        defeatNpc("Defeat Skotizo 1 Time", "Defeat Skotizo beneath the Catacombs of Kourend.", "Kourend", TaskDifficulty.HARD, "", "Skotizo");
        defeatNpc("Defeat Araxxor 1 Time", "Defeat the Araxxor in Morytania Spider cave.", "Morytania", TaskDifficulty.HARD, "Slayer 92", "Araxxor");
        defeatNpc("Defeat the Royal Titans solo", "Defeat the Royal Titans without any other player joining.", "General", TaskDifficulty.HARD, "", "Royal Titans");
        defeatNpc("Defeat the Corporeal Beast", "Defeat the Corporeal Beast in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", "Corporeal Beast");
        defeatNpc("Defeat Zalcano", "Defeat Zalcano in Prifddinas.", "Tirannwn", TaskDifficulty.HARD, "", "Zalcano");

        equipItem("Equip a Dragon Defender", "Equip a Dragon Defender.", "General", TaskDifficulty.HARD, "Defence 60 + Warriors' Guild access", "Dragon defender");
        equipItem("Equip a Trident of the Seas", "Equip a Trident of the Seas.", "General", TaskDifficulty.HARD, "Magic 75, Slayer 87", "Trident of the seas");
        equipItem("Equip any Full Barrows Armour Set", "Equip a full set of any Barrows armour + weapon.", "Morytania", TaskDifficulty.HARD, "Defence 70",
            "Guthan's helm", "Guthan's platebody", "Guthan's chainskirt", "Guthan's warspear",
            "Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe",
            "Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail",
            "Torag's helm", "Torag's platebody", "Torag's platelegs", "Torag's hammers",
            "Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Karil's crossbow",
            "Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Ahrim's staff");
        equipItem("Equip a Dark Bow in Tirannwn", "Equip a Dark Bow in Tirannwn.", "Tirannwn", TaskDifficulty.HARD, "Ranged 60, Slayer 90", "Dark bow");
        equipItem("Equip a Dragon 2-Handed Sword in the Wilderness", "Equip a Dragon 2h Sword in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "Attack 60", "Dragon 2h sword");
        equipItem("Equip a Malediction Ward", "Equip a Malediction Ward.", "Wilderness", TaskDifficulty.HARD, "Defence 60", "Malediction ward");

        craftItem("Assemble a Slayer Helm", "Assemble a Slayer Helm.", "Morytania", TaskDifficulty.HARD, "Slayer 58, Crafting 55 + Malevolent Masquerade",
            "Black mask", "Spiny helmet", "Nose peg", "Earmuffs", "Facemask", "Enchanted gem");
        craftItem("Create an Amulet of Blood Fury", "Create an Amulet of Blood Fury.", "Morytania", TaskDifficulty.HARD, "Crafting 90, Magic 87 + Sins of the Father",
            "Amulet of fury", "Blood shard");

        addTask("Complete the Corrupted Gauntlet", "Complete the Corrupted Gauntlet in Prifddinas.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("Complete Tzhaar-Ket-Rak's third challenge", "Complete Tzhaar-Ket-Rak's third challenge.", "General", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1);
        addTask("25 Chambers of Xeric", "Complete the Chambers of Xeric Normal or Challenge Mode 25 times.", "Kourend", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 25);

        // =====================================================================
        // ELITE TASKS (200 pts)
        // =====================================================================
        defeatNpc("Defeat Awakened Whisperer", "Defeat Awakened Whisperer.", "General", TaskDifficulty.ELITE, "", "Whisperer");
        defeatNpc("Defeat Awakened Leviathan", "Defeat Awakened Leviathan.", "General", TaskDifficulty.ELITE, "", "Leviathan");
        defeatNpc("Defeat Awakened Duke Sucellus", "Defeat Awakened Duke Sucellus.", "General", TaskDifficulty.ELITE, "", "Duke Sucellus");
        defeatNpc("Defeat Awakened Vardorvis", "Defeat Awakened Vardorvis.", "General", TaskDifficulty.ELITE, "", "Vardorvis");
        defeatNpc("Defeat Vorkath 5 Times Without Special Damage", "Defeat Vorkath 5 times in a row without taking special attack damage.", "Fremennik", TaskDifficulty.ELITE, "", "Vorkath");
        defeatNpc("Defeat Phosani's Nightmare", "Defeat Phosani's Nightmare in the Sisterhood Sanctuary.", "Morytania", TaskDifficulty.ELITE, "", "Phosani's Nightmare");

        equipItem("Equip a Godsword", "Equip any Godsword.", "General", TaskDifficulty.ELITE, "Attack 75, Smithing 80",
            "Armadyl godsword", "Bandos godsword", "Saradomin godsword", "Zamorak godsword");
        equipItem("Equip the Bellator Ring", "Equip the Bellator Ring.", "General", TaskDifficulty.ELITE, "", "Bellator ring");
        equipItem("Equip the Venator Ring", "Equip the Venator Ring.", "General", TaskDifficulty.ELITE, "", "Venator ring");
        equipItem("Equip the Magus Ring", "Equip the Magus Ring.", "General", TaskDifficulty.ELITE, "", "Magus ring");
        equipItem("Equip a Piece of Masori Armour", "Equip a Masori Mask, Masori Body or Masori chaps.", "Desert", TaskDifficulty.ELITE, "Ranged 80, Defence 30 + Beneath Cursed Sands",
            "Masori mask", "Masori body", "Masori chaps");
        equipItem("Equip an Abyssal Tentacle", "Equip an Abyssal Tentacle.", "General", TaskDifficulty.ELITE, "Magic 50, Attack 75, Slayer 87", "Abyssal tentacle");
        equipItem("Equip an Occult Necklace", "Equip an Occult Necklace.", "General", TaskDifficulty.ELITE, "Magic 70, Slayer 93", "Occult necklace");
        equipItem("Equip Some Zenyte Jewelry", "Equip any piece of Zenyte Jewelry.", "General", TaskDifficulty.ELITE, "Crafting 89",
            "Necklace of anguish", "Tormented bracelet", "Ring of suffering", "Amulet of torture");
        equipItem("Equip a Fire Cape", "Equip a Fire Cape.", "General", TaskDifficulty.ELITE, "TzHaar Fight Cave completion", "Fire cape");
        equipItem("Equip a piece of Radiant Oathplate", "Equip a piece of Radiant oathplate armour.", "General", TaskDifficulty.ELITE, "Defence 78",
            "Radiant oathplate helm", "Radiant oathplate body", "Radiant oathplate legs");
        equipItem("Equip any Ancestral piece", "Equip an Ancestral Hat, robe top or robe bottom.", "General", TaskDifficulty.ELITE, "Magic 75, Defence 65",
            "Ancestral hat", "Ancestral robe top", "Ancestral robe bottom");
        equipItem("Equip a Dragon Hunter Lance", "Equip a Dragon hunter lance.", "General", TaskDifficulty.ELITE, "Slayer 95, Attack 78", "Dragon hunter lance");
        equipItem("Equip a Dragon Chainbody in the Kharidian Desert", "Equip a Dragon Chainbody in the Kharidian Desert.", "Desert", TaskDifficulty.ELITE, "Defence 60", "Dragon chainbody");
        equipItem("Equip a Piece of the Dagon'Hai Set", "Equip any piece of the Dagon'hai robe set.", "Wilderness", TaskDifficulty.ELITE, "Magic 70, Defence 40",
            "Dagon'hai hat", "Dagon'hai robe top", "Dagon'hai robe bottom",
            "Larran's small key", "Larran's big key");
        equipItem("Equip the Voidwaker", "Equip the Voidwaker.", "Wilderness", TaskDifficulty.ELITE, "Attack 75", "Voidwaker");
        equipItem("Equip Avernic Treads", "Equip Avernic treads.", "General", TaskDifficulty.ELITE, "Defence 80, Magic 80, Ranged 80, Strength 80", "Avernic treads");

        craftItem("Craft a Toxic Blowpipe", "Craft a Toxic Blowpipe.", "General", TaskDifficulty.ELITE, "Fletching 78",
            "Tanzanite fang", "Chisel");

        addTask("Cast Ice Barrage", "Cast the Ice Barrage spell.", "General", TaskDifficulty.ELITE, TaskType.SPELL, "Magic 94", 1);
        addTask("Use a prayer altar to restore 90 prayer in Prifddinas", "Use a prayer altar to restore 90 prayer points in Prifddinas.", "Tirannwn", TaskDifficulty.ELITE, TaskType.PRAYER, "Prayer 90", 1);
        addTask("Imbue a God Cape", "Imbue a Saradomin, Guthix or Zamorak Cape.", "Wilderness", TaskDifficulty.ELITE, TaskType.MISC, "Magic 75 + Mage Arena II",
            1, "Imbued saradomin cape", "Imbued guthix cape", "Imbued zamorak cape");
        addTask("Complete the Theatre of Blood 25 Times", "Complete the Theatre of Blood on Normal or Hard Mode 25 times.", "Morytania", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 25);
        addTask("Complete Wave 12 of Fortis Colosseum", "Complete Wave 12 of Fortis Colosseum.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1);

        // --- Elite: Combat Achievements & Slayer ---
        addTask("Complete 200 Slayer Tasks", "Complete 200 Slayer Tasks.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 200);
        addTask("Complete all tasks for 5 bosses", "Complete all Combat Achievements for 5 different bosses.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 5);
        addTask("Complete all tasks for 10 bosses", "Complete all Combat Achievements for 10 different bosses.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 10);
        addTask("Complete 10 Speed Tasks", "Complete 10 Combat Achievement Speed tasks.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 10);
        addTask("Complete 20 Speed Tasks", "Complete 20 Combat Achievement Speed tasks.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 20);
        addTask("Complete 30 Speed Tasks", "Complete 30 Combat Achievement Speed tasks.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 30);
        addTask("50 Combat Achievements", "Complete 50 Combat Achievements.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 50);
        addTask("100 Combat Achievements", "Complete 100 Combat Achievements.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 100);
        addTask("150 Combat Achievements", "Complete 150 Combat Achievements.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 150);
        addTask("200 Combat Achievements", "Complete 200 Combat Achievements.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 200);
        addTask("250 Combat Achievements", "Complete 250 Combat Achievements.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 250);
        addTask("Combat Achievements Medium Tier", "Obtain enough points to unlock the medium tier of Combat Achievements.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1);
        addTask("Combat Achievements Hard Tier", "Obtain enough points to unlock the hard tier of Combat Achievements.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1);
        addTask("Defeat 4 unique Echo Bosses", "Defeat 4 unique Echo Bosses.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 4);
        addTask("Defeat 75 Echo Bosses", "Defeat 75 Echo Bosses.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 75);
        addTask("Defeat 150 Echo Bosses", "Defeat 150 Echo Bosses.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 150);
        addTask("Equip four unique Echo Items", "Equip four unique echo items.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 4);
        defeatNpc("Defeat 50 Superior slayer creatures", "Defeat 50 Superior slayer creatures.", "General", TaskDifficulty.ELITE, "Bigger and Badder unlocked", "Superior");
        defeatNpc("Defeat the Abyssal Sire 50 Times", "Defeat the Abyssal Sire 50 times.", "General", TaskDifficulty.ELITE, "Slayer 85", "Abyssal Sire");
        defeatNpc("Defeat the Abyssal Sire 150 Times", "Defeat the Abyssal Sire 150 times.", "General", TaskDifficulty.ELITE, "Slayer 85", "Abyssal Sire");
        defeatNpc("Defeat the Abyssal Sire 300 Times", "Defeat the Abyssal Sire 300 times.", "General", TaskDifficulty.ELITE, "Slayer 85", "Abyssal Sire");
        addTask("Abyssal Sire Combat Achievements", "Complete all Combat Achievements for Abyssal Sire.", "General", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1);
        addTask("Activate an Imbued Heart", "Obtain and activate an Imbued heart.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Bigger and Badder unlocked", 1, "Imbued heart");

        // --- Elite: Weapons & armour ---
        equipItem("Equip a Dragonfire Shield", "Equip a Dragonfire Shield.", "General", TaskDifficulty.ELITE, "Defence 75", "Dragonfire shield");
        equipItem("Equip an Abyssal Bludgeon", "Equip an Abyssal Bludgeon.", "General", TaskDifficulty.ELITE, "Attack 70, Strength 70, Slayer 85", "Abyssal bludgeon");
        equipItem("Equip an Abyssal Dagger", "Equip an Abyssal Dagger.", "General", TaskDifficulty.ELITE, "Attack 70, Slayer 85", "Abyssal dagger");
        equipItem("Equip an Abyssal Whip", "Equip an Abyssal Whip.", "General", TaskDifficulty.ELITE, "Attack 70, Slayer 85", "Abyssal whip");
        equipItem("Equip a piece of Virtus", "Equip a Virtus Mask, Robe top, or Robe Bottoms.", "General", TaskDifficulty.ELITE, "Magic 78, Defence 75",
            "Virtus mask", "Virtus robe top", "Virtus robe bottom");
        equipItem("Equip the Soulreaper Axe", "Equip the Soulreaper Axe.", "General", TaskDifficulty.ELITE, "Attack 80, Strength 80", "Soulreaper axe");
        equipItem("Equip an Eternal Slayer Ring", "Craft and equip an Eternal slayer ring.", "General", TaskDifficulty.ELITE, "Slayer 5, Crafting 75", "Eternal slayer ring");

        // --- Elite: Crafting/Herblore/Hunter/Smithing/Fletching ---
        addTask("Craft an Onyx Amulet", "Craft an Onyx Amulet.", "General", TaskDifficulty.ELITE, TaskType.CRAFT_ITEM, "Crafting 90", 1, "Onyx", "Gold bar", "Amulet mould");
        addTask("Make a Super Combat Potion", "Make a Super Combat Potion.", "General", TaskDifficulty.ELITE, TaskType.CRAFT_ITEM, "Herblore 90", 1, "Torstol", "Super attack(4)", "Super strength(4)", "Super defence(4)");
        addTask("Fletch 100 Dragon Javelins", "Fletch 100 Dragon Javelins.", "General", TaskDifficulty.ELITE, TaskType.CRAFT_ITEM, "Fletching 92", 100, "Dragon javelin heads", "Javelin shaft");
        addTask("Mine a size 8+ shooting star", "Mine a shooting star which is size 8 or 9.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Mining 80", 1, "Stardust");
        addTask("Catch 500 Chinchompas", "Catch 500 Chinchompas.", "General", TaskDifficulty.ELITE, TaskType.CATCH_ITEM, "Hunter 53", 500, "Chinchompa", "Red chinchompa", "Black chinchompa");

        // --- Elite: Clues/Collection ---
        addTask("75 Master Clue Scrolls", "Open 75 Reward caskets for Master clue scrolls.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 75);
        addTask("Fill 25 Master Clue Collection Log Slots", "Fill 25 slots in the Master Clue section.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 25);
        addTask("Gain 25 Unique Items From Elite Clues", "Gain 25 unique items from Elite Clue Scroll Reward Caskets.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 25);
        addTask("Gain 25 Unique Items From Master Clues", "Gain 25 unique items from Master Clue Scroll Reward Caskets.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 25);
        addTask("350 Collection log slots", "Obtain 350 unique Collection Log slots.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 350);
        addTask("500 Collection log slots", "Obtain 500 unique Collection Log slots.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 500);

        // --- Elite: Pets ---
        addTask("Obtain a Boss Pet", "Obtain any Boss Pet.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Obtain a Skilling Pet", "Obtain any Skilling Pet.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);

        // --- Elite: PoH ---
        addTask("Add a Jar to a Display Case", "Add a Jar to a Display Case in your PoH Achievement Gallery.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Smith a Dragonfire Shield", "Use an Anvil to smith a Dragonfire Shield.", "General", TaskDifficulty.ELITE, TaskType.CRAFT_ITEM, "Smithing 90", 1, "Draconic visage", "Anti-dragon shield");

        // --- Elite: Levels & Totals ---
        addTask("Reach Total Level 2000", "Reach a Total Level of 2000.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Reach Total Level 2100", "Reach a Total Level of 2100.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Reach Total Level 2200", "Reach a Total Level of 2200.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Reach Combat Level 120", "Reach Combat Level 120.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Reach Combat Level 126", "Reach Combat Level 126.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Reach Base Level 80", "Reach level 80 in every skill.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Base Level 90", "Reach level 90 in every skill.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "", 1);
        addTask("Achieve Your First Level 90", "Reach level 90 in any skill.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "", 1);
        addTask("Achieve Your First Level 95", "Reach level 95 in any skill.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "", 1);

        // --- Elite: 99s (22 skills) ---
        addTask("Reach Level 99 Attack", "Reach level 99 in Attack.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Attack 99", 1);
        addTask("Reach Level 99 Strength", "Reach level 99 in Strength.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Strength 99", 1);
        addTask("Reach Level 99 Defence", "Reach level 99 in Defence.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Defence 99", 1);
        addTask("Reach Level 99 Hitpoints", "Reach level 99 in Hitpoints.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Hitpoints 99", 1);
        addTask("Reach Level 99 Ranged", "Reach level 99 in Ranged.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Ranged 99", 1);
        addTask("Reach Level 99 Magic", "Reach level 99 in Magic.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Magic 99", 1);
        addTask("Reach Level 99 Prayer", "Reach level 99 in Prayer.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Prayer 99", 1);
        addTask("Reach Level 99 Agility", "Reach level 99 in Agility.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Agility 99", 1);
        addTask("Reach Level 99 Herblore", "Reach level 99 in Herblore.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Herblore 99", 1);
        addTask("Reach Level 99 Thieving", "Reach level 99 in Thieving.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Thieving 99", 1);
        addTask("Reach Level 99 Crafting", "Reach level 99 in Crafting.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Crafting 99", 1);
        addTask("Reach Level 99 Fletching", "Reach level 99 in Fletching.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Fletching 99", 1);
        addTask("Reach Level 99 Slayer", "Reach level 99 in Slayer.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Slayer 99", 1);
        addTask("Reach Level 99 Hunter", "Reach level 99 in Hunter.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Hunter 99", 1);
        addTask("Reach Level 99 Mining", "Reach level 99 in Mining.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Mining 99", 1);
        addTask("Reach Level 99 Smithing", "Reach level 99 in Smithing.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Smithing 99", 1);
        addTask("Reach Level 99 Fishing", "Reach level 99 in Fishing.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Fishing 99", 1);
        addTask("Reach Level 99 Cooking", "Reach level 99 in Cooking.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Cooking 99", 1);
        addTask("Reach Level 99 Firemaking", "Reach level 99 in Firemaking.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Firemaking 99", 1);
        addTask("Reach Level 99 Woodcutting", "Reach level 99 in Woodcutting.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Woodcutting 99", 1);
        addTask("Reach Level 99 Farming", "Reach level 99 in Farming.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Farming 99", 1);
        addTask("Reach Level 99 Runecraft", "Reach level 99 in Runecraft.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Runecraft 99", 1);
        addTask("Reach Level 99 Construction", "Reach level 99 in Construction.", "General", TaskDifficulty.ELITE, TaskType.SKILL_LEVEL, "Construction 99", 1);

        // --- Elite: XP Milestones (Combat) ---
        addTask("Obtain 25 Million XP in a combat skill", "Obtain 25 million experience in any combat skill.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Obtain 35 Million XP in a combat skill", "Obtain 35 million experience in any combat skill.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Obtain 100 Million XP in a combat skill", "Obtain 100 million experience in any combat skill.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);
        addTask("Obtain 25 Million XP in 5 non-combat skills", "Obtain 25M XP in 5 different non-combat skills.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 5);
        addTask("Obtain 35 Million XP in 3 non-combat skills", "Obtain 35M XP in 3 different non-combat skills.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 3);
        addTask("Obtain 100 Million XP in any non-combat skill", "Obtain 100 million XP in any non-combat skill.", "General", TaskDifficulty.ELITE, TaskType.MISC, "", 1);

        // --- Elite: XP Milestones (per skill, 25M/35M/50M) ---
        addTask("Obtain 25 Million Agility XP", "Obtain 25M XP in Agility.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Agility 99", 1);
        addTask("Obtain 25 Million Construction XP", "Obtain 25M XP in Construction.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Construction 99", 1);
        addTask("Obtain 25 Million Cooking XP", "Obtain 25M XP in Cooking.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Cooking 99", 1);
        addTask("Obtain 25 Million Crafting XP", "Obtain 25M XP in Crafting.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Crafting 99", 1);
        addTask("Obtain 25 Million Farming XP", "Obtain 25M XP in Farming.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Farming 99", 1);
        addTask("Obtain 25 Million Firemaking XP", "Obtain 25M XP in Firemaking.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Firemaking 99", 1);
        addTask("Obtain 25 Million Fishing XP", "Obtain 25M XP in Fishing.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Fishing 99", 1);
        addTask("Obtain 25 Million Fletching XP", "Obtain 25M XP in Fletching.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Fletching 99", 1);
        addTask("Obtain 25 Million Herblore XP", "Obtain 25M XP in Herblore.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Herblore 99", 1);
        addTask("Obtain 25 Million Hunter XP", "Obtain 25M XP in Hunter.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Hunter 99", 1);
        addTask("Obtain 25 Million Mining XP", "Obtain 25M XP in Mining.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Mining 99", 1);
        addTask("Obtain 25 Million Prayer XP", "Obtain 25M XP in Prayer.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Prayer 99", 1);
        addTask("Obtain 25 Million Runecraft XP", "Obtain 25M XP in Runecraft.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Runecraft 99", 1);
        addTask("Obtain 25 Million Slayer XP", "Obtain 25M XP in Slayer.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Slayer 99", 1);
        addTask("Obtain 25 Million Smithing XP", "Obtain 25M XP in Smithing.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Smithing 99", 1);
        addTask("Obtain 25 Million Thieving XP", "Obtain 25M XP in Thieving.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Thieving 99", 1);
        addTask("Obtain 25 Million Woodcutting XP", "Obtain 25M XP in Woodcutting.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Woodcutting 99", 1);
        addTask("Obtain 35 Million Agility XP", "Obtain 35M XP in Agility.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Agility 99", 1);
        addTask("Obtain 35 Million Construction XP", "Obtain 35M XP in Construction.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Construction 99", 1);
        addTask("Obtain 35 Million Cooking XP", "Obtain 35M XP in Cooking.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Cooking 99", 1);
        addTask("Obtain 35 Million Crafting XP", "Obtain 35M XP in Crafting.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Crafting 99", 1);
        addTask("Obtain 35 Million Farming XP", "Obtain 35M XP in Farming.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Farming 99", 1);
        addTask("Obtain 35 Million Firemaking XP", "Obtain 35M XP in Firemaking.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Firemaking 99", 1);
        addTask("Obtain 35 Million Fishing XP", "Obtain 35M XP in Fishing.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Fishing 99", 1);
        addTask("Obtain 35 Million Fletching XP", "Obtain 35M XP in Fletching.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Fletching 99", 1);
        addTask("Obtain 35 Million Herblore XP", "Obtain 35M XP in Herblore.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Herblore 99", 1);
        addTask("Obtain 35 Million Hunter XP", "Obtain 35M XP in Hunter.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Hunter 99", 1);
        addTask("Obtain 35 Million Mining XP", "Obtain 35M XP in Mining.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Mining 99", 1);
        addTask("Obtain 35 Million Prayer XP", "Obtain 35M XP in Prayer.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Prayer 99", 1);
        addTask("Obtain 35 Million Runecraft XP", "Obtain 35M XP in Runecraft.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Runecraft 99", 1);
        addTask("Obtain 35 Million Slayer XP", "Obtain 35M XP in Slayer.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Slayer 99", 1);
        addTask("Obtain 35 Million Smithing XP", "Obtain 35M XP in Smithing.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Smithing 99", 1);
        addTask("Obtain 35 Million Thieving XP", "Obtain 35M XP in Thieving.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Thieving 99", 1);
        addTask("Obtain 35 Million Woodcutting XP", "Obtain 35M XP in Woodcutting.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Woodcutting 99", 1);
        addTask("Obtain 50 Million Attack XP", "Obtain 50M XP in Attack.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Attack 99", 1);
        addTask("Obtain 50 Million Strength XP", "Obtain 50M XP in Strength.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Strength 99", 1);
        addTask("Obtain 50 Million Defence XP", "Obtain 50M XP in Defence.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Defence 99", 1);
        addTask("Obtain 50 Million Hitpoints XP", "Obtain 50M XP in Hitpoints.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Hitpoints 99", 1);
        addTask("Obtain 50 Million Ranged XP", "Obtain 50M XP in Ranged.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Ranged 99", 1);
        addTask("Obtain 50 Million Magic XP", "Obtain 50M XP in Magic.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Magic 99", 1);
        addTask("Obtain 50 Million Prayer XP", "Obtain 50M XP in Prayer.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Prayer 99", 1);
        addTask("Obtain 50 Million Agility XP", "Obtain 50M XP in Agility.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Agility 99", 1);
        addTask("Obtain 50 Million Construction XP", "Obtain 50M XP in Construction.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Construction 99", 1);
        addTask("Obtain 50 Million Cooking XP", "Obtain 50M XP in Cooking.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Cooking 99", 1);
        addTask("Obtain 50 Million Crafting XP", "Obtain 50M XP in Crafting.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Crafting 99", 1);
        addTask("Obtain 50 Million Farming XP", "Obtain 50M XP in Farming.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Farming 99", 1);
        addTask("Obtain 50 Million Firemaking XP", "Obtain 50M XP in Firemaking.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Firemaking 99", 1);
        addTask("Obtain 50 Million Fishing XP", "Obtain 50M XP in Fishing.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Fishing 99", 1);
        addTask("Obtain 50 Million Fletching XP", "Obtain 50M XP in Fletching.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Fletching 99", 1);
        addTask("Obtain 50 Million Herblore XP", "Obtain 50M XP in Herblore.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Herblore 99", 1);
        addTask("Obtain 50 Million Hunter XP", "Obtain 50M XP in Hunter.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Hunter 99", 1);
        addTask("Obtain 50 Million Mining XP", "Obtain 50M XP in Mining.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Mining 99", 1);
        addTask("Obtain 50 Million Runecraft XP", "Obtain 50M XP in Runecraft.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Runecraft 99", 1);
        addTask("Obtain 50 Million Slayer XP", "Obtain 50M XP in Slayer.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Slayer 99", 1);
        addTask("Obtain 50 Million Smithing XP", "Obtain 50M XP in Smithing.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Smithing 99", 1);
        addTask("Obtain 50 Million Thieving XP", "Obtain 50M XP in Thieving.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Thieving 99", 1);
        addTask("Obtain 50 Million Woodcutting XP", "Obtain 50M XP in Woodcutting.", "General", TaskDifficulty.ELITE, TaskType.MISC, "Woodcutting 99", 1);

        // =====================================================================
        // MASTER TASKS (400 pts)
        // =====================================================================
        defeatNpc("Defeat Nex Solo", "Defeat Nex in a private instance without help from any other player.", "General", TaskDifficulty.MASTER, "", "Nex");

        equipItem("Equip the Osmumten's Fang (or)", "Equip Osmumten's Fang (or).", "Desert", TaskDifficulty.MASTER, "Attack 82 + Beneath Cursed Sands", "Osmumten's fang (or)");
        equipItem("Equip an Infernal Cape", "Equip an Infernal Cape.", "General", TaskDifficulty.MASTER, "The Inferno completion", "Infernal cape");
        equipItem("Equip a Corrupted Weapon", "Equip a Corrupted Blade of Saeldor or Bow of Faerdhinen.", "Tirannwn", TaskDifficulty.MASTER, "Attack 80 or Ranged 80 + Agility 70",
            "Blade of saeldor (c)", "Bow of faerdhinen (c)");

        addTask("Complete Tzhaar-Ket-Rak's Special challenge", "Complete Tzhaar-Ket-Rak's league-only challenge.", "General", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 1);

        // --- Master: 400-pt Elite-tier endgame ---
        equipItem("Equip a Scythe of Vitur", "Equip a Scythe of Vitur.", "General", TaskDifficulty.MASTER, "Attack 80, Strength 90", "Scythe of vitur");
        equipItem("Equip a Twisted Bow", "Equip a Twisted bow.", "General", TaskDifficulty.MASTER, "Ranged 85", "Twisted bow");
        equipItem("Equip full Virtus", "Equip full Virtus outfit.", "General", TaskDifficulty.MASTER, "Magic 78, Defence 75",
            "Virtus mask", "Virtus robe top", "Virtus robe bottom");
        equipItem("Equip the Tumeken's Shadow", "Equip the Tumeken's Shadow.", "General", TaskDifficulty.MASTER, "Magic 85", "Tumeken's shadow");
        addTask("Build a Demonic Throne", "Build a Demonic Throne in your PoH.", "General", TaskDifficulty.MASTER, TaskType.MISC, "Construction 99", 1);
        addTask("Obtain 200 Million XP in a combat skill", "Obtain 200M XP in any combat skill.", "General", TaskDifficulty.MASTER, TaskType.MISC, "", 1);
        addTask("Obtain 200 Million XP in any non-combat skill", "Obtain 200M XP in any non-combat skill.", "General", TaskDifficulty.MASTER, TaskType.MISC, "", 1);
        addTask("Obtain 50 Million XP in 3 non-combat skills", "Obtain 50M XP in 3 non-combat skills.", "General", TaskDifficulty.MASTER, TaskType.MISC, "", 3);
        addTask("750 Collection log slots", "Obtain 750 unique Collection Log slots.", "General", TaskDifficulty.MASTER, TaskType.MISC, "", 750);
        addTask("Reach Base Level 95", "Reach level 95 in every skill.", "General", TaskDifficulty.MASTER, TaskType.SKILL_LEVEL, "", 1);
        addTask("Reach Total Level 2277", "Reach the highest possible Total Level of 2277.", "General", TaskDifficulty.MASTER, TaskType.MISC, "", 1);
        addTask("Combat Achievements Elite Tier", "Obtain enough points to unlock the elite tier of Combat Achievements.", "General", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 1);

        // =====================================================================
        // AUDIT ADDITIONS — generated from wiki diff (May 2026)
        // 123 tasks for General + Varlamore that were missing from the database.
        // =====================================================================

        // --- Audit additions: General (medium) (1) ---
        chopItem("Chop Some Logs With a Rune Axe", "Chop any kind of logs using a rune axe.", "General", TaskDifficulty.MEDIUM, "", 1, "rune axe");

        // --- Audit additions: General (hard) (1) ---
        defeatNpc("Defeat 10 Superior slayer creatures", "Defeat 10 Superior slayer creatures.", "General", TaskDifficulty.HARD, "Slayer 5 + Bigger and Badder or T3 Relic", "Superior");

        // --- Audit additions: General (elite) (1) ---
        defeatNpc("Slay an Abyssal Demon", "Slay an Abyssal Demon whilst on an Abyssal Demon Slayer Task.", "General", TaskDifficulty.ELITE, "", "Abyssal Demon");

        // --- Audit additions: Varlamore (medium) (57) ---
        addTask("Complete Wave 1 of Fortis Colosseum", "Complete Wave 1 of Fortis Colosseum.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Fortis Colosseum");
        defeatNpc("Defeat Hueycoatl 1 Time", "Defeat the Hueycoatl in Varlamore.", "Varlamore", TaskDifficulty.MEDIUM, "", "the Hueycoatl");
        defeatNpc("Defeat Amoxliatl 1 Time", "Defeat the Amoxliatl in Varlamore.", "Varlamore", TaskDifficulty.MEDIUM, "", "Amoxliatl");
        defeatNpc("Defeat 10 Frost Crabs", "Defeat 10 Frost Crabs.", "Varlamore", TaskDifficulty.MEDIUM, "", "Frost Crab");
        defeatNpc("Defeat a Dire Wolf", "Defeat a Dire Wolf in Varlamore.", "Varlamore", TaskDifficulty.MEDIUM, "", "Dire Wolf");
        defeatNpc("Defeat a Jaguar without taking any damage from it", "Defeat a fully grown jaguar without taking any damage from it.", "Varlamore", TaskDifficulty.MEDIUM, "", "jaguar");
        defeatNpc("Defeat a Oryx with melee", "Defeat an Oryx in the Avium Savannah with melee.", "Varlamore", TaskDifficulty.MEDIUM, "", "Oryx");
        defeatNpc("Defeat the Moons of Peril", "Defeat the Moons of Peril.", "Varlamore", TaskDifficulty.MEDIUM, "", "Moons of Peril");
        defeatNpc("Defeat the Moons of Peril 10 times", "Defeat the Moons of Peril 10 times.", "Varlamore", TaskDifficulty.MEDIUM, "", "Moons of Peril");
        defeatNpc("Defeat the Moons of Peril 25 times", "Defeat the Moons of Peril 25 times.", "Varlamore", TaskDifficulty.MEDIUM, "", "Moons of Peril");
        equipItem("Equip an egg", "Hold an egg in your hands.", "Varlamore", TaskDifficulty.MEDIUM, "", "Humphrey Dumphrey");
        equipItem("Equip Glacial Temotli", "Equip the Glacial temotli.", "Varlamore", TaskDifficulty.MEDIUM, "", "Glacial temotli");
        equipItem("Equip a piece of Alchemists outfit", "Equip the Alchemist labcoat, pants, or gloves from the Mixology shop.", "Varlamore", TaskDifficulty.MEDIUM, "", "Alchemist labcoat", "Alchemist pants", "Alchemist gloves");
        equipItem("Equip a piece of Hueycoatl armour", "Equip a piece of Hueycoatl armour.", "Varlamore", TaskDifficulty.MEDIUM, "", "Hueycoatl hide armour");
        equipItem("Equip an orange", "Equip an orange.", "Varlamore", TaskDifficulty.MEDIUM, "", "Orange (hat)");
        equipItem("Equip Pendant of Ates", "Equip the Pendant of ates.", "Varlamore", TaskDifficulty.MEDIUM, "", "Pendant of ates");
        equipItem("Equip any piece of armour from the moons of peril", "Equip any piece of armour from the Blood, Frost, or Eclipse moon set.", "Varlamore", TaskDifficulty.MEDIUM, "", "Moon equipment");
        equipItem("Equip Sulphur Blades", "Equip the Sulphur blades.", "Varlamore", TaskDifficulty.MEDIUM, "", "Sulphur blades");
        equipItem("Equip Earthbound Tecpatl", "Equip the Earthbound tecpatl.", "Varlamore", TaskDifficulty.MEDIUM, "", "Earthbound tecpatl");
        addTask("Activate all Statues of Ates", "Activate all statues of ates.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Pendant of ates");
        addTask("Jump on Yama's stepping stones 666 times", "Jump on Yama's stepping stones in his League domain 666 times.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Yama's Lair");
        addTask("Activate the Statue of Ates", "Activate the Statue of Ates in Aldarin.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Statue (Ates)", "Aldarin");
        addTask("Bury some wyrm bones near a Wyrm skeleton", "Bury some wyrm bones or wyrmling bones near the Colossal Wyrm Remains.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "wyrm bones", "wyrmling bones", "Colossal Wyrm Remains");
        addTask("Enter a dark cave in Varlamore", "Enter a dark cave in Varlamore, near the Hunter Guild.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Cave entrance (The Burrow)");
        addTask("Fill a Grape Barrel for the Foreman", "Fill a Grape Barrel for the Foreman in Aldarin.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Grape barrel", "Vineyard foreman", "Aldarin");
        addTask("Complete Death on the Isle", "Complete Death on the Isle.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Death on the Isle");
        addTask("Complete Shadows of Custodia", "Complete the Shadows of Custodia quest.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Shadows of Custodia");
        addTask("Complete the Heart of Darkness", "Complete The Heart of Darkness quest.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "The Heart of Darkness");
        addTask("Complete a Ribbiting Tale", "Complete a Ribbiting Tale of a Lily Pad Labour Dispute.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "The Ribbiting Tale of a Lily Pad Labour Dispute");
        addTask("Complete Meat and Greet", "Complete Meat and Greet.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Meat and Greet");
        addTask("Complete At First Light", "Complete the At First Light quest.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "At First Light");
        craftItem("Make a Greenman statue", "Make a Greenman statue.", "Varlamore", TaskDifficulty.MEDIUM, "", "Greenman statue");
        addTask("Fully decorate a Willow totem in the Auburn Valley", "Fully decorate a Willow totem in the Auburn Valley.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Willow totem");
        mineItem("Mine 20 Mithril Ore in the Stonecutter Outpost", "Mine 20 Mithril Ore in the Stonecutter Outpost.", "Varlamore", TaskDifficulty.MEDIUM, "", 20, "Mithril Ore");
        mineItem("Mine 250 Blessed Bone Shards", "Mine 250 Blessed Bone Shards.", "Varlamore", TaskDifficulty.MEDIUM, "", 250, "Blessed bone shards");
        addTask("Pay an Urchin for Information", "Pay an Urchin for Information in the Stealing Valuables minigame.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Stealing Valuables");
        addTask("Pickpocket a knight of varlamore 20 times", "Pickpocket a Knight of Varlamore 20 times.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Knight of Varlamore");
        addTask("Steal 100 Valuables", "Steal 100 Valuables.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 100, "Valuables");
        addTask("Steal 15 House Keys", "Steal 15 House Keys.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 15, "House keys");
        addTask("Steal 25 Valuables", "Steal 25 Valuables.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 25, "Valuables");
        addTask("Steal a House Key", "Steal a House Key.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "House keys");
        addTask("Steal from the Fortis Spice Stall", "Steal from the Fortis Spice Stall.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Spice Stall");
        addTask("Store some bowstrings inside a bowstring spool", "Store some bowstrings inside a bowstring spool.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "bow strings", "bow string spool");
        addTask("Teleport to Cam Torum using a Calcified Moth", "Teleport to Cam Torum using a Calcified Moth.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Cam Torum", "Calcified moth");
        addTask("Teleport to Civitas illa Fortis", "Teleport to Civitas illa Fortis using the standard spellbook.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1);
        addTask("Use the pottery oven in Civitas", "Use the pottery oven in Civitas to make something out of clay.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "pottery oven", "soft clay");
        addTask("Break down 10 calcified deposits", "Break down 10 Calcified deposits.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Calcified deposit");
        addTask("Build a Quetzal Landing Site", "Build a Quetzal Landing Site.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Landing Site");
        catchItem("Catch a Jerboa", "Catch an Embertailed jerboa.", "Varlamore", TaskDifficulty.MEDIUM, "", 1, "Embertailed jerboa");
        addTask("Complete 10 Hunter Rumours", "Complete 10 Hunter Rumours.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 10);
        addTask("Complete 10 laps of the Varlamore Agility Course", "Complete 10 laps of the Colossal Wyrm Agility Course.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 10, "Colossal Wyrm Agility Course");
        addTask("Complete 25 Hunter Rumours", "Complete 25 Hunter Rumours.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 25);
        addTask("Create a Quetzal Whistle", "Create a Quetzal Whistle.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Quetzal Whistle");
        addTask("Exchange an Ent seed with an Ent", "Exchange an Ent seed with an Ent.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Ent seed", "Ent (Vale Totems)");
        addTask("Fish a House Key", "Fish a House Key.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "House keys");
        addTask("Fully decorate a Maple totem in the Auburn Valley", "Fully decorate a Maple totem in the Auburn Valley.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Maple totem");
        addTask("Fully decorate an Oak totem in the Auburn Valley", "Fully decorate an Oak totem in the Auburn Valley.", "Varlamore", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Oak totem");

        // --- Audit additions: Varlamore (hard) (42) ---
        addTask("Hueycoatl Combat Achievements", "Complete all of the Combat Achievements for Hueycoatl.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Hueycoatl");
        addTask("Amoxliatl Combat Achievements", "Complete all of the Combat Achievements for Amoxliatl.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Amoxliatl");
        addTask("Use the Bank Chest inside Fortis Colosseum", "Use the Bank Chest inside Fortis Colosseum.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Fortis Colosseum");
        addTask("Use the Fortis Salute emote", "Use the Fortis Salute emote.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Fortis Salute");
        defeatNpc("Defeat the Doom of Mokhiatl", "Defeat the Doom of Mokhaiotl at any delve level.", "Varlamore", TaskDifficulty.HARD, "", "Doom of Mokhaiotl");
        defeatNpc("Defeat the Moons of Peril 50 times", "Defeat the Moons of Peril 50 times.", "Varlamore", TaskDifficulty.HARD, "", "Moons of Peril");
        defeatNpc("Defeat Amoxliatl 50 Times", "Defeat Amoxliatl in Varlamore 50 times.", "Varlamore", TaskDifficulty.HARD, "", "Amoxliatl");
        defeatNpc("Defeat Hueycoatl 50 Times", "Defeat Hueycoatl in Varlamore 50 times.", "Varlamore", TaskDifficulty.HARD, "", "Hueycoatl");
        defeatNpc("Defeat Vardorvis", "Defeat Vardorvis.", "Varlamore", TaskDifficulty.HARD, "", "Vardorvis");
        defeatNpc("Defeat Vardorvis 150 times", "Defeat Vardorvis 150 times.", "Varlamore", TaskDifficulty.HARD, "", "Vardorvis");
        defeatNpc("Defeat Vardorvis 50 times", "Defeat Vardorvis 50 times.", "Varlamore", TaskDifficulty.HARD, "", "Vardorvis");
        equipItem("Equip a Fletching knife", "Equip a Fletching knife.", "Varlamore", TaskDifficulty.HARD, "", "Fletching knife");
        equipItem("Equip a Greenman mask", "Equip a Greenman mask.", "Varlamore", TaskDifficulty.HARD, "", "Greenman mask");
        equipItem("Equip a piece of Sunfire Fanatic", "Equip a piece of Sunfire Fanatic armour.", "Varlamore", TaskDifficulty.HARD, "", "Sunfire Fanatic armour");
        equipItem("Equip an Antler guard", "Equip an Antler guard.", "Varlamore", TaskDifficulty.HARD, "", "Antler guard");
        equipItem("Equip Echo Boots", "Equip Echo Boots.", "Varlamore", TaskDifficulty.HARD, "", "Echo Boots");
        equipItem("Equip full Alchemists outfit", "Equip the Alchemist labcoat, pants, and gloves from the Mixology shop.", "Varlamore", TaskDifficulty.HARD, "", "Alchemist labcoat", "Alchemist pants", "Alchemist gloves");
        equipItem("Equip full Blood Moon armour", "Equip full Blood moon armour.", "Varlamore", TaskDifficulty.HARD, "", "Blood moon armour");
        equipItem("Equip full Blue Moon armour", "Equip full Blue moon armour.", "Varlamore", TaskDifficulty.HARD, "", "Blue moon armour");
        equipItem("Equip full Eclipse Moon armour", "Equip full Eclipse moon armour.", "Varlamore", TaskDifficulty.HARD, "", "Eclipse moon armour");
        equipItem("Equip full Guild Hunter Outfit", "Equip full Guild Hunter Outfit.", "Varlamore", TaskDifficulty.HARD, "", "Guild Hunter Outfit");
        equipItem("Equip full Hueycoatl armour", "Equip full Hueycoatl armour.", "Varlamore", TaskDifficulty.HARD, "", "Hueycoatl hide armour");
        equipItem("Equip full Sunfire Fanatic", "Equip a full set of Sunfire Fanatic armour.", "Varlamore", TaskDifficulty.HARD, "", "Sunfire Fanatic armour");
        addTask("Open a chest with the moon key", "Open a chest with the moon key inside the Ruins of Tapoyauik.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Chest (moon key)", "moon key", "Ruins of Tapoyauik");
        addTask("Build all Quetzal landing sites", "Build all quetzal landing sites.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Landing Site");
        addTask("Complete the Final Dawn", "Complete The Final Dawn quest.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "The Final Dawn");
        addTask("Fully decorate a Yew totem in the Auburn Valley", "Fully decorate a Yew totem in the Auburn Valley.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Yew totem");
        craftItem("Make 100 Prayer Regeneration Potions", "Make 100 Prayer Regeneration Potions.", "Varlamore", TaskDifficulty.HARD, "", "Prayer regeneration potion");
        craftItem("Make 50 Goading Potions", "Make 50 Goading Potions.", "Varlamore", TaskDifficulty.HARD, "", "Goading potion");
        craftItem("Make a Greenman carving", "Make a Greenman carving.", "Varlamore", TaskDifficulty.HARD, "", "Greenman carving");
        addTask("Obtain the Huntsman's Kit", "Obtain the Huntsman's Kit.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Huntsman's Kit");
        addTask("Purchase the Reagents Pouch", "Purchase the Reagents Pouch from the Mixology shop.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Reagent pouch");
        addTask("Steal a Blessed Bone Statuette", "Steal a Blessed Bone Statuette.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Blessed Bone Statuette");
        addTask("Check the health of Mahogany Tree in Marcellus's Patch", "Check the health of Mahogany Tree in Marcellus's Patch.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Mahogany tree (Farming)", "Marcellus");
        chopItem("Chop 20 Magic Logs in Varlamore", "Chop 20 Magic Logs in Varlamore.", "Varlamore", TaskDifficulty.HARD, "", 20, "Magic Logs");
        addTask("Complete 50 Hunter Rumours", "Complete 50 Hunter Rumours.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 50, "Hunters' Rumours");
        cookItem("Cook 100 Moonlight Antelopes", "Cook 100 Moonlight Antelopes.", "Varlamore", TaskDifficulty.HARD, "", 100, "Cooked moonlight antelope");
        craftItem("Craft 1000 Sunfire Runes", "Craft 1000 Sunfire Runes.", "Varlamore", TaskDifficulty.HARD, "", "Sunfire rune");
        addTask("Create 100 Jugs of Blessed Sunfire Wine", "Create 100 Jugs of Blessed Sunfire Wine.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 100, "Jug of blessed sunfire wine");
        craftItem("Fletch some Atlatl darts", "Fletch some Atlatl darts.", "Varlamore", TaskDifficulty.HARD, "", "Atlatl darts");
        catchItem("Catch a Moonlight moth bare-handed", "Catch a moonlight moth barehanded.", "Varlamore", TaskDifficulty.HARD, "", 1, "moonlight moth");
        addTask("Fully Decorate a Magic Totem in the Auburn Valley", "Fully decorate a Magic totem in the Auburn Valley.", "Varlamore", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Magic totem");

        // --- Audit additions: Varlamore (elite) (18) ---
        addTask("Perilous Moons Combat Achievements", "Complete all of the Combat Achievements for Perilous Moons.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Moons of Peril");
        addTask("Vardorvis Combat Achievements", "Complete all of the Combat Achievements for Vardorvis.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Vardorvis");
        addTask("Obtain 40,000 Glory", "Obtain 40,000 Glory in the Fortis Colosseum.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 40000, "Fortis Colosseum");
        addTask("Complete 1 Deep delve", "Defeat the Doom of Mokhaiotl at delve level 8.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Doom of Mokhaiotl");
        addTask("Complete 25 Deep delves", "Defeat the Doom of Mokhaiotl at delve level 8 or above 25 times.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 25, "Doom of Mokhaiotl");
        addTask("Complete 75 Deep delves", "Defeat the Doom of Mokhaiotl at delve level 8 or above 75 times.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 75, "Doom of Mokhaiotl");
        defeatNpc("Defeat Vardorvis 300 times", "Defeat Vardorvis 300 times.", "Varlamore", TaskDifficulty.ELITE, "", "Vardorvis");
        defeatNpc("Defeat Sol Heredit 10 times", "Defeat Sol Heredit 10 times.", "Varlamore", TaskDifficulty.ELITE, "", "Sol Heredit");
        defeatNpc("Defeat Sol Heredit 5 times", "Defeat Sol Heredit 5 times.", "Varlamore", TaskDifficulty.ELITE, "", "Sol Heredit");
        equipItem("Equip a Tecu Salamander", "Equip a Tecu Salamander.", "Varlamore", TaskDifficulty.ELITE, "", "Tecu Salamander");
        equipItem("Equip the Confliction gauntlets", "Equip the Confliction gauntlets.", "Varlamore", TaskDifficulty.ELITE, "", "Confliction gauntlets");
        equipItem("Equip the Eye of Ayak", "Equip the Eye of ayak.", "Varlamore", TaskDifficulty.ELITE, "", "Eye of ayak");
        equipItem("Equip the Ultor Ring", "Equip the Ultor Ring.", "Varlamore", TaskDifficulty.ELITE, "", "Ultor Ring");
        equipItem("Equip Tonalztics of Ralos", "Equip Tonalztics of Ralos.", "Varlamore", TaskDifficulty.ELITE, "", "Tonalztics of Ralos");
        addTask("Open the Varlamore Moon Chest", "Open the Varlamore Moon Chest.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Chest (moon key)");
        addTask("Store 10k bowstrings inside a bowstring spool", "Store at least 10,000 bowstrings inside a bowstring spool.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 10000, "bowstring", "bowstring spool");
        addTask("Purchase the Chugging Barrel", "Purchase the Chugging Barrel from the Mixology shop.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Chugging Barrel");
        addTask("Fully decorate a Redwood totem in the Auburn Valley", "Fully decorate a Redwood totem in the Auburn Valley.", "Varlamore", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Redwood totem", "Auburn Valley");

        // --- Audit additions: Varlamore (master) (4) ---
        addTask("Colosseum Combat Achievements", "Complete all of the Combat Achievements for Fortis Colosseum.", "Varlamore", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 1, "Fortis Colosseum");
        addTask("Doom of Mokhaiotl Combat achievements", "Complete all the Doom of Mokhaiotl combat achievements.", "Varlamore", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 1, "Doom of Mokhaiotl");
        addTask("Obtain 58,000 Glory", "Obtain 58,000 Glory in the Fortis Colosseum.", "Varlamore", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 58000, "Fortis Colosseum");
        equipItem("Equip Blessed Dizana's Quiver", "Equip Blessed dizana's quiver.", "Varlamore", TaskDifficulty.MASTER, "", "Blessed dizana's quiver");

        // --- Audit additions: Karamja (elite) (13) ---
        addTask("Complete the Elite Karamja Diary", "Complete all of the Elite tasks in the Karamja Achievement Diary.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Karamja Achievement Diary");
        addTask("Complete the Fight Caves 10 Times", "Complete the TzHaar Fight Cave in Mor Ul Rek 10 times.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "TzHaar Fight Cave", "Mor Ul Rek");
        addTask("Complete the Fight Caves 5 Times", "Complete the TzHaar Fight Cave in Mor Ul Rek 5 times.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "TzHaar Fight Cave", "Mor Ul Rek");
        addTask("Sacrifice a Fire Cape to Access the Inferno", "Sacrifice a Fire Cape to access the Inferno in Mor Ul Rek.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Fire Cape", "Inferno", "Mor Ul Rek");
        addTask("Complete the Inferno 10 Times", "Complete the Inferno in Mor Ul Rek 10 times.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Inferno", "Mor Ul Rek");
        addTask("Complete the Inferno 5 Times", "Complete the Inferno in Mor Ul Rek 5 times.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Inferno", "Mor Ul Rek");
        addTask("Complete Tzhaar-Ket-Rak's fifth challenge", "Complete Tzhaar-Ket-Rak's fifth challenge.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Tzhaar-Ket-Rak", "TzHaar-Ket-Rak's Challenges");
        addTask("Complete Tzhaar-Ket-Rak's fourth challenge", "Complete Tzhaar-Ket-Rak's fourth challenge.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Tzhaar-Ket-Rak", "TzHaar-Ket-Rak's Challenges");
        addTask("Complete Tzhaar-Ket-Rak's sixth challenge", "Complete Tzhaar-Ket-Rak's sixth challenge.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Tzhaar-Ket-Rak", "TzHaar-Ket-Rak's Challenges");
        addTask("Pay Saniboch for Permanent Access", "Pay Saniboch for Permanent Access into his dungeon.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Saniboch", "Brimhaven Dungeon");
        addTask("Purchase an Onyx in Mor Ul Rek", "Purchase an Onyx from an Ore and Gem Store in Mor Ul Rek.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Uncut onyx", "TzHaar-Hur-Lek's Ore and Gem Store", "Mor Ul Rek");
        addTask("Pickpocket a Diamond From a TzHaar", "Pickpocket an uncut diamond from a TzHaar-Hur.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "uncut diamond", "TzHaar-Hur");
        addTask("Grow a spirit tree on Karamja", "Check the health of a Spirit tree you've grown on Karamja.", "Karamja", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Spirit Tree (Farming)");

        // --- Audit additions: Karamja (master) (6) ---
        addTask("The Inferno Combat Achievements", "Complete all of the Combat Achievements for The Inferno.", "Karamja", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 1, "Inferno");
        addTask("TzHaar-Ket-Rak's Combat Achievements", "Complete all of the Combat Achievements for TzHaar-Ket-Rak's Challenges.", "Karamja", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 1, "TzHaar-Ket-Rak's Challenges");
        addTask("The Fight Caves Combat Achievements", "Complete all of the Combat Achievements for The Fight Caves.", "Karamja", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 1, "Fight Caves");
        defeatNpc("Defeat 6,6,6 Unbound Jal-tok Jad", "Defeat 18 Unbound JalTok-Jad's in the infinite Jad challenge.", "Karamja", TaskDifficulty.MASTER, "", "Unbound JalTok-Jad");
        equipItem("Equip a Pirate Hook", "Equip a Pirate Hook from Brimhaven Agility Arena.", "Karamja", TaskDifficulty.MASTER, "", "Pirate's hook");
        addTask("Complete the Inferno 15 Times", "Complete the Inferno in Mor Ul Rek 15 times.", "Karamja", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 1, "Inferno", "Mor Ul Rek");

        // --- Audit additions: Tirannwn (easy) (6) ---
        defeatNpc("Defeat a Moss Giant in Tirannwn", "Defeat a Moss Giant in Tirannwn.", "Tirannwn", TaskDifficulty.EASY, "", "Moss Giant");
        addTask("Talk to Ilfeen in Tirannwn", "Talk to Ilfeen in Tirannwn.", "Tirannwn", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Ilfeen");
        addTask("Use the Bank in Lletya", "Use the Bank in Lletya.", "Tirannwn", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Bank booth");
        addTask("Charter a Ship From Prifddinas to Port Tyras", "Take a Charter Ship from Prifddinas to Port Tyras.", "Tirannwn", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Charter Ship", "Port Tyras");
        cookItem("Cook a Rabbit in Tirannwn", "Cook a Rabbit anywhere within Tirannwn.", "Tirannwn", TaskDifficulty.EASY, "", 1, "Rabbit");
        addTask("Cross a Trap in Isafdar", "Successfully cross any kind of trap in Isafdar.", "Tirannwn", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Isafdar");

        // --- Audit additions: Tirannwn (medium) (13) ---
        defeatNpc("Defeat a Bloodveld in Tirannwn", "Defeat a Bloodveld in Tirannwn.", "Tirannwn", TaskDifficulty.MEDIUM, "", "Bloodveld");
        defeatNpc("Defeat a Kurask in Tirannwn", "Defeat a Kurask in Tirannwn.", "Tirannwn", TaskDifficulty.MEDIUM, "", "Kurask");
        defeatNpc("Defeat an Elf in Tirannwn", "Defeat an Elf in Tirannwn.", "Tirannwn", TaskDifficulty.MEDIUM, "", "Elf");
        addTask("Pick up 10 Whiteberries in Tirannwn", "Pick up 10 Whiteberries in Tirannwn.", "Tirannwn", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "White berries");
        addTask("Ring all of the Prifddinas bells", "Ring all of the Prifddinas bells.", "Tirannwn", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Bell (Prifddinas)");
        addTask("Use an Elven Teleport Crystal", "Use a teleport crystal or eternal teleport crystal.", "Tirannwn", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "teleport crystal", "eternal teleport crystal");
        mineItem("Mine 25 gold rocks in Tirannwn", "Mine 25 gold rocks in Tirannwn.", "Tirannwn", TaskDifficulty.MEDIUM, "", 25, "gold rocks");
        addTask("Move Your House to Prifddinas", "Move your Player Owned House to Prifddinas.", "Tirannwn", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Player Owned House");
        addTask("Successfully hop over the Tripwire in Tirannwn", "Successfully hop over the Tripwire in Tirannwn.", "Tirannwn", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Tripwire");
        addTask("Thieve a tiara from a Silver Stall in Tirannwn", "Thieve a tiara from a Silver Stall in Tirannwn.", "Tirannwn", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "tiara", "Silver Stall");
        catchItem("Catch a Crystal Impling", "Catch a Crystal Impling in Prifddinas.", "Tirannwn", TaskDifficulty.MEDIUM, "", 1, "Crystal Impling");
        addTask("Check a grown Papaya Tree in Lletya", "Check the health of a Papaya Tree you've grown in Lletya.", "Tirannwn", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Papaya Tree");
        chopItem("Chop 100 Maples in Tirannwn", "Chop 100 Maples in Tirannwn.", "Tirannwn", TaskDifficulty.MEDIUM, "", 100, "Maple tree");

        // --- Audit additions: Tirannwn (hard) (24) ---
        addTask("Zalcano Combat Achievements", "Complete all of the Combat Achievements for Zalcano.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Zalcano");
        addTask("Load a blowpipe with Rune Darts", "Load a Toxic Blowpipe with Rune Darts.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Toxic Blowpipe", "Rune Darts");
        defeatNpc("Defeat 50 Elves in Tirannwn", "Defeat 50 Elves in Tirannwn.", "Tirannwn", TaskDifficulty.HARD, "", "Elf");
        defeatNpc("Defeat the memory of Seren", "Defeat the Fragment of Seren by replaying her fight using the Memoriam Device.", "Tirannwn", TaskDifficulty.HARD, "", "Fragment of Seren", "Memoriam Device");
        defeatNpc("Defeat a Dark Beast in Tirannwn", "Defeat a Dark Beast in Tirannwn.", "Tirannwn", TaskDifficulty.HARD, "", "Dark Beast");
        defeatNpc("Defeat a Nechryael in Tirannwn", "Defeat a Nechryael in Tirannwn.", "Tirannwn", TaskDifficulty.HARD, "", "Nechryael");
        defeatNpc("Defeat Zalcano 50 Times", "Defeat Zalcano in Prifddinas 50 times.", "Tirannwn", TaskDifficulty.HARD, "", "Zalcano");
        defeatNpc("Defeat Zulrah", "Defeat Zulrah at the Poison Waste.", "Tirannwn", TaskDifficulty.HARD, "", "Zulrah");
        defeatNpc("Defeat Zulrah 50 Times", "Defeat Zulrah at the Poison Waste 50 times.", "Tirannwn", TaskDifficulty.HARD, "", "Zulrah");
        equipItem("Equip a Crystal Bow", "Equip a Crystal Bow.", "Tirannwn", TaskDifficulty.HARD, "", "Crystal Bow");
        equipItem("Equip a Crystal Shield", "Equip a Crystal Shield.", "Tirannwn", TaskDifficulty.HARD, "", "Crystal Shield");
        equipItem("Equip any piece of Crystal Armour", "Equip either the Crystal Helmet, Body or Legs.", "Tirannwn", TaskDifficulty.HARD, "", "Crystal Helmet", "Crystal body", "Crystal legs");
        equipItem("Equip a one-hand Leaf-bladed weapon in Tirannwn", "Equip a leaf-bladed sword, leaf-bladed battleaxe, or leaf-bladed spear in Tirannwn.", "Tirannwn", TaskDifficulty.HARD, "", "leaf-bladed sword", "leaf-bladed battleaxe", "leaf-bladed spear");
        addTask("Open the Enhanced Crystal Chest", "Open the Enhanced Crystal Chest in Prifddinas.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Enhanced Crystal Chest");
        addTask("Find Every Memoriam Crystal", "Find every Memoriam Crystal in Prifddinas and add them to the Memoriam Device.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Memoriam Crystal", "Memoriam Device");
        addTask("Receive a Crystal Acorn from Pennant", "Trade in a crystal weapon seed, crystal armour seed, or crystal tool seed to Pennant for a crystal acorn.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "crystal weapon seed", "crystal armour seed", "crystal tool seed", "Pennant", "crystal acorn");
        mineItem("Mine 200 Soft Clay in Tirannwn", "Mine 200 Soft Clay in Tirannwn.", "Tirannwn", TaskDifficulty.HARD, "", 200, "Soft Clay");
        addTask("Pickpocket an Elf 50 Times", "Pickpocket an Elf 50 times.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Elf");
        addTask("Receive a Dragonstone Amulet from an Impling", "Receive a Dragonstone Amulet from a Crystal Impling.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Dragonstone Amulet", "Crystal Impling");
        addTask("Create 25 Divine Super Strength Potions", "Create 25 Divine Super Strength Potions.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 25, "Divine super strength potion");
        addTask("Dissect 50 Sacred Eels", "Dissect 50 Sacred Eels.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Sacred Eel");
        addTask("Harvest Some Snape Grass in Prifddinas", "Harvest some Snape grass from the Prifddinas allotment patch.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Snape grass");
        craftItem("Make 100 Mahogany Planks in Prifddinas", "Make 100 mahogany planks in Prifddinas.", "Tirannwn", TaskDifficulty.HARD, "", "mahogany plank");
        addTask("Thieve a Diamond from a Gem Stall in Tirannwn", "Thieve a Diamond from a Gem Stall in Tirannwn.", "Tirannwn", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Diamond", "Gem Stall");

        // --- Audit additions: Tirannwn (elite) (26) ---
        addTask("Zulrah Combat Achievements", "Complete all of the Combat Achievements for Zulrah.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Zulrah");
        addTask("Gauntlet Combat Achievements", "Complete all of the Combat Achievements for the Gauntlet & Corrupted Gauntlet.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Gauntlet", "Corrupted Gauntlet");
        addTask("Load a blowpipe with Dragon Darts", "Load a Toxic Blowpipe with Dragon Darts.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Toxic Blowpipe", "Dragon Dart");
        addTask("Obtain a Crystal Tool Seed", "Obtain a crystal tool seed as a drop from Zalcano.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "crystal tool seed", "Zalcano");
        craftItem("Craft a Toxic Trident", "Craft a Toxic Trident.", "Tirannwn", TaskDifficulty.ELITE, "", "Toxic Trident");
        addTask("Dismantle a Zulrah scale unique", "Dismantle a Zulrah scale unique for 20,000 scales.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Zulrah scale");
        addTask("Complete the Corrupted Gauntlet in 4:30", "Complete the Corrupted Gauntlet in less than 4:30.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Corrupted Gauntlet", "Corrupted Hunllef (Echo)");
        defeatNpc("Defeat Zalcano 100 Times", "Defeat Zalcano in Prifddinas 100 times.", "Tirannwn", TaskDifficulty.ELITE, "", "Zalcano");
        defeatNpc("Defeat Zulrah 150 Times", "Defeat Zulrah at the Poison Waste 150 times.", "Tirannwn", TaskDifficulty.ELITE, "", "Zulrah");
        defeatNpc("Defeat Zulrah 300 Times", "Defeat Zulrah at the Poison Waste 300 times.", "Tirannwn", TaskDifficulty.ELITE, "", "Zulrah");
        equipItem("Equip a Crystal Grail", "Equip a Crystal Grail.", "Tirannwn", TaskDifficulty.ELITE, "", "Crystal Grail");
        equipItem("Equip a Full Crystal Armour Set", "Equip a full set of Crystal armour.", "Tirannwn", TaskDifficulty.ELITE, "", "Crystal armour");
        equipItem("Equip a Piece of the Dragonstone Armour Set", "Equip any piece of the Dragonstone armour set.", "Tirannwn", TaskDifficulty.ELITE, "", "Dragonstone armour set");
        equipItem("Equip a Serpentine Helm", "Equip a Serpentine Helm.", "Tirannwn", TaskDifficulty.ELITE, "", "Serpentine Helm");
        equipItem("Equip an Enhanced Crystal Weapon", "Equip a Blade of Saeldor or Bow of faerdhinen.", "Tirannwn", TaskDifficulty.ELITE, "", "Blade of Saeldor", "Bow of faerdhinen");
        equipItem("Equip an Elven Signet", "Equip an Elven Signet.", "Tirannwn", TaskDifficulty.ELITE, "", "Elven Signet");
        addTask("Complete the Corrupted Gauntlet 50 Times", "Complete the Corrupted Gauntlet in Prifddinas 50 times.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Corrupted Gauntlet");
        addTask("Complete the Corrupted Gauntlet 100 Times", "Complete the Corrupted Gauntlet in Prifddinas 100 times.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Corrupted Gauntlet");
        addTask("Obtain a Zalcano Shard", "Obtain a Zalcano shard as a drop from Zalcano in Prifddinas.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Zalcano shard", "Zalcano");
        catchItem("Catch 300 Red Chinchompas in Tirannwn", "Catch 300 Red Chinchompas in Tirannwn.", "Tirannwn", TaskDifficulty.ELITE, "", 300, "Red chinchompa");
        addTask("Check the health of 5 Crystal Trees", "Check the health of 5 Crystal Trees.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Crystal Tree");
        addTask("Create 100 Divine Ranging Potions", "Create 100 Divine Ranging Potions.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 100, "Divine Ranging Potion");
        addTask("Create 100 Divine Super Combat Potions", "Create 100 Divine Super Combat Potions.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 100, "Divine Super Combat Potion");
        addTask("Dissect 250 Sacred Eels", "Dissect 250 Sacred Eels.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Sacred Eel");
        mineItem("Mine 100 Runite Ore in Tirannwn", "Mine 100 runite ore in Tirannwn.", "Tirannwn", TaskDifficulty.ELITE, "", 100, "runite ore");
        addTask("Create 50 Anti-venom+ Potions", "Make 50 anti-venom+ potions.", "Tirannwn", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 50, "anti-venom+");

        // --- Audit additions: Tirannwn (master) (1) ---
        equipItem("Equip a Crystal Crown", "Equip a Crystal Crown.", "Tirannwn", TaskDifficulty.MASTER, "", "Crystal Crown");

        // --- Audit additions: Wilderness (easy) (11) ---
        defeatNpc("Defeat a Fire Giant in the Wilderness", "Defeat a Fire Giant in the Wilderness.", "Wilderness", TaskDifficulty.EASY, "", "Fire Giant");
        defeatNpc("Defeat a Mammoth", "Defeat a mammoth in the Wilderness.", "Wilderness", TaskDifficulty.EASY, "", "mammoth");
        defeatNpc("Defeat a Zombie Pirate", "Defeat a zombie pirate.", "Wilderness", TaskDifficulty.EASY, "", "zombie pirate");
        equipItem("Equip any Team cape", "Equip any team cape in the Wilderness.", "Wilderness", TaskDifficulty.EASY, "", "team cape");
        addTask("Open a Looting Bag", "Open a Looting Bag.", "Wilderness", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Looting Bag");
        addTask("Use the Bank at the Mage Arena", "Use the Bank at the Mage Arena.", "Wilderness", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Mage Arena bank");
        addTask("Visit Ferox Enclave", "Visit Ferox Enclave.", "Wilderness", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Ferox Enclave");
        addTask("Enter the Wilderness God Wars Dungeon", "Enter the Wilderness God Wars Dungeon.", "Wilderness", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Wilderness God Wars Dungeon");
        addTask("Enter the Wilderness Resource Area", "Enter the Wilderness Resource Area.", "Wilderness", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Wilderness Resource Area");
        addTask("Order a Drink at The Old Nite", "Order a drink at The Old Nite.", "Wilderness", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "The Old Nite");
        addTask("Sacrifice Some Bones at the Chaos Temple", "Sacrifice some Bones at the Western Chaos Temple altar.", "Wilderness", TaskDifficulty.EASY, TaskType.ACTIVITY, "", 1, "Chaos Temple (hut)");

        // --- Audit additions: Wilderness (medium) (24) ---
        addTask("Complete the Easy Wilderness Diary", "Complete all of the Easy tasks in the Wilderness Achievement Diary.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Wilderness Achievement Diary");
        addTask("Complete the Medium Wilderness Diary", "Complete all of the Medium tasks in the Wilderness Achievement Diary.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Wilderness Achievement Diary");
        addTask("Obtain an Ecumenical Key", "Obtain an Ecumenical Key as a drop in the Wilderness God Wars Dungeon.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Ecumenical Key");
        addTask("Open 1 Muddy Chest", "Open 1 Muddy Chest.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Muddy Chest");
        addTask("Open 1 Zombie Pirate Locker", "Open 1 Zombie Pirate Locker.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Zombie Pirate's Locker");
        addTask("Open 15 Larran's Chests", "Open 15 of either Larran's Small Chest or Larran's Big Chest.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Larran's Small Chest", "Larran's Big Chest");
        addTask("Open One of Larran's Chests", "Open either Larran's Small Chest or Larran's Big Chest.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Larran's Small Chest", "Larran's Big Chest");
        addTask("Cast Claws of Guthix", "Cast the Claws of Guthix spell outside of the Mage Arena.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Claws of Guthix");
        addTask("Cast Flames of Zamorak", "Cast the Flames of Zamorak spell outside of the Mage Arena.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Flames of Zamorak");
        addTask("Cast Saradomin Strike", "Cast the Saradomin Strike spell outside of the Mage Arena.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Saradomin Strike");
        defeatNpc("Defeat a Green Dragon in the Wilderness", "Defeat a Green Dragon in the Wilderness.", "Wilderness", TaskDifficulty.MEDIUM, "", "Green dragon");
        defeatNpc("Defeat a Lava Dragon in the Wilderness", "Defeat a Lava Dragon in the Wilderness.", "Wilderness", TaskDifficulty.MEDIUM, "", "Lava Dragon");
        defeatNpc("Defeat the Chaos Fanatic", "Defeat the Chaos Fanatic in the Wilderness.", "Wilderness", TaskDifficulty.MEDIUM, "", "Chaos Fanatic");
        defeatNpc("Defeat the Crazy Archaeologist", "Defeat the Crazy Archaeologist in the Wilderness.", "Wilderness", TaskDifficulty.MEDIUM, "", "Crazy Archaeologist");
        defeatNpc("Defeat a Revenant Dragon", "Defeat a Revenant dragon inside the Revenant Caves.", "Wilderness", TaskDifficulty.MEDIUM, "", "Revenant dragon");
        equipItem("Equip a Bracelet of Ethereum", "Equip a Bracelet of Ethereum.", "Wilderness", TaskDifficulty.MEDIUM, "", "Bracelet of Ethereum");
        equipItem("Equip a Fedora", "Equip a Fedora.", "Wilderness", TaskDifficulty.MEDIUM, "", "Fedora");
        equipItem("Equip a God Cape", "Equip either a Saradomin Cape, a Guthix Cape or a Zamorak Cape.", "Wilderness", TaskDifficulty.MEDIUM, "", "Saradomin Cape", "Guthix Cape", "Zamorak Cape");
        addTask("Destroy the one ring to rule them all", "Destroy the one ring to rule them all.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1);
        addTask("Enter the Deep Wilderness Dungeon", "Enter the Deep Wilderness Dungeon.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Deep Wilderness Dungeon");
        addTask("Use the Abyss", "Use the Abyss to access a Runecrafting Altar.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Abyss", "Runic altar");
        addTask("Bury Some Lava Dragon Bones", "Bury some Lava dragon bones.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Lava dragon bones");
        catchItem("Catch a Black Salamander", "Catch a Black Salamander in the Wilderness.", "Wilderness", TaskDifficulty.MEDIUM, "", 1, "Black salamander");
        addTask("Complete the Wilderness Agility Course", "Complete a lap of the Wilderness Agility Course.", "Wilderness", TaskDifficulty.MEDIUM, TaskType.ACTIVITY, "", 1, "Wilderness Agility Course");

        // --- Audit additions: Wilderness (hard) (34) ---
        addTask("Complete the Hard Wilderness Diary", "Complete all of the Hard tasks in the Wilderness Achievement Diary.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Wilderness Achievement Diary");
        addTask("Open 10 Muddy Chests", "Open 10 Muddy Chests.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Muddy Chest");
        addTask("Open 15 Zombie Pirate Lockers", "Open 15 Zombie Pirate Lockers.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Zombie Pirate's Locker");
        addTask("Open 50 Larran's Chests", "Open 50 of either Larran's Small Chest or Larran's Big Chest.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Larran's Small Chest", "Larran's Big Chest");
        addTask("Open 50 Zombie Pirate Lockers", "Open 50 Zombie Pirate Lockers.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Zombie Pirate's Locker");
        addTask("Use a teleport anchoring scroll", "Use a teleport anchoring scroll.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "teleport anchoring scroll");
        addTask("Fully charge a Bracelet of Ethereum", "Fully charge a Bracelet of ethereum.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Bracelet of ethereum");
        defeatNpc("Defeat Callisto 300 times", "Defeat Callisto or Artio 300 times.", "Wilderness", TaskDifficulty.HARD, "", "Callisto", "Artio");
        defeatNpc("Defeat Venenatis 300 times", "Defeat Venenatis or Spindel 300 times.", "Wilderness", TaskDifficulty.HARD, "", "Venenatis", "Spindel");
        defeatNpc("Defeat Vet'ion 300 times", "Defeat Vet'ion or Calvar'ion 300 times.", "Wilderness", TaskDifficulty.HARD, "", "Vet'ion", "Calvar'ion");
        defeatNpc("Defeat Callisto", "Defeat Callisto in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", "Callisto");
        defeatNpc("Defeat Callisto 150 times", "Defeat Callisto or Artio 150 times.", "Wilderness", TaskDifficulty.HARD, "", "Callisto", "Artio");
        defeatNpc("Defeat Callisto 50 times", "Defeat Callisto or Artio 50 times.", "Wilderness", TaskDifficulty.HARD, "", "Callisto", "Artio");
        defeatNpc("Defeat Scorpia", "Defeat Scorpia in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", "Scorpia");
        defeatNpc("Defeat the Chaos Elemental", "Defeat the Chaos Elemental in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", "Chaos Elemental");
        defeatNpc("Defeat the Corporeal Beast 50 Times", "Defeat the Corporeal Beast in the Wilderness 50 times.", "Wilderness", TaskDifficulty.HARD, "", "Corporeal Beast");
        defeatNpc("Defeat the King Black Dragon", "Defeat the King Black Dragon in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", "King Black Dragon");
        defeatNpc("Defeat Venenatis", "Defeat Venenatis in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", "Venenatis");
        defeatNpc("Defeat Venenatis 150 times", "Defeat Venenatis or Spindel 150 times.", "Wilderness", TaskDifficulty.HARD, "", "Venenatis", "Spindel");
        defeatNpc("Defeat Venenatis 50 times", "Defeat Venenatis or Spindel 50 times.", "Wilderness", TaskDifficulty.HARD, "", "Venenatis", "Spindel");
        defeatNpc("Defeat Vet'ion", "Defeat Vet'ion in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", "Vet'ion");
        defeatNpc("Defeat Vet'ion 150 times", "Defeat Vet'ion or Calvar'ion 150 times.", "Wilderness", TaskDifficulty.HARD, "", "Vet'ion", "Calvar'ion");
        defeatNpc("Defeat Vet'ion 50 times", "Defeat Vet'ion or Calvar'ion 50 times.", "Wilderness", TaskDifficulty.HARD, "", "Vet'ion", "Calvar'ion");
        defeatNpc("Defeat a Runite golem", "Defeat a runite golem inside the Wilderness Resource Area.", "Wilderness", TaskDifficulty.HARD, "", "runite golem");
        equipItem("Equip an Odium Ward", "Equip an Odium ward.", "Wilderness", TaskDifficulty.HARD, "", "Odium ward");
        equipItem("Equip 100 Black Chinchompas", "Equip a stack of at least 100 Black Chinchompas.", "Wilderness", TaskDifficulty.HARD, "", "Black chinchompa");
        equipItem("Equip 250 Black Chinchompas", "Equip a stack of at least 250 Black Chinchompas.", "Wilderness", TaskDifficulty.HARD, "", "Black chinchompa");
        equipItem("Equip an Enchanted Slayer Staff", "Equip an Enchanted Slayer Staff.", "Wilderness", TaskDifficulty.HARD, "", "Slayer's staff (e)");
        equipItem("Equip a Pair of Dragon Boots in the Wilderness", "Equip a pair of dragon boots while inside the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", "dragon boots");
        addTask("Redeem 50 Wilderness Agility Tickets", "Redeem 50 Wilderness Agility Tickets in one go.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Wilderness agility ticket");
        addTask("Sacrifice Some Dragon Bones at the Chaos Temple", "Sacrifice some Dragon Bones at the Western Chaos Temple altar.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Dragon Bones", "Chaos Temple (hut)");
        catchItem("Catch 100 Dark Crabs", "Catch 100 dark crabs in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", 100, "dark crab");
        addTask("Cross the Chaos Temple Stone Shortcut", "Cross the Chaos Temple Stone Shortcut.", "Wilderness", TaskDifficulty.HARD, TaskType.ACTIVITY, "", 1, "Stepping stone (Wilderness Chaos Temple)");
        catchItem("Catch a Black Chinchompa", "Catch a Black Chinchompa in the Wilderness.", "Wilderness", TaskDifficulty.HARD, "", 1, "Black chinchompa");

        // --- Audit additions: Wilderness (elite) (22) ---
        addTask("Complete the Elite Wilderness Diary", "Complete all of the Elite tasks in the Wilderness Achievement Diary.", "Wilderness", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Wilderness Achievement Diary");
        defeatNpc("Defeat the Corporeal Beast 150 Times", "Defeat the Corporeal Beast in the Wilderness 150 times.", "Wilderness", TaskDifficulty.ELITE, "", "Corporeal Beast");
        defeatNpc("Defeat the Corporeal Beast 250 Times", "Defeat the Corporeal Beast in the Wilderness 250 times.", "Wilderness", TaskDifficulty.ELITE, "", "Corporeal Beast");
        equipItem("Equip 500 Black Chinchompas", "Equip a stack of at least 500 Black Chinchompas.", "Wilderness", TaskDifficulty.ELITE, "", "Black chinchompa");
        equipItem("Equip a Blessed Spirit Shield", "Equip a Blessed Spirit Shield.", "Wilderness", TaskDifficulty.ELITE, "", "Blessed Spirit Shield");
        equipItem("Equip a Full Dagon'Hai Set", "Equip a full set of Dagon'hai robes.", "Wilderness", TaskDifficulty.ELITE, "", "Dagon'hai robes");
        equipItem("Equip a Ring of the Gods", "Equip a Ring of the Gods.", "Wilderness", TaskDifficulty.ELITE, "", "Ring of the Gods");
        equipItem("Equip a Thammaron's Sceptre", "Equip a Thammaron's Sceptre.", "Wilderness", TaskDifficulty.ELITE, "", "Thammaron's Sceptre");
        equipItem("Equip a Treasonous Ring", "Equip a Treasonous Ring.", "Wilderness", TaskDifficulty.ELITE, "", "Treasonous Ring");
        equipItem("Equip a Tyrannical Ring", "Equip a Tyrannical Ring.", "Wilderness", TaskDifficulty.ELITE, "", "Tyrannical Ring");
        equipItem("Equip Craw's Bow", "Equip Craw's bow.", "Wilderness", TaskDifficulty.ELITE, "", "Craw's bow");
        equipItem("Equip full Elder chaos robe", "Equip the Elder Chaos Hood, Robe, and top.", "Wilderness", TaskDifficulty.ELITE, "", "Elder Chaos Hood", "Elder chaos robe", "Elder chaos top");
        equipItem("Equip the Accursed Sceptre", "Equip the Accursed Sceptre.", "Wilderness", TaskDifficulty.ELITE, "", "Accursed Sceptre");
        equipItem("Equip the Ursine Chainmace", "Equip the Ursine Chainmace.", "Wilderness", TaskDifficulty.ELITE, "", "Ursine Chainmace");
        equipItem("Equip the Webweaver", "Equip the Webweaver.", "Wilderness", TaskDifficulty.ELITE, "", "Webweaver");
        equipItem("Equip Viggora's Chainmace", "Equip Viggora's Chainmace.", "Wilderness", TaskDifficulty.ELITE, "", "Viggora's Chainmace");
        addTask("Loot a dragonstone from the Rogues Castle", "Loot a dragonstone from the chest in Rogue's Castle.", "Wilderness", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "dragonstone", "Rogue's Castle");
        craftItem("Make an Extended Antifire Potion", "Make an Extended Antifire Potion.", "Wilderness", TaskDifficulty.ELITE, "", "Extended Antifire Potion");
        mineItem("Mine Some Runite Ore in the Wilderness", "Mine some runite ore in the Wilderness.", "Wilderness", TaskDifficulty.ELITE, "", 1, "runite ore");
        cookItem("Cook a Dark Crab", "Cook a Dark Crab.", "Wilderness", TaskDifficulty.ELITE, "", 1, "Dark Crab");
        addTask("Cross a Pillar shortcut in the Revenant Caves", "Cross a pillar shortcut in the Revenant Caves.", "Wilderness", TaskDifficulty.ELITE, TaskType.ACTIVITY, "", 1, "Pillar (Revenant Caves)", "Revenant Caves");
        mineItem("Mine some Runite ore with a Dragon Pickaxe", "Mine some runite ore with a dragon pickaxe within the Wilderness.", "Wilderness", TaskDifficulty.ELITE, "", 1, "runite ore", "dragon pickaxe");

        // --- Audit additions: Wilderness (master) (3) ---
        addTask("Obtain Every Revenant Weapon", "Obtain Craws Bow, Viggora's Chainmace and Thammaron's Sceptre as drops from Revenants.", "Wilderness", TaskDifficulty.MASTER, TaskType.ACTIVITY, "", 1, "Craws Bow", "Viggora's Chainmace", "Thammaron's Sceptre", "Revenants");
        equipItem("Equip a Spectral or Arcane Spirit Shield", "Equip either a Spectral Spirit Shield or an Arcane Spirit Shield.", "Wilderness", TaskDifficulty.MASTER, "", "Spectral Spirit Shield", "Arcane Spirit Shield");
        equipItem("Equip an Elysian Spirit Shield", "Equip an Elysian Spirit Shield.", "Wilderness", TaskDifficulty.MASTER, "", "Elysian Spirit Shield");

        // Build reverse lookup maps
        buildLookupMaps();
    }

    // =========================================================================
    // Helper methods for adding tasks
    // =========================================================================

    private static void defeatNpc(String name, String desc, String area, TaskDifficulty diff, String reqs, String... npcs)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.DEFEAT_NPC).requirements(reqs).matchKeywords(npcs).quantity(1)
            .build());
    }

    private static void equipItem(String name, String desc, String area, TaskDifficulty diff, String reqs, String... items)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.EQUIP_ITEM).requirements(reqs).matchKeywords(items).quantity(1)
            .build());
    }

    private static void craftItem(String name, String desc, String area, TaskDifficulty diff, String reqs, String... items)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.CRAFT_ITEM).requirements(reqs).matchKeywords(items).quantity(1)
            .build());
    }

    private static void catchItem(String name, String desc, String area, TaskDifficulty diff, String reqs, String... items)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.CATCH_ITEM).requirements(reqs).matchKeywords(items).quantity(1)
            .build());
    }

    private static void catchItem(String name, String desc, String area, TaskDifficulty diff, String reqs, int qty, String... items)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.CATCH_ITEM).requirements(reqs).matchKeywords(items).quantity(qty)
            .build());
    }

    private static void cookItem(String name, String desc, String area, TaskDifficulty diff, String reqs, int qty, String... items)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.COOK_ITEM).requirements(reqs).matchKeywords(items).quantity(qty)
            .build());
    }

    private static void mineItem(String name, String desc, String area, TaskDifficulty diff, String reqs, int qty, String... items)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.MINE_ITEM).requirements(reqs).matchKeywords(items).quantity(qty)
            .build());
    }

    private static void chopItem(String name, String desc, String area, TaskDifficulty diff, String reqs, int qty, String... items)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.CHOP_ITEM).requirements(reqs).matchKeywords(items).quantity(qty)
            .build());
    }

    private static void burnItem(String name, String desc, String area, TaskDifficulty diff, String reqs, int qty, String... items)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.BURN_ITEM).requirements(reqs).matchKeywords(items).quantity(qty)
            .build());
    }

    private static void cleanItem(String name, String desc, String area, TaskDifficulty diff, String reqs, int qty, String... items)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(TaskType.CLEAN_ITEM).requirements(reqs).matchKeywords(items).quantity(qty)
            .build());
    }

    private static void addTask(String name, String desc, String area, TaskDifficulty diff, TaskType type, String reqs, int qty, String... keywords)
    {
        ALL_TASKS.add(DemonicPactsTask.builder()
            .name(name).description(desc).area(area).difficulty(diff)
            .type(type).requirements(reqs).matchKeywords(keywords).quantity(qty)
            .build());
    }

    // =========================================================================
    // Lookup map construction
    // =========================================================================

    private static void buildLookupMaps()
    {
        for (DemonicPactsTask task : ALL_TASKS)
        {
            if (task.getMatchKeywords() == null) continue;

            Map<String, List<DemonicPactsTask>> targetMap;
            switch (task.getType())
            {
                case DEFEAT_NPC:
                    targetMap = NPC_TASKS;
                    break;
                case EQUIP_ITEM:
                case CRAFT_ITEM:
                case CATCH_ITEM:
                case COOK_ITEM:
                case MINE_ITEM:
                case CHOP_ITEM:
                case BURN_ITEM:
                case CLEAN_ITEM:
                    targetMap = ITEM_TASKS;
                    break;
                default:
                    // Put misc keyword-bearing tasks into both maps for broader matching
                    for (String kw : task.getMatchKeywords())
                    {
                        String key = kw.toLowerCase();
                        ITEM_TASKS.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
                        NPC_TASKS.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
                    }
                    continue;
            }

            for (String kw : task.getMatchKeywords())
            {
                String key = kw.toLowerCase();
                targetMap.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
            }
        }

        // Build OBJECT_TASKS by mapping world object names to their corresponding tasks.
        // We include COOK/SMELT/etc. tasks here because the player may need to gather
        // the raw material from this object even if the only task referencing it is
        // a downstream one (e.g. "Cook 100 Sharks" implies "fish sharks here first").
        for (Map.Entry<String, String[]> entry : OBJECT_TO_TASK_KEYWORDS.entrySet())
        {
            String objectName = entry.getKey();
            for (String itemKeyword : entry.getValue())
            {
                List<DemonicPactsTask> tasks = ITEM_TASKS.get(itemKeyword.toLowerCase());
                if (tasks != null)
                {
                    OBJECT_TASKS.computeIfAbsent(objectName, k -> new ArrayList<>()).addAll(tasks);
                }
            }
        }
    }

    // =========================================================================
    // Public API
    // =========================================================================

    public static List<DemonicPactsTask> getAllTasks()
    {
        return Collections.unmodifiableList(ALL_TASKS);
    }

    /**
     * Find tasks matching an NPC name (case-insensitive).
     */
    public static List<DemonicPactsTask> findNpcTasks(String npcName)
    {
        if (npcName == null) return Collections.emptyList();
        List<DemonicPactsTask> tasks = NPC_TASKS.get(npcName.toLowerCase());
        return tasks != null ? tasks : Collections.emptyList();
    }

    /**
     * Find tasks matching an item name (case-insensitive).
     */
    public static List<DemonicPactsTask> findItemTasks(String itemName)
    {
        if (itemName == null) return Collections.emptyList();
        if (ITEM_BLOCKLIST.contains(itemName.toLowerCase())) return Collections.emptyList();
        List<DemonicPactsTask> tasks = ITEM_TASKS.get(itemName.toLowerCase());
        return tasks != null ? tasks : Collections.emptyList();
    }

    /**
     * Check if any task matches this NPC name.
     */
    public static boolean isTaskNpc(String npcName)
    {
        return npcName != null && NPC_TASKS.containsKey(npcName.toLowerCase());
    }

    /**
     * Check if any task matches this item name.
     */
    public static boolean isTaskItem(String itemName)
    {
        return itemName != null && ITEM_TASKS.containsKey(itemName.toLowerCase());
    }

    /**
     * Find tasks matching a world object name (rocks, trees, patches).
     * Case-insensitive.
     */
    public static List<DemonicPactsTask> findObjectTasks(String objectName)
    {
        if (objectName == null) return Collections.emptyList();
        List<DemonicPactsTask> tasks = OBJECT_TASKS.get(objectName.toLowerCase());
        return tasks != null ? tasks : Collections.emptyList();
    }

    /**
     * Check if any task matches this world object name.
     */
    public static boolean isTaskObject(String objectName)
    {
        return objectName != null && OBJECT_TASKS.containsKey(objectName.toLowerCase());
    }

    /**
     * Get tasks filtered by difficulty.
     */
    public static List<DemonicPactsTask> getTasksByDifficulty(TaskDifficulty difficulty)
    {
        return ALL_TASKS.stream()
            .filter(t -> t.getDifficulty() == difficulty)
            .collect(Collectors.toList());
    }

    /**
     * Get tasks filtered by area.
     */
    public static List<DemonicPactsTask> getTasksByArea(String area)
    {
        return ALL_TASKS.stream()
            .filter(t -> t.getArea().equalsIgnoreCase(area))
            .collect(Collectors.toList());
    }
}
