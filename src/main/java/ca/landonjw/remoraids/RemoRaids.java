package ca.landonjw.remoraids;

import ca.landonjw.remoraids.api.BossAPIProvider;
import ca.landonjw.remoraids.api.IBossAPI;
import ca.landonjw.remoraids.api.boss.IBoss;
import ca.landonjw.remoraids.api.boss.IBossCreator;
import ca.landonjw.remoraids.api.spawning.IBossSpawner;
import ca.landonjw.remoraids.implementation.BossAPI;
import ca.landonjw.remoraids.implementation.boss.Boss;
import ca.landonjw.remoraids.implementation.boss.BossCreator;
import ca.landonjw.remoraids.implementation.commands.RaidsCommand;
import ca.landonjw.remoraids.implementation.listeners.BossDropListener;
import ca.landonjw.remoraids.implementation.listeners.BossUpdateListener;
import ca.landonjw.remoraids.implementation.listeners.EngageListener;
import ca.landonjw.remoraids.implementation.listeners.StatueInteractListener;
import ca.landonjw.remoraids.implementation.spawning.TimedSpawnListener;
import ca.landonjw.remoraids.internal.api.APIRegistrationUtil;
import ca.landonjw.remoraids.internal.api.config.Config;
import ca.landonjw.remoraids.internal.config.GeneralConfig;
import ca.landonjw.remoraids.internal.config.MessageConfig;
import ca.landonjw.remoraids.internal.config.RestraintsConfig;
import ca.landonjw.remoraids.internal.config.readers.ForgeConfig;
import ca.landonjw.remoraids.internal.config.readers.ForgeConfigAdapter;
import ca.landonjw.remoraids.internal.inventory.api.InventoryAPI;
import ca.landonjw.remoraids.internal.network.RaidsDropPacketHandler;
import ca.landonjw.remoraids.internal.tasks.TaskTickListener;
import ca.landonjw.remoraids.internal.text.Callback;
import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.Moveset;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.enums.forms.EnumBidoof;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;

@Mod(
        modid = RemoRaids.MOD_ID,
        name = RemoRaids.MOD_NAME,
        version = RemoRaids.VERSION,
        acceptableRemoteVersions = "*"
)
public class RemoRaids {

    public static final String MOD_ID = "remoraids";
    public static final String MOD_NAME = "RemoRaids";
    public static final String VERSION = "1.0.0";

    private static RemoRaids instance;

    public static final EventBus EVENT_BUS = new EventBus();
    public static final Logger logger = LogManager.getLogger(MOD_NAME);

    private static Config generalConfig;
    private static Config restraintsConfig;
    private static Config messageConfig;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event){
        instance = this;
        APIRegistrationUtil.register(new BossAPI());
        InventoryAPI.register();

        File directory = new File(event.getModConfigurationDirectory(), "remoraids");

        generalConfig = new ForgeConfig(new ForgeConfigAdapter(directory.toPath().resolve("general.conf")), new GeneralConfig());
        restraintsConfig = new ForgeConfig(new ForgeConfigAdapter(directory.toPath().resolve("restraints.conf")), new RestraintsConfig());
        messageConfig = new ForgeConfig(new ForgeConfigAdapter(directory.toPath().resolve("messages.conf")), new MessageConfig());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event){
        MinecraftForge.EVENT_BUS.register(new TaskTickListener());
        MinecraftForge.EVENT_BUS.register(new BossUpdateListener());

        Pixelmon.EVENT_BUS.register(new EngageListener());
        Pixelmon.EVENT_BUS.register(new BossDropListener());
        Pixelmon.EVENT_BUS.register(new StatueInteractListener());

        RemoRaids.EVENT_BUS.register(new TimedSpawnListener());

        getBossAPI().getRaidRegistry().registerBuilderSupplier(IBossCreator.class, BossCreator::new);
        getBossAPI().getRaidRegistry().registerBuilderSupplier(IBoss.IBossBuilder.class, Boss.BossBuilder::new);

        RemoRaids.EVENT_BUS.register(this);
        RaidsDropPacketHandler.initialize();
    }

    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent event){
        event.registerServerCommand(new Callback());
        event.registerServerCommand(new RaidsCommand());

        IBossSpawner test = IBossCreator.initialize()
                .boss(IBoss.builder()
                        .species(EnumSpecies.Bidoof)
                        .form(EnumBidoof.SIRDOOFUSIII)
                        .level(50)
                        .stat(StatsType.HP, 10000, false)
                        .shiny(true)
                        .ability("Wonderguard")
                        .moveset(new Moveset(Lists.newArrayList(new Attack("Tackle"), new Attack("Pound"), null, null).toArray(new Attack[]{})))
                        .build()
                )
                .announcement(false, "This is a test announcement message")
                .location(FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld(), 69, 69, 420, 90f)
                .build();
        System.out.println(test.serialize());
    }

    public static IBossAPI getBossAPI(){
        return BossAPIProvider.get();
    }

    public static Config getGeneralConfig() {
        return generalConfig;
    }

    public static Config getMessageConfig() {
        return messageConfig;
    }

    public static Config getRestraintsConfig() {
        return restraintsConfig;
    }

    public static InputStream getResourceStream(String path) {
        return RemoRaids.instance.getClass().getClassLoader().getResourceAsStream(path);
    }

}