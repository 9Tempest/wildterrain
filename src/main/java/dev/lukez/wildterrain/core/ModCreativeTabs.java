package dev.lukez.wildterrain.core;

import dev.lukez.wildterrain.WildTerrain;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WildTerrain.MOD_ID);

    public static final RegistryObject<CreativeModeTab> WILD_TERRAIN = CREATIVE_MODE_TABS.register("wild_terrain",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .title(Component.translatable("itemGroup.wildterrain"))
                    .icon(() -> ModItems.MOSSQUILL_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(ModItems.MOSSQUILL_SPAWN_EGG.get()))
                    .build());

    private ModCreativeTabs() {
    }
}
