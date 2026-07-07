package dev.lukez.wildterrain.core;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.entity.Mossquill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WildTerrain.MOD_ID);

    public static final RegistryObject<EntityType<Mossquill>> MOSSQUILL = ENTITY_TYPES.register("mossquill",
            () -> EntityType.Builder.of(Mossquill::new, MobCategory.CREATURE)
                    .sized(0.8F, 0.75F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(WildTerrain.MOD_ID, "mossquill").toString()));

    private ModEntities() {
    }
}
