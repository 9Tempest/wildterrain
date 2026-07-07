package dev.lukez.wildterrain.client.model;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.entity.Mossquill;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MossquillModel<T extends Mossquill> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(WildTerrain.MOD_ID, "mossquill"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;

    public MossquillModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.tail = body.getChild("tail");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -3.0F, -5.0F, 8.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 17)
                        .addBox(-2.5F, -2.5F, -4.0F, 5.0F, 5.0F, 4.0F)
                        .texOffs(18, 17)
                        .addBox(-1.5F, -0.5F, -6.0F, 3.0F, 2.0F, 2.0F)
                        .texOffs(30, 17)
                        .addBox(-2.0F, -5.0F, -2.5F, 1.0F, 3.0F, 1.0F)
                        .texOffs(30, 17)
                        .mirror()
                        .addBox(1.0F, -5.0F, -2.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, -5.0F));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(28, 0)
                        .addBox(-1.5F, -1.0F, 0.0F, 3.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, -1.0F, 4.5F));

        body.addOrReplaceChild("quill_center",
                CubeListBuilder.create()
                        .texOffs(46, 0)
                        .addBox(-0.5F, -7.0F, -4.0F, 1.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("quill_left",
                CubeListBuilder.create()
                        .texOffs(46, 13)
                        .addBox(-2.5F, -6.0F, -3.5F, 1.0F, 4.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("quill_right",
                CubeListBuilder.create()
                        .texOffs(46, 13)
                        .mirror()
                        .addBox(1.5F, -6.0F, -3.5F, 1.0F, 4.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("front_left_leg",
                CubeListBuilder.create().texOffs(0, 27).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(2.5F, 19.0F, -3.0F));
        root.addOrReplaceChild("front_right_leg",
                CubeListBuilder.create().texOffs(0, 27).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(-2.5F, 19.0F, -3.0F));
        root.addOrReplaceChild("back_left_leg",
                CubeListBuilder.create().texOffs(9, 27).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(2.5F, 19.0F, 3.0F));
        root.addOrReplaceChild("back_right_leg",
                CubeListBuilder.create().texOffs(9, 27).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(-2.5F, 19.0F, 3.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);

        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        tail.yRot = Mth.cos(ageInTicks * 0.15F) * 0.15F;

        float walk = limbSwing * 0.6662F;
        frontLeftLeg.xRot = Mth.cos(walk) * 1.2F * limbSwingAmount;
        backRightLeg.xRot = Mth.cos(walk) * 1.2F * limbSwingAmount;
        frontRightLeg.xRot = Mth.cos(walk + Mth.PI) * 1.2F * limbSwingAmount;
        backLeftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.2F * limbSwingAmount;
    }
}
