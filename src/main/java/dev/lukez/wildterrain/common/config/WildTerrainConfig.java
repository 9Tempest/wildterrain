package dev.lukez.wildterrain.common.config;

import java.util.Locale;
import net.minecraftforge.common.ForgeConfigSpec;

public final class WildTerrainConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> XINGSING_AI_MODE;
    private static final ForgeConfigSpec.BooleanValue XINGSING_RECORD_TRAINING_DATA;
    private static final ForgeConfigSpec.BooleanValue XINGSING_RECORD_HUMAN_CORRECTIONS;
    private static final ForgeConfigSpec.IntValue XINGSING_MAX_LOGGED_ENTITIES;
    private static final ForgeConfigSpec.IntValue XINGSING_DECISION_INTERVAL_TICKS;
    private static final ForgeConfigSpec.IntValue XINGSING_WARN_SOUND_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.BooleanValue XINGSING_ALLOW_FETCH_DEATH_DROPS;
    private static final ForgeConfigSpec.BooleanValue XINGSING_ALLOW_MODEL_INFERENCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("xingsing");
        XINGSING_AI_MODE = builder
                .comment("disabled, teacher, or model. Model mode always falls back to teacher when no safe policy is loaded.")
                .define("aiMode", "model");
        XINGSING_RECORD_TRAINING_DATA = builder
                .comment("Writes privacy-safe local JSONL decision records under run/wildterrain-ai when enabled.")
                .define("recordTrainingData", false);
        XINGSING_RECORD_HUMAN_CORRECTIONS = builder
                .comment("Allows /wt_ai xingsing label to append local correction labels.")
                .define("recordHumanCorrections", false);
        XINGSING_MAX_LOGGED_ENTITIES = builder
                .comment("Maximum Xingsing entities that may record decisions per server level.")
                .defineInRange("maxLoggedEntities", 16, 1, 128);
        XINGSING_DECISION_INTERVAL_TICKS = builder
                .comment("How often the option policy chooses a high-level behavior.")
                .defineInRange("decisionIntervalTicks", 10, 4, 60);
        XINGSING_WARN_SOUND_COOLDOWN_TICKS = builder
                .comment("Minimum ticks between warning chirps per Xingsing.")
                .defineInRange("warnSoundCooldownTicks", 80, 20, 400);
        XINGSING_ALLOW_FETCH_DEATH_DROPS = builder
                .comment("If false, Xingsing only fetches items recently tossed by a player.")
                .define("allowFetchDeathDrops", false);
        XINGSING_ALLOW_MODEL_INFERENCE = builder
                .comment("Extra safety gate for loading exported model weights in aiMode=model.")
                .define("allowModelInference", true);
        builder.pop();
        SPEC = builder.build();
    }

    private WildTerrainConfig() {
    }

    public static XingsingAiMode xingsingAiMode() {
        try {
            return XingsingAiMode.valueOf(XINGSING_AI_MODE.get().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return XingsingAiMode.TEACHER;
        }
    }

    public static boolean setXingsingAiMode(String value) {
        try {
            XingsingAiMode mode = XingsingAiMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            XINGSING_AI_MODE.set(mode.name().toLowerCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean xingsingRecordTrainingData() {
        return XINGSING_RECORD_TRAINING_DATA.get();
    }

    public static boolean xingsingRecordHumanCorrections() {
        return XINGSING_RECORD_HUMAN_CORRECTIONS.get();
    }

    public static int xingsingMaxLoggedEntities() {
        return XINGSING_MAX_LOGGED_ENTITIES.get();
    }

    public static int xingsingDecisionIntervalTicks() {
        return XINGSING_DECISION_INTERVAL_TICKS.get();
    }

    public static int xingsingWarnSoundCooldownTicks() {
        return XINGSING_WARN_SOUND_COOLDOWN_TICKS.get();
    }

    public static boolean xingsingAllowFetchDeathDrops() {
        return XINGSING_ALLOW_FETCH_DEATH_DROPS.get();
    }

    public static boolean xingsingAllowModelInference() {
        return XINGSING_ALLOW_MODEL_INFERENCE.get();
    }

    public static void setXingsingAllowModelInference(boolean enabled) {
        XINGSING_ALLOW_MODEL_INFERENCE.set(enabled);
    }

    public enum XingsingAiMode {
        DISABLED,
        TEACHER,
        MODEL
    }
}
