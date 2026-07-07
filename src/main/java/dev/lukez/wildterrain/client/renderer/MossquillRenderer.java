package dev.lukez.wildterrain.client.renderer;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.client.model.MossquillModel;
import dev.lukez.wildterrain.common.entity.Mossquill;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MossquillRenderer extends MobRenderer<Mossquill, MossquillModel<Mossquill>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(WildTerrain.MOD_ID, "textures/entity/mossquill.png");

    public MossquillRenderer(EntityRendererProvider.Context context) {
        super(context, new MossquillModel<>(context.bakeLayer(MossquillModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(Mossquill entity) {
        return TEXTURE;
    }
}
