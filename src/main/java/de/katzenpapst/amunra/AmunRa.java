package de.katzenpapst.amunra;

import micdoodle8.mods.galacticraft.api.GalacticraftRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody.ScalableDistance;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.Moon;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.api.galaxies.SolarSystem;
import micdoodle8.mods.galacticraft.api.galaxies.Star;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.api.world.IAtmosphericGas;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;
import micdoodle8.mods.galacticraft.core.util.CreativeTabGC;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import de.katzenpapst.amunra.block.ARBlocks;
import de.katzenpapst.amunra.block.BlockBasicMulti;
import de.katzenpapst.amunra.block.SubBlock;
import de.katzenpapst.amunra.proxy.ARSidedProxy;
import de.katzenpapst.amunra.world.anubis.AnubisWorldProvider;
import de.katzenpapst.amunra.world.maahes.MaahesWorldProvider;
import de.katzenpapst.amunra.world.neper.NeperWorldProvider;

@Mod(modid = AmunRa.MODID, version = AmunRa.VERSION, dependencies = "required-after:GalacticraftCore",
	name = AmunRa.MODNAME)
public class AmunRa
{
	public static final String MODID = "GalacticraftAmunRa";
    public static final String MODNAME = "Pra's Galacticraft Mod";
    public static final String VERSION = "0.0.1";
    
    @Instance(AmunRa.MODID)
    public static AmunRa instance;
    
    public static final String ASSETPREFIX = "amunra";
    public static final String TEXTUREPREFIX = ASSETPREFIX + ":";
    
    public Star starRa = null;
    public Planet starAmun = null;
    public SolarSystem systemAmunRa = null;
    
    public Planet planetOsiris = null;
    public Planet planetHorus = null;
    public Planet planetBaal = null;
    public Planet planetAnubis = null;
    public Planet asteroidBeltMehen = null;
    public Planet planetSekhmet = null;
    
    public Moon moonBaalRings = null;
    public Moon moonKhonsu;
    public Moon moonNeper;
    public Moon moonIah;
    public Moon moonBastet;
    public Moon moonMaahes;
    public Moon moonThoth;
    public Moon moonSeth;
    
    public Moon moonKebe;
    
    private int dimNeper;
    private int dimMaahes;
    private int dimAnubis;
    
    public static CreativeTabs arTab;
    
    protected BlockBasicMulti basicMultiBlock;
	
	@SidedProxy(clientSide = "de.katzenpapst.amunra.proxy.ClientProxy", serverSide = "de.katzenpapst.amunra.proxy.ServerProxy")
    public static ARSidedProxy proxy;
    
	@EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());

		config.load();

		// Configuration goes here.
		dimNeper = config.get("dimension_ids", "Neper", 20).getInt();
		dimMaahes = config.get("dimension_ids", "Maahes", 21).getInt();
		dimAnubis = config.get("dimension_ids", "Anubis", 22).getInt();
			
		config.save();
        proxy.preInit(event);
    }
	
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	AmunRa.arTab = new CreativeTabGC(CreativeTabs.getNextID(), "GalacticraftBlocks", Item.getItemFromBlock(GCBlocks.machineBase2), 0);
		
    	ARBlocks.initBlocks();
    	
    	
    	
    	// some example code
        System.out.println("DIRT BLOCK >> "+Blocks.dirt.getUnlocalizedName());
        initCelestialBodies();
        proxy.init(event);
        
      //  GCBlocks
    }
    
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        proxy.postInit(event);
    }
    
    protected void initCelestialBodies() {
    	
    	systemAmunRa = new SolarSystem("systemAmunRa", "milkyWay");    	
    	starRa = new Star("starRa");
    	systemAmunRa.setMainStar(starRa).setMapPosition(new Vector3(1.5F, -1.15F, 0.0F));
    	GalaxyRegistry.registerSolarSystem(systemAmunRa);
    	
    	starRa.setBodyIcon(new ResourceLocation(this.ASSETPREFIX, "textures/gui/celestialbodies/sun-red2.png"));
    	starRa.setParentSolarSystem(systemAmunRa);
    	
    	
    	
    	starAmun = createPlanet("starAmun", "sun-blue.png", Math.PI * 0.1, 0.7, 0.9);
    	starAmun.setParentSolarSystem(systemAmunRa);
    	GalaxyRegistry.registerPlanet(starAmun);
    	
    	
    	// two inner planets
    	planetOsiris = createPlanet("osiris", "planet-mercury.png", Math.PI * 0.8, 0.34, 0.4);
    	planetOsiris.setParentSolarSystem(systemAmunRa);
    	GalaxyRegistry.registerPlanet(planetOsiris);
    	
    	planetHorus = createPlanet("horus", "planet-desert.png", Math.PI * 1.3, 0.55, 0.458);
    	planetHorus.setParentSolarSystem(systemAmunRa);
    	GalaxyRegistry.registerPlanet(planetHorus);
    	
    	// gas giant
    	planetBaal = createPlanet("baal", "planet-gas03.png", Math.PI * 1.9, 1.2, 1.4);
    	planetBaal.setParentSolarSystem(systemAmunRa);
    	GalaxyRegistry.registerPlanet(planetBaal);
    	
    	// .. and its moons
    	// ring, aka innermost moon
    	// the regular moon has a distance of 13
    	moonBaalRings = createMoon("baalRings", "micromoon.png", 1.58, 9, 100);
    	moonBaalRings.setParentPlanet(planetBaal);
    	GalaxyRegistry.registerMoon(moonBaalRings);
    	
    	// moon god, but something to do with the creation of life? so maybe stuff here as well
    	moonKhonsu = createMoon("khonsu", "moon.png", 1.9*Math.PI, 12.45, 110);
    	moonKhonsu.setParentPlanet(planetBaal);
    	GalaxyRegistry.registerMoon(moonKhonsu);
    	
    	// this will have an oxygen atmosphere. neper was some kind of a grain god, so
    	moonNeper = createMoon("neper", "planet-life-o2.png", 1.58, 14.9, 140);
    	moonNeper.atmosphere.add(IAtmosphericGas.NITROGEN);
    	moonNeper.atmosphere.add(IAtmosphericGas.OXYGEN);
    	moonNeper.atmosphere.add(IAtmosphericGas.CO2);
    	moonNeper.atmosphere.add(IAtmosphericGas.HELIUM);
    	moonNeper.setDimensionInfo(dimNeper, NeperWorldProvider.class);
    	moonNeper.setParentPlanet(planetBaal);
    	moonNeper.setTierRequired(3);
    	GalacticraftRegistry.registerTeleportType(NeperWorldProvider.class, new NeperWorldProvider());
    	GalaxyRegistry.registerMoon(moonNeper);
    	
    	// just some dead rock. iah was a moon god
    	moonIah = createMoon("iah", "moon.png", 3.1, 18.5, 162);
    	moonIah.setParentPlanet(planetBaal);
    	GalaxyRegistry.registerMoon(moonIah);
    	
    	
    	
    	// an asteroid belt. todo figure the other stuff out later
    	asteroidBeltMehen = createPlanet("asteroidBeltMehen", "micromoon.png", Math.PI * 0.19, 1.4, 1.6);
    	asteroidBeltMehen.setParentSolarSystem(systemAmunRa);
    	GalaxyRegistry.registerPlanet(asteroidBeltMehen);
    	
    	// another gas giant?
    	planetSekhmet = createPlanet("sekhmet", "planet-gas02.png", Math.PI * 0.6, 1.6, 1.8);
    	planetSekhmet.setParentSolarSystem(systemAmunRa);
    	GalaxyRegistry.registerPlanet(planetSekhmet);
    	

    	// ... and it's moons
    	// cat goddess, of course it's a moon of sekhmet
    	moonBastet = createMoon("bast", "moon.png", 3.1, 9.8, 122);
    	moonBastet.setParentPlanet(planetSekhmet);
    	GalaxyRegistry.registerMoon(moonBastet);
    	
    	// lion goddess, dito
    	moonMaahes = createMoon("maahes", "planet-life-ch4.png", 4.514, 11.4, 136);
    	moonMaahes.setParentPlanet(planetSekhmet);
    	moonMaahes.atmosphere.add(IAtmosphericGas.CO2);
    	moonMaahes.atmosphere.add(IAtmosphericGas.METHANE);
    	moonMaahes.atmosphere.add(IAtmosphericGas.HYDROGEN);
    	moonMaahes.atmosphere.add(IAtmosphericGas.ARGON);
    	moonMaahes.setDimensionInfo(dimMaahes, MaahesWorldProvider.class);
    	moonMaahes.setTierRequired(3);
    	GalacticraftRegistry.registerTeleportType(MaahesWorldProvider.class, new MaahesWorldProvider());
    	
    	GalaxyRegistry.registerMoon(moonMaahes);
    	
    	moonThoth = createMoon("thoth", "moon.png", 1.9, 15.5, 145);
    	moonThoth.setParentPlanet(planetSekhmet);
    	GalaxyRegistry.registerMoon(moonThoth);
    	
    	moonSeth = createMoon("seth", "moon.png", 6, 17.98, 198);
    	moonSeth.setParentPlanet(planetSekhmet);
    	GalaxyRegistry.registerMoon(moonSeth);
    	/*
moon.bast=Bastet
moon.maahes=Maahes
moon.thoth=Thoth
moon.seth=Seth*/
    	
    	// a small rocky planet
    	planetAnubis = createPlanet("anubis", "moon.png", Math.PI * 0.36, 1.9, 2.2);
    	planetAnubis.setParentSolarSystem(systemAmunRa);
    	planetAnubis.setDimensionInfo(dimAnubis, AnubisWorldProvider.class);
    	GalacticraftRegistry.registerTeleportType(AnubisWorldProvider.class, new AnubisWorldProvider());
    	planetAnubis.setTierRequired(3);
    	GalaxyRegistry.registerPlanet(planetAnubis);
    	
    	//..with a moon nonetheless
    	moonKebe = createMoon("kebe", "moon.png", 5.1, 19, 253);
    	moonKebe.setParentPlanet(planetAnubis);
    	GalaxyRegistry.registerMoon(moonKebe);
    	
    	
    	
    	
    	/*moonTutorial = (Moon) new Moon("Tutorial").setParentPlanet(GalacticraftCore.planetOverworld).setRelativeSize(0.0017F).setRelativeDistanceFromCenter(new CelestialBody.ScalableDistance(8F, 8F));
        moonTutorial.setRelativeOrbitTime(100F).setTierRequired(1).setBodyIcon(new ResourceLocation(this.ASSETPREFIX, "textures/gui/celestial/moonTutorial.png"));
        moonTutorial.setDimensionInfo(3, TutorialWorldProvider.class);
       
        GalaxyRegistry.registerMoon(moonTutorial);
        GalacticraftRegistry.registerTeleportType(TutorialWorldProvider.class, new TutorialWorldProvider());*/
    }
    
    protected Planet createPlanet(String name, String texture, double phaseShift, double distance, double orbitTime) {
    	Planet pl = new Planet(name);
    	setCelestialBodyStuff(pl, texture, phaseShift, distance, orbitTime);
    	return pl;
    }
    
    protected Moon createMoon(String name, String texture, double phaseShift, double distance, double orbitTime) {
    	Moon pl = new Moon(name);
    	setCelestialBodyStuff(pl, texture, phaseShift, distance, orbitTime);
    	return pl;
    }
    
    protected void setCelestialBodyStuff(CelestialBody body, String texture, double phaseShift, double distance, double orbitTime) {
    	body.setBodyIcon(new ResourceLocation(this.ASSETPREFIX, "textures/gui/celestialbodies/"+texture))
	    	.setPhaseShift((float) phaseShift)
	    	.setRelativeDistanceFromCenter(new ScalableDistance((float)distance, (float)distance))
	    	.setRelativeOrbitTime((float)orbitTime);
    }
}
