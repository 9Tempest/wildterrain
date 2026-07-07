package dev.lukez.wildterrain.common.entity;

import dev.lukez.wildterrain.core.ModEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
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
}
