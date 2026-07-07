package dev.lukez.wildterrain.common.entity;

import dev.lukez.wildterrain.common.config.WildTerrainConfig;
import dev.lukez.wildterrain.common.entity.ai.policy.PolicyResourceLoader;
import dev.lukez.wildterrain.common.entity.ai.policy.TinyMlpPolicy;
import dev.lukez.wildterrain.common.entity.ai.xingsing.PlayerActionMemory;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingActionAdapter;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingActionMaskBuilder;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingDecisionLogger;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingObservation;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingObservationBuilder;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingOption;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingRuleTeacher;
import dev.lukez.wildterrain.core.ModEntities;
import java.util.UUID;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class Xingsing extends Animal {
    public static final int ANIMATION_NONE = 0;
    public static final int ANIMATION_OBSERVE = 1;
    public static final int ANIMATION_MIMIC_JUMP = 2;
    public static final int ANIMATION_MIMIC_SNEAK = 3;
    public static final int ANIMATION_FETCH = 4;
    public static final int ANIMATION_RETURN_ITEM = 5;
    public static final int ANIMATION_WARN = 6;
    public static final int ANIMATION_PLAY = 7;

    private static final int OBSERVE_TICKS = 34;
    private static final int MIMIC_JUMP_TICKS = 28;
    private static final int MIMIC_SNEAK_TICKS = 44;
    private static final int FETCH_TICKS = 48;
    private static final int RETURN_ITEM_TICKS = 48;
    private static final int WARN_TICKS = 54;
    private static final int PLAY_TICKS = 50;
    private static final int TRUST_DECAY_INTERVAL = 200;
    private static final EntityDataAccessor<Integer> DATA_ANIMATION =
            SynchedEntityData.defineId(Xingsing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_TICKS =
            SynchedEntityData.defineId(Xingsing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CURRENT_OPTION =
            SynchedEntityData.defineId(Xingsing.class, EntityDataSerializers.INT);
    private static Optional<TinyMlpPolicy> cachedPolicy = Optional.empty();
    @Nullable
    private static MinecraftServer cachedPolicyServer;
    private static boolean policyLoadAttempted;

    private final XingsingObservationBuilder observationBuilder = new XingsingObservationBuilder();
    private final XingsingActionMaskBuilder maskBuilder = new XingsingActionMaskBuilder();
    private final XingsingRuleTeacher teacher = new XingsingRuleTeacher();
    private final XingsingActionAdapter actionAdapter = new XingsingActionAdapter();
    private float trust = 0.35F;
    private float fear = 0.05F;
    private float mischief = 0.55F;
    private long lastFedTick = Long.MIN_VALUE / 4;
    private long lastHurtByPlayerTick = Long.MIN_VALUE / 4;
    @Nullable
    private UUID favoritePlayer;
    private ItemStack carriedItem = ItemStack.EMPTY;
    @Nullable
    private UUID carriedItemOwner;
    private long carriedItemTick = Long.MIN_VALUE / 4;
    private int warningCooldown;
    private int optionElapsedTicks;

    public Xingsing(EntityType<? extends Xingsing> entityType, Level level) {
        super(entityType, level);
        setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public static boolean checkXingsingSpawnRules(EntityType<Xingsing> type, ServerLevelAccessor level,
                                                  MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState floor = level.getBlockState(pos.below());
        boolean naturalFloor = floor.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || floor.is(BlockTags.LOGS)
                || floor.is(BlockTags.LEAVES)
                || floor.is(Blocks.BAMBOO)
                || floor.is(Blocks.MOSS_BLOCK);
        return naturalFloor && level.getRawBrightness(pos, 0) > 7;
    }

    public static boolean isFavoriteFood(ItemStack stack) {
        return stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES)
                || stack.is(Items.COCOA_BEANS)
                || stack.is(Items.MELON_SLICE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ANIMATION, ANIMATION_NONE);
        entityData.define(DATA_ANIMATION_TICKS, 0);
        entityData.define(DATA_CURRENT_OPTION, XingsingOption.IDLE_GROOM.id());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.45D));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(3, new TemptGoal(this, 1.15D,
                Ingredient.of(Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.COCOA_BEANS, Items.MELON_SLICE), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.15D));
        goalSelector.addGoal(8, new RandomStrollGoal(this, 0.95D));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            tickAnimationState();
            tickSocialState();
            tickPolicy();
            if (warningCooldown > 0) {
                warningCooldown--;
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (isFavoriteFood(held) && !isBaby()) {
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                favoritePlayer = player.getUUID();
                lastFedTick = level().getGameTime();
                PlayerActionMemory.markFedXingsing(player, lastFedTick);
                rewardTrust(0.16F, -0.12F);
                fear = Mth.clamp(fear - 0.12F, 0.0F, 1.0F);
                startAnimation(ANIMATION_PLAY, PLAY_TICKS);
                playSound(SoundEvents.PARROT_AMBIENT, 0.55F, 1.45F + getRandom().nextFloat() * 0.2F);
                level().broadcastEntityEvent(this, (byte) 28);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isFavoriteFood(stack);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return ModEntities.XINGSING.get().create(level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && !level().isClientSide) {
            favoritePlayer = favoritePlayer != null ? favoritePlayer : player.getUUID();
            lastHurtByPlayerTick = level().getGameTime();
            trust = Mth.clamp(trust - 0.22F, 0.0F, 1.0F);
            fear = Mth.clamp(fear + 0.32F, 0.0F, 1.0F);
            mischief = Mth.clamp(mischief - 0.06F, 0.0F, 1.0F);
            startAnimation(ANIMATION_WARN, WARN_TICKS);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && hasCarriedItem()) {
            dropCarriedItem(null);
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Trust", trust);
        tag.putFloat("Fear", fear);
        tag.putFloat("Mischief", mischief);
        tag.putLong("LastFedTick", lastFedTick);
        tag.putLong("LastHurtByPlayerTick", lastHurtByPlayerTick);
        if (favoritePlayer != null) {
            tag.putUUID("FavoritePlayer", favoritePlayer);
        }
        if (hasCarriedItem()) {
            tag.put("CarriedItem", carriedItem.save(new CompoundTag()));
            if (carriedItemOwner != null) {
                tag.putUUID("CarriedItemOwner", carriedItemOwner);
            }
            tag.putLong("CarriedItemTick", carriedItemTick);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        trust = Mth.clamp(tag.getFloat("Trust"), 0.0F, 1.0F);
        fear = Mth.clamp(tag.getFloat("Fear"), 0.0F, 1.0F);
        mischief = Mth.clamp(tag.getFloat("Mischief"), 0.0F, 1.0F);
        lastFedTick = tag.getLong("LastFedTick");
        lastHurtByPlayerTick = tag.getLong("LastHurtByPlayerTick");
        favoritePlayer = tag.hasUUID("FavoritePlayer") ? tag.getUUID("FavoritePlayer") : null;
        carriedItem = tag.contains("CarriedItem") ? ItemStack.of(tag.getCompound("CarriedItem")) : ItemStack.EMPTY;
        carriedItemOwner = tag.hasUUID("CarriedItemOwner") ? tag.getUUID("CarriedItemOwner") : null;
        carriedItemTick = tag.getLong("CarriedItemTick");
        setItemSlot(EquipmentSlot.MAINHAND, carriedItem.copy());
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 28) {
            startAnimation(ANIMATION_PLAY, PLAY_TICKS);
            for (int i = 0; i < 8; i++) {
                level().addParticle(ParticleTypes.HAPPY_VILLAGER,
                        getRandomX(0.6D), getY(0.8D + getRandom().nextDouble() * 0.4D), getRandomZ(0.6D),
                        0.0D, 0.035D, 0.0D);
            }
            return;
        }
        super.handleEntityEvent(id);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PARROT_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
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

    public void startAnimation(int animation, int ticks) {
        entityData.set(DATA_ANIMATION, animation);
        entityData.set(DATA_ANIMATION_TICKS, ticks);
    }

    public XingsingOption getCurrentOption() {
        return XingsingOption.byId(entityData.get(DATA_CURRENT_OPTION));
    }

    public void setCurrentOption(XingsingOption option) {
        if (getCurrentOption() != option) {
            optionElapsedTicks = 0;
        }
        entityData.set(DATA_CURRENT_OPTION, option.id());
    }

    public int getOptionElapsedTicks() {
        return optionElapsedTicks;
    }

    public float getTrust() {
        return trust;
    }

    public float getFear() {
        return fear;
    }

    public float getMischief() {
        return mischief;
    }

    public void rewardTrust(float trustDelta, float mischiefDelta) {
        trust = Mth.clamp(trust + trustDelta, 0.0F, 1.0F);
        mischief = Mth.clamp(mischief + mischiefDelta, 0.0F, 1.0F);
    }

    @Nullable
    public UUID getFavoritePlayer() {
        return favoritePlayer;
    }

    public boolean hasCarriedItem() {
        return !carriedItem.isEmpty();
    }

    public void setCarriedItem(ItemStack stack, @Nullable UUID owner) {
        carriedItem = stack.copy();
        carriedItemOwner = owner;
        carriedItemTick = level().getGameTime();
        setItemSlot(EquipmentSlot.MAINHAND, carriedItem.copy());
        startAnimation(ANIMATION_FETCH, FETCH_TICKS);
        level().gameEvent(GameEvent.ENTITY_INTERACT, blockPosition(), GameEvent.Context.of(this));
    }

    public void dropCarriedItem(@Nullable Player preferredOwner) {
        if (!hasCarriedItem() || !(level() instanceof ServerLevel serverLevel)) {
            carriedItem = ItemStack.EMPTY;
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            return;
        }
        ItemStack drop = carriedItem.copy();
        carriedItem = ItemStack.EMPTY;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        double x = preferredOwner == null ? getX() : preferredOwner.getX();
        double y = preferredOwner == null ? getY() + 0.25D : preferredOwner.getY() + 0.25D;
        double z = preferredOwner == null ? getZ() : preferredOwner.getZ();
        ItemEntity item = new ItemEntity(serverLevel, x, y, z, drop);
        if (carriedItemOwner != null) {
            item.setThrower(carriedItemOwner);
        }
        item.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(item);
        carriedItemOwner = null;
        carriedItemTick = Long.MIN_VALUE / 4;
        startAnimation(ANIMATION_RETURN_ITEM, RETURN_ITEM_TICKS);
    }

    @Nullable
    public UUID getCarriedItemOwner() {
        return carriedItemOwner;
    }

    public int getCarriedItemAgeTicks() {
        if (!hasCarriedItem()) {
            return 0;
        }
        return (int) Math.max(0L, level().getGameTime() - carriedItemTick);
    }

    public String getCarriedItemId() {
        if (!hasCarriedItem()) {
            return "";
        }
        return ForgeRegistries.ITEMS.getKey(carriedItem.getItem()).toString();
    }

    public int getWarningCooldown() {
        return warningCooldown;
    }

    public void setWarningCooldown(int warningCooldown) {
        this.warningCooldown = warningCooldown;
    }

    public int getTicksSinceFed() {
        return lastFedTick <= Long.MIN_VALUE / 8 ? 999999 : (int) Math.max(0L, level().getGameTime() - lastFedTick);
    }

    public int getTicksSinceHurtByPlayer() {
        return lastHurtByPlayerTick <= Long.MIN_VALUE / 8
                ? 999999 : (int) Math.max(0L, level().getGameTime() - lastHurtByPlayerTick);
    }

    private void tickPolicy() {
        optionElapsedTicks++;
        if (WildTerrainConfig.xingsingAiMode() == WildTerrainConfig.XingsingAiMode.DISABLED) {
            return;
        }
        int interval = WildTerrainConfig.xingsingDecisionIntervalTicks();
        if (tickCount % interval == 0) {
            XingsingObservation observation = observationBuilder.build(this);
            boolean[] mask = maskBuilder.build(this, observation);
            XingsingOption teacherAction = teacher.select(observation, mask);
            XingsingOption selected = selectPolicyAction(observation, mask, teacherAction);
            actionAdapter.start(this, observation, selected);
            XingsingDecisionLogger.logDecision(this, observation, mask, teacherAction, selected);
        }
        actionAdapter.tick(this);
    }

    private XingsingOption selectPolicyAction(XingsingObservation observation, boolean[] mask,
                                              XingsingOption teacherAction) {
        if (WildTerrainConfig.xingsingAiMode() != WildTerrainConfig.XingsingAiMode.MODEL
                || !WildTerrainConfig.xingsingAllowModelInference()) {
            return teacherAction;
        }

        TinyMlpPolicy policy = getPolicy();
        if (policy == null) {
            return teacherAction;
        }

        try {
            XingsingOption selected = policy.select(observation.vector(), mask, new Random(getRandom().nextLong()));
            if (mask[selected.id()] && actionAdapter.canStart(this, observation, selected)) {
                return selected;
            }
        } catch (RuntimeException ex) {
            dev.lukez.wildterrain.WildTerrain.LOGGER.warn(
                    "Xingsing policy inference failed; falling back to teacher", ex);
        }
        return teacherAction;
    }

    @Nullable
    private TinyMlpPolicy getPolicy() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        MinecraftServer server = serverLevel.getServer();
        if (!policyLoadAttempted || cachedPolicyServer != server) {
            cachedPolicyServer = server;
            cachedPolicy = PolicyResourceLoader.loadXingsingPolicy(server);
            policyLoadAttempted = true;
            cachedPolicy.ifPresentOrElse(
                    policy -> dev.lukez.wildterrain.WildTerrain.LOGGER.info(
                            "Loaded Xingsing model policy with {} inputs and {} outputs.",
                            policy.inputSize(), policy.outputSize()),
                    () -> dev.lukez.wildterrain.WildTerrain.LOGGER.info(
                            "No Xingsing model policy loaded; teacher fallback remains active."));
        }
        return cachedPolicy.orElse(null);
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

    private void tickSocialState() {
        if (tickCount % TRUST_DECAY_INTERVAL != 0) {
            return;
        }
        if (fear > 0.0F) {
            fear = Mth.clamp(fear - 0.015F, 0.0F, 1.0F);
        }
        if (trust > 0.35F && getTicksSinceFed() > 2400) {
            trust = Mth.clamp(trust - 0.01F, 0.0F, 1.0F);
        }
        if (mischief < 0.55F && getTicksSinceHurtByPlayer() > 1200) {
            mischief = Mth.clamp(mischief + 0.01F, 0.0F, 1.0F);
        }
    }

    private static int animationDuration(int animation) {
        return switch (animation) {
            case ANIMATION_OBSERVE -> OBSERVE_TICKS;
            case ANIMATION_MIMIC_JUMP -> MIMIC_JUMP_TICKS;
            case ANIMATION_MIMIC_SNEAK -> MIMIC_SNEAK_TICKS;
            case ANIMATION_FETCH -> FETCH_TICKS;
            case ANIMATION_RETURN_ITEM -> RETURN_ITEM_TICKS;
            case ANIMATION_WARN -> WARN_TICKS;
            case ANIMATION_PLAY -> PLAY_TICKS;
            default -> 1;
        };
    }
}
