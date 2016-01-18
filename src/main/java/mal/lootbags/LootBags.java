package mal.lootbags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.logging.log4j.Level;

import mal.lootbags.blocks.BlockRecycler;
import mal.lootbags.handler.ItemDumpCommand;
import mal.lootbags.handler.LootSourceCommand;
import mal.lootbags.handler.MobDropHandler;
import mal.lootbags.item.LootbagItem;
import mal.lootbags.network.CommonProxy;
import mal.lootbags.network.LootbagsPacketHandler;
import mal.lootbags.tileentity.TileEntityRecycler;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;

@Mod(modid = LootBags.MODID, version = LootBags.VERSION)
public class LootBags {
	public static final String MODID = "lootbags";
	public static final String VERSION = "1.8.1";

	public static int[] MONSTERDROPCHANCES = new int[5];
	public static int[] PASSIVEDROPCHANCES = new int[5];
	public static int[] PLAYERDROPCHANCES = new int[5];
	
	/*public static int CMONSTERDROPCHANCE = 40;
	public static int CPASSIVEDROPCHANCE = 20;
	public static int CPLAYERDROPCHANCE = 5;
	
	public static int UMONSTERDROPCHANCE = 40;
	public static int UPASSIVEDROPCHANCE = 20;
	public static int UPLAYERDROPCHANCE = 5;
	
	public static int RMONSTERDROPCHANCE = 40;
	public static int RPASSIVEDROPCHANCE = 20;
	public static int RPLAYERDROPCHANCE = 5;
	
	public static int EMONSTERDROPCHANCE = 40;
	public static int EPASSIVEDROPCHANCE = 20;
	public static int EPLAYERDROPCHANCE = 5;
	
	public static int LMONSTERDROPCHANCE = 40;
	public static int LPASSIVEDROPCHANCE = 20;
	public static int LPLAYERDROPCHANCE = 5;*/
	
	public static int SPECIALDROPCHANCE = 250;
	
	public static int DROPRESOLUTION = 1000;
	
	public static int CHESTQUALITYWEIGHT = 20;
	
	public static int CPERCENTILE = 100;
	public static int UPERCENTILE = 75;
	public static int RPERCENTILE = 50;
	public static int EPERCENTILE = 25;
	public static int LPERCENTILE = 5;
	
	public static boolean REVERSEQUALITY = true;//reverses the quality to determine what can be dropped from a bag
	
	public static boolean SHOWSECRETBAGS = true;//shows the secret bags in NEI/creative inventory
	
	public static final int MINCHANCE = 0;
	public static final int MAXCHANCE = 1000;
	
	public static boolean LIMITONEBAGPERDROP = false;
	public static int BAGFROMPLAYERKILL = 2;//limit bag drops to only EntityPlayer kills, 0 is any source, 1 is EntityPlayer, 2 is forced real players
	public static int PREVENTDUPLICATELOOT = 0;//prevents the same item from showing up twice in a bag, 0 is not at all, 1 is if item and damage are the same, 2 is if item is the same
	public static int MINITEMSDROPPED = 1;//minimum number of items dropped by a bag
	public static int MAXITEMSDROPPED = 5;//maximum number of items dropped by a bag
	
	public static int MAXREROLLCOUNT = 50;
	public static int TOTALVALUEPERBAG = 1000;//total amount of drop chance required to create a lootbag
	
	public static String[] LOOTCATEGORYLIST = null;
	public static ArrayList<ArrayList<ItemStack>> BLACKLIST = new ArrayList<ArrayList<ItemStack>>();
	public static ArrayList<String> MODBLACKLIST = new ArrayList<String>();
	
	public static String[] LOOTBAGINDUNGEONLOOT;
	
	public static LootMap LOOTMAP = new LootMap();
	
	private String[][] blacklistlist = new String[6][];
	private String[][] whitelistlist = new String[6][];
	
	private HashMap<String,Integer> totalvaluemap = new HashMap<String,Integer>();
	
	private boolean disableRecycler = false;
	
	private int numBagsToUpgrade = 9;
	private int maxTierCraftable = 4;
	
	public static BagTypes[] emptyBags;
	
	private static Random random = new Random();
	
	@SidedProxy(clientSide="mal.lootbags.network.ClientProxy", serverSide="mal.lootbags.network.CommonProxy")
	public static CommonProxy prox;

	public static LootbagItem lootbag = new LootbagItem();
	public static BlockRecycler recycler = new BlockRecycler();

	@Instance(value = LootBags.MODID)
	public static LootBags LootBagsInstance;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MobDropHandler handler = new MobDropHandler();
		MinecraftForge.EVENT_BUS.register(handler);
		NetworkRegistry.INSTANCE.registerGuiHandler(LootBagsInstance, prox);
		
		FMLLog.log(Level.INFO, "Your current LootBags version is: " + this.VERSION);
		
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		
		Property prop = config.get("Drop Chances", "2 Common Bag Monster Drop Chance", 200);
		prop.comment = "This controls the drop chance for monsters, passive mobs, and players for each bag in a resolution up to 0.1%.";
		MONSTERDROPCHANCES[0] = prop.getInt();
		PASSIVEDROPCHANCES[0] = config.get("Drop Chances", "2 Common Bag Passive Mob Drop Chance", 100).getInt();
		PLAYERDROPCHANCES[0] = config.get("Drop Chances", "2 Common Bag Player Drop Chance", 100).getInt();
		
		MONSTERDROPCHANCES[1] = config.get("Drop Chances", "3 Uncommon Bag Monster Drop Chance", 100).getInt();
		PASSIVEDROPCHANCES[1] = config.get("Drop Chances", "3 Uncommon Bag Passive Mob Drop Chance", 50).getInt();
		PLAYERDROPCHANCES[1] = config.get("Drop Chances", "3 Uncommon Bag Player Drop Chance", 50).getInt();
		
		MONSTERDROPCHANCES[2] = config.get("Drop Chances", "4 Rare Bag Monster Drop Chance", 50).getInt();
		PASSIVEDROPCHANCES[2] = config.get("Drop Chances", "4 Rare Bag Passive Mob Drop Chance", 25).getInt();
		PLAYERDROPCHANCES[2] = config.get("Drop Chances", "4 Rare Bag Player Drop Chance", 25).getInt();
		
		MONSTERDROPCHANCES[3] = config.get("Drop Chances", "5 Epic Bag Monster Drop Chance", 25).getInt();
		PASSIVEDROPCHANCES[3] = config.get("Drop Chances", "5 Epic Bag Passive Mob Drop Chance", 10).getInt();
		PLAYERDROPCHANCES[3] = config.get("Drop Chances", "5 Epic Bag Player Drop Chance", 10).getInt();
		
		MONSTERDROPCHANCES[4] = config.get("Drop Chances", "6 Legendary Bag Monster Drop Chance", 10).getInt();
		PASSIVEDROPCHANCES[4] = config.get("Drop Chances", "6 Legendary Bag Passive Mob Drop Chance", 5).getInt();
		PLAYERDROPCHANCES[4] = config.get("Drop Chances", "6 Legendary Bag Player Drop Chance", 5).getInt();
		
		prop = config.get("Drop Chances", "1 Weighting Resolution", 1000);
		prop.comment = "This is the resolution of the bag drop chances.  Only change this if you want bags with rarity resolutions > 0.1%";
		DROPRESOLUTION = prop.getInt();
		
		prop = config.get("Drop Chances", "7 Special Bag Drop Chance", 250);
		prop.comment = "This is the chance for any of the special hidden bags to appear, most are obtained by killing specific named entities.";
		SPECIALDROPCHANCE = prop.getInt();
		
		Property prop2 = config.get("Loot Categories", "ChestGenHooks Dropped",  new String[]{ChestGenHooks.DUNGEON_CHEST, ChestGenHooks.MINESHAFT_CORRIDOR, 
				ChestGenHooks.PYRAMID_DESERT_CHEST, ChestGenHooks.PYRAMID_JUNGLE_CHEST, ChestGenHooks.PYRAMID_JUNGLE_DISPENSER,
				ChestGenHooks.STRONGHOLD_CORRIDOR, ChestGenHooks.STRONGHOLD_CROSSING, ChestGenHooks.STRONGHOLD_LIBRARY, ChestGenHooks.VILLAGE_BLACKSMITH});
		prop2.comment = "This is a list of the loot sources the bags pull from to generate the loot tables.  Probably a good idea to not mess with this unless you know what you're doing.";
		LOOTCATEGORYLIST = prop2.getStringList();
		prop2 = config.get("Loot Categories", "Chest Drop Weight", 20);
		prop2.comment = "This is the weighting of the bags in any of the worldgen chests.";
		CHESTQUALITYWEIGHT = prop2.getInt();
		if(CHESTQUALITYWEIGHT <= 0)
		{
			FMLLog.log(Level.INFO, "Chest Weighting < 1, this causes problems for everything and is terrible.  Setting it to 1 instead.");
			CHESTQUALITYWEIGHT = 1;
		}
		
		Property prop3 = config.get("Blacklisted Items", "Global Blacklist", new String[]{"lootbags itemlootbag 0"});
		prop3.comment = "Adding a modid and internal item name or Ore Dictionary name to this list will prevent the bag from dropping the item.  Tries for Ore Dictionary before trying through the modlist." +
				"The modlist must be in the form <modid> <itemname> <damage> on a single line or it won't work right.  Example to blacklist iron ingots: minecraft iron_ingot 0 <OR> ingotIron.  An entire mod" +
				"can be blacklisted by just entering the modid and nothing else.";
		blacklistlist[0] = prop3.getStringList();
		prop3 = config.get("Blacklisted Bag Items", "Common Bag Blacklist", new String[]{});
		prop3.comment = "These blacklists are related to the associated bag type, so an item blacklisted in Common bags still will show up in" +
				" other bag types.";
		blacklistlist[1] = prop3.getStringList();
		prop3 = config.get("Blacklisted Bag Items", "Uncommon Bag Blacklist", new String[]{});
		blacklistlist[2] = prop3.getStringList();
		prop3 = config.get("Blacklisted Bag Items", "Rare Bag Blacklist", new String[]{});
		blacklistlist[3] = prop3.getStringList();
		prop3 = config.get("Blacklisted Bag Items", "Epic Bag Blacklist", new String[]{});
		blacklistlist[4] = prop3.getStringList();
		prop3 = config.get("Blacklisted Bag Items", "Legendary Bag Blacklist", new String[]{});
		blacklistlist[5] = prop3.getStringList();
		
		Property prop4 = config.get("Whitelisted Items", "Global Whitelist", new String[]{});
		prop4.comment = "Adding a modid and internal item name or Ore Dictionary name to this list will add the item to the Loot Bag drop table.  Example to whitelist up to 16 iron ingots with a weight of 50" +
				": minecraft iron_ingot 0 16 50 <OR> ingotIron 16 50 ALL.  The last word in the OreDictionary option is either ALL items that match or ONE item that matches.";
		whitelistlist[0] = prop4.getStringList();
		prop4 = config.get("Whitelisted Bag Items", "Common Bag Whitelist", new String[]{});
		prop4.comment = "These whitelists are related to the associated bag type, so an item whitelisted in Common bags will not show up in" +
				" other bag types.";
		whitelistlist[1] = prop4.getStringList();
		prop4 = config.get("Whitelisted Bag Items", "Uncommon Bag Whitelist", new String[]{});
		whitelistlist[2] = prop4.getStringList();
		prop4 = config.get("Whitelisted Bag Items", "Rare Bag Whitelist", new String[]{});
		whitelistlist[3] = prop4.getStringList();
		prop4 = config.get("Whitelisted Bag Items", "Epic Bag Whitelist", new String[]{});
		whitelistlist[4] = prop4.getStringList();
		prop4 = config.get("Whitelisted Bag Items", "Legendary Bag Whitelist", new String[]{});
		whitelistlist[5] = prop4.getStringList();
		
		Property prop5 = config.get("Loot Categories", "Loot Bags in worldgen chests", new String[]{ChestGenHooks.DUNGEON_CHEST, ChestGenHooks.MINESHAFT_CORRIDOR, 
				ChestGenHooks.PYRAMID_DESERT_CHEST, ChestGenHooks.PYRAMID_JUNGLE_CHEST, ChestGenHooks.PYRAMID_JUNGLE_DISPENSER,
				ChestGenHooks.STRONGHOLD_CORRIDOR, ChestGenHooks.STRONGHOLD_CROSSING, ChestGenHooks.STRONGHOLD_LIBRARY, ChestGenHooks.VILLAGE_BLACKSMITH});
		prop5.comment = "This adds the loot bags to each of the loot tables listed.";
		LOOTBAGINDUNGEONLOOT = prop5.getStringList();
		
		Property prop6 = config.get(Configuration.CATEGORY_GENERAL, "Maximum Rerolls Allowed", 50);
		prop6.comment = "If the bag encounters an item it cannot place in the bag it will reroll, this sets a limit to the number of times the bag will" +
				" reroll before it just skips the slot.  Extremely high or low numbers may result in undesired performance of the mod.";
		MAXREROLLCOUNT = prop6.getInt();
		
		prop6 = config.get(Configuration.CATEGORY_GENERAL, "Maximum Items Per Bag", 5);
		prop6.comment = "This is the maximum number of items that can be contained in a bag.  It must be no less than 1, no greater than 5, and equal or larger than the minimum number of items.";
		MAXITEMSDROPPED = prop6.getInt();
		
		prop6 = config.get(Configuration.CATEGORY_GENERAL, "Minimum Items Per Bag", 1);
		prop6.comment = "This is the minimum number of items that can be contained in a bag.  It must be no less than 1, no greater than 5, and equal or smaller than the maximum number of items.";
		MINITEMSDROPPED = prop6.getInt();
		
		if(MINITEMSDROPPED < 1)
		{
			FMLLog.log(Level.WARN, "Minimum items dropped must be at least 1.  Setting minimum to 1.");
			MINITEMSDROPPED = 1;
		}
		if(MINITEMSDROPPED > 5)
		{
			FMLLog.log(Level.WARN, "Minimum items dropped cannot exceed 5.  Setting minimum to 5.");
			MINITEMSDROPPED = 5;
		}
		
		if(MAXITEMSDROPPED < 1)
		{
			FMLLog.log(Level.WARN, "Minimum items dropped must be at least 1.  Setting minimum to 1.");
			MAXITEMSDROPPED = 1;
		}
		if(MAXITEMSDROPPED > 5)
		{
			FMLLog.log(Level.WARN, "Minimum items dropped cannot exceed 5.  Setting minimum to 5.");
			MAXITEMSDROPPED = 5;
		}
		
		if(MINITEMSDROPPED > MAXITEMSDROPPED)
		{
			FMLLog.log(Level.WARN, "Minimum dropped items greater than maximum.  Swapping values.");
			int temp = MINITEMSDROPPED;
			MINITEMSDROPPED = MAXITEMSDROPPED;
			MAXITEMSDROPPED = temp;
		}
		
		prop6 = config.get(Configuration.CATEGORY_GENERAL, "Prevent Item Duplicates", "NONE");
		prop6.comment = "This will limit how items can show up in the bag.  If the word is NONE then any sort of duplicates are allowed.  DAMAGE will prevent items with the same class and damage, but same class and different damage values are allowed.  ITEM will prevent any duplicates of class." +
				"If the loot table is small (<50 items in total), there may be some performance issues, so limiting the maximum number of items may be a better option.";
		String dupe = prop6.getString();
		if(dupe.equalsIgnoreCase("NONE"))
			PREVENTDUPLICATELOOT = 0;
		else if(dupe.equalsIgnoreCase("DAMAGE"))
			PREVENTDUPLICATELOOT = 2;
		else if(dupe.equalsIgnoreCase("ITEM"))
			PREVENTDUPLICATELOOT = 1;
		else
		{
			FMLLog.log(Level.WARN, "Duplicate prevention word: " + dupe + " not recognized.  Using NONE instead.");
			PREVENTDUPLICATELOOT = 0;
		}
		
		prop6 = config.get(Configuration.CATEGORY_GENERAL, "Show Secret Bags", false);
		prop6.comment = "This if true will show all the secret bags in NEI or creative inventory.  Kind of ruins the fun if you ask me.";
		SHOWSECRETBAGS = prop6.getBoolean();
		
		Property prop7 = config.get(Configuration.CATEGORY_GENERAL,  "Total Loot Value to Create a New Bag", 1000);
		prop7.comment = "This is kind of ambiguous, but essentially it's the total amount of stuff ranked based off of rarity you need to make a new bag in the recycler.  " +
				"The rarer something is the more it's worth and once the recycler has collected this amount of value it will make a new loot bag. The larger the max stack size " +
				"is the lower the value is as well.";
		TOTALVALUEPERBAG = prop7.getInt();
		
		Property prop8 = config.get(Configuration.CATEGORY_GENERAL, "Disable Recycler Recipe", false);
		disableRecycler = prop8.getBoolean();
		
		Property prop9 = config.get(Configuration.CATEGORY_GENERAL, "Number of Bags to Upgrade", 4);
		prop9.comment = "The number of bags needed to upgrade a bag into it's next level counterpart.";
		numBagsToUpgrade = prop9.getInt();
		if(numBagsToUpgrade<1)
		{
			FMLLog.log(Level.WARN, "Number of bags to upgrade must be at least 1.");
			numBagsToUpgrade = 1;
		}
		if(numBagsToUpgrade>9)
		{
			FMLLog.log(Level.WARN, "Number of bags to upgrade cannot be larger than 9.");
			numBagsToUpgrade = 9;
		}
		
		Property prop10 = config.get(Configuration.CATEGORY_GENERAL, "Max Tier Craftable", "Legendary");
		prop10.comment = "Maxiumum tier of bag that can be crafted from other bags.  None will disable bag crafting.  Allowable names: None, Uncommon, Rare, Epic, Legendary.";
		String tier = prop10.getString();
		if(tier.equalsIgnoreCase("none"))
			maxTierCraftable=-1;
		else if(tier.equalsIgnoreCase("uncommon"))
			maxTierCraftable=1;
		else if(tier.equalsIgnoreCase("rare"))
			maxTierCraftable=2;
		else if(tier.equalsIgnoreCase("epic"))
			maxTierCraftable=3;
		else if(tier.equalsIgnoreCase("legendary"))
			maxTierCraftable=4;
		else
		{
			FMLLog.log(Level.WARN, "Invalid tier name: " + tier + ".  Setting tier to allow all crafting.");
			maxTierCraftable=4;
		}
		
		prop10 = config.get(Configuration.CATEGORY_GENERAL, "Valid Kill Methods", "All");
		prop10.comment = "Sources of entity death that are counted to determine if a bag can drop.  Allowable names: All, Player, Real.  All is any source of death, Player is any player entity including mod fake players, Real is only real players.";
		String method = prop10.getString();
		if(method.equalsIgnoreCase("all"))
			BAGFROMPLAYERKILL = 0;
		else if(method.equalsIgnoreCase("player"))
			BAGFROMPLAYERKILL = 1;
		else if(method.equalsIgnoreCase("real"))
			BAGFROMPLAYERKILL = 2;
		else
		{
			FMLLog.log(Level.WARN, "Invalid death source: " + method + ".  Setting method to allow all sources.");
			BAGFROMPLAYERKILL=0;
		}
		
		Property prop11 = config.get(Configuration.CATEGORY_GENERAL, "Limit bag drop to one bag per death", true);
		prop11.comment = "This limits the loot bags to only drop one bag.  Bag weighting is dependant on drop chances.";
		LIMITONEBAGPERDROP = prop11.getBoolean();
		
		prop11 = config.get(Configuration.CATEGORY_GENERAL, "Reverse Rarity Weights", false);
		prop11.comment = "This will reverse the loot distribution for bags, i.e. rarer bags have larger loot tables and common bags have more limited loot tables.";
		REVERSEQUALITY = prop11.getBoolean();
		
		config.save();
		
		if(MONSTERDROPCHANCES[0]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster common drop chance cannot be below 0%, adjusting to 0%");
			MONSTERDROPCHANCES[0]=MINCHANCE;
		}
		else if(MONSTERDROPCHANCES[0]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster common drop chance cannot be above 100%, adjusting to 100%");
			MONSTERDROPCHANCES[0]=MAXCHANCE;
		}
		
		if(PASSIVEDROPCHANCES[0]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob common drop chance cannot be below 0%, adjusting to 0%");
			PASSIVEDROPCHANCES[0]=MINCHANCE;
		}
		else if(PASSIVEDROPCHANCES[0]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob common drop chance cannot be above 100%, adjusting to 100%");
			PASSIVEDROPCHANCES[0]=MAXCHANCE;
		}
		
		if(PLAYERDROPCHANCES[0]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop common chance cannot be below 0%, adjusting to 0%");
			PLAYERDROPCHANCES[0]=MINCHANCE;
		}
		else if(PLAYERDROPCHANCES[0]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop common chance cannot be above 100%, adjusting to 100%");
			PLAYERDROPCHANCES[0]=MAXCHANCE;
		}
		
		if(MONSTERDROPCHANCES[1]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster uncommon drop chance cannot be below 0%, adjusting to 0%");
			MONSTERDROPCHANCES[1]=MINCHANCE;
		}
		else if(MONSTERDROPCHANCES[1]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster uncommon drop chance cannot be above 100%, adjusting to 100%");
			MONSTERDROPCHANCES[1]=MAXCHANCE;
		}
		
		if(PASSIVEDROPCHANCES[1]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob uncommon drop chance cannot be below 0%, adjusting to 0%");
			PASSIVEDROPCHANCES[1]=MINCHANCE;
		}
		else if(PASSIVEDROPCHANCES[1]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob uncommon drop chance cannot be above 100%, adjusting to 100%");
			PASSIVEDROPCHANCES[1]=MAXCHANCE;
		}
		
		if(PLAYERDROPCHANCES[1]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop uncommon chance cannot be below 0%, adjusting to 0%");
			PLAYERDROPCHANCES[1]=MINCHANCE;
		}
		else if(PLAYERDROPCHANCES[1]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop uncommon chance cannot be above 100%, adjusting to 100%");
			PLAYERDROPCHANCES[1]=MAXCHANCE;
		}
		
		if(MONSTERDROPCHANCES[2]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster rare drop chance cannot be below 0%, adjusting to 0%");
			MONSTERDROPCHANCES[2]=MINCHANCE;
		}
		else if(MONSTERDROPCHANCES[2]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster rare drop chance cannot be above 100%, adjusting to 100%");
			MONSTERDROPCHANCES[2]=MAXCHANCE;
		}
		
		if(PASSIVEDROPCHANCES[2]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob rare drop chance cannot be below 0%, adjusting to 0%");
			PASSIVEDROPCHANCES[2]=MINCHANCE;
		}
		else if(PASSIVEDROPCHANCES[2]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob rare drop chance cannot be above 100%, adjusting to 100%");
			PASSIVEDROPCHANCES[2]=MAXCHANCE;
		}
		
		if(PLAYERDROPCHANCES[2]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop rare chance cannot be below 0%, adjusting to 0%");
			PLAYERDROPCHANCES[2]=MINCHANCE;
		}
		else if(PLAYERDROPCHANCES[2]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop rare chance cannot be above 100%, adjusting to 100%");
			PLAYERDROPCHANCES[2]=MAXCHANCE;
		}
		
		if(MONSTERDROPCHANCES[3]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster epic drop chance cannot be below 0%, adjusting to 0%");
			MONSTERDROPCHANCES[3]=MINCHANCE;
		}
		else if(MONSTERDROPCHANCES[3]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster epic drop chance cannot be above 100%, adjusting to 100%");
			MONSTERDROPCHANCES[3]=MAXCHANCE;
		}
		
		if(PASSIVEDROPCHANCES[3]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob epic drop chance cannot be below 0%, adjusting to 0%");
			PASSIVEDROPCHANCES[3]=MINCHANCE;
		}
		else if(PASSIVEDROPCHANCES[3]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob epic drop chance cannot be above 100%, adjusting to 100%");
			PASSIVEDROPCHANCES[3]=MAXCHANCE;
		}
		
		if(PLAYERDROPCHANCES[3]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop epic chance cannot be below 0%, adjusting to 0%");
			PLAYERDROPCHANCES[3]=MINCHANCE;
		}
		else if(PLAYERDROPCHANCES[3]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop epic chance cannot be above 100%, adjusting to 100%");
			PLAYERDROPCHANCES[3]=MAXCHANCE;
		}
		
		if(MONSTERDROPCHANCES[4]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster legendary drop chance cannot be below 0%, adjusting to 0%");
			MONSTERDROPCHANCES[4]=MINCHANCE;
		}
		else if(MONSTERDROPCHANCES[4]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Monster legendary drop chance cannot be above 100%, adjusting to 100%");
			MONSTERDROPCHANCES[4]=MAXCHANCE;
		}
		
		if(PASSIVEDROPCHANCES[4]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob legendary drop chance cannot be below 0%, adjusting to 0%");
			PASSIVEDROPCHANCES[4]=MINCHANCE;
		}
		else if(PASSIVEDROPCHANCES[4]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Passive Mob legendary drop chance cannot be above 100%, adjusting to 100%");
			PASSIVEDROPCHANCES[4]=MAXCHANCE;
		}
		
		if(PLAYERDROPCHANCES[4]<MINCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop legendary chance cannot be below 0%, adjusting to 0%");
			PLAYERDROPCHANCES[4]=MINCHANCE;
		}
		else if(PLAYERDROPCHANCES[4]>MAXCHANCE)
		{
			FMLLog.log(Level.WARN, "Player drop legendary chance cannot be above 100%, adjusting to 100%");
			PLAYERDROPCHANCES[4]=MAXCHANCE;
		}
		
/*		if(LOOTCATEGORYLIST.length<=0)
		{
			FMLLog.log(Level.WARN, "Drop tables must contain at least one ChestGenHook, adding DUNGEON_CHEST as a default.");
			LOOTCATEGORYLIST = new String[]{ChestGenHooks.DUNGEON_CHEST};
		}*/
		
		if(MAXREROLLCOUNT<=0)
		{
			FMLLog.log(Level.WARN, "Reroll count has to be at least 1 (fancy error prevention stuff)");
			MAXREROLLCOUNT=1;
		}
		
		if(TOTALVALUEPERBAG<=0)
		{
			FMLLog.log(Level.WARN, "Free or negative value required for lootbag creation is not a good thing.  Setting it to 1.");
			TOTALVALUEPERBAG=1;
		}
		
		LootbagsPacketHandler.init();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		GameRegistry.registerItem(lootbag, "itemlootbag");
		GameRegistry.registerBlock(recycler, "blockrecycler");
		GameRegistry.registerTileEntity(TileEntityRecycler.class, "tileentityrecycler");
		
		if(!disableRecycler)
			CraftingManager.getInstance().getRecipeList().add(new ShapedOreRecipe(new ItemStack(recycler), new Object[]{"SSS", "SCS", "SIS", 'S', "stone", 'C', new ItemStack(Blocks.chest), 'I', "ingotIron"}));
		
		if(LOOTBAGINDUNGEONLOOT.length>0)
		{
			WeightedRandomChestContent con = new WeightedRandomChestContent(new ItemStack(lootbag, 1, 0), 1, 1, CHESTQUALITYWEIGHT);
			for(String s:LOOTBAGINDUNGEONLOOT)
			{
				ChestGenHooks.addItem(s, con);
			}
		}

		for(int i = 0; i < blacklistlist.length; i++)
		{
			ArrayList<ItemStack> blstack = new ArrayList<ItemStack>();
			for(String s: blacklistlist[i])
			{
				if(!OreDictionary.getOres(s).isEmpty())
				{
					FMLLog.log(Level.INFO, "Added Blacklist items from OreDictionary: " + s);
					blstack.addAll(OreDictionary.getOres(s));
				}
				else
				{
					String trim = s.trim();
					if(!trim.isEmpty())
					{
						String[] words = trim.split("\\s+");
						if(words.length == 1 && i == 0)
						{
							if(Loader.isModLoaded(words[0]) || words[0].equalsIgnoreCase("minecraft"))
							{
								MODBLACKLIST.add(words[0]);
								FMLLog.log(Level.INFO, "Blacklisted Mod with ID: " + words[0] + ".");
							}
						}
						if(words.length == 3)
						{
							ItemStack stack = null;
							//one of these should be not null
							Block block = GameRegistry.findBlock(words[0], words[1]);
							Item item = GameRegistry.findItem(words[0], words[1]);
							if(item != null)
								stack = new ItemStack(item,1,Integer.parseInt(words[2]));
							else if(block != null)
								stack = new ItemStack(block,1,Integer.parseInt(words[2]));
							if(stack != null && stack.getItem() != null)
							{
								FMLLog.log(Level.INFO, i + " Added Blacklist item: " + stack.toString());
								blstack.add(stack);
							}

						}
					}
				}
			}
			BLACKLIST.add((ArrayList<ItemStack>) blstack.clone());
		}
		
		for(int i = 0; i < LOOTCATEGORYLIST.length; i++)
		{
			LOOTMAP.addLootCategory(LOOTCATEGORYLIST[i]);
		}
		LOOTMAP.addWhitelistedItems(whitelistlist);
		LOOTMAP.printMap();
		
		emptyBags = LOOTMAP.checkEmptyBags();
		
		for(int i = 0; i < maxTierCraftable; i++)
		{
			if(emptyBags[i] == null)
			{
				Object[] c = new Object[numBagsToUpgrade];
				for(int j = 0; j < c.length; j++)
				{
					c[j] = new ItemStack(lootbag, 1, i);
				}
				int returnbag = i+1;
				while(emptyBags[returnbag]!=null && returnbag<maxTierCraftable && returnbag<emptyBags.length)
				{
					returnbag++;
				}
				CraftingManager.getInstance().getRecipeList().add(new ShapelessOreRecipe(new ItemStack(lootbag, 1, returnbag), c));
				//CraftingManager.getInstance().getRecipeList().add(new ShapelessOreRecipe(new ItemStack(lootbag, numBagsToUpgrade, i), new Object[]{new ItemStack(lootbag, 1, i+1)}));
			}
		}
		
		//remove empty bags from the drop chances
		for(int i = 0; i < emptyBags.length; i++)
		{
			if(emptyBags[i]!=null)
			{
				MONSTERDROPCHANCES[i]=0;
				PASSIVEDROPCHANCES[i]=0;
				PLAYERDROPCHANCES[i]=0;
			}
		}
	}
	
	@EventHandler
	public void serverLoad(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new ItemDumpCommand());
		event.registerServerCommand(new LootSourceCommand());
	}
	
	public static ArrayList<ItemStack> getLootbagDropList()
	{
		return LOOTMAP.getMapAsList();
	}
	
	/**
	 * Checks to see if an item can be dropped by a lootbag
	 */
	public static boolean isItemDroppable(ItemStack item)
	{
		for(ArrayList<ItemStack> as:BLACKLIST)
		{
			for(ItemStack is: as)
			{
				if(areItemStacksEqualItem(is, item, false, false))
					return false;
			}
		}
		UniqueIdentifier u = GameRegistry.findUniqueIdentifierFor(item.getItem());
		for(String modid:MODBLACKLIST)
		{
			if(modid.equalsIgnoreCase(u.modId))
				return false;
		}
		
		return LOOTMAP.isItemInMap(item);
	}
	
	public static int getItemValue(ItemStack item)
	{
		for(WeightedRandomChestContent c : LOOTMAP.getMapAsChestList())
		{
			if(areItemStacksEqualItem(c.theItemId, item, false, false))
			{
				double value = Math.ceil(LOOTMAP.getTotalWeight()/(c.itemWeight*((item.getMaxStackSize()==1)?(1):(8))));
				if(value <= 0)
					value = 1;
				return (int)value;
			}
		}
		
		return 0;
	}
    
    public static boolean areItemStacksEqualItem(ItemStack is1, ItemStack is2, boolean alwaysUseDamage, boolean considerNBT)
    {
    	if(is1==null ^ is2==null)
    		return false;
    	if(Item.getIdFromItem(is1.getItem()) != Item.getIdFromItem(is2.getItem()))
    		return false;
    	if(!(is1.getMetadata() == is2.getMetadata() || (!alwaysUseDamage && (!is1.getHasSubtypes() && !is2.getHasSubtypes()))))
    		return false;
    	if(considerNBT && !ItemStack.areItemStackTagsEqual(is1, is2))
    		return false;
    	return true;
    }
}
/*******************************************************************************
 * Copyright (c) 2015 Malorolam.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the included license.
 * 
 *********************************************************************************/
