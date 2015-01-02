package witchinggadgets.common.util.handler;

import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.entities.monster.EntityCultistKnight;
import thaumcraft.common.entities.monster.boss.EntityCultistLeader;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileInfusionMatrix;
import travellersgear.api.TravellersGearAPI;
import witchinggadgets.WitchingGadgets;
import witchinggadgets.api.IPrimordial;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.blocks.tiles.MultipartEssentiaBuffer;
import witchinggadgets.common.blocks.tiles.MultipartEssentiaTube;
import witchinggadgets.common.blocks.tiles.MultipartEssentiaTube_Filtered;
import witchinggadgets.common.blocks.tiles.MultipartEssentiaTube_Valve;
import witchinggadgets.common.items.ItemMaterials;
import witchinggadgets.common.items.baubles.ItemCloak;
import witchinggadgets.common.items.baubles.ItemMagicalBaubles;
import witchinggadgets.common.items.tools.ItemBag;
import witchinggadgets.common.util.Lib;
import witchinggadgets.common.util.Utilities;
import witchinggadgets.common.util.network.PacketClientNotifier;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public class EventHandler
{
	@SubscribeEvent
	public void onEntityConstructing(EntityConstructing event)
	{
	}
	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event)
	{
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void entityHurt(LivingHurtEvent event)
	{
		if(event.source.getSourceOfDamage() instanceof EntityPlayer && ((EntityPlayer)event.source.getSourceOfDamage()).getCurrentEquippedItem()!=null)
		{
			EntityPlayer player = (EntityPlayer)event.source.getSourceOfDamage();
			if(player.getCurrentEquippedItem().getItem().equals(WGContent.ItemPrimordialHammer) && (event.entityLiving instanceof EntitySlime || event.entityLiving.getClass().getName().endsWith("BlueSlime")) )
				event.ammount *= 2;

			if(EnchantmentHelper.getEnchantmentLevel(WGContent.enc_backstab.effectId,player.getCurrentEquippedItem())>0 )
			{
				Vec3 targetVec = event.entityLiving.getLookVec();
				Vec3 attackVec = player.getLookVec();
				if(Math.signum(targetVec.xCoord)==Math.signum(attackVec.xCoord) && Math.signum(targetVec.zCoord)==Math.signum(attackVec.zCoord))
				{
					float mod =1+ .2f * EnchantmentHelper.getEnchantmentLevel(WGContent.enc_backstab.effectId,player.getCurrentEquippedItem());
					if(event.entityLiving instanceof EntityCreature && !player.equals( ((EntityCreature)event.entityLiving).getAttackTarget() ) )
						mod += .4f;
					event.ammount *= mod;
				}
			}
		}
	}

	@SubscribeEvent
	public void onLivingSetTarget(LivingSetAttackTargetEvent event)
	{
		if (!(event.target instanceof EntityPlayer))
			return;
		if(event.entityLiving instanceof EntityCreature)
		{
			EntityPlayer player = (EntityPlayer) event.target;
			if(EnchantmentHelper.getEnchantmentLevel(WGContent.enc_stealth.effectId,player.getCurrentArmor(0))>0 || EnchantmentHelper.getEnchantmentLevel(WGContent.enc_stealth.effectId,player.getCurrentArmor(1))>0)
			{
				float chance = EnchantmentHelper.getEnchantmentLevel(WGContent.enc_stealth.effectId,player.getCurrentArmor(0))*.2f + EnchantmentHelper.getEnchantmentLevel(WGContent.enc_stealth.effectId,player.getCurrentArmor(1))*.2f;
				Vec3 targetVec = event.entityLiving.getLookVec();
				Vec3 attackVec = player.getLookVec();
				if(Math.signum(targetVec.xCoord)!=Math.signum(attackVec.xCoord) || Math.signum(targetVec.zCoord)!=Math.signum(attackVec.zCoord))
					chance-=.5f;
				if(player.getRNG().nextFloat()<chance)
					((EntityCreature)event.entityLiving).setAttackTarget(null);
				else
				{
					for(EntityCreature e : (List<EntityCreature>)player.worldObj.getEntitiesWithinAABB(EntityCreature.class, AxisAlignedBB.getBoundingBox(player.posX-5,player.posY-5,player.posZ-5, player.posX+5,player.posY+5,player.posZ+5)))
						if(e!=null && !(e instanceof IBossDisplayData) && player.equals(e.getAttackTarget()))
							e.setAttackTarget(player);
				}
			}
		}
	}
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		ItemStack cloak = Utilities.getActiveMagicalCloak(event.entityPlayer);
		if(cloak!=null && cloak.hasTagCompound() && cloak.getTagCompound().getBoolean("isSpectral"))
			event.setCanceled(true);

		if(event.entityPlayer.getCurrentEquippedItem()!=null && Block.getBlockFromItem(event.entityPlayer.getCurrentEquippedItem().getItem()) == ConfigBlocks.blockTube && event.entityPlayer.getCurrentEquippedItem().getItemDamage()!=7 && event.entityPlayer.getCurrentEquippedItem().getItemDamage()!=2)
			if(event.action==PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && event.world.getTileEntity(event.x,event.y,event.z) instanceof TileMultipart)
			{
				TileMultipart mp = (TileMultipart)event.world.getTileEntity(event.x,event.y,event.z);
				int meta = event.entityPlayer.getCurrentEquippedItem().getItemDamage();
				TMultiPart part = meta==1?new MultipartEssentiaTube_Valve(meta): meta==3?new MultipartEssentiaTube_Filtered(meta): meta==4?new MultipartEssentiaBuffer(meta): new MultipartEssentiaTube(meta); 
				if(mp.canAddPart(part) && !event.world.isRemote)
				{
					TileMultipart.addPart(event.world, new BlockCoord(event.x,event.y,event.z), part);
					event.useItem = Event.Result.DENY;
					if(!event.entityPlayer.capabilities.isCreativeMode)
						event.entityPlayer.getCurrentEquippedItem().stackSize--;
				}
			}
	}
	@SubscribeEvent
	public void onPlayerInteractWithEntity(EntityInteractEvent event)
	{
		ItemStack cloak = Utilities.getActiveMagicalCloak(event.entityPlayer);
		if(cloak!=null && cloak.hasTagCompound() && cloak.getTagCompound().getBoolean("isSpectral"))
			event.setCanceled(true);
	}
	@SubscribeEvent
	public void onPlayerAttackEntity(AttackEntityEvent event)
	{
		ItemStack cloak = Utilities.getActiveMagicalCloak(event.entityPlayer);
		if(cloak!=null && cloak.hasTagCompound() && cloak.getTagCompound().getBoolean("isSpectral"))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public void onPlayerBreaking(PlayerEvent.BreakSpeed event)
	{
		if(TravellersGearAPI.getExtendedInventory(event.entityPlayer)[2]!=null && TravellersGearAPI.getExtendedInventory(event.entityPlayer)[2].getItem() instanceof ItemMagicalBaubles && TravellersGearAPI.getExtendedInventory(event.entityPlayer)[2].getItemDamage()==3)
		{
			Block block = event.entityPlayer.worldObj.getBlock(event.x,event.y,event.z);
			if(!event.entityPlayer.onGround)
				event.newSpeed *= 5.0F;
			if(event.entityPlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(event.entityPlayer))
				event.newSpeed *= 5.0F;

			float hardness = block.getBlockHardness(event.entityPlayer.worldObj, event.x,event.y,event.z);
			if(hardness>20)
				event.newSpeed = 5+hardness;
		}

	}

	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onLivingDrop(LivingDropsEvent event)
	{
		if(event.entityLiving instanceof EntityWolf)
		{
			EntityWolf enemy = (EntityWolf) event.entityLiving;
			for(int i=0; i<2+Math.min(4, event.lootingLevel); i++)
				if (enemy.worldObj.rand.nextInt(Math.max(1, 3 - event.lootingLevel)) == 0)
				{
					EntityItem entityitem = new EntityItem(event.entityLiving.worldObj, event.entityLiving.posX, event.entityLiving.posY, event.entityLiving.posZ, new ItemStack(WGContent.ItemMaterial,1,6));
					entityitem.delayBeforeCanPickup = 10;
					event.drops.add(entityitem);
				}
		}
		if(event.entityLiving instanceof EntityCultistKnight && event.entityLiving.worldObj.rand.nextInt(10)<1+event.lootingLevel)
			event.drops.add(new EntityItem(event.entityLiving.worldObj, event.entityLiving.posX, event.entityLiving.posY, event.entityLiving.posZ, ItemMagicalBaubles.getItemWithTitle(new ItemStack(WGContent.ItemMagicalBaubles,1,4),Lib.TITLE+"crimsonKnight")));
		if(event.entityLiving instanceof EntityCultistLeader && event.entityLiving.worldObj.rand.nextInt(2)==0)
			event.drops.add(new EntityItem(event.entityLiving.worldObj, event.entityLiving.posX, event.entityLiving.posY, event.entityLiving.posZ, ItemMagicalBaubles.getItemWithTitle(new ItemStack(WGContent.ItemMagicalBaubles,1,4),Lib.TITLE+"crimsonPraetor")));

		if(event.recentlyHit && event.source!=null && event.source.getSourceOfDamage() instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer)event.source.getSourceOfDamage(); 
			if(player.getCurrentEquippedItem()!=null && player.getCurrentEquippedItem().getItem().equals(WGContent.ItemPrimordialSword))
			{

				ItemStack head=null;
				if(event.entityLiving instanceof EntitySkeleton)
					head = new ItemStack(Items.skull,1, ((EntitySkeleton)event.entityLiving).getSkeletonType());
				else if(event.entityLiving instanceof EntityZombie)
					head = new ItemStack(Items.skull,1, 2);
				else if(event.entityLiving instanceof EntityCreeper)
					head = new ItemStack(Items.skull,1, 4);
				else if(event.entityLiving instanceof EntityPlayer)
				{
					head = new ItemStack(Items.skull,1, 3);
					NBTTagCompound tag = new NBTTagCompound();
					tag.setString("SkullOwner", player.getDisplayName());
					head.setTagCompound(tag);
				}
				else if(Loader.isModLoaded("IguanaTweaksTConstruct"))
				{
					Item ith = GameRegistry.findItem("IguanaTweaksTConstruct", "skullItem");
					if(event.entityLiving instanceof EntityEnderman)
						head = new ItemStack(ith,1, 0);
					else if(event.entityLiving instanceof EntityPigZombie)
						head = new ItemStack(ith,1, 1);
					else if(event.entityLiving instanceof EntityBlaze)
						head = new ItemStack(ith,1, 2);
					else if(EntityList.getEntityString(event.entityLiving).equals("Blizz"))
						head = new ItemStack(ith,1, 3);
				}

				if(head!=null)
				{
					Iterator<EntityItem> i = event.drops.iterator();
					while (i.hasNext())
					{
						EntityItem eitem = i.next();
						if(eitem!=null && OreDictionary.itemMatches(eitem.getEntityItem(), head, true))
							return;
					}
					event.entityLiving.worldObj.spawnEntityInWorld(new EntityItem(event.entityLiving.worldObj, event.entityLiving.posX,event.entityLiving.posY,event.entityLiving.posZ, head));
				}
			}
		}
	}

	@SubscribeEvent
	public void onItemPickup(EntityItemPickupEvent event)
	{
		for(int i=0; i<event.entityPlayer.inventory.getSizeInventory(); i++)
			if(event.entityPlayer.inventory.getStackInSlot(i)!=null && event.entityPlayer.inventory.getStackInSlot(i).getItem() instanceof ItemBag && event.entityPlayer.inventory.getStackInSlot(i).getItemDamage()==1)
			{
				ItemStack[] filter = ((ItemBag)event.entityPlayer.inventory.getStackInSlot(i).getItem()).getStoredItems(event.entityPlayer.inventory.getStackInSlot(i));
				for(ItemStack f : filter)
					if(OreDictionary.itemMatches(f, event.item.getEntityItem(), true))
					{
						AspectList al = ThaumcraftCraftingManager.getObjectTags(event.item.getEntityItem());
						al = ThaumcraftCraftingManager.getBonusTags(event.item.getEntityItem(), al);
						if(al!=null && al.size()>=0)
						{
							AspectList primals = ResearchManager.reduceToPrimals(al);
							Aspect a = primals.getAspects()[event.entityPlayer.getRNG().nextInt(primals.getAspects().length)];
							if(a!=null)
							{
								int slot = InventoryUtils.isWandInHotbarWithRoom(a, 1, event.entityPlayer);
								if(slot>=0)
								{
									ItemWandCasting wand = (ItemWandCasting)event.entityPlayer.inventory.mainInventory[slot].getItem();
									wand.addVis(event.entityPlayer.inventory.mainInventory[slot], a, primals.getAmount(a), true);
								}
							}
						}
						event.item.setDead();
						event.setCanceled(true);
						return;
					}
			}
	}

	@SubscribeEvent
	public void onCrafted(ItemCraftedEvent event)
	{
		ItemStack output = event.crafting;
		IInventory craftMatrix = event.craftMatrix;
		if(output.getItem().equals(WGContent.ItemKama))
		{
			for(int matrixSlot = 0; matrixSlot < 9; matrixSlot++)
			{
				ItemStack stackInMatrix = craftMatrix.getStackInSlot(matrixSlot);
				if( stackInMatrix!=null && stackInMatrix.getItem().equals(WGContent.ItemCloak))
					output.setTagCompound(stackInMatrix.getTagCompound());
			}
		}
		if(output.getItem().equals(WGContent.ItemMaterial) && output.getItemDamage() == 10)
		{
			for(int matrixSlot = 0; matrixSlot < 9; matrixSlot++)
			{
				ItemStack stackInMatrix = craftMatrix.getStackInSlot(matrixSlot);
				if( (stackInMatrix != null) && (stackInMatrix.getItem() instanceof ItemMaterials) && stackInMatrix.getItemDamage() == 9)
				{
					stackInMatrix.stackSize += 1;
					craftMatrix.setInventorySlotContents(matrixSlot, stackInMatrix);
				}
			}
		}
		if(output.getItem() instanceof IPrimordial && !event.player.worldObj.isRemote)
		{
			if(((IPrimordial)output.getItem()).getReturnedPearls(output)>0)
			{
				double iX = event.player.posX;
				double iY = event.player.posY+1;
				double iZ = event.player.posZ;
				for(int yy=-16; yy<=16; yy++)
					for(int zz=-16; zz<=16; zz++)
						for(int xx=-16; xx<=16; xx++)
							if(event.player.worldObj.getTileEntity((int)event.player.posX+xx, (int)event.player.posY+yy, (int)event.player.posZ+zz) instanceof TileInfusionMatrix)
							{
								iX = event.player.posX+xx;
								iY = event.player.posY+yy-.5;
								iZ = event.player.posZ+zz;
							}
				EntitySpecialItem entityitem = new EntitySpecialItem(event.player.worldObj, iX, iY, iZ, new ItemStack(ConfigItems.itemEldritchObject, ((IPrimordial)output.getItem()).getReturnedPearls(output) ,3));
				entityitem.setVelocity(0,0,0);
				event.player.worldObj.spawnEntityInWorld(entityitem);
			}
		}
	}

	@SubscribeEvent
	public void playerLogin(PlayerLoggedInEvent event)
	{
		WitchingGadgets.packetPipeline.sendTo(new PacketClientNotifier(0), (EntityPlayerMP) event.player);
	}
}