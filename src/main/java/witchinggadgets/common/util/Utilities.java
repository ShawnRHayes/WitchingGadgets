package witchinggadgets.common.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.logging.log4j.Level;

import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileNode;
import travellersgear.api.TravellersGearAPI;
import witchinggadgets.WitchingGadgets;
import witchinggadgets.common.items.baubles.ItemCloak;
import baubles.api.BaublesApi;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class Utilities
{
	/**========================== THAUMIC UTILITY METHODS ========================== */

	/**
	 * Tries to access the ItemWandCasting Class from Thaumcraft in order to perform the vis reduction on a wand
	 * @param is The ItemStack representing the wand the player is holding
	 * @param player The respective player
	 * @param aspects The list of!PRIMAL! aspects to consume
	 * @param doit if this is false it will not subtract the vis but merely check if it's possible
	 * @return
	 */
	public static boolean consumeVisFromWand(ItemStack is, EntityPlayer player, AspectList aspects, boolean doit)
	{
		return ((ItemWandCasting)is.getItem()).consumeAllVisCrafting(is, player, aspects, doit);
	}

	/**
	 * Tries to find a matching Arcane Recipe from the given inventory. This returns the recipe rather than the result like the original Thaumcraft method.
	 * @param iInventory the inventory
	 * @param player the user
	 * @return
	 */
	public static IArcaneRecipe findMatchingArcaneRecipe(IInventory iInventory, EntityPlayer player)
	{
		int var2 = 0;
		ItemStack s0 = null;
		ItemStack s1 = null;
		for (int slot = 0; slot < 9; slot++)
		{
			ItemStack s2 = iInventory.getStackInSlot(slot);
			if (s2 != null)
			{
				if (var2 == 0) {
					s0 = s2;
				}
				if (var2 == 1) {
					s1 = s2;
				}
				var2++;
			}
		}
		IArcaneRecipe var13 = null;
		for (Object var11 : ThaumcraftApi.getCraftingRecipes()) {
			if (((var11 instanceof IArcaneRecipe)) && (((IArcaneRecipe)var11).matches(iInventory, player.worldObj, player)))
			{
				var13 = (IArcaneRecipe)var11;
				break;
			}
		}
		return var13 == null ? null : var13;
	}

	/**
	 * A method taken from Thaumcraft's ScanManager, since Azanor made it private .~.
	 * @param world
	 * @param node
	 * @return
	 */
	public static AspectList generateNodeAspects(World world, String node)
	{
		AspectList tags = new AspectList();

		ArrayList<Integer> loc = TileNode.locations.get(node);
		if ((loc != null) && (loc.size() > 0))
		{
			int dim = loc.get(0).intValue();
			int x = loc.get(1).intValue();
			int y = loc.get(2).intValue();
			int z = loc.get(3).intValue();
			if (dim == world.provider.dimensionId)
			{
				TileEntity tnb = world.getTileEntity(x, y, z);
				if ((tnb != null) && ((tnb instanceof INode)))
				{
					AspectList ta = ((INode)tnb).getAspects();
					for (Aspect a : ta.getAspectsSorted()) {
						tags.merge(a, Math.max(4, ta.getAmount(a) / 10));
					}
					switch (((INode)tnb).getNodeType().ordinal())
					{
					case 1: 
						tags.merge(Aspect.ENTROPY, 4); break;
					case 2: 
						tags.merge(Aspect.HUNGER, 4); break;
					case 3: 
						tags.merge(Aspect.TAINT, 4); break;
					case 4: 
						tags.merge(Aspect.HEAL, 2);tags.add(Aspect.ORDER, 2); break;
					case 5: 
						tags.merge(Aspect.DEATH, 2);tags.add(Aspect.DARKNESS, 2);
					}
				}
			}
		}
		return tags.size() > 0 ? tags : null;
	}

	/**
	 * Writes the given ScanResult to a new NBTTag Compound
	 */
	public static NBTTagCompound writeScanResultToNBT(ScanResult scan)
	{
		NBTTagCompound tag = new NBTTagCompound();
		tag.setByte("type", scan.type);
		tag.setInteger("blockId", scan.id);
		tag.setInteger("blockMeta", scan.meta);
		if(scan.entity!=null)
		{
			NBTTagCompound entityTag = new NBTTagCompound();
			scan.entity.writeToNBT(entityTag);
			tag.setTag("entity", entityTag);
			tag.setString("entityClass",scan.entity.getClass().getName());
		}
		tag.setString("phenomena", scan.phenomena);
		return tag;
	}
	/**
	 * Returns a ScanResult from a given NBTTagCompound. Make sure you're passing a valid NBT, otherwise, who knows what will happen! D=
	 */
	public static ScanResult readScanResultFromNBT(NBTTagCompound tag, World world)
	{
		byte type = tag.getByte("type");
		int blockId = tag.getInteger("blockId");
		int blockMeta = tag.getInteger("blockMeta");
		Entity entity = null;
		if(tag.hasKey("entity"))
		{
			try{
				Class<Entity> clazz = (Class<Entity>) Class.forName(tag.getString("entityClass"));
				Constructor<Entity> cons = clazz.getConstructor(World.class);
				entity = cons.newInstance(world);
				entity.readFromNBT(tag.getCompoundTag("entity"));
			}catch(Exception e)
			{
				e.printStackTrace();
				entity = null;
			}
		}
		String phenomena = tag.getString("phenomena");
		ScanResult scan = new ScanResult(type, blockId, blockMeta, entity, phenomena);
		return scan;
	}

	/**
	 * Finds the closest loaded(!) node to the given ChunkCoordinates and returns its tag.
	 */
	public static String findCloseNode(World world, ChunkCoordinates cc)
	{
		Map.Entry<String, ArrayList<Integer>> closest = null;

		for(Map.Entry<String, ArrayList<Integer>> e : TileNode.locations.entrySet())
			if(e.getValue().get(0) == world.provider.dimensionId)
				if(closest==null || cc.getDistanceSquared(e.getValue().get(1), e.getValue().get(2), e.getValue().get(3))<cc.getDistanceSquared(closest.getValue().get(1), closest.getValue().get(2), closest.getValue().get(3)) )
					closest = e;

		if(closest!=null)
			return closest.getKey();
		return null;
	}

	/**
	 * Checks if a research of the given key exists within the given category
	 */
	public static boolean researchExists(String category, String key)
	{
		return ((ResearchCategoryList)ResearchCategories.researchCategories.get(category)).research.containsKey(key);
	}


	/**
	 * Removes Vis from items in the Inventory, ignoring discounts.
	 */
	public static boolean consumeVisFromInventoryWithoutDiscount(EntityPlayer player, AspectList cost)
	{
		IInventory baubles = BaublesApi.getBaubles(player);
		for (int b=0; b<4; b++)
		{
			if(baubles.getStackInSlot(b)!=null && baubles.getStackInSlot(b).getItem() instanceof ItemAmuletVis)
			{
				boolean comnsumed = ((ItemAmuletVis)baubles.getStackInSlot(b).getItem()).consumeAllVis(baubles.getStackInSlot(b), player, cost, true, true);
				if (comnsumed)
					return true;
			}
		}
		for(int i=player.inventory.mainInventory.length-1; i>=0; i--)
		{
			ItemStack item = player.inventory.mainInventory[i];
			if(item != null && item.getItem() instanceof ItemWandCasting)
			{
				boolean comnsumed = ((ItemWandCasting)item.getItem()).consumeAllVisCrafting(item, player, cost, true);
				if (comnsumed)
					return true;
			}
		}
		return false;
	}

	/**========================== COMMON UTILITY METHODS ========================== */

	public static boolean isBlockPlaceable(World world, int x, int y, int z)
	{
		boolean flag = false;
		Block b = world.getBlock(x,y,z);
		if(world.isAirBlock(x, y, z))
		{
			flag = true;
		}
		else if (b.equals(Blocks.snow) || b.equals(Blocks.vine) || b.equals(Blocks.tallgrass) || b.equals(Blocks.deadbush)
				|| (b.isReplaceable(world, x, y, z)))
		{
			flag = true;
		}
		return flag;
	}

	/**
	 * Tries to find a matching Recipe from the given inventory. This returns the recipe rather than the result like the original Minecraft method.
	 * @param iInventory the inventory
	 * @param player the user
	 * @return the recipe
	 */
	public static IRecipe findMatchingRecipe(InventoryCrafting par1InventoryCrafting, World par2World)
	{
		int i = 0;
		ItemStack itemstack = null;
		ItemStack itemstack1 = null;
		int j;

		for (j = 0; j < par1InventoryCrafting.getSizeInventory(); ++j)
		{
			ItemStack itemstack2 = par1InventoryCrafting.getStackInSlot(j);

			if (itemstack2 != null)
			{
				if (i == 0)
				{
					itemstack = itemstack2;
				}

				if (i == 1)
				{
					itemstack1 = itemstack2;
				}

				++i;
			}
		}

		List lRecipes= null;
		try{
			lRecipes = ReflectionHelper.getPrivateValue(CraftingManager.class, CraftingManager.getInstance(), "recipes");
		}catch(Exception e){}
		if(lRecipes == null)
			return null;
		for (j = 0; j < lRecipes.size(); ++j)
		{
			IRecipe irecipe = (IRecipe)lRecipes.get(j);

			if (irecipe.matches(par1InventoryCrafting, par2World))
			{
				return irecipe;
			}
		}
		return null;
	}

	public static boolean compareToOreName(ItemStack item, String oreName)
	{
		for(int oid : OreDictionary.getOreIDs(item))
			if(OreDictionary.getOreName(oid).equalsIgnoreCase(oreName))
				return true;
		return false;
	}
	public static boolean stacksMatchWithOreDic(ItemStack stack0, ItemStack stack1)
	{
		for(int oid0 : OreDictionary.getOreIDs(stack0))
			for(int oid1 : OreDictionary.getOreIDs(stack1))
				if(oid0==oid1)
					return true;
		return false;
	}

	static String[] dyes = new String[]{"dyeBlack","dyeRed","dyeGreen","dyeBrown","dyeBlue","dyePurple","dyeCyan","dyeLightGray","dyeGray","dyePink","dyeLime","dyeYellow","dyeLightBlue","dyeMagenta","dyeOrange","dyeWhite"};
	public static boolean isDye(ItemStack item)
	{
		if(compareToOreName(item,"dye"))
			return true;
		for(String d : dyes)
			if(Utilities.compareToOreName(item, d))
				return true;
		return false;
	}
	public static int getDamageForDye(ItemStack item)
	{
		for(int d=0;d<dyes.length;d++)
			if(Utilities.compareToOreName(item, dyes[d]))
				return d;
		return -1;
	}

	public static boolean isOre(ItemStack item)
	{
		for(int oid : OreDictionary.getOreIDs(item))
			if(OreDictionary.getOreName(oid).startsWith("ore"))
				return true;
		return false;
	}
	public static boolean isOre(World world, int x, int y, int z)
	{
		return isOre( new ItemStack(world.getBlock(x,y,z),1,world.getBlockMetadata(x,y,z)) );
	}

	public static String getTitleCase(String s)
	{
		return s.substring(0, 1).toUpperCase()+s.substring(1).toLowerCase();
	}

	public static void extendPotionArray(int extendBy)
	{
		WitchingGadgets.logger.log(Level.INFO,"Attempting to extend PotionArray by "+extendBy);
		Potion[] potions = new Potion[Potion.potionTypes.length + extendBy];
		for (int i = 0; i < Potion.potionTypes.length; i++)
			potions[i] = Potion.potionTypes[i];
		try
		{
			Field field = null;
			Field[] fields = Potion.class.getDeclaredFields();
			for (Field f : fields)
				if (f.getType().toString().equals("class [Lnet.minecraft.potion.Potion;"))
				{
					field = f;
					break;
				}

			field.setAccessible(true);

			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & 0xFFFFFFEF);

			field.set(null, potions);

			WitchingGadgets.logger.log(Level.INFO,"Variable "+Potion.potionTypes.length);
			WitchingGadgets.logger.log(Level.INFO,"Reflection "+((Potion[])Potion.class.getFields()[0].get(null)).length);

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static int getNextPotionId(int start)
	{
		if ((Potion.potionTypes != null) && (start > 0) && (start < Potion.potionTypes.length) && (Potion.potionTypes[start] == null))
			return start;

		start++;
		if (start < 256)
			start = getNextPotionId(start);
		else
			start = -1;
		return start;
	}
	public static int getNextEnchantmentId(int start)
	{
		if ((Enchantment.enchantmentsList != null) && (start > 0) && (start < Enchantment.enchantmentsList.length) && (Enchantment.enchantmentsList[start] == null))
			return start;

		start++;
		if (start < 256)
			start = getNextEnchantmentId(start);
		else
			start = -1;
		return start;
	}

	public static ItemStack getAspectBean(Aspect a)
	{
		ItemStack bean = new ItemStack(ConfigItems.itemManaBean);
		((ItemManaBean)bean.getItem()).setAspects(bean, new AspectList().add(a, 1));
		return bean;
	}
	public static ItemStack getShard(Aspect a)
	{
		if(!Aspect.getPrimalAspects().contains(a))
			return null;
		int meta = a.equals(Aspect.AIR) ? 0 : a.equals(Aspect.FIRE) ? 1 : a.equals(Aspect.WATER) ? 2 : a.equals(Aspect.EARTH) ? 3 : a.equals(Aspect.ORDER) ? 4 : 5;
		ItemStack shard = new ItemStack(ConfigItems.itemShard,1,meta);
		return shard;
	}

	public static ItemStack getActiveMagicalCloak(EntityPlayer player)
	{
		if(TravellersGearAPI.getExtendedInventory(player)[0]!=null && TravellersGearAPI.getExtendedInventory(player)[0].getItem() instanceof ItemCloak)
			return TravellersGearAPI.getExtendedInventory(player)[0];
		else if(BaublesApi.getBaubles(player).getStackInSlot(3)!=null && BaublesApi.getBaubles(player).getStackInSlot(3).getItem() instanceof ItemCloak)
			return BaublesApi.getBaubles(player).getStackInSlot(3);
		return null;
	}
	public static void updateActiveMagicalCloak(EntityPlayer player, ItemStack cloak)
	{
		if(TravellersGearAPI.getExtendedInventory(player)[0]!=null && TravellersGearAPI.getExtendedInventory(player)[0].getItem() instanceof ItemCloak)
		{
			ItemStack[] tgInv = TravellersGearAPI.getExtendedInventory(player);
			if(tgInv[0].getItemDamage() == cloak.getItemDamage())
				tgInv[0]=cloak;
			TravellersGearAPI.setExtendedInventory(player, tgInv);
		}
		else if(BaublesApi.getBaubles(player).getStackInSlot(3)!=null && BaublesApi.getBaubles(player).getStackInSlot(3).getItem() instanceof ItemCloak)
		{
			if(BaublesApi.getBaubles(player).getStackInSlot(3).getItemDamage() == cloak.getItemDamage())
				BaublesApi.getBaubles(player).setInventorySlotContents(3, cloak);
			BaublesApi.getBaubles(player).markDirty();
		}
	}

	public static ItemStack copyStackWithSize(ItemStack stack, int i)
	{
		ItemStack s = new ItemStack(stack.getItem(),i,stack.getItemDamage());
		if(stack.hasTagCompound())
			s.setTagCompound(stack.getTagCompound());
		return s;
	}

	public static boolean isRightMaterial(Material mat, Material[] materials)
	{
		for(Material m : materials)
			if(m == mat)
				return true;
		return false;
	}
}