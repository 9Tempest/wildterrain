package dev.lukez.wildterrain.core;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.item.MossquillFieldGuideItem;
import dev.lukez.wildterrain.common.item.XingsingFieldGuideItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WildTerrain.MOD_ID);

    public static final RegistryObject<Item> MOSSQUILL_SPAWN_EGG = ITEMS.register("mossquill_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MOSSQUILL, 0x496b3b, 0xd5e889, new Item.Properties()));

    public static final RegistryObject<Item> MOSSQUILL_FIELD_GUIDE = ITEMS.register("mossquill_field_guide",
            () -> new MossquillFieldGuideItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> XINGSING_SPAWN_EGG = ITEMS.register("xingsing_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.XINGSING, 0xf2eee2, 0x8ec7d8, new Item.Properties()));

    public static final RegistryObject<Item> XINGSING_FIELD_GUIDE = ITEMS.register("xingsing_field_guide",
            () -> new XingsingFieldGuideItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private ModItems() {
    }
}
