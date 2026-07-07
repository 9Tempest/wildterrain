package dev.lukez.wildterrain.common.entity.ai.xingsing;

import dev.lukez.wildterrain.common.config.WildTerrainConfig;
import dev.lukez.wildterrain.common.entity.Xingsing;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class XingsingActionAdapter {
    private static final int DEFAULT_OPTION_TIMEOUT = 70;
    private UUID targetEntity;
    private XingsingOption currentOption = XingsingOption.IDLE_GROOM;
    private int optionTicks;

    public boolean canStart(Xingsing mob, XingsingObservation obs, XingsingOption option) {
        return switch (option) {
            case PICKUP_ITEM -> !mob.hasCarriedItem() && obs.nearestEligibleItemUuid != null;
            case RETURN_ITEM -> mob.hasCarriedItem();
            case MIRROR_JUMP -> mob.onGround() && obs.nearestPlayerJumpedRecently;
            case WARN_HOSTILE -> obs.nearestHostileUuid != null && mob.getWarningCooldown() <= 0;
            case FLEE_TO_TREE -> obs.escapePathScore > 0.25F;
            case LEAD_TO_FRUIT -> false;
            default -> true;
        };
    }

    public void start(Xingsing mob, XingsingObservation obs, XingsingOption option) {
        if (!canStart(mob, obs, option)) {
            option = XingsingOption.IDLE_GROOM;
        }
        currentOption = option;
        optionTicks = DEFAULT_OPTION_TIMEOUT;
        targetEntity = selectTarget(obs, option);
        mob.setCurrentOption(option);

        switch (option) {
            case OBSERVE_PLAYER, APPROACH_PLAYER -> mob.startAnimation(Xingsing.ANIMATION_OBSERVE, 34);
            case MIRROR_JUMP -> startMirrorJump(mob);
            case MIRROR_SNEAK -> mob.startAnimation(Xingsing.ANIMATION_MIMIC_SNEAK, 44);
            case PICKUP_ITEM -> mob.startAnimation(Xingsing.ANIMATION_FETCH, 48);
            case RETURN_ITEM -> mob.startAnimation(Xingsing.ANIMATION_RETURN_ITEM, 48);
            case WARN_HOSTILE -> startWarning(mob);
            case PLAY_CHASE -> mob.startAnimation(Xingsing.ANIMATION_PLAY, 50);
            case FLEE_TO_TREE, CLIMB_TO_PERCH -> mob.startAnimation(Xingsing.ANIMATION_PLAY, 40);
            default -> mob.startAnimation(Xingsing.ANIMATION_OBSERVE, 24);
        }
    }

    public void tick(Xingsing mob) {
        optionTicks--;
        if (optionTicks <= 0) {
            stop(mob);
            return;
        }

        switch (currentOption) {
            case OBSERVE_PLAYER -> observePlayer(mob);
            case APPROACH_PLAYER -> approachPlayer(mob);
            case KEEP_PLAY_DISTANCE -> keepPlayDistance(mob);
            case MIRROR_SPRINT, PLAY_CHASE -> playDash(mob);
            case PICKUP_ITEM -> fetchItem(mob);
            case RETURN_ITEM -> returnItem(mob);
            case WARN_HOSTILE -> warnHostile(mob);
            case FLEE_TO_TREE -> fleeToTree(mob);
            case CLIMB_TO_PERCH -> climbToPerch(mob);
            case MIRROR_SNEAK -> sneakMimic(mob);
            default -> idle(mob);
        }
    }

    public void stop(Xingsing mob) {
        mob.getNavigation().stop();
        currentOption = XingsingOption.IDLE_GROOM;
        targetEntity = null;
        mob.setCurrentOption(XingsingOption.IDLE_GROOM);
    }

    private void observePlayer(Xingsing mob) {
        Entity target = resolveTarget(mob);
        if (target == null) {
            idle(mob);
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = mob.distanceToSqr(target);
        if (distance < 16.0D) {
            mob.getNavigation().stop();
        } else if (distance > 49.0D) {
            mob.getNavigation().moveTo(target, 1.05D);
        }
    }

    private void approachPlayer(Xingsing mob) {
        Entity target = resolveTarget(mob);
        if (target == null) {
            idle(mob);
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (mob.distanceToSqr(target) > 10.0D) {
            mob.getNavigation().moveTo(target, 1.18D);
        } else {
            mob.getNavigation().stop();
        }
    }

    private void keepPlayDistance(Xingsing mob) {
        Entity target = resolveTarget(mob);
        if (target == null) {
            idle(mob);
            return;
        }
        Vec3 away = mob.position().subtract(target.position()).normalize();
        Vec3 destination = mob.position().add(away.scale(4.0D));
        mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.2D);
        mob.getLookControl().setLookAt(target, 25.0F, 25.0F);
    }

    private void playDash(Xingsing mob) {
        Entity target = resolveTarget(mob);
        if (target == null) {
            idle(mob);
            return;
        }
        if (mob.distanceToSqr(target) > 20.0D) {
            mob.getNavigation().moveTo(target, 1.35D);
        } else {
            Vec3 sideways = mob.position().subtract(target.position()).cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
            if (sideways.lengthSqr() < 0.01D) {
                sideways = mob.getLookAngle();
            }
            Vec3 destination = mob.position().add(sideways.scale(3.0D));
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.35D);
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void fetchItem(Xingsing mob) {
        ItemEntity item = resolveItemTarget(mob);
        if (item == null || !DroppedItemOwnershipTracker.isEligible(item, mob, mob.level().getGameTime())
                || !DroppedItemOwnershipTracker.reserve(item, mob)) {
            stop(mob);
            return;
        }
        mob.getLookControl().setLookAt(item, 30.0F, 30.0F);
        if (mob.distanceToSqr(item) > 2.2D) {
            mob.getNavigation().moveTo(item, 1.28D);
            return;
        }

        ItemStack fetched = item.getItem().copy();
        UUID owner = DroppedItemOwnershipTracker.ownerOf(item);
        mob.setCarriedItem(fetched, owner);
        DroppedItemOwnershipTracker.remove(item);
        item.discard();
        mob.startAnimation(Xingsing.ANIMATION_RETURN_ITEM, 50);
        currentOption = XingsingOption.RETURN_ITEM;
        mob.setCurrentOption(XingsingOption.RETURN_ITEM);
        optionTicks = DEFAULT_OPTION_TIMEOUT;
    }

    private void returnItem(Xingsing mob) {
        if (!mob.hasCarriedItem()) {
            stop(mob);
            return;
        }
        Player owner = findCarriedItemOwner(mob);
        if (owner == null) {
            if (mob.getCarriedItemAgeTicks() > 20 * 20) {
                mob.dropCarriedItem(null);
                stop(mob);
            }
            return;
        }
        mob.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        if (mob.distanceToSqr(owner) > 5.0D) {
            mob.getNavigation().moveTo(owner, mob.getMischief() > 0.65F && optionTicks > 35 ? 0.95D : 1.25D);
            return;
        }
        mob.dropCarriedItem(owner);
        mob.rewardTrust(0.08F, -0.04F);
        stop(mob);
    }

    private void warnHostile(Xingsing mob) {
        Entity target = resolveTarget(mob);
        if (target == null) {
            stop(mob);
            return;
        }
        mob.getLookControl().setLookAt(target, 45.0F, 45.0F);
        if (mob.tickCount % 12 == 0 && mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY(1.0D), mob.getZ(),
                    2, 0.25D, 0.2D, 0.25D, 0.02D);
        }
        Vec3 away = mob.position().subtract(target.position()).normalize();
        Vec3 destination = mob.position().add(away.scale(5.0D));
        mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.28D);
    }

    private void fleeToTree(Xingsing mob) {
        Optional<BlockPos> perch = findPerch(mob, 9);
        if (perch.isPresent()) {
            BlockPos pos = perch.get();
            mob.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 1.35D);
            return;
        }
        Entity target = resolveTarget(mob);
        if (target != null) {
            Vec3 away = mob.position().subtract(target.position()).normalize();
            Vec3 destination = mob.position().add(away.scale(7.0D));
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.35D);
        }
    }

    private void climbToPerch(Xingsing mob) {
        Optional<BlockPos> perch = findPerch(mob, 7);
        if (perch.isPresent()) {
            BlockPos pos = perch.get();
            mob.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 1.18D);
        } else {
            idle(mob);
        }
    }

    private void sneakMimic(Xingsing mob) {
        Entity target = resolveTarget(mob);
        if (target != null) {
            mob.getLookControl().setLookAt(target, 25.0F, 25.0F);
        }
        mob.getNavigation().stop();
    }

    private void idle(Xingsing mob) {
        if (mob.getRandom().nextInt(40) == 0) {
            mob.startAnimation(Xingsing.ANIMATION_OBSERVE, 26);
        }
    }

    private void startMirrorJump(Xingsing mob) {
        mob.startAnimation(Xingsing.ANIMATION_MIMIC_JUMP, 28);
        if (mob.onGround()) {
            mob.getJumpControl().jump();
            mob.playSound(SoundEvents.FOX_AMBIENT, 0.35F, 1.55F + mob.getRandom().nextFloat() * 0.2F);
        }
    }

    private void startWarning(Xingsing mob) {
        mob.startAnimation(Xingsing.ANIMATION_WARN, 54);
        if (mob.getWarningCooldown() <= 0) {
            mob.playSound(SoundEvents.PARROT_IMITATE_CREEPER, 0.85F, 1.25F);
            mob.setWarningCooldown(WildTerrainConfig.xingsingWarnSoundCooldownTicks());
        }
    }

    @Nullable
    private UUID selectTarget(XingsingObservation obs, XingsingOption option) {
        return switch (option) {
            case PICKUP_ITEM -> obs.nearestEligibleItemUuid;
            case WARN_HOSTILE, FLEE_TO_TREE -> obs.nearestHostileUuid;
            case RETURN_ITEM, OBSERVE_PLAYER, APPROACH_PLAYER, KEEP_PLAY_DISTANCE,
                    MIRROR_SPRINT, PLAY_CHASE, MIRROR_SNEAK -> obs.favoritePlayerUuid != null
                    ? obs.favoritePlayerUuid : obs.nearestPlayerUuid;
            default -> obs.nearestPlayerUuid;
        };
    }

    @Nullable
    private Entity resolveTarget(Xingsing mob) {
        if (targetEntity == null || !(mob.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(targetEntity);
    }

    @Nullable
    private ItemEntity resolveItemTarget(Xingsing mob) {
        Entity entity = resolveTarget(mob);
        return entity instanceof ItemEntity item ? item : null;
    }

    @Nullable
    private Player findCarriedItemOwner(Xingsing mob) {
        UUID owner = mob.getCarriedItemOwner();
        if (!(mob.level() instanceof ServerLevel serverLevel) || owner == null) {
            return mob.level().getNearestPlayer(mob, 16.0D);
        }
        Entity entity = serverLevel.getEntity(owner);
        return entity instanceof Player player ? player : mob.level().getNearestPlayer(mob, 16.0D);
    }

    private Optional<BlockPos> findPerch(Xingsing mob, int radius) {
        Level level = mob.level();
        BlockPos origin = mob.blockPosition();
        return BlockPos.betweenClosedStream(origin.offset(-radius, -1, -radius), origin.offset(radius, 8, radius))
                .filter(pos -> isPerch(level, pos))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
    }

    private boolean isPerch(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        return (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock
                || state.is(Blocks.BAMBOO))
                && above.getCollisionShape(level, pos.above()).isEmpty();
    }
}
