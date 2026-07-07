package dev.lukez.wildterrain.core;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.entity.Mossquill;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WildTerrain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CommonModEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MOSSQUILL.get(), Mossquill.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.MOSSQUILL.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mossquill::checkMossquillSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private CommonModEvents() {
    }
}
