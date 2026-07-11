package dev.lukez.wildterrain.common.entity.ai.xingsing.collect;

import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingObservation;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public enum XingsingCollectionScenario {
    COVERAGE("coverage", null),
    SETTLE_NEAR_PLAYER("settle_near_player", XingsingOption.OBSERVE_PLAYER),
    MIMIC_JUMP("mimic_jump", XingsingOption.MIRROR_JUMP),
    MIMIC_SNEAK("mimic_sneak", XingsingOption.MIRROR_SNEAK),
    MIMIC_SPRINT("mimic_sprint", XingsingOption.MIRROR_SPRINT),
    FETCH_ITEM("fetch_item", XingsingOption.PICKUP_ITEM),
    RETURN_ITEM("return_item", XingsingOption.RETURN_ITEM),
    HOSTILE_WARNING("hostile_warning", XingsingOption.WARN_HOSTILE),
    FLEE_TO_TREE("flee_to_tree", XingsingOption.FLEE_TO_TREE),
    CLIMB_TO_PERCH("climb_to_perch", XingsingOption.CLIMB_TO_PERCH),
    PLAY_CHASE("play_chase", XingsingOption.PLAY_CHASE);

    private static final XingsingCollectionScenario[] COVERAGE_SEQUENCE = Arrays.stream(values())
            .filter(scenario -> scenario != COVERAGE)
            .toArray(XingsingCollectionScenario[]::new);

    private final String id;
    private final XingsingOption targetOption;

    XingsingCollectionScenario(String id, XingsingOption targetOption) {
        this.id = id;
        this.targetOption = targetOption;
    }

    public String id() {
        return id;
    }

    public XingsingOption targetOption() {
        return targetOption;
    }

    public boolean isCoverage() {
        return this == COVERAGE;
    }

    public float reward(XingsingObservation obs, XingsingOption teacherAction, XingsingOption policyAction) {
        if (this == COVERAGE) {
            return 0.0F;
        }
        float reward = 0.0F;
        if (targetOption != null && policyAction == targetOption) {
            reward += 1.0F;
        }
        if (targetOption != null && teacherAction == targetOption) {
            reward += 0.25F;
        }
        reward += switch (this) {
            case SETTLE_NEAR_PLAYER -> obs.nearestPlayerVisible
                    && (policyAction == XingsingOption.OBSERVE_PLAYER || policyAction == XingsingOption.IDLE_GROOM)
                    ? 0.5F : 0.0F;
            case FETCH_ITEM -> obs.fetchInterestScore > 0.65F || obs.carryingItem ? 0.35F : 0.0F;
            case RETURN_ITEM -> obs.returnItemUrgency > 0.35F ? 0.35F : 0.0F;
            case HOSTILE_WARNING -> obs.threatScore > 0.5F ? 0.35F : 0.0F;
            case FLEE_TO_TREE -> obs.dangerScore > 0.45F && obs.escapePathScore > 0.25F ? 0.35F : 0.0F;
            case CLIMB_TO_PERCH -> obs.perchReachable ? 0.35F : 0.0F;
            case PLAY_CHASE -> obs.playInterestScore > 0.6F ? 0.35F : 0.0F;
            case MIMIC_JUMP -> obs.imitateJumpScore > 0.6F ? 0.35F : 0.0F;
            case MIMIC_SNEAK -> obs.imitateSneakScore > 0.6F ? 0.35F : 0.0F;
            case MIMIC_SPRINT -> obs.imitateSprintScore > 0.55F ? 0.35F : 0.0F;
            case COVERAGE -> 0.0F;
        };
        return reward;
    }

    public static XingsingCollectionScenario coverageScenario(int episodeIndex) {
        return COVERAGE_SEQUENCE[Math.floorMod(episodeIndex, COVERAGE_SEQUENCE.length)];
    }

    public static XingsingCollectionScenario byName(String rawName) {
        String name = rawName.trim().toLowerCase(Locale.ROOT);
        return Stream.of(values())
                .filter(scenario -> scenario.id.equals(name))
                .findFirst()
                .orElse(null);
    }

    public static String[] ids() {
        return Stream.of(values()).map(XingsingCollectionScenario::id).toArray(String[]::new);
    }
}
