package dev.lukez.wildterrain.core;

import dev.lukez.wildterrain.WildTerrain;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WildTerrain.MOD_ID);

    public static final RegistryObject<Item> MOSSQUILL_SPAWN_EGG = ITEMS.register("mossquill_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MOSSQUILL, 0x496b3b, 0xd5e889, new Item.Properties()));

    private ModItems() {
    }
}
