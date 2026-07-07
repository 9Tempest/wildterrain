package dev.lukez.wildterrain.common.entity.ai.xingsing;

import dev.lukez.wildterrain.common.entity.Xingsing;

public final class XingsingActionMaskBuilder {
    public boolean[] build(Xingsing mob, XingsingObservation obs) {
        boolean[] mask = new boolean[XingsingOption.COUNT];
        boolean immediateDanger = Xingsing.hasImmediateDanger(obs);
        boolean socialMovementAllowed = !mob.isSettlingIn() || immediateDanger;
        allow(mask, XingsingOption.IDLE_GROOM);
        allow(mask, XingsingOption.OBSERVE_PLAYER, obs.nearestPlayerVisible);
        allow(mask, XingsingOption.APPROACH_PLAYER, obs.nearestPlayerVisible && obs.nearestPlayerDistanceNorm > 0.12F);
        allow(mask, XingsingOption.KEEP_PLAY_DISTANCE, socialMovementAllowed
                && obs.nearestPlayerVisible && obs.nearestPlayerDistanceNorm < 0.28F);
        allow(mask, XingsingOption.MIRROR_JUMP, obs.nearestPlayerJumpedRecently && mob.onGround() && !obs.lavaNearby);
        allow(mask, XingsingOption.MIRROR_SNEAK, obs.nearestPlayerSneaking && obs.trust > 0.2F);
        allow(mask, XingsingOption.MIRROR_SPRINT, socialMovementAllowed
                && obs.nearestPlayerSprinting && obs.dashPathScore > 0.45F);
        allow(mask, XingsingOption.PICKUP_ITEM, !obs.carryingItem && obs.nearestEligibleItemUuid != null
                && obs.safePathToItemScore > 0.4F);
        allow(mask, XingsingOption.RETURN_ITEM, obs.carryingItem && obs.safeReturnPathScore > 0.3F);
        allow(mask, XingsingOption.PLAY_CHASE, socialMovementAllowed
                && obs.trust > 0.35F && obs.playInterestScore > 0.35F && obs.dashPathScore > 0.45F);
        allow(mask, XingsingOption.CLIMB_TO_PERCH, socialMovementAllowed
                && obs.perchReachable && obs.perchEscapeScore > 0.25F);
        allow(mask, XingsingOption.WARN_HOSTILE, obs.nearestHostileUuid != null && obs.threatScore > 0.25F);
        allow(mask, XingsingOption.FLEE_TO_TREE, obs.escapePathScore > 0.25F && immediateDanger);
        mask[XingsingOption.LEAD_TO_FRUIT.id()] = false;
        return mask;
    }

    private static void allow(boolean[] mask, XingsingOption option) {
        allow(mask, option, true);
    }

    private static void allow(boolean[] mask, XingsingOption option, boolean condition) {
        mask[option.id()] = condition;
    }
}
