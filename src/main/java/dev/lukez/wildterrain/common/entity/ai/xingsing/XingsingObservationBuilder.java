package dev.lukez.wildterrain.common.entity.ai.xingsing;

import dev.lukez.wildterrain.common.entity.Xingsing;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class XingsingObservationBuilder {
    private static final double PLAYER_SCAN_RANGE = 32.0D;
    private static final double ITEM_SCAN_RANGE = 14.0D;
    private static final double HOSTILE_SCAN_RANGE = 18.0D;

    public XingsingObservation build(Xingsing mob) {
        Level level = mob.level();
        long now = level.getGameTime();
        XingsingObservation.Builder builder = new XingsingObservation.Builder();
        builder.gameTime = now;
        builder.healthRatio = clamp01(mob.getHealth() / Math.max(1.0F, mob.getMaxHealth()));
        builder.baby = mob.isBaby();
        builder.grounded = mob.onGround();
        builder.inWater = mob.isInWaterOrBubble();
        builder.carryingItem = mob.hasCarriedItem();
        builder.trust = mob.getTrust();
        builder.fear = mob.getFear();
        builder.mischief = mob.getMischief();
        builder.favoritePlayerUuid = mob.getFavoritePlayer();

        Player nearestPlayer = findNearestPlayer(mob);
        if (nearestPlayer != null) {
            fillPlayerFeatures(builder, mob, nearestPlayer, now, false);
        }

        Player favoritePlayer = findFavoritePlayer(mob, nearestPlayer);
        if (favoritePlayer != null) {
            fillPlayerFeatures(builder, mob, favoritePlayer, now, true);
        }

        ItemEntity item = findNearestEligibleItem(mob, now);
        if (item != null) {
            builder.nearestEligibleItemUuid = item.getUUID();
            builder.nearestEligibleItemVisible = mob.hasLineOfSight(item);
            builder.nearestEligibleItemDistanceNorm = distanceNorm(mob.distanceTo(item), ITEM_SCAN_RANGE);
            UUID owner = DroppedItemOwnershipTracker.ownerOf(item);
            builder.nearestEligibleItemOwnerIsFavorite = owner != null && owner.equals(mob.getFavoritePlayer());
            DroppedItemOwnershipTracker.get(item).ifPresent(entry ->
                    builder.nearestEligibleItemAgeNorm = clamp01((now - entry.dropTick()) / (20.0F * 45.0F)));
            builder.safePathToItemScore = DroppedItemOwnershipTracker.isSafeItemPosition(level, item.blockPosition())
                    ? 1.0F : 0.0F;
        }

        LivingEntity hostile = findNearestHostile(mob);
        if (hostile != null) {
            builder.nearestHostileUuid = hostile.getUUID();
            builder.nearestHostileVisible = mob.hasLineOfSight(hostile);
            builder.nearestHostileDistanceNorm = distanceNorm(mob.distanceTo(hostile), HOSTILE_SCAN_RANGE);
            builder.creeperNearby = hostile instanceof Creeper;
            LivingEntity target = hostile instanceof net.minecraft.world.entity.Mob mobHostile
                    ? mobHostile.getTarget() : null;
            builder.hostileTargetingXingsing = target == mob;
            builder.hostileTargetingPlayer = target instanceof Player;
            builder.threatScore = threatScore(mob, hostile, builder);
        }

        fillWorldFeatures(builder, mob);
        fillDerivedFeatures(builder, mob);
        fillVector(builder, mob);
        return builder.build();
    }

    @Nullable
    private Player findNearestPlayer(Xingsing mob) {
        return mob.level().getNearestPlayer(TargetingConditions.forNonCombat().range(PLAYER_SCAN_RANGE), mob,
                mob.getX(), mob.getEyeY(), mob.getZ());
    }

    @Nullable
    private Player findFavoritePlayer(Xingsing mob, @Nullable Player fallback) {
        UUID favorite = mob.getFavoritePlayer();
        if (favorite == null) {
            return fallback;
        }
        List<Player> players = mob.level().getEntitiesOfClass(Player.class,
                mob.getBoundingBox().inflate(PLAYER_SCAN_RANGE));
        for (Player player : players) {
            if (player.getUUID().equals(favorite)) {
                return player;
            }
        }
        return fallback;
    }

    private void fillPlayerFeatures(XingsingObservation.Builder builder, Xingsing mob, Player player,
                                    long now, boolean favorite) {
        PlayerActionMemory.Memory memory = PlayerActionMemory.get(player);
        float distance = distanceNorm(mob.distanceTo(player), PLAYER_SCAN_RANGE);
        boolean visible = mob.hasLineOfSight(player);
        if (favorite) {
            builder.favoritePlayerUuid = player.getUUID();
            builder.favoritePlayerVisible = visible;
            builder.favoritePlayerDistanceNorm = distance;
            return;
        }

        builder.nearestPlayerUuid = player.getUUID();
        builder.nearestPlayerVisible = visible;
        builder.nearestPlayerDistanceNorm = distance;
        builder.nearestPlayerSneaking = player.isShiftKeyDown() || memory.sneakAgeNorm(now) < 0.35F;
        builder.nearestPlayerSprinting = player.isSprinting() || memory.sprintAgeNorm(now) < 0.35F;
        builder.nearestPlayerJumpedRecently = memory.jumpAgeNorm(now) < 0.35F;
        builder.nearestPlayerDroppedItemRecently = memory.dropAgeNorm(now) < 0.45F;
        builder.nearestPlayerHoldingFood = Xingsing.isFavoriteFood(player.getMainHandItem())
                || Xingsing.isFavoriteFood(player.getOffhandItem());
        builder.nearestPlayerHoldingWeapon = looksLikeWeapon(player.getMainHandItem())
                || looksLikeWeapon(player.getOffhandItem());
        builder.nearestPlayerAttackedRecently = memory.attackAgeNorm(now) < 0.45F;
        builder.ownerUnderAttack = memory.hurtAgeNorm(now) < 0.35F;
    }

    @Nullable
    private ItemEntity findNearestEligibleItem(Xingsing mob, long now) {
        return mob.level().getEntitiesOfClass(ItemEntity.class,
                        mob.getBoundingBox().inflate(ITEM_SCAN_RANGE),
                        item -> DroppedItemOwnershipTracker.isEligible(item, mob, now))
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    @Nullable
    private LivingEntity findNearestHostile(Xingsing mob) {
        return mob.level().getEntitiesOfClass(LivingEntity.class,
                        mob.getBoundingBox().inflate(HOSTILE_SCAN_RANGE),
                        entity -> entity instanceof Enemy && entity.isAlive() && !entity.isSpectator())
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    private static void fillWorldFeatures(XingsingObservation.Builder builder, Xingsing mob) {
        Level level = mob.level();
        BlockPos center = mob.blockPosition();
        int canopyHits = 0;
        int bambooHits = 0;
        int vineHits = 0;
        int lavaHits = 0;
        int ledgeHits = 0;
        double nearestPerchSqr = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -2, -5), center.offset(5, 7, 5))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock || state.is(BlockTags.LOGS)) {
                canopyHits++;
                if (level.getBlockState(pos.above()).isAir() && pos.getY() >= center.getY()) {
                    nearestPerchSqr = Math.min(nearestPerchSqr, pos.distSqr(center));
                }
            }
            if (state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)) {
                bambooHits++;
            }
            if (state.getBlock() instanceof VineBlock) {
                vineHits++;
            }
            if (state.is(Blocks.LAVA) || state.getFluidState().is(FluidTags.LAVA)) {
                lavaHits++;
            }
        }

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -4, -3), center.offset(3, -1, 3))) {
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                ledgeHits++;
            }
        }

        builder.treeCoverScore = clamp01(canopyHits / 24.0F);
        builder.bambooNearby = bambooHits > 0;
        builder.vinesNearby = vineHits > 0;
        builder.lavaNearby = lavaHits > 0;
        builder.cliffNearby = ledgeHits > 10;
        builder.perchReachable = nearestPerchSqr < Double.MAX_VALUE;
        builder.perchDistanceNorm = builder.perchReachable
                ? distanceNorm((float) Math.sqrt(nearestPerchSqr), 9.0D) : 1.0F;
        builder.escapePathScore = clamp01((builder.treeCoverScore + (builder.perchReachable ? 0.5F : 0.0F)
                + (builder.vinesNearby ? 0.2F : 0.0F)) / 1.2F);
        builder.dashPathScore = builder.lavaNearby || builder.cliffNearby ? 0.25F : 0.9F;
        builder.safeReturnPathScore = builder.lavaNearby ? 0.35F : 0.9F;
    }

    private static void fillDerivedFeatures(XingsingObservation.Builder builder, Xingsing mob) {
        builder.imitateJumpScore = builder.nearestPlayerJumpedRecently && builder.nearestPlayerDistanceNorm < 0.25F
                ? 0.85F : 0.0F;
        builder.imitateSneakScore = builder.nearestPlayerSneaking && builder.nearestPlayerDistanceNorm < 0.3F
                ? 0.8F : 0.0F;
        builder.imitateSprintScore = builder.nearestPlayerSprinting && builder.nearestPlayerDistanceNorm < 0.45F
                ? 0.72F : 0.0F;
        builder.fetchInterestScore = !builder.carryingItem && builder.nearestEligibleItemUuid != null
                ? clamp01(1.0F - builder.nearestEligibleItemDistanceNorm + builder.mischief * 0.25F) : 0.0F;
        builder.returnItemUrgency = builder.carryingItem
                ? clamp01(0.35F + builder.trust * 0.4F - builder.mischief * 0.2F) : 0.0F;
        builder.playInterestScore = builder.nearestPlayerVisible && !builder.nearestPlayerHoldingWeapon
                ? clamp01(builder.trust * 0.55F + builder.mischief * 0.45F - builder.fear * 0.4F) : 0.0F;
        builder.trustSafetyScore = clamp01(builder.trust - builder.fear * 0.5F);
        builder.dangerScore = clamp01(builder.threatScore + (builder.lavaNearby ? 0.25F : 0.0F)
                + (builder.cliffNearby ? 0.2F : 0.0F));
        builder.perchEscapeScore = clamp01(builder.escapePathScore * 0.65F + builder.fear * 0.35F);
        builder.annoyanceRiskScore = clamp01((builder.nearestPlayerDistanceNorm < 0.08F ? 0.45F : 0.0F)
                + builder.mischief * 0.3F + builder.fear * 0.2F);
    }

    private static void fillVector(XingsingObservation.Builder builder, Xingsing mob) {
        int i = 0;
        builder.vector[i++] = builder.healthRatio;
        builder.vector[i++] = mob.getAge() / 24000.0F;
        builder.vector[i++] = bool(builder.baby);
        builder.vector[i++] = bool(builder.grounded);
        builder.vector[i++] = bool(builder.inWater);
        builder.vector[i++] = bool(mob.onClimbable());
        builder.vector[i++] = bool(builder.fear > 0.65F);
        for (XingsingOption option : XingsingOption.values()) {
            builder.vector[i++] = mob.getCurrentOption() == option ? 1.0F : 0.0F;
        }
        builder.vector[i++] = clamp01(mob.getOptionElapsedTicks() / 80.0F);
        for (int animation = 0; animation < 8; animation++) {
            builder.vector[i++] = mob.getAnimationState() == animation ? 1.0F : 0.0F;
        }
        builder.vector[i++] = bool(builder.carryingItem);
        builder.vector[i++] = clamp01(mob.getCarriedItemAgeTicks() / 1200.0F);
        builder.vector[i++] = builder.trust;
        builder.vector[i++] = builder.fear;
        builder.vector[i++] = builder.mischief;
        builder.vector[i++] = clamp01(mob.getTicksSinceFed() / 2400.0F);
        builder.vector[i++] = clamp01(mob.getTicksSinceHurtByPlayer() / 2400.0F);
        builder.vector[i++] = builder.nearestPlayerDistanceNorm;
        builder.vector[i++] = bool(builder.nearestPlayerVisible);
        builder.vector[i++] = bool(builder.nearestPlayerSneaking);
        builder.vector[i++] = bool(builder.nearestPlayerSprinting);
        builder.vector[i++] = bool(builder.nearestPlayerJumpedRecently);
        builder.vector[i++] = bool(builder.nearestPlayerDroppedItemRecently);
        builder.vector[i++] = bool(builder.nearestPlayerHoldingFood);
        builder.vector[i++] = bool(builder.nearestPlayerHoldingWeapon);
        builder.vector[i++] = bool(builder.nearestPlayerAttackedRecently);
        builder.vector[i++] = builder.favoritePlayerDistanceNorm;
        builder.vector[i++] = bool(builder.favoritePlayerVisible);
        builder.vector[i++] = builder.nearestEligibleItemDistanceNorm;
        builder.vector[i++] = bool(builder.nearestEligibleItemVisible);
        builder.vector[i++] = bool(builder.nearestEligibleItemOwnerIsFavorite);
        builder.vector[i++] = builder.nearestEligibleItemAgeNorm;
        builder.vector[i++] = builder.safePathToItemScore;
        builder.vector[i++] = builder.safeReturnPathScore;
        builder.vector[i++] = builder.treeCoverScore;
        builder.vector[i++] = bool(builder.bambooNearby);
        builder.vector[i++] = bool(builder.vinesNearby);
        builder.vector[i++] = builder.perchDistanceNorm;
        builder.vector[i++] = bool(builder.perchReachable);
        builder.vector[i++] = bool(builder.lavaNearby);
        builder.vector[i++] = bool(builder.cliffNearby);
        builder.vector[i++] = builder.escapePathScore;
        builder.vector[i++] = builder.dashPathScore;
        builder.vector[i++] = builder.nearestHostileDistanceNorm;
        builder.vector[i++] = bool(builder.nearestHostileVisible);
        builder.vector[i++] = bool(builder.hostileTargetingPlayer);
        builder.vector[i++] = bool(builder.hostileTargetingXingsing);
        builder.vector[i++] = bool(builder.creeperNearby);
        builder.vector[i++] = bool(builder.ownerUnderAttack);
        builder.vector[i++] = builder.threatScore;
        builder.vector[i++] = builder.imitateJumpScore;
        builder.vector[i++] = builder.imitateSneakScore;
        builder.vector[i++] = builder.imitateSprintScore;
        builder.vector[i++] = builder.fetchInterestScore;
        builder.vector[i++] = builder.returnItemUrgency;
        builder.vector[i++] = builder.playInterestScore;
        builder.vector[i++] = builder.trustSafetyScore;
        builder.vector[i++] = builder.dangerScore;
        builder.vector[i++] = builder.perchEscapeScore;
        builder.vector[i++] = builder.annoyanceRiskScore;

        builder.namedDebug.put("trust", builder.trust);
        builder.namedDebug.put("fear", builder.fear);
        builder.namedDebug.put("mischief", builder.mischief);
        builder.namedDebug.put("nearest_player_distance_norm", builder.nearestPlayerDistanceNorm);
        builder.namedDebug.put("nearest_eligible_item_distance_norm", builder.nearestEligibleItemDistanceNorm);
        builder.namedDebug.put("threat_score", builder.threatScore);
        builder.namedDebug.put("fetch_interest_score", builder.fetchInterestScore);
        builder.namedDebug.put("return_item_urgency", builder.returnItemUrgency);
    }

    private static boolean looksLikeWeapon(ItemStack stack) {
        return stack.is(Items.WOODEN_SWORD)
                || stack.is(Items.STONE_SWORD)
                || stack.is(Items.IRON_SWORD)
                || stack.is(Items.GOLDEN_SWORD)
                || stack.is(Items.DIAMOND_SWORD)
                || stack.is(Items.NETHERITE_SWORD)
                || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT);
    }

    private static float threatScore(Xingsing mob, LivingEntity hostile, XingsingObservation.Builder builder) {
        float closeness = 1.0F - distanceNorm(mob.distanceTo(hostile), HOSTILE_SCAN_RANGE);
        float targeting = builder.hostileTargetingXingsing || builder.hostileTargetingPlayer ? 0.3F : 0.0F;
        float creeper = builder.creeperNearby ? 0.2F : 0.0F;
        return clamp01(closeness * 0.65F + targeting + creeper);
    }

    private static float distanceNorm(float distance, double maxDistance) {
        return clamp01((float) (distance / maxDistance));
    }

    private static float bool(boolean value) {
        return value ? 1.0F : 0.0F;
    }

    private static float clamp01(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }
}
