package dev.lukez.wildterrain.common.entity.ai.xingsing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

public final class XingsingObservation {
    public static final int SPEC_VERSION = 1;
    public static final int VECTOR_SIZE = 110;

    private final float[] vector;
    private final Map<String, Float> namedDebug;

    public final long gameTime;
    public final float healthRatio;
    public final boolean baby;
    public final boolean grounded;
    public final boolean inWater;
    public final boolean carryingItem;
    public final float trust;
    public final float fear;
    public final float mischief;
    public final boolean nearestPlayerVisible;
    public final float nearestPlayerDistanceNorm;
    public final boolean nearestPlayerSneaking;
    public final boolean nearestPlayerSprinting;
    public final boolean nearestPlayerJumpedRecently;
    public final boolean nearestPlayerDroppedItemRecently;
    public final boolean nearestPlayerHoldingFood;
    public final boolean nearestPlayerHoldingWeapon;
    public final boolean nearestPlayerAttackedRecently;
    public final boolean favoritePlayerVisible;
    public final float favoritePlayerDistanceNorm;
    public final boolean nearestEligibleItemVisible;
    public final float nearestEligibleItemDistanceNorm;
    public final boolean nearestEligibleItemOwnerIsFavorite;
    public final float nearestEligibleItemAgeNorm;
    public final float safePathToItemScore;
    public final float safeReturnPathScore;
    public final float treeCoverScore;
    public final boolean bambooNearby;
    public final boolean vinesNearby;
    public final boolean perchReachable;
    public final float perchDistanceNorm;
    public final boolean lavaNearby;
    public final boolean cliffNearby;
    public final float escapePathScore;
    public final float dashPathScore;
    public final boolean nearestHostileVisible;
    public final float nearestHostileDistanceNorm;
    public final boolean hostileTargetingPlayer;
    public final boolean hostileTargetingXingsing;
    public final boolean creeperNearby;
    public final boolean ownerUnderAttack;
    public final float threatScore;
    public final float imitateJumpScore;
    public final float imitateSneakScore;
    public final float imitateSprintScore;
    public final float fetchInterestScore;
    public final float returnItemUrgency;
    public final float playInterestScore;
    public final float trustSafetyScore;
    public final float dangerScore;
    public final float perchEscapeScore;
    public final float annoyanceRiskScore;
    @Nullable
    public final UUID nearestPlayerUuid;
    @Nullable
    public final UUID favoritePlayerUuid;
    @Nullable
    public final UUID nearestEligibleItemUuid;
    @Nullable
    public final UUID nearestHostileUuid;

    XingsingObservation(Builder builder) {
        vector = builder.vector;
        namedDebug = builder.namedDebug;
        gameTime = builder.gameTime;
        healthRatio = builder.healthRatio;
        baby = builder.baby;
        grounded = builder.grounded;
        inWater = builder.inWater;
        carryingItem = builder.carryingItem;
        trust = builder.trust;
        fear = builder.fear;
        mischief = builder.mischief;
        nearestPlayerVisible = builder.nearestPlayerVisible;
        nearestPlayerDistanceNorm = builder.nearestPlayerDistanceNorm;
        nearestPlayerSneaking = builder.nearestPlayerSneaking;
        nearestPlayerSprinting = builder.nearestPlayerSprinting;
        nearestPlayerJumpedRecently = builder.nearestPlayerJumpedRecently;
        nearestPlayerDroppedItemRecently = builder.nearestPlayerDroppedItemRecently;
        nearestPlayerHoldingFood = builder.nearestPlayerHoldingFood;
        nearestPlayerHoldingWeapon = builder.nearestPlayerHoldingWeapon;
        nearestPlayerAttackedRecently = builder.nearestPlayerAttackedRecently;
        favoritePlayerVisible = builder.favoritePlayerVisible;
        favoritePlayerDistanceNorm = builder.favoritePlayerDistanceNorm;
        nearestEligibleItemVisible = builder.nearestEligibleItemVisible;
        nearestEligibleItemDistanceNorm = builder.nearestEligibleItemDistanceNorm;
        nearestEligibleItemOwnerIsFavorite = builder.nearestEligibleItemOwnerIsFavorite;
        nearestEligibleItemAgeNorm = builder.nearestEligibleItemAgeNorm;
        safePathToItemScore = builder.safePathToItemScore;
        safeReturnPathScore = builder.safeReturnPathScore;
        treeCoverScore = builder.treeCoverScore;
        bambooNearby = builder.bambooNearby;
        vinesNearby = builder.vinesNearby;
        perchReachable = builder.perchReachable;
        perchDistanceNorm = builder.perchDistanceNorm;
        lavaNearby = builder.lavaNearby;
        cliffNearby = builder.cliffNearby;
        escapePathScore = builder.escapePathScore;
        dashPathScore = builder.dashPathScore;
        nearestHostileVisible = builder.nearestHostileVisible;
        nearestHostileDistanceNorm = builder.nearestHostileDistanceNorm;
        hostileTargetingPlayer = builder.hostileTargetingPlayer;
        hostileTargetingXingsing = builder.hostileTargetingXingsing;
        creeperNearby = builder.creeperNearby;
        ownerUnderAttack = builder.ownerUnderAttack;
        threatScore = builder.threatScore;
        imitateJumpScore = builder.imitateJumpScore;
        imitateSneakScore = builder.imitateSneakScore;
        imitateSprintScore = builder.imitateSprintScore;
        fetchInterestScore = builder.fetchInterestScore;
        returnItemUrgency = builder.returnItemUrgency;
        playInterestScore = builder.playInterestScore;
        trustSafetyScore = builder.trustSafetyScore;
        dangerScore = builder.dangerScore;
        perchEscapeScore = builder.perchEscapeScore;
        annoyanceRiskScore = builder.annoyanceRiskScore;
        nearestPlayerUuid = builder.nearestPlayerUuid;
        favoritePlayerUuid = builder.favoritePlayerUuid;
        nearestEligibleItemUuid = builder.nearestEligibleItemUuid;
        nearestHostileUuid = builder.nearestHostileUuid;
    }

    public float[] vector() {
        return vector;
    }

    public Map<String, Float> namedDebug() {
        return namedDebug;
    }

    public static final class Builder {
        final float[] vector = new float[VECTOR_SIZE];
        final Map<String, Float> namedDebug = new LinkedHashMap<>();
        long gameTime;
        float healthRatio;
        boolean baby;
        boolean grounded;
        boolean inWater;
        boolean carryingItem;
        float trust;
        float fear;
        float mischief;
        boolean nearestPlayerVisible;
        float nearestPlayerDistanceNorm = 1.0F;
        boolean nearestPlayerSneaking;
        boolean nearestPlayerSprinting;
        boolean nearestPlayerJumpedRecently;
        boolean nearestPlayerDroppedItemRecently;
        boolean nearestPlayerHoldingFood;
        boolean nearestPlayerHoldingWeapon;
        boolean nearestPlayerAttackedRecently;
        boolean favoritePlayerVisible;
        float favoritePlayerDistanceNorm = 1.0F;
        boolean nearestEligibleItemVisible;
        float nearestEligibleItemDistanceNorm = 1.0F;
        boolean nearestEligibleItemOwnerIsFavorite;
        float nearestEligibleItemAgeNorm = 1.0F;
        float safePathToItemScore;
        float safeReturnPathScore;
        float treeCoverScore;
        boolean bambooNearby;
        boolean vinesNearby;
        boolean perchReachable;
        float perchDistanceNorm = 1.0F;
        boolean lavaNearby;
        boolean cliffNearby;
        float escapePathScore;
        float dashPathScore;
        boolean nearestHostileVisible;
        float nearestHostileDistanceNorm = 1.0F;
        boolean hostileTargetingPlayer;
        boolean hostileTargetingXingsing;
        boolean creeperNearby;
        boolean ownerUnderAttack;
        float threatScore;
        float imitateJumpScore;
        float imitateSneakScore;
        float imitateSprintScore;
        float fetchInterestScore;
        float returnItemUrgency;
        float playInterestScore;
        float trustSafetyScore;
        float dangerScore;
        float perchEscapeScore;
        float annoyanceRiskScore;
        @Nullable UUID nearestPlayerUuid;
        @Nullable UUID favoritePlayerUuid;
        @Nullable UUID nearestEligibleItemUuid;
        @Nullable UUID nearestHostileUuid;

        public XingsingObservation build() {
            return new XingsingObservation(this);
        }
    }
}
