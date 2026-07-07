package dev.lukez.wildterrain.common.entity;

import dev.lukez.wildterrain.core.ModEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.ForgeEventFactory;

public class Mossquill extends Animal {
    public static final int ANIMATION_NONE = 0;
    public static final int ANIMATION_GRAZE = 1;
    public static final int ANIMATION_DELIGHT = 2;
    public static final int ANIMATION_SNIFF = 3;

    private static final int GRAZE_TICKS = 54;
    private static final int DELIGHT_TICKS = 46;
    private static final int SNIFF_TICKS = 34;
    private static final EntityDataAccessor<Integer> DATA_ANIMATION =
            SynchedEntityData.defineId(Mossquill.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_TICKS =
            SynchedEntityData.defineId(Mossquill.class, EntityDataSerializers.INT);

    public Mossquill(EntityType<? extends Mossquill> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 18.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ANIMATION, ANIMATION_NONE);
        entityData.define(DATA_ANIMATION_TICKS, 0);
    }

    public static boolean checkMossquillSpawnRules(EntityType<Mossquill> type, ServerLevelAccessor level,
                                                   MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState floor = level.getBlockState(pos.below());
        boolean naturalFloor = floor.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || floor.is(Blocks.MOSS_BLOCK)
                || floor.is(Blocks.MOSS_CARPET)
                || floor.is(Blocks.MOSSY_COBBLESTONE)
                || floor.is(Blocks.MOSSY_STONE_BRICKS);

        return naturalFloor && level.getRawBrightness(pos, 0) > 7;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1D,
                Ingredient.of(Items.GLOW_BERRIES, Items.MOSS_BLOCK, Items.MOSS_CARPET), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.9D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            tickAnimationState();
            maybeStartIdleSniff();
        }
        tryMossOldStone();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.GLOW_BERRIES) && !isBaby()) {
            if (!level().isClientSide) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 0));
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                startAnimation(ANIMATION_DELIGHT, DELIGHT_TICKS);
                level().broadcastEntityEvent(this, (byte) 18);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.MOSS_BLOCK) || stack.is(Items.MOSS_CARPET);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return ModEntities.MOSSQUILL.get().create(level);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 18) {
            startAnimation(ANIMATION_DELIGHT, DELIGHT_TICKS);
            for (int i = 0; i < 8; i++) {
                level().addParticle(ParticleTypes.HAPPY_VILLAGER,
                        getRandomX(0.7D), getY(0.65D + getRandom().nextDouble() * 0.35D), getRandomZ(0.7D),
                        0.0D, 0.035D, 0.0D);
            }
            return;
        }
        super.handleEntityEvent(id);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.FOX_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    private void tryMossOldStone() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 400 != 0 || getRandom().nextInt(4) != 0) {
            return;
        }
        if (!ForgeEventFactory.getMobGriefingEvent(serverLevel, this)) {
            return;
        }

        BlockPos below = blockPosition().below();
        BlockState replacement = mossedVariant(serverLevel.getBlockState(below));
        if (replacement == null) {
            return;
        }

        if (serverLevel.setBlock(below, replacement, 3)) {
            startAnimation(ANIMATION_GRAZE, GRAZE_TICKS);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, below, GameEvent.Context.of(this, replacement));
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY(0.65D), getZ(),
                    5, 0.35D, 0.2D, 0.35D, 0.01D);
        }
    }

    @Nullable
    private static BlockState mossedVariant(BlockState state) {
        if (state.is(Blocks.COBBLESTONE)) {
            return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        }
        if (state.is(Blocks.STONE_BRICKS) || state.is(Blocks.CRACKED_STONE_BRICKS)) {
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        return null;
    }

    public int getAnimationState() {
        return entityData.get(DATA_ANIMATION);
    }

    public int getAnimationTicks() {
        return entityData.get(DATA_ANIMATION_TICKS);
    }

    public float getAnimationIntensity(int animation, float partialTick) {
        if (getAnimationState() != animation) {
            return 0.0F;
        }

        int duration = animationDuration(animation);
        float remaining = Mth.clamp(getAnimationTicks() - partialTick, 0.0F, duration);
        float ramp = Math.min(remaining, duration - remaining);
        return Mth.clamp(ramp / 6.0F, 0.0F, 1.0F);
    }

    private void startAnimation(int animation, int ticks) {
        entityData.set(DATA_ANIMATION, animation);
        entityData.set(DATA_ANIMATION_TICKS, ticks);
    }

    private void tickAnimationState() {
        int ticks = getAnimationTicks();
        if (ticks <= 0) {
            if (getAnimationState() != ANIMATION_NONE) {
                startAnimation(ANIMATION_NONE, 0);
            }
            return;
        }

        entityData.set(DATA_ANIMATION_TICKS, ticks - 1);
        if (ticks == 1) {
            entityData.set(DATA_ANIMATION, ANIMATION_NONE);
        }
    }

    private void maybeStartIdleSniff() {
        if (getAnimationState() == ANIMATION_NONE && getRandom().nextInt(260) == 0) {
            startAnimation(ANIMATION_SNIFF, SNIFF_TICKS);
        }
    }

    private static int animationDuration(int animation) {
        if (animation == ANIMATION_GRAZE) {
            return GRAZE_TICKS;
        }
        if (animation == ANIMATION_DELIGHT) {
            return DELIGHT_TICKS;
        }
        if (animation == ANIMATION_SNIFF) {
            return SNIFF_TICKS;
        }
        return 1;
    }
}
