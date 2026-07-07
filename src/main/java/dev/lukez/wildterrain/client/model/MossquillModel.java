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
    private final ModelPart mossBlanket;
    private final ModelPart quillCenter;
    private final ModelPart quillLeft;
    private final ModelPart quillRight;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart muzzle;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;

    public MossquillModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.tail = body.getChild("tail");
        this.mossBlanket = body.getChild("moss_blanket");
        this.quillCenter = body.getChild("quill_center");
        this.quillLeft = body.getChild("quill_left");
        this.quillRight = body.getChild("quill_right");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.muzzle = head.getChild("muzzle");
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
                        .addBox(-4.5F, -3.0F, -5.5F, 9.0F, 6.0F, 11.0F)
                        .texOffs(38, 24)
                        .addBox(-3.5F, 2.3F, -4.0F, 7.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        body.addOrReplaceChild("moss_blanket",
                CubeListBuilder.create()
                        .texOffs(0, 34)
                        .addBox(-4.6F, -4.0F, -5.0F, 9.2F, 1.0F, 10.0F),
                PartPose.ZERO);

        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-2.5F, -2.6F, -4.0F, 5.0F, 5.0F, 4.0F)
                        .texOffs(18, 22)
                        .addBox(-2.0F, -2.7F, -4.15F, 4.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, -1.2F, -5.4F));

        head.addOrReplaceChild("muzzle",
                CubeListBuilder.create()
                        .texOffs(18, 18)
                        .addBox(-1.5F, -0.7F, -2.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.1F, -3.8F));

        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create()
                        .texOffs(30, 18)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(1.5F, -2.0F, -1.8F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create()
                        .texOffs(30, 18)
                        .mirror()
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-1.5F, -2.0F, -1.8F));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(28, 0)
                        .addBox(-1.5F, -1.1F, 0.0F, 3.0F, 3.0F, 5.5F)
                        .texOffs(28, 9)
                        .addBox(-1.0F, -1.8F, 2.5F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, -1.1F, 5.0F));

        body.addOrReplaceChild("quill_center",
                CubeListBuilder.create()
                        .texOffs(46, 0)
                        .addBox(-0.5F, -7.0F, -4.7F, 1.0F, 5.0F, 9.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("quill_left",
                CubeListBuilder.create()
                        .texOffs(46, 13)
                        .addBox(-2.6F, -6.1F, -4.0F, 1.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("quill_right",
                CubeListBuilder.create()
                        .texOffs(46, 13)
                        .mirror()
                        .addBox(1.6F, -6.1F, -4.0F, 1.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("front_left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 48)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                        .texOffs(8, 48)
                        .addBox(-1.2F, 4.2F, -1.4F, 2.4F, 0.8F, 2.8F),
                PartPose.offset(2.5F, 19.0F, -3.0F));
        root.addOrReplaceChild("front_right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 48)
                        .mirror()
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                        .texOffs(8, 48)
                        .mirror()
                        .addBox(-1.2F, 4.2F, -1.4F, 2.4F, 0.8F, 2.8F),
                PartPose.offset(-2.5F, 19.0F, -3.0F));
        root.addOrReplaceChild("back_left_leg",
                CubeListBuilder.create()
                        .texOffs(18, 48)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                        .texOffs(26, 48)
                        .addBox(-1.2F, 4.2F, -1.4F, 2.4F, 0.8F, 2.8F),
                PartPose.offset(2.5F, 19.0F, 3.0F));
        root.addOrReplaceChild("back_right_leg",
                CubeListBuilder.create()
                        .texOffs(18, 48)
                        .mirror()
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                        .texOffs(26, 48)
                        .mirror()
                        .addBox(-1.2F, 4.2F, -1.4F, 2.4F, 0.8F, 2.8F),
                PartPose.offset(-2.5F, 19.0F, 3.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);

        float partialTick = ageInTicks - entity.tickCount;
        float graze = entity.getAnimationIntensity(Mossquill.ANIMATION_GRAZE, partialTick);
        float delight = entity.getAnimationIntensity(Mossquill.ANIMATION_DELIGHT, partialTick);
        float sniff = entity.getAnimationIntensity(Mossquill.ANIMATION_SNIFF, partialTick);
        float idleBreath = Mth.sin(ageInTicks * 0.09F);
        float idleSway = Mth.cos(ageInTicks * 0.07F);
        float walk = limbSwing * 0.6662F;
        float walkAmount = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);

        body.y += idleBreath * 0.12F - delight * 0.25F + graze * 0.65F;
        body.xRot += graze * 0.24F + delight * Mth.sin(ageInTicks * 0.55F) * 0.06F;
        body.zRot += idleSway * 0.018F + Mth.sin(walk) * 0.055F * walkAmount;
        mossBlanket.y += Mth.sin(ageInTicks * 0.18F) * 0.035F;

        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        head.xRot += graze * (0.95F + Mth.sin(ageInTicks * 0.55F) * 0.08F);
        head.xRot += sniff * (Mth.sin(ageInTicks * 0.8F) * 0.18F - 0.18F);
        head.xRot -= delight * 0.22F;
        head.y += graze * 1.45F + sniff * Mth.sin(ageInTicks * 0.75F) * 0.18F;
        muzzle.z -= sniff * 0.28F;

        leftEar.xRot += sniff * Mth.sin(ageInTicks * 1.4F) * 0.28F - delight * 0.18F;
        rightEar.xRot += sniff * Mth.cos(ageInTicks * 1.3F) * 0.28F - delight * 0.18F;
        leftEar.zRot -= 0.08F + delight * 0.16F;
        rightEar.zRot += 0.08F + delight * 0.16F;

        tail.yRot = Mth.cos(ageInTicks * 0.15F) * 0.12F + Mth.sin(walk) * 0.18F * walkAmount;
        tail.yRot += delight * Mth.sin(ageInTicks * 0.7F) * 0.45F;
        tail.xRot += delight * 0.32F - graze * 0.22F;

        quillCenter.xRot += Mth.sin(ageInTicks * 0.11F) * 0.025F - delight * 0.18F + graze * 0.12F;
        quillLeft.zRot -= 0.07F + delight * 0.22F + Mth.sin(ageInTicks * 0.13F) * 0.025F;
        quillRight.zRot += 0.07F + delight * 0.22F + Mth.cos(ageInTicks * 0.13F) * 0.025F;
        quillLeft.xRot += graze * 0.08F;
        quillRight.xRot += graze * 0.08F;

        frontLeftLeg.xRot = Mth.cos(walk) * 1.15F * walkAmount - graze * 0.18F;
        backRightLeg.xRot = Mth.cos(walk) * 1.15F * walkAmount;
        frontRightLeg.xRot = Mth.cos(walk + Mth.PI) * 1.15F * walkAmount - graze * 0.18F;
        backLeftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.15F * walkAmount;
        frontLeftLeg.zRot = graze * 0.08F;
        frontRightLeg.zRot = -graze * 0.08F;
    }
}
