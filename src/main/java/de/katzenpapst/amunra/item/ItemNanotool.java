package de.katzenpapst.amunra.item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.UseHoeEvent;

import buildcraft.api.tools.IToolWrench;
import cofh.api.block.IDismantleable;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.api.tool.ITool;
import de.katzenpapst.amunra.AmunRa;
import de.katzenpapst.amunra.GuiIds;
import de.katzenpapst.amunra.helper.InteroperabilityHelper;

@Optional.InterfaceList({
        @Optional.Interface(iface = "crazypants.enderio.api.tool.ITool", modid = "EnderIO", striprefs = true),
        @Optional.Interface(iface = "buildcraft.api.tools.IToolWrench", modid = "BuildCraft|Core", striprefs = true) })
public class ItemNanotool extends ItemAbstractBatteryUser implements ITool, IToolWrench {

    protected IIcon[] icons = null;

    protected float efficiencyOnProperMaterial = 6.0F;

    protected String[] textures = { "nanotool", "nanotool-axe", "nanotool-hoe", "nanotool-pickaxe",
            "nanotool-shears", "nanotool-shovel", "nanotool-wrench" };

    public enum Mode {
        WORKBENCH,
        AXE,
        HOE,
        PICKAXE,
        SHEARS,
        SHOVEL,
        WRENCH
    }

    // total power with default battery = 15000
    // a diamond tool has 1562 small uses
    // with small = 20, it will be 750 uses
    public final float energyCostUseSmall = 20.0F;
    public final float energyCostUseBig = 40.0F;
    public final float energyCostSwitch = 60.0F;

    protected HashMap<Mode, Set<String>> toolClassesSet;

    public ItemNanotool(final String name) {
        this.setUnlocalizedName(name);

        this.icons = new IIcon[this.textures.length];

        this.setTextureName(AmunRa.TEXTUREPREFIX + "nanotool-empty");

        // init this stuff
        this.toolClassesSet = new HashMap<>();

        final Set<String> axe = new HashSet<>();
        axe.add("axe");
        this.toolClassesSet.put(Mode.AXE, axe);

        final Set<String> hoe = new HashSet<>();
        hoe.add("hoe");
        this.toolClassesSet.put(Mode.HOE, hoe);

        final Set<String> pick = new HashSet<>();
        pick.add("pickaxe");
        this.toolClassesSet.put(Mode.PICKAXE, pick);

        final Set<String> shovel = new HashSet<>();
        shovel.add("shovel");
        this.toolClassesSet.put(Mode.SHOVEL, shovel);

        final Set<String> empty = new HashSet<>();
        this.toolClassesSet.put(Mode.SHEARS, empty);
        this.toolClassesSet.put(Mode.WRENCH, empty);
        this.toolClassesSet.put(Mode.WORKBENCH, empty);
    }

    protected void setMode(final ItemStack stack, final Mode m) {
        if (stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger("toolMode", m.ordinal());
    }

    public Mode getMode(final ItemStack stack) {
        final int ord = this.getModeInt(stack);
        return Mode.values()[ord];
    }

    protected int getModeInt(final ItemStack stack) {
        final NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            return 0;
        }
        return nbt.getInteger("toolMode");
    }

    @Override
    public CreativeTabs getCreativeTab() {
        return AmunRa.arTab;
    }

    public boolean hasEnoughEnergyAndMode(final ItemStack stack, final float energy, final Mode mode) {
        return this.getMode(stack) == mode && this.hasEnoughEnergy(stack, energy);
    }

    public boolean hasEnoughEnergy(final ItemStack stack, final float energy) {
        final float storedEnergy = this.getElectricityStored(stack);
        return storedEnergy >= energy;
    }

    /**
     * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
     */
    @Override
    public ItemStack onItemRightClick(final ItemStack itemStack, final World world, final EntityPlayer entityPlayer) {
        if (entityPlayer.isSneaking()) {
            // the wrench sometimes works when sneak-rightclicking
            if (this.hasEnoughEnergyAndMode(itemStack, this.energyCostUseBig, Mode.WRENCH) && (Minecraft.getMinecraft().objectMouseOver.typeOfHit == MovingObjectType.BLOCK)) {
                return super.onItemRightClick(itemStack, world, entityPlayer);
            }
            // try switching
            if (this.hasEnoughEnergy(itemStack, this.energyCostSwitch)) {
                Mode m = this.getMode(itemStack);
                m = this.getNextMode(m);
                this.consumePower(itemStack, entityPlayer, this.energyCostSwitch);
                this.setMode(itemStack, m);
            }
            return itemStack;
        }
        if (this.hasEnoughEnergyAndMode(itemStack, this.energyCostUseBig, Mode.WORKBENCH)) {
            this.consumePower(itemStack, entityPlayer, this.energyCostUseBig);
            entityPlayer.openGui(
                    AmunRa.instance,
                    GuiIds.GUI_CRAFTING,
                    world,
                    (int) entityPlayer.posX,
                    (int) entityPlayer.posY,
                    (int) entityPlayer.posZ);
            return itemStack;
        }
        //
        return super.onItemRightClick(itemStack, world, entityPlayer);
    }

    public Mode getNextMode(final Mode fromMode) {
        return switch (fromMode) {
            case WORKBENCH -> Mode.PICKAXE;
            case PICKAXE -> Mode.SHOVEL;
            case SHOVEL -> Mode.AXE;
            case AXE -> Mode.HOE;
            case HOE -> Mode.SHEARS;
            case SHEARS -> Mode.WRENCH;
            case WRENCH -> Mode.WORKBENCH;
            default -> Mode.PICKAXE;
        };

    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    public IIcon getIcon(final ItemStack stack, final int renderPass, final EntityPlayer player, final ItemStack usingItem, final int useRemaining) {
        return this.getIconIndex(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(final IIconRegister iconRegister) {
        super.registerIcons(iconRegister);
        for (int i = 0; i < this.textures.length; i++) {
            this.icons[i] = iconRegister.registerIcon(AmunRa.TEXTUREPREFIX + this.textures[i]);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(final ItemStack stack) {
        final float energy = this.getElectricityStored(stack);
        if (energy <= 0) {
            return this.itemIcon;
        }

        return this.icons[this.getModeInt(stack)];
        // return this.getIconFromDamage(stack.getItemDamage());
    }

    @Override
    public Set<String> getToolClasses(final ItemStack stack) {
        final float energy = this.getElectricityStored(stack);
        if (energy > 0) {
            final Mode m = this.getMode(stack);
            return this.toolClassesSet.get(m);
        }
        return super.getToolClasses(stack);
    }

    /**
     * Queries the harvest level of this item stack for the specifred tool class, Returns -1 if this tool is not of the
     * specified type
     *
     * @param stack     This item stack instance
     * @param toolClass Tool Class
     * @return Harvest level, or -1 if not the specified tool type.
     */
    @Override
    public int getHarvestLevel(final ItemStack stack, final String toolClass) {
        final float energy = this.getElectricityStored(stack);
        if (energy < this.energyCostUseSmall) {
            return -1;
        }
        final Mode m = this.getMode(stack);
        if (!this.toolClassesSet.get(m).contains(toolClass)) {
            return -1;
        }
        return 5;
    }

    /**
     * Metadata-sensitive version of getStrVsBlock
     * 
     * @param itemstack The Item Stack
     * @param block     The block the item is trying to break
     * @param metadata  The items current metadata
     * @return The damage strength
     */
    @Override
    public float getDigSpeed(final ItemStack itemstack, final Block block, final int metadata) {
        if (!this.hasEnoughEnergy(itemstack, this.energyCostUseSmall)) {
            return 1.0F;
        }
        if (ForgeHooks.isToolEffective(itemstack, block, metadata)
                || this.isEffectiveAgainst(this.getMode(itemstack), block)) {
            return this.efficiencyOnProperMaterial;
        }

        return super.getDigSpeed(itemstack, block, metadata);
        // return func_150893_a(itemstack, block);
    }

    protected boolean isEffectiveAgainst(final Mode m, final Block b) {

        return switch (m) {
            case AXE -> b.getMaterial() == Material.wood || b.getMaterial() == Material.plants
                                    || b.getMaterial() == Material.vine;
            case PICKAXE -> b.getMaterial() == Material.iron || b.getMaterial() == Material.anvil
                                    || b.getMaterial() == Material.rock;
            case SHEARS -> b.getMaterial() == Material.leaves || b.getMaterial() == Material.cloth
                                    || b.getMaterial() == Material.carpet
                                    || b == Blocks.web
                                    || b == Blocks.redstone_wire
                                    || b == Blocks.tripwire;
            case SHOVEL -> b.getMaterial() == Material.clay || b.getMaterial() == Material.ground
                                    || b.getMaterial() == Material.clay;
            case WRENCH, WORKBENCH, HOE -> false;
            default -> false;
        };
    }

    protected String getTypeString(final Mode m) {
        return switch (m) {
            case AXE -> "item.nanotool.mode.axe";
            case HOE -> "item.nanotool.mode.hoe";
            case PICKAXE -> "item.nanotool.mode.pickaxe";
            case SHEARS -> "item.nanotool.mode.shears";
            case SHOVEL -> "item.nanotool.mode.shovel";
            case WORKBENCH -> "item.nanotool.mode.workbench";
            case WRENCH -> "item.nanotool.mode.wrench";
            default -> "";
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addInformation(final ItemStack itemStack, final EntityPlayer entityPlayer, final List list, final boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        final Mode m = this.getMode(itemStack);

        list.add(
                StatCollector.translateToLocal("item.nanotool.mode-prefix") + ": "
                        + StatCollector.translateToLocal(this.getTypeString(m)));
    }

    // damaging start
    /**
     * Current implementations of this method in child classes do not use the entry argument beside ev. They just raise
     * the damage on the stack.
     */
    @Override
    public boolean hitEntity(final ItemStack stack, final EntityLivingBase entity1, final EntityLivingBase user) {
        if (this.hasEnoughEnergy(stack, this.energyCostUseBig)) {
            this.consumePower(stack, user, this.energyCostUseBig);
            return true;
        }
        return false;
    }

    // damaging end

    // shearing
    @Override
    public boolean onBlockDestroyed(final ItemStack stack, final World world, final Block block, final int x, final int y, final int z,
            final EntityLivingBase entity) {
        if (!this.hasEnoughEnergy(stack, this.energyCostUseSmall)) {
            return false;
        }
        this.consumePower(stack, entity, this.energyCostUseSmall);
        if (this.getMode(stack) == Mode.SHEARS) {

            if (block.getMaterial() != Material.leaves && block != Blocks.web
                    && block != Blocks.tallgrass
                    && block != Blocks.vine
                    && block != Blocks.tripwire
                    && !(block instanceof IShearable)) {
                return super.onBlockDestroyed(stack, world, block, x, y, z, entity);
            }
            return true;
        }
        return super.onBlockDestroyed(stack, world, block, x, y, z, entity);
    }

    /**
     * I think this is a "is effective against"
     * 
     * @param block
     * @return
     */
    @Override
    public boolean func_150897_b(final Block block) {
        // I have no choice here...
        return super.func_150897_b(block);
    }

    /**
     * ItemStack sensitive version of {@link #canHarvestBlock(Block)}
     * 
     * @param par1Block The block trying to harvest
     * @param itemStack The itemstack used to harvest the block
     * @return true if can harvest the block
     */
    @Override
    public boolean canHarvestBlock(final Block par1Block, final ItemStack itemStack) {
        return this.isEffectiveAgainst(this.getMode(itemStack), par1Block);
    }

    protected void consumePower(final ItemStack itemStack, final EntityLivingBase user, final float power) {
        EntityPlayer player = null;
        if (user instanceof EntityPlayer) {
            player = (EntityPlayer) user;

        }
        if (player == null || !player.capabilities.isCreativeMode) {
            this.setElectricity(itemStack, this.getElectricityStored(itemStack) - power);
        }
    }

    /**
     * Seems to be a variant of getDigSpeed?
     * 
     * @param stack
     * @param block
     * @return
     */
    @Override
    public float func_150893_a(final ItemStack stack, final Block block) {
        if (this.hasEnoughEnergyAndMode(stack, this.energyCostUseSmall, Mode.SHEARS)) {
            return Items.shears.func_150893_a(stack, block);
        }

        return super.func_150893_a(stack, block);
    }

    /**
     * Returns true if the item can be used on the given entity, e.g. shears on sheep.
     */
    @Override
    public boolean itemInteractionForEntity(final ItemStack itemstack, final EntityPlayer player, final EntityLivingBase entity) {
        if (this.hasEnoughEnergyAndMode(itemstack, this.energyCostUseBig, Mode.SHEARS)) {
            this.consumePower(itemstack, player, this.energyCostUseBig);
            return Items.shears.itemInteractionForEntity(itemstack, player, entity);
        }
        return super.itemInteractionForEntity(itemstack, player, entity);
    }

    @Override
    public boolean onBlockStartBreak(final ItemStack itemstack, final int x, final int y, final int z, final EntityPlayer player) {
        if (this.hasEnoughEnergyAndMode(itemstack, this.energyCostUseSmall, Mode.SHEARS)) {
            return Items.shears.onBlockStartBreak(itemstack, x, y, z, player);
        }
        return super.onBlockStartBreak(itemstack, x, y, z, player);
    }

    // hoeing
    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    @Override
    public boolean onItemUse(final ItemStack stack, final EntityPlayer player, final World world, final int x, final int y, final int z, final int side,
            final float hitX, final float hitY, final float hitZ) {
        if (this.hasEnoughEnergyAndMode(stack, this.energyCostUseSmall, Mode.HOE)) {
            // if(this.getMode(stack) == Mode.HOE) {
            if (!player.canPlayerEdit(x, y, z, side, stack)) {
                return false;
            }
            final UseHoeEvent event = new UseHoeEvent(player, stack, world, x, y, z);
            if (MinecraftForge.EVENT_BUS.post(event)) {
                return false;
            }

            if (event.getResult() == Result.ALLOW) {
                this.consumePower(stack, player, this.energyCostUseSmall);
                // stack.damageItem(1, player);
                return true;
            }

            final Block block = world.getBlock(x, y, z);

            if (side != 0 && world.getBlock(x, y + 1, z).isAir(world, x, y + 1, z)
                    && (block == Blocks.grass || block == Blocks.dirt)) {
                final Block block1 = Blocks.farmland;
                world.playSoundEffect(
                        x + 0.5F,
                        y + 0.5F,
                        z + 0.5F,
                        block1.stepSound.getStepResourcePath(),
                        (block1.stepSound.getVolume() + 1.0F) / 2.0F,
                        block1.stepSound.getPitch() * 0.8F);

                if (world.isRemote) {
                } else {
                    world.setBlock(x, y, z, block1);
                    // stack.damageItem(1, player);
                    this.consumePower(stack, player, this.energyCostUseSmall);
                }
                return true;
            } else {
                return false;
            }
        }
        return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    // wrenching
    @Override
    public boolean canWrench(final EntityPlayer entityPlayer, final int x, final int y, final int z) {
        final ItemStack stack = entityPlayer.inventory.getCurrentItem();

        return this.hasEnoughEnergyAndMode(stack, this.energyCostUseSmall, Mode.WRENCH);
    }

    @Override
    public void wrenchUsed(final EntityPlayer entityPlayer, final int x, final int y, final int z) {
        final ItemStack stack = entityPlayer.inventory.getCurrentItem();

        if (this.hasEnoughEnergyAndMode(stack, this.energyCostUseSmall, Mode.WRENCH)) {
            this.consumePower(stack, entityPlayer, this.energyCostUseSmall);
        }
    }

    // EnderIO
    @Override
    public boolean canUse(final ItemStack stack, final EntityPlayer player, final int x, final int y, final int z) {
        return this.hasEnoughEnergyAndMode(stack, this.energyCostUseSmall, Mode.WRENCH);
    }

    @Override
    public void used(final ItemStack stack, final EntityPlayer player, final int x, final int y, final int z) {
        this.consumePower(stack, player, this.energyCostUseSmall);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isFull3D() {
        return true;
    }

    private boolean attemptDismantle(final EntityPlayer entityPlayer, final Block block, final World world, final int x, final int y, final int z) {
        if (InteroperabilityHelper.hasIDismantleable && (block instanceof IDismantleable
                && ((IDismantleable) block).canDismantle(entityPlayer, world, x, y, z))) {

            ((IDismantleable) block).dismantleBlock(entityPlayer, world, x, y, z, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean onItemUseFirst(final ItemStack stack, final EntityPlayer entityPlayer, final World world, final int x, final int y, final int z,
            final int side, final float hitX, final float hitY, final float hitZ) {
        if (this.hasEnoughEnergyAndMode(stack, this.energyCostUseSmall, Mode.WRENCH)) {

            if (world.isRemote) return false;

            final Block blockID = world.getBlock(x, y, z);

            // try dismantle
            if (entityPlayer.isSneaking() && this.attemptDismantle(entityPlayer, blockID, world, x, y, z)) {

                return true;

            }
            if (blockID == Blocks.furnace || blockID == Blocks.lit_furnace
                    || blockID == Blocks.dropper
                    || blockID == Blocks.hopper
                    || blockID == Blocks.dispenser
                    || blockID == Blocks.piston
                    || blockID == Blocks.sticky_piston) {
                        final int metadata = world.getBlockMetadata(x, y, z);

                        int[] rotationMatrix = { 1, 2, 3, 4, 5, 0 };

                        if (blockID == Blocks.furnace || blockID == Blocks.lit_furnace) {
                            rotationMatrix = ForgeDirection.ROTATION_MATRIX[0];
                        }

                        world.setBlockMetadataWithNotify(
                                x,
                                y,
                                z,
                                ForgeDirection.getOrientation(rotationMatrix[metadata]).ordinal(),
                                3);
                        this.wrenchUsed(entityPlayer, x, y, z);

                        return true;
                    }

            return false;
        }
        return super.onItemUseFirst(stack, entityPlayer, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean shouldHideFacades(final ItemStack stack, final EntityPlayer player) {
        return this.getMode(stack) == Mode.WRENCH;
        // return true;
    }

    /**
     *
     * Should this item, when held, allow sneak-clicks to pass through to the underlying block?
     *
     * @param world  The world
     * @param x      The X Position
     * @param y      The X Position
     * @param z      The X Position
     * @param player The Player that is wielding the item
     * @return
     */
    @Override
    public boolean doesSneakBypassUse(final World world, final int x, final int y, final int z, final EntityPlayer player) {
        final ItemStack stack = player.inventory.getCurrentItem();

        if (this.hasEnoughEnergyAndMode(stack, this.energyCostUseSmall, Mode.WRENCH) && (Minecraft.getMinecraft().objectMouseOver.typeOfHit == MovingObjectType.BLOCK)) {
            return true;
        }

        return false;
    }

    // try this

}
