package de.katzenpapst.amunra.network.packet;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import de.katzenpapst.amunra.AmunRa;
import de.katzenpapst.amunra.client.gui.GuiARCelestialSelection;
import de.katzenpapst.amunra.client.gui.GuiMothershipSelection;
import de.katzenpapst.amunra.client.gui.GuiMothershipSettings;
import de.katzenpapst.amunra.client.gui.GuiShuttleSelection;
import de.katzenpapst.amunra.crafting.RecipeHelper;
import de.katzenpapst.amunra.entity.spaceship.EntityShuttleFake;
import de.katzenpapst.amunra.helper.PlayerID;
import de.katzenpapst.amunra.helper.ShuttleTeleportHelper;
import de.katzenpapst.amunra.mothership.Mothership;
import de.katzenpapst.amunra.mothership.MothershipWorldData;
import de.katzenpapst.amunra.mothership.MothershipWorldProvider;
import de.katzenpapst.amunra.tick.TickHandlerServer;
import de.katzenpapst.amunra.tile.TileEntityGravitation;
import de.katzenpapst.amunra.tile.TileEntityHydroponics;
import de.katzenpapst.amunra.tile.TileEntityShuttleDock;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.Satellite;
import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.client.gui.screen.GuiCelestialSelection;
import micdoodle8.mods.galacticraft.core.client.gui.screen.GuiCelestialSelection.MapMode;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import micdoodle8.mods.galacticraft.core.network.IPacket;
import micdoodle8.mods.galacticraft.core.network.NetworkUtil;
import micdoodle8.mods.galacticraft.core.network.PacketSimple;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import micdoodle8.mods.galacticraft.core.util.PlayerUtil;

public class PacketSimpleAR extends Packet implements IPacket {

    public enum EnumSimplePacket {

        // ===================== SERVER =====================
        /**
         * Teleport the current player in his shuttle. params: - dimension_id: target dimension
         */
        S_TELEPORT_SHUTTLE(Side.SERVER, Integer.class),

        /**
         * If the player is currently in shuttle GUI, send him back
         */
        S_CANCEL_SHUTTLE(Side.SERVER),

        /**
         * Create a new mothership params: - body_name: the body the new ship is created around
         */
        S_CREATE_MOTHERSHIP(Side.SERVER, String.class),

        /**
         * Start a mothership transit params: - mothership_id: id of the ship to move - body_name: target body name
         */
        S_MOTHERSHIP_TRANSIT_START(Side.SERVER, Integer.class, String.class),

        /**
         * Causes a mothership world provider to update itself (count total mass, etc) params: - dimension_id: dimension
         * ID of the world
         */
        S_MOTHERSHIP_UPDATE(Side.SERVER, Integer.class),

        /**
         * Sends Mothership customisation parameters to the server params: - mothership_id: ID of the ship - nbt_data:
         * subset of mothership data
         */
        S_SET_MOTHERSHIP_SETTINGS(Side.SERVER, Integer.class, NBTTagCompound.class),

        /**
         * Sends a username to the server and hopes that the server finds a user by that name params: - mothership_id -
         * player_name - type
         */
        S_ADD_MOTHERSHIP_PLAYER(Side.SERVER, Integer.class, String.class, Integer.class),

        /**
         * Performs a shuttle dock operation params: - x - y - z coordinates of the dock - op ordinal of the operation
         */
        S_DOCK_OPERATION(Side.SERVER, Integer.class, Integer.class, Integer.class, Integer.class),

        /**
         * Performs a hydroponics tile operation - x - y - z coordinates of the dock - op ordinal of the operation
         */
        S_HYDROPONICS_OPERATION(Side.SERVER, Integer.class, Integer.class, Integer.class, Integer.class),

        /**
         * Updates the gravity side of an artifical gravity block - BlockVec3 pos: position of the block - BlockVec3
         * min: min of the AABB - BlockVec3 max: max of the AABB - Double gravityStrength
         */
        S_ARTIFICIAL_GRAVITY_SETTINGS(Side.SERVER, BlockVec3.class, BlockVec3.class, BlockVec3.class, Double.class),

        // ===================== CLIENT =====================
        /**
         * Signals the client that it has to open the shuttle GUI now params: - player_name: name of the player, is it
         * really needed - dimension_list: copied over from GC, I should find a way to get rid of that
         */
        C_OPEN_SHUTTLE_GUI(Side.CLIENT, String.class, String.class),

        /**
         * Contains the list of motherships from the server, which the client has to replace his own list with params: -
         * nbt_data: the nbt-encoded mothership list
         */
        C_UPDATE_MOTHERSHIP_LIST(Side.CLIENT, NBTTagCompound.class),

        /**
         * Signals the client that a new mothership has been created params: - nbt_data: the newly-created mothership
         */
        C_NEW_MOTHERSHIP_CREATED(Side.CLIENT, NBTTagCompound.class),

        /**
         * Signals the player that his attempt at mothership creation failed
         */
        C_MOTHERSHIP_CREATION_FAILED(Side.CLIENT),

        /**
         * Sent back to all clients, informing them that the transit has started params: - mothership_id: id of the ship
         * - target_body_name: name of the target - travel_time: the travel time in ticks, as calculated by the server
         */
        C_MOTHERSHIP_TRANSIT_STARTED(Side.CLIENT, Integer.class, String.class, Long.class),

        /**
         * Informs the client that an attempt to start a mothership transit has failed params: - mothership_id
         */
        C_MOTHERSHIP_TRANSIT_FAILED(Side.CLIENT, Integer.class),

        /**
         * Informs the client that a transit has ended params: - mothership_id
         */
        C_MOTHERSHIP_TRANSIT_ENDED(Side.CLIENT, Integer.class),

        /**
         * Returns the data from a previous S_MOTHERSHIP_UPDATE to all clients in the dimension
         *
         * params: - dimension_id: the dimension id of the ship - nbt_data: the data to be read by the
         * MothershipWorldProvider
         */
        C_MOTHERSHIP_DATA(Side.CLIENT, Integer.class, NBTTagCompound.class),

        /**
         * Tells the client that the prev. add player operation failed params: - error_message - player_name
         */
        C_ADD_MOTHERSHIP_PLAYER_FAILED(Side.CLIENT, String.class, String.class),

        /**
         * Sends changed mothership setting to clients
         *
         * params: - mothership_id - nbt_data
         */
        C_MOTHERSHIP_SETTINGS_CHANGED(Side.CLIENT, Integer.class, NBTTagCompound.class),

        /**
         * Tells the client that a previous S_TELEPORT_SHUTTLE has failed because of a permission error params: -
         * owner_name
         */
        C_TELEPORT_SHUTTLE_PERMISSION_ERROR(Side.CLIENT, String.class),

        /**
         * Tells the client that a previous S_TELEPORT_SHUTTLE has failed for any other random reason params: -
         * error_msg
         */
        C_TELEPORT_SHUTTLE_FAIL(Side.CLIENT, String.class);

        private Side targetSide;
        private Class<?>[] decodeAs;

        EnumSimplePacket(final Side targetSide, final Class<?>... decodeAs) {
            this.targetSide = targetSide;
            this.decodeAs = decodeAs;
        }

        public Side getTargetSide() {
            return this.targetSide;
        }

        public Class<?>[] getDecodeClasses() {
            return this.decodeAs;
        }
    }

    private EnumSimplePacket type;
    private List<Object> data;

    public PacketSimpleAR() {}

    public PacketSimpleAR(final EnumSimplePacket packetType, final Object... data) {
        this(packetType, Arrays.asList(data));
    }

    public PacketSimpleAR(final EnumSimplePacket packetType, final List<Object> data) {
        if (packetType.getDecodeClasses().length != data.size()) {
            AmunRa.LOGGER.warn("Found data length different than packet type", new RuntimeException());
        }

        this.type = packetType;
        this.data = data;
    }

    @Override
    public void encodeInto(ChannelHandlerContext context, ByteBuf buffer) {
        buffer.writeInt(this.type.ordinal());

        try {
            NetworkUtil.encodeData(buffer, this.data);
        } catch (final IOException e) {
            AmunRa.LOGGER.error("Could not encode data", e);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext context, ByteBuf buffer) {
        this.type = EnumSimplePacket.values()[buffer.readInt()];

        try {
            if (this.type.getDecodeClasses().length > 0) {
                this.data = NetworkUtil.decodeData(this.type.getDecodeClasses(), buffer);
            }
            if (buffer.readableBytes() > 0) {
                // GCLog.severe("Galacticraft packet length problem for packet type " + this.type.toString());
            }
        } catch (final Exception e) {
            AmunRa.LOGGER.error("Error handling simple packet type: " + this.type + " " + buffer, e);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void handleClientSide(EntityPlayer player) {
        if (!(player instanceof EntityClientPlayerMP)) {
            return;
        }

        NBTTagCompound nbt;

        MothershipWorldData mData = TickHandlerServer.mothershipData;

        Mothership motherShip;
        CelestialBody targetBody;

        switch (this.type) {
            case C_OPEN_SHUTTLE_GUI:

                if (String.valueOf(this.data.get(0))
                        .equals(FMLClientHandler.instance().getClient().thePlayer.getGameProfile().getName())) {
                    // TODO refactor
                    final String dimensionList = (String) this.data.get(1);
                    final String[] destinations = dimensionList.split("\\?");
                    final List<CelestialBody> possibleCelestialBodies = Lists.newArrayList();
                    final Map<Integer, Map<String, GuiCelestialSelection.StationDataGUI>> spaceStationData = Maps
                            .newHashMap();
                    for (final String str : destinations) {
                        CelestialBody celestialBody = ShuttleTeleportHelper.getReachableCelestialBodiesForName(str);

                        if (celestialBody == null && str.contains("$")) {
                            final String[] values = str.split("\\$");

                            final int homePlanetID = Integer.parseInt(values[4]);

                            for (final Satellite satellite : GalaxyRegistry.getRegisteredSatellites().values()) {
                                if (satellite.getParentPlanet().getDimensionID() == homePlanetID) {
                                    celestialBody = satellite;
                                    break;
                                }
                            }

                            if (!spaceStationData.containsKey(homePlanetID)) {
                                spaceStationData
                                        .put(homePlanetID, new HashMap<String, GuiCelestialSelection.StationDataGUI>());
                            }

                            spaceStationData.get(homePlanetID).put(
                                    values[1],
                                    new GuiCelestialSelection.StationDataGUI(values[2], Integer.parseInt(values[3])));
                        }

                        if (celestialBody != null) {
                            possibleCelestialBodies.add(celestialBody);
                        }
                    }

                    if (FMLClientHandler.instance().getClient().theWorld != null) {
                        if (!(FMLClientHandler.instance().getClient().currentScreen instanceof GuiShuttleSelection)) {
                            final GuiShuttleSelection gui = new GuiShuttleSelection(
                                    MapMode.TRAVEL,
                                    possibleCelestialBodies);
                            gui.spaceStationMap = spaceStationData;
                            FMLClientHandler.instance().getClient().displayGuiScreen(gui);
                        } else {
                            final GuiShuttleSelection gui = (GuiShuttleSelection) FMLClientHandler.instance()
                                    .getClient().currentScreen;
                            gui.setPossibleBodies(possibleCelestialBodies);
                            gui.spaceStationMap = spaceStationData;
                        }
                    }
                }
                break;
            case C_UPDATE_MOTHERSHIP_LIST:
                nbt = (NBTTagCompound) this.data.get(0);
                if (mData == null) {
                    mData = new MothershipWorldData(MothershipWorldData.saveDataID);
                    TickHandlerServer.mothershipData = mData;
                }
                // mData.updateFromNBT(nbt);

                if (FMLClientHandler.instance()
                        .getClient().currentScreen instanceof GuiShuttleSelection guiShuttleSelection) {
                    guiShuttleSelection.mothershipListUpdated();
                }

                break;
            case C_NEW_MOTHERSHIP_CREATED:
                nbt = (NBTTagCompound) this.data.get(0);

                motherShip = Mothership.createFromNBT(nbt);

                motherShip = TickHandlerServer.mothershipData.addMothership(motherShip);
                if (FMLClientHandler.instance()
                        .getClient().currentScreen instanceof GuiARCelestialSelection guiARCelestialSelection) {
                    guiARCelestialSelection.newMothershipCreated(motherShip);
                }

                break;
            case C_MOTHERSHIP_CREATION_FAILED:
                if (FMLClientHandler.instance()
                        .getClient().currentScreen instanceof GuiARCelestialSelection guiARCelestialSelection) {
                    guiARCelestialSelection.mothershipCreationFailed();
                }
                break;
            case C_MOTHERSHIP_TRANSIT_STARTED:// (Side.CLIENT, Integer.class, String.class, Long.class),
                motherShip = mData.getByMothershipId((int) this.data.get(0));
                targetBody = Mothership.findBodyByNamePath((String) this.data.get(1));
                final long travelTime = (long) this.data.get(2);

                motherShip.startTransit(targetBody, travelTime);

                if (FMLClientHandler.instance()
                        .getClient().currentScreen instanceof GuiShuttleSelection guiShuttleSelection) {
                    guiShuttleSelection.mothershipPositionChanged(motherShip);
                }
                break;
            case C_MOTHERSHIP_TRANSIT_FAILED:
                // not sure what to actually do here
                break;
            case C_MOTHERSHIP_TRANSIT_ENDED: // (Side.CLIENT, Integer.class);
                motherShip = mData.getByMothershipId((int) this.data.get(0));

                motherShip.getWorldProviderClient().endTransit();

                if (FMLClientHandler.instance()
                        .getClient().currentScreen instanceof GuiShuttleSelection guiShuttleSelection) {
                    guiShuttleSelection.mothershipPositionChanged(motherShip);
                }
                break;
            case C_MOTHERSHIP_DATA:
                final int dimId = (int) this.data.get(0);
                nbt = (NBTTagCompound) this.data.get(1);
                final WorldProvider playerWorldProvider = player.getEntityWorld().provider;
                if (playerWorldProvider.dimensionId == dimId
                        && playerWorldProvider instanceof MothershipWorldProvider mothershipProvider) {
                    // don't do this otherwise
                    mothershipProvider.readFromNBT(nbt);
                    if (FMLClientHandler.instance()
                            .getClient().currentScreen instanceof GuiMothershipSelection guiMothershipSelection) {
                        guiMothershipSelection.mothershipUpdateRecieved();
                    }
                }
                break;
            case C_ADD_MOTHERSHIP_PLAYER_FAILED:
                if (FMLClientHandler.instance().getClient().currentScreen instanceof GuiMothershipSettings) {
                    final String msg = (String) this.data.get(0);
                    final String name = (String) this.data.get(1);
                    ((GuiMothershipSettings) FMLClientHandler.instance().getClient().currentScreen)
                            .mothershipOperationFailed(GCCoreUtil.translateWithFormat(msg, name));
                }
                break;
            case C_MOTHERSHIP_SETTINGS_CHANGED:
                final int mothershipId = (int) this.data.get(0);
                nbt = (NBTTagCompound) this.data.get(1);
                final Mothership mShip = TickHandlerServer.mothershipData.getByMothershipId(mothershipId);
                mShip.readSettingsFromNBT(nbt);

                if (player.worldObj.provider.dimensionId == mShip.getDimensionID() && FMLClientHandler.instance()
                        .getClient().currentScreen instanceof GuiMothershipSettings guiMothershipSettings) {
                    guiMothershipSettings.mothershipResponsePacketRecieved();
                }
                break;
            case C_TELEPORT_SHUTTLE_PERMISSION_ERROR:
                if (FMLClientHandler.instance()
                        .getClient().currentScreen instanceof GuiShuttleSelection guiShuttleSelection) {
                    final String owner = (String) this.data.get(0);
                    guiShuttleSelection.showMessageBox(
                            GCCoreUtil.translate("gui.message.mothership.permissionError"),
                            GCCoreUtil.translateWithFormat("gui.message.mothership.notAllowed", owner));
                }
                break;
            default:
                break;
        } // end of case
    }

    private PlayerID getPlayerIdByName(final WorldServer world, final String name) {
        final EntityPlayer otherPlayer = world.getPlayerEntityByName(name);
        if (otherPlayer == null) {
            return null;
        }
        return new PlayerID(otherPlayer);
    }

    @Override
    public void handleServerSide(EntityPlayer player) {

        final EntityPlayerMP playerBase = PlayerUtil.getPlayerBaseServerFromPlayer(player, false);
        MinecraftServer mcServer;

        if (playerBase == null) {
            return;
        }

        final GCPlayerStats stats = GCPlayerStats.get(playerBase);

        String bodyName;
        Integer mothershipId;
        Mothership mShip;
        CelestialBody targetBody;
        WorldServer world;
        MothershipWorldProvider provider;
        TileEntity tileEntity;

        int x;
        int y;
        int z;
        int op;

        switch (this.type) {
            case S_TELEPORT_SHUTTLE: // S_TELEPORT_ENTITY
                try {
                    // final WorldProvider provider = WorldUtil.getProviderForNameServer((String) this.data.get(0));
                    final int dim = (int) this.data.get(0);
                    AmunRa.LOGGER.info("Will teleport to ({})", dim);

                    if (playerBase.worldObj instanceof WorldServer) {
                        mShip = TickHandlerServer.mothershipData.getByDimensionId(dim);
                        // check if the target is a mothership
                        // if the player is currently not on the target MS, check permissions
                        if (mShip != null && playerBase.dimension != dim
                                && !mShip.isPlayerLandingPermitted(playerBase)) {
                            AmunRa.packetPipeline.sendTo(
                                    new PacketSimpleAR(
                                            PacketSimpleAR.EnumSimplePacket.C_TELEPORT_SHUTTLE_PERMISSION_ERROR,
                                            mShip.getOwner().getName()),
                                    playerBase);
                            return;
                        }

                        world = (WorldServer) playerBase.worldObj;
                        // replace this now
                        ShuttleTeleportHelper.transferEntityToDimension(playerBase, dim, world);

                        stats.teleportCooldown = 10;
                        GalacticraftCore.packetPipeline.sendTo(
                                new PacketSimple(PacketSimple.EnumSimplePacket.C_CLOSE_GUI, new Object[] {}),
                                playerBase);

                    }
                } catch (final Exception e) {
                    AmunRa.LOGGER.error(
                            "Error occurred when attempting to transfer entity to dimension: " + this.data.get(0),
                            e);
                }
                break;
            case S_CANCEL_SHUTTLE:
                if (playerBase.worldObj instanceof WorldServer) {
                    if (playerBase.ridingEntity instanceof EntityShuttleFake) {
                        // player is actually in the sky
                        world = (WorldServer) playerBase.worldObj;
                        ShuttleTeleportHelper.transferEntityToDimension(playerBase, world.provider.dimensionId, world);
                        stats.teleportCooldown = 10;
                    }
                    GalacticraftCore.packetPipeline.sendTo(
                            new PacketSimple(PacketSimple.EnumSimplePacket.C_CLOSE_GUI, new Object[] {}),
                            playerBase);
                }
                break;
            case S_CREATE_MOTHERSHIP:

                bodyName = (String) this.data.get(0);
                targetBody = Mothership.findBodyByNamePath(bodyName);
                boolean isSuccessful = false;

                if (Mothership.canBeOrbited(targetBody) && (AmunRa.config.maxNumMotherships < 0
                        || TickHandlerServer.mothershipData.getNumMothershipsForPlayer(playerBase)
                                < AmunRa.config.maxNumMotherships)) {
                    // the matches consumes the actual items
                    if (playerBase.capabilities.isCreativeMode
                            || RecipeHelper.mothershipRecipe.matches(playerBase, true)) {
                        TickHandlerServer.mothershipData.registerNewMothership(playerBase, targetBody);
                        isSuccessful = true;
                    }
                }
                if (!isSuccessful) {
                    AmunRa.packetPipeline.sendTo(
                            new PacketSimpleAR(PacketSimpleAR.EnumSimplePacket.C_MOTHERSHIP_CREATION_FAILED),
                            playerBase);
                }
                break;
            case S_MOTHERSHIP_TRANSIT_START: // (Side.SERVER, Integer.class, String.class),
                // expects motheship ID and target name path
                mothershipId = (int) this.data.get(0);
                bodyName = (String) this.data.get(1);

                mShip = TickHandlerServer.mothershipData.getByMothershipId(mothershipId);
                targetBody = Mothership.findBodyByNamePath(bodyName);

                provider = mShip.getWorldProviderServer();

                if (provider != null && provider.startTransit(targetBody, false)) {
                    AmunRa.packetPipeline.sendToAll(
                            new PacketSimpleAR(
                                    PacketSimpleAR.EnumSimplePacket.C_MOTHERSHIP_TRANSIT_STARTED,
                                    mothershipId,
                                    bodyName,
                                    mShip.getTotalTravelTime()));
                } else {
                    AmunRa.packetPipeline.sendToDimension(
                            new PacketSimpleAR(
                                    PacketSimpleAR.EnumSimplePacket.C_MOTHERSHIP_TRANSIT_FAILED,
                                    mothershipId),
                            mShip.getDimensionID());
                }

                break;
            case S_MOTHERSHIP_UPDATE:
                // why am I doing it like this?
                final int dimId = (int) this.data.get(0);
                mcServer = FMLCommonHandler.instance().getMinecraftServerInstance();
                world = mcServer.worldServerForDimension(dimId);

                if (world.provider instanceof MothershipWorldProvider mothershipProvider) {
                    mothershipProvider.asyncSendMothershipDataToClient();
                }
                break;
            case S_SET_MOTHERSHIP_SETTINGS:
                mothershipId = (int) this.data.get(0);
                NBTTagCompound nbt = (NBTTagCompound) this.data.get(1);
                mShip = TickHandlerServer.mothershipData.getByMothershipId(mothershipId);
                mShip.readSettingsFromNBT(nbt);

                nbt = new NBTTagCompound();
                mShip.writeSettingsToNBT(nbt);

                AmunRa.packetPipeline.sendToAll(
                        new PacketSimpleAR(
                                PacketSimpleAR.EnumSimplePacket.C_MOTHERSHIP_SETTINGS_CHANGED,
                                mothershipId,
                                nbt));
                TickHandlerServer.mothershipData.markDirty();
                break;
            case S_ADD_MOTHERSHIP_PLAYER:
                mothershipId = (int) this.data.get(0);
                final String name = (String) this.data.get(1);
                final int type = (int) this.data.get(2);
                mShip = TickHandlerServer.mothershipData.getByMothershipId(mothershipId);
                if (playerBase.worldObj instanceof WorldServer) {
                    world = (WorldServer) playerBase.worldObj;

                    final PlayerID playerId = this.getPlayerIdByName(world, name);

                    // EntityPlayer otherPlayer = world.getPlayerEntityByName(name);
                    if (playerId != null) {
                        if (playerId.isSameUser(player)) {
                            AmunRa.packetPipeline.sendTo(
                                    new PacketSimpleAR(
                                            PacketSimpleAR.EnumSimplePacket.C_ADD_MOTHERSHIP_PLAYER_FAILED,
                                            "tile.mothershipSettings.permission.addUserErrorSelf",
                                            name),
                                    playerBase);
                        } else {
                            if (type == 0) {
                                mShip.addPlayerToListLanding(playerId);
                            } else if (type == 1) {
                                mShip.addPlayerToListUsage(playerId);
                            }
                            nbt = new NBTTagCompound();
                            mShip.writeSettingsToNBT(nbt);
                            AmunRa.packetPipeline.sendToAll(
                                    new PacketSimpleAR(
                                            PacketSimpleAR.EnumSimplePacket.C_MOTHERSHIP_SETTINGS_CHANGED,
                                            mothershipId,
                                            nbt));
                            TickHandlerServer.mothershipData.markDirty();
                        }
                    } else {
                        AmunRa.packetPipeline.sendTo(
                                new PacketSimpleAR(
                                        PacketSimpleAR.EnumSimplePacket.C_ADD_MOTHERSHIP_PLAYER_FAILED,
                                        "tile.mothershipSettings.permission.addUserError",
                                        name),
                                playerBase);
                    }
                }

                break;
            case S_DOCK_OPERATION:
                x = (int) this.data.get(0);
                y = (int) this.data.get(1);
                z = (int) this.data.get(2);
                op = (int) this.data.get(3);
                tileEntity = playerBase.worldObj.getTileEntity(x, y, z);
                if (tileEntity instanceof TileEntityShuttleDock tileShuttleDock) {
                    tileShuttleDock.performDockOperation(op, playerBase);
                }
                break;
            case S_HYDROPONICS_OPERATION:
                x = (int) this.data.get(0);
                y = (int) this.data.get(1);
                z = (int) this.data.get(2);
                op = (int) this.data.get(3);

                tileEntity = playerBase.worldObj.getTileEntity(x, y, z);
                if (tileEntity instanceof TileEntityHydroponics tileHydroponics) {
                    tileHydroponics.performOperation(op, playerBase);
                }
                break;
            case S_ARTIFICIAL_GRAVITY_SETTINGS:
                final BlockVec3 pos = (BlockVec3) this.data.get(0);
                final BlockVec3 min = (BlockVec3) this.data.get(1);
                final BlockVec3 max = (BlockVec3) this.data.get(2);
                final double strength = (double) this.data.get(3);
                final AxisAlignedBB box = AxisAlignedBB.getBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z);

                tileEntity = pos.getTileEntity(playerBase.worldObj);
                if (tileEntity instanceof TileEntityGravitation tileGravitation) {
                    tileGravitation.setGravityBox(box);
                    tileGravitation.setGravityForce(strength);
                    tileGravitation.updateEnergyConsumption();
                    tileEntity.markDirty();
                    playerBase.worldObj.markBlockForUpdate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void readPacketData(PacketBuffer data) {
        this.decodeInto(null, data);
    }

    @Override
    public void writePacketData(PacketBuffer data) {
        this.encodeInto(null, data);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void processPacket(INetHandler handler) {
        /*
         * if (this.type != EnumSimplePacket.C_UPDATE_SPACESTATION_LIST && this.type !=
         * EnumSimplePacket.C_UPDATE_PLANETS_LIST && this.type != EnumSimplePacket.C_UPDATE_CONFIGS) { return; }
         */
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            this.handleClientSide(FMLClientHandler.instance().getClientPlayerEntity());
        } /**/
    }

}
