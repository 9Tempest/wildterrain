package dev.lukez.wildterrain.common.entity.ai.xingsing;

public final class XingsingRuleTeacher {
    public XingsingOption select(XingsingObservation obs, boolean[] mask) {
        if (obs.healthRatio < 0.35F && allowed(mask, XingsingOption.FLEE_TO_TREE)) {
            return XingsingOption.FLEE_TO_TREE;
        }
        if (obs.threatScore > 0.65F && allowed(mask, XingsingOption.WARN_HOSTILE)) {
            return XingsingOption.WARN_HOSTILE;
        }
        if (obs.carryingItem && obs.returnItemUrgency > 0.35F && allowed(mask, XingsingOption.RETURN_ITEM)) {
            return XingsingOption.RETURN_ITEM;
        }
        if (obs.fetchInterestScore > 0.7F && allowed(mask, XingsingOption.PICKUP_ITEM)) {
            return XingsingOption.PICKUP_ITEM;
        }
        if (obs.imitateJumpScore > 0.6F && allowed(mask, XingsingOption.MIRROR_JUMP)) {
            return XingsingOption.MIRROR_JUMP;
        }
        if (obs.imitateSneakScore > 0.6F && allowed(mask, XingsingOption.MIRROR_SNEAK)) {
            return XingsingOption.MIRROR_SNEAK;
        }
        if (obs.imitateSprintScore > 0.55F && obs.trust > 0.35F && allowed(mask, XingsingOption.MIRROR_SPRINT)) {
            return XingsingOption.MIRROR_SPRINT;
        }
        if (obs.playInterestScore > 0.65F && obs.trust > 0.45F && allowed(mask, XingsingOption.PLAY_CHASE)) {
            return XingsingOption.PLAY_CHASE;
        }
        if (obs.perchEscapeScore > 0.55F && obs.mischief > 0.55F && allowed(mask, XingsingOption.CLIMB_TO_PERCH)) {
            return XingsingOption.CLIMB_TO_PERCH;
        }
        if (obs.nearestPlayerVisible && obs.trust > 0.25F && allowed(mask, XingsingOption.OBSERVE_PLAYER)) {
            return XingsingOption.OBSERVE_PLAYER;
        }
        return XingsingOption.IDLE_GROOM;
    }

    private static boolean allowed(boolean[] mask, XingsingOption option) {
        return option.id() >= 0 && option.id() < mask.length && mask[option.id()];
    }
}
