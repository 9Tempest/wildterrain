package dev.lukez.wildterrain.client.model;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.entity.Xingsing;
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

public class XingsingModel<T extends Xingsing> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(WildTerrain.MOD_ID, "xingsing"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart muzzle;
    private final ModelPart tail;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public XingsingModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.chest = body.getChild("chest");
        this.head = chest.getChild("head");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.muzzle = head.getChild("muzzle");
        this.tail = body.getChild("tail");
        this.leftArm = chest.getChild("left_arm");
        this.rightArm = chest.getChild("right_arm");
        this.leftLeg = body.getChild("left_leg");
        this.rightLeg = body.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.2F, -4.0F, -2.5F, 6.4F, 8.0F, 5.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));

        PartDefinition chest = body.addOrReplaceChild("chest",
                CubeListBuilder.create()
                        .texOffs(0, 14)
                        .addBox(-3.6F, -5.0F, -2.7F, 7.2F, 6.0F, 5.4F)
                        .texOffs(30, 18)
                        .addBox(-2.6F, -5.2F, -2.9F, 5.2F, 1.0F, 5.8F),
                PartPose.offset(0.0F, -3.5F, 0.0F));

        PartDefinition head = chest.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 26)
                        .addBox(-2.7F, -4.4F, -2.4F, 5.4F, 5.0F, 4.8F)
                        .texOffs(22, 26)
                        .addBox(-2.1F, -4.7F, -2.65F, 4.2F, 1.0F, 3.0F),
                PartPose.offset(0.0F, -4.6F, -0.8F));

        head.addOrReplaceChild("muzzle",
                CubeListBuilder.create()
                        .texOffs(20, 31)
                        .addBox(-1.55F, -0.75F, -2.0F, 3.1F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -0.4F, -2.1F));

        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create()
                        .texOffs(36, 25)
                        .addBox(-0.5F, -3.8F, -0.45F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(2.0F, -3.0F, -0.5F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create()
                        .texOffs(36, 25)
                        .mirror()
                        .addBox(-0.5F, -3.8F, -0.45F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(-2.0F, -3.0F, -0.5F));

        chest.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(28, 0)
                        .addBox(-0.9F, -0.5F, -1.0F, 1.8F, 9.0F, 2.0F)
                        .texOffs(36, 0)
                        .addBox(-1.1F, 7.4F, -1.3F, 2.2F, 1.4F, 2.6F),
                PartPose.offset(3.7F, -3.7F, -0.1F));
        chest.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(28, 0)
                        .mirror()
                        .addBox(-0.9F, -0.5F, -1.0F, 1.8F, 9.0F, 2.0F)
                        .texOffs(36, 0)
                        .mirror()
                        .addBox(-1.1F, 7.4F, -1.3F, 2.2F, 1.4F, 2.6F),
                PartPose.offset(-3.7F, -3.7F, -0.1F));

        body.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 38)
                        .addBox(-1.1F, -0.3F, -1.1F, 2.2F, 7.3F, 2.2F)
                        .texOffs(10, 38)
                        .addBox(-1.4F, 6.2F, -1.6F, 2.8F, 1.0F, 3.2F),
                PartPose.offset(1.7F, 2.1F, 0.0F));
        body.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 38)
                        .mirror()
                        .addBox(-1.1F, -0.3F, -1.1F, 2.2F, 7.3F, 2.2F)
                        .texOffs(10, 38)
                        .mirror()
                        .addBox(-1.4F, 6.2F, -1.6F, 2.8F, 1.0F, 3.2F),
                PartPose.offset(-1.7F, 2.1F, 0.0F));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(42, 5)
                        .addBox(-0.8F, -0.8F, 0.0F, 1.6F, 1.6F, 6.0F)
                        .texOffs(42, 13)
                        .addBox(-1.0F, -1.0F, 4.8F, 2.0F, 2.0F, 2.2F),
                PartPose.offset(0.0F, -1.0F, 2.1F));

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
        float observe = entity.getAnimationIntensity(Xingsing.ANIMATION_OBSERVE, partialTick);
        float mimicJump = entity.getAnimationIntensity(Xingsing.ANIMATION_MIMIC_JUMP, partialTick);
        float mimicSneak = entity.getAnimationIntensity(Xingsing.ANIMATION_MIMIC_SNEAK, partialTick);
        float fetch = entity.getAnimationIntensity(Xingsing.ANIMATION_FETCH, partialTick);
        float returning = entity.getAnimationIntensity(Xingsing.ANIMATION_RETURN_ITEM, partialTick);
        float warn = entity.getAnimationIntensity(Xingsing.ANIMATION_WARN, partialTick);
        float play = entity.getAnimationIntensity(Xingsing.ANIMATION_PLAY, partialTick);
        float walk = limbSwing * 0.6662F;
        float walkAmount = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float idle = Mth.sin(ageInTicks * 0.1F);

        body.y += idle * 0.14F - mimicJump * 0.8F + mimicSneak * 1.4F;
        body.xRot += mimicSneak * 0.28F - mimicJump * 0.18F;
        chest.xRot += observe * -0.08F + warn * -0.18F + mimicSneak * 0.35F;
        chest.zRot += Mth.sin(ageInTicks * 0.08F) * 0.02F + play * Mth.sin(ageInTicks * 0.45F) * 0.08F;

        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD - observe * 0.12F + warn * 0.12F;
        head.y += mimicSneak * 0.35F + observe * Mth.sin(ageInTicks * 0.35F) * 0.12F;
        muzzle.z -= fetch * 0.22F;

        leftEar.xRot -= 0.15F + observe * 0.28F + warn * 0.42F;
        rightEar.xRot -= 0.15F + observe * 0.28F + warn * 0.42F;
        leftEar.zRot -= 0.18F + warn * 0.18F;
        rightEar.zRot += 0.18F + warn * 0.18F;

        leftArm.xRot = Mth.cos(walk + Mth.PI) * 1.25F * walkAmount;
        rightArm.xRot = Mth.cos(walk) * 1.25F * walkAmount;
        leftLeg.xRot = Mth.cos(walk) * 1.15F * walkAmount - mimicSneak * 0.5F;
        rightLeg.xRot = Mth.cos(walk + Mth.PI) * 1.15F * walkAmount - mimicSneak * 0.5F;

        leftArm.xRot -= fetch * 0.75F + returning * 0.45F;
        rightArm.xRot -= fetch * 0.75F + returning * 0.45F;
        leftArm.zRot += fetch * 0.18F - warn * 0.35F;
        rightArm.zRot -= fetch * 0.18F - warn * 0.35F;
        leftArm.yRot += play * 0.18F + returning * 0.12F;
        rightArm.yRot -= play * 0.18F + returning * 0.12F;

        tail.yRot = Mth.sin(ageInTicks * 0.18F) * 0.18F
                + Mth.sin(walk) * 0.28F * walkAmount
                + play * Mth.sin(ageInTicks * 0.8F) * 0.45F;
        tail.xRot += 0.18F + warn * 0.32F - mimicSneak * 0.28F;

        if (mimicJump > 0.0F) {
            leftLeg.xRot -= 0.55F * mimicJump;
            rightLeg.xRot -= 0.55F * mimicJump;
            leftArm.xRot += 0.45F * mimicJump;
            rightArm.xRot += 0.45F * mimicJump;
            tail.xRot += 0.45F * mimicJump;
        }
    }
}
