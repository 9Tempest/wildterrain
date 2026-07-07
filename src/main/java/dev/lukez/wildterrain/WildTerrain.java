package dev.lukez.wildterrain;

import com.mojang.logging.LogUtils;
import dev.lukez.wildterrain.core.ModCreativeTabs;
import dev.lukez.wildterrain.core.ModEntities;
import dev.lukez.wildterrain.core.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WildTerrain.MOD_ID)
public final class WildTerrain {
    public static final String MOD_ID = "wildterrain";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WildTerrain(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Wild Terrain is ready: creatures, ecology hooks, terrain hooks, and ruins.");
    }
}
