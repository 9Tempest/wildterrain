package dev.lukez.wildterrain.common.entity.ai.xingsing;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.config.WildTerrainConfig;
import dev.lukez.wildterrain.common.entity.Xingsing;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;

public final class XingsingDecisionLogger {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    @Nullable
    private static Boolean recordingOverride;
    @Nullable
    private static BufferedWriter writer;
    @Nullable
    private static String sessionId;

    private XingsingDecisionLogger() {
    }

    public static void setRecordingOverride(@Nullable Boolean enabled) {
        recordingOverride = enabled;
    }

    public static boolean isRecording() {
        return recordingOverride != null ? recordingOverride : WildTerrainConfig.xingsingRecordTrainingData();
    }

    public static void logDecision(Xingsing mob, XingsingObservation obs, boolean[] mask,
                                   XingsingOption teacherAction, XingsingOption policyAction) {
        if (!isRecording() || mob.level().isClientSide) {
            return;
        }
        try {
            ensureWriter();
            if (writer == null) {
                return;
            }
            writer.write(recordJson(mob, obs, mask, teacherAction, policyAction));
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
            WildTerrain.LOGGER.warn("Failed to write Xingsing decision log", ex);
        }
    }

    public static void logCorrection(String playerHash, XingsingOption option, long gameTime) {
        if (!WildTerrainConfig.xingsingRecordHumanCorrections() && !isRecording()) {
            return;
        }
        try {
            ensureWriter();
            if (writer == null) {
                return;
            }
            writer.write("{\"schema_version\":1,\"type\":\"human_correction\",\"tick\":" + gameTime
                    + ",\"player_hash\":\"" + playerHash + "\",\"label\":\"" + option.name() + "\"}");
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
            WildTerrain.LOGGER.warn("Failed to write Xingsing correction log", ex);
        }
    }

    private static void ensureWriter() throws IOException {
        if (writer != null) {
            return;
        }
        String day = LocalDate.now().format(DATE);
        sessionId = "session-" + System.currentTimeMillis();
        Path dir = FMLPaths.GAMEDIR.get().resolve("wildterrain-ai/logs/xingsing").resolve(day);
        Files.createDirectories(dir);
        writer = Files.newBufferedWriter(dir.resolve(sessionId + ".jsonl"), StandardCharsets.UTF_8);
    }

    private static String recordJson(Xingsing mob, XingsingObservation obs, boolean[] mask,
                                     XingsingOption teacherAction, XingsingOption policyAction) {
        StringBuilder json = new StringBuilder(2048);
        json.append('{');
        field(json, "schema_version", "1", false);
        field(json, "repo_mod_version", "\"0.1.x-dev\"", true);
        field(json, "policy_version", "\"teacher_v1\"", true);
        field(json, "episode_id", quote(sessionId == null ? "manual_playtest" : sessionId), true);
        field(json, "tick", Long.toString(obs.gameTime), true);
        ResourceKey<Level> dimension = mob.level().dimension();
        field(json, "dimension", quote(dimension.location().toString()), true);
        field(json, "entity_uuid", quote(mob.getUUID().toString()), true);
        field(json, "entity_type", "\"wildterrain:xingsing\"", true);
        json.append(",\"obs\":[");
        float[] vector = obs.vector();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(String.format(Locale.ROOT, "%.5f", vector[i]));
        }
        json.append(']');
        json.append(",\"obs_named_debug\":{");
        int debugIndex = 0;
        for (Map.Entry<String, Float> entry : obs.namedDebug().entrySet()) {
            if (debugIndex++ > 0) {
                json.append(',');
            }
            json.append(quote(entry.getKey())).append(':')
                    .append(String.format(Locale.ROOT, "%.5f", entry.getValue()));
        }
        json.append('}');
        json.append(",\"action_mask\":[");
        for (int i = 0; i < mask.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(mask[i]);
        }
        json.append(']');
        field(json, "teacher_action", quote(teacherAction.name()), true);
        field(json, "policy_action", quote(policyAction.name()), true);
        field(json, "reward", "0.0", true);
        field(json, "done", "false", true);
        json.append(",\"metadata\":{");
        field(json, "favorite_player_hash", quote(hashUuid(mob.getFavoritePlayer())), false);
        field(json, "carried_item_id", quote(mob.getCarriedItemId()), true);
        json.append("}}");
        return json.toString();
    }

    private static void field(StringBuilder json, String name, String value, boolean comma) {
        if (comma) {
            json.append(',');
        }
        json.append(quote(name)).append(':').append(value);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String hashUuid(@Nullable UUID uuid) {
        if (uuid == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(uuid.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder("sha256:");
            for (byte b : bytes) {
                out.append(String.format(Locale.ROOT, "%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "sha256-unavailable";
        }
    }
}
