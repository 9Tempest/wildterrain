package dev.lukez.wildterrain.client.renderer;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.client.model.XingsingModel;
import dev.lukez.wildterrain.common.entity.Xingsing;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class XingsingRenderer extends MobRenderer<Xingsing, XingsingModel<Xingsing>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(WildTerrain.MOD_ID, "textures/entity/xingsing.png");

    public XingsingRenderer(EntityRendererProvider.Context context) {
        super(context, new XingsingModel<>(context.bakeLayer(XingsingModel.LAYER_LOCATION)), 0.32F);
    }

    @Override
    public ResourceLocation getTextureLocation(Xingsing entity) {
        return TEXTURE;
    }
}
