package dev.lukez.wildterrain.client;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.client.model.MossquillModel;
import dev.lukez.wildterrain.client.model.XingsingModel;
import dev.lukez.wildterrain.client.renderer.MossquillRenderer;
import dev.lukez.wildterrain.client.renderer.XingsingRenderer;
import dev.lukez.wildterrain.core.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WildTerrain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MOSSQUILL.get(), MossquillRenderer::new);
        event.registerEntityRenderer(ModEntities.XINGSING.get(), XingsingRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MossquillModel.LAYER_LOCATION, MossquillModel::createBodyLayer);
        event.registerLayerDefinition(XingsingModel.LAYER_LOCATION, XingsingModel::createBodyLayer);
    }

    private ClientModEvents() {
    }
}
