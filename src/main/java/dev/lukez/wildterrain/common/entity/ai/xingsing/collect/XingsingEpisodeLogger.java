package dev.lukez.wildterrain.common.entity.ai.xingsing.collect;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.entity.Xingsing;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingObservation;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingOption;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraftforge.fml.loading.FMLPaths;

public final class XingsingEpisodeLogger implements AutoCloseable {
    private final String runId;
    private final Path runDir;
    private final Path manifestPath;
    private final Path episodesDir;
    private final XingsingCollectionScenario requestedScenario;
    private final String mode;
    private final int requestedEpisodes;
    private final int ticksPerEpisode;
    private final String playerHash;
    private final long startedAtMs;
    private int transitionsWritten;

    private XingsingEpisodeLogger(String runId, Path runDir, XingsingCollectionScenario requestedScenario,
                                  String mode, int requestedEpisodes, int ticksPerEpisode, String playerHash) {
        this.runId = runId;
        this.runDir = runDir;
        this.manifestPath = runDir.resolve("manifest.json");
        this.episodesDir = runDir.resolve("episodes");
        this.requestedScenario = requestedScenario;
        this.mode = mode;
        this.requestedEpisodes = requestedEpisodes;
        this.ticksPerEpisode = ticksPerEpisode;
        this.playerHash = playerHash;
        this.startedAtMs = System.currentTimeMillis();
    }

    public static XingsingEpisodeLogger open(XingsingCollectionScenario requestedScenario, String mode,
                                             int requestedEpisodes, int ticksPerEpisode, UUID playerUuid)
            throws IOException {
        String runId = "run-" + System.currentTimeMillis();
        Path runDir = FMLPaths.GAMEDIR.get().resolve("wildterrain-ai/runs/xingsing").resolve(runId);
        XingsingEpisodeLogger logger = new XingsingEpisodeLogger(runId, runDir, requestedScenario, mode,
                requestedEpisodes, ticksPerEpisode, hashUuid(playerUuid));
        Files.createDirectories(logger.episodesDir);
        logger.writeManifest(0, "running");
        return logger;
    }

    public BufferedWriter openEpisode(String episodeId) throws IOException {
        return Files.newBufferedWriter(episodesDir.resolve(episodeId + ".jsonl"), StandardCharsets.UTF_8);
    }

    public void writeTransition(BufferedWriter writer, DecisionSample sample,
                                @Nullable XingsingObservation nextObservation,
                                @Nullable boolean[] nextMask, boolean done) throws IOException {
        writer.write(transitionJson(sample, nextObservation, nextMask, done));
        writer.newLine();
        writer.flush();
        transitionsWritten++;
    }

    public void writeManifest(int episodesCompleted, String status) {
        try {
            Files.createDirectories(runDir);
            Files.writeString(manifestPath, manifestJson(episodesCompleted, status), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            WildTerrain.LOGGER.warn("Failed to write Xingsing collection manifest", ex);
        }
    }

    public int transitionsWritten() {
        return transitionsWritten;
    }

    public String runId() {
        return runId;
    }

    public Path runDir() {
        return runDir;
    }

    @Override
    public void close() {
    }

    private String manifestJson(int episodesCompleted, String status) {
        StringBuilder json = new StringBuilder(1024);
        json.append('{');
        field(json, "schema_version", "2", false);
        field(json, "type", quote("xingsing_collection_manifest"), true);
        field(json, "run_id", quote(runId), true);
        field(json, "entity_type", quote("wildterrain:xingsing"), true);
        field(json, "obs_spec_version", Integer.toString(XingsingObservation.SPEC_VERSION), true);
        field(json, "vector_size", Integer.toString(XingsingObservation.VECTOR_SIZE), true);
        field(json, "num_actions", Integer.toString(XingsingOption.COUNT), true);
        field(json, "started_at_epoch_ms", Long.toString(startedAtMs), true);
        field(json, "updated_at", quote(Instant.now().toString()), true);
        field(json, "requested_scenario", quote(requestedScenario.id()), true);
        field(json, "mode", quote(mode), true);
        field(json, "episodes_requested", Integer.toString(requestedEpisodes), true);
        field(json, "ticks_per_episode", Integer.toString(ticksPerEpisode), true);
        field(json, "episodes_completed", Integer.toString(episodesCompleted), true);
        field(json, "transitions_written", Integer.toString(transitionsWritten), true);
        field(json, "status", quote(status), true);
        field(json, "run_dir", quote(runDir.toString()), true);
        field(json, "player_hash", quote(playerHash), true);
        json.append(",\"available_scenarios\":[");
        String[] scenarios = XingsingCollectionScenario.ids();
        for (int i = 0; i < scenarios.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(quote(scenarios[i]));
        }
        json.append("]}");
        return json.toString();
    }

    private String transitionJson(DecisionSample sample, @Nullable XingsingObservation nextObservation,
                                  @Nullable boolean[] nextMask, boolean done) {
        StringBuilder json = new StringBuilder(4096);
        json.append('{');
        field(json, "schema_version", "2", false);
        field(json, "type", quote("transition"), true);
        field(json, "run_id", quote(runId), true);
        field(json, "episode_id", quote(sample.episodeId()), true);
        field(json, "scenario_id", quote(sample.scenario().id()), true);
        field(json, "target_action", quote(sample.scenario().targetOption() == null
                ? "" : sample.scenario().targetOption().name()), true);
        field(json, "seed", Long.toString(sample.seed()), true);
        field(json, "step", Integer.toString(sample.step()), true);
        field(json, "episode_tick", Integer.toString(sample.episodeTick()), true);
        field(json, "tick", Long.toString(sample.observation().gameTime), true);
        field(json, "mode", quote(mode), true);
        field(json, "entity_uuid", quote(sample.mob().getUUID().toString()), true);
        field(json, "entity_type", quote("wildterrain:xingsing"), true);
        appendVector(json, "obs", sample.observation());
        appendDebug(json, sample.observation());
        appendMask(json, "action_mask", sample.mask());
        field(json, "teacher_action", quote(sample.teacherAction().name()), true);
        field(json, "policy_action", quote(sample.policyAction().name()), true);
        field(json, "reward", String.format(Locale.ROOT, "%.5f", sample.reward()), true);
        field(json, "done", Boolean.toString(done), true);
        if (nextObservation == null) {
            json.append(",\"next_obs\":null");
            json.append(",\"next_action_mask\":null");
        } else {
            appendVector(json, "next_obs", nextObservation);
            appendMask(json, "next_action_mask", nextMask == null ? new boolean[0] : nextMask);
        }
        json.append(",\"metadata\":{");
        field(json, "favorite_player_hash", quote(hashUuid(sample.mob().getFavoritePlayer())), false);
        field(json, "collector_player_hash", quote(playerHash), true);
        field(json, "current_option", quote(sample.mob().getCurrentOption().name()), true);
        field(json, "carried_item_id", quote(sample.mob().getCarriedItemId()), true);
        field(json, "health_ratio", String.format(Locale.ROOT, "%.5f", sample.observation().healthRatio), true);
        field(json, "trust", String.format(Locale.ROOT, "%.5f", sample.observation().trust), true);
        field(json, "fear", String.format(Locale.ROOT, "%.5f", sample.observation().fear), true);
        json.append("}}");
        return json.toString();
    }

    private static void appendVector(StringBuilder json, String name, XingsingObservation observation) {
        json.append(",").append(quote(name)).append(":[");
        float[] vector = observation.vector();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(String.format(Locale.ROOT, "%.5f", vector[i]));
        }
        json.append(']');
    }

    private static void appendMask(StringBuilder json, String name, boolean[] mask) {
        json.append(",").append(quote(name)).append(":[");
        for (int i = 0; i < mask.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(mask[i]);
        }
        json.append(']');
    }

    private static void appendDebug(StringBuilder json, XingsingObservation observation) {
        json.append(",\"obs_named_debug\":{");
        int index = 0;
        for (Map.Entry<String, Float> entry : observation.namedDebug().entrySet()) {
            if (index++ > 0) {
                json.append(',');
            }
            json.append(quote(entry.getKey())).append(':')
                    .append(String.format(Locale.ROOT, "%.5f", entry.getValue()));
        }
        json.append('}');
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

    public record DecisionSample(String episodeId, XingsingCollectionScenario scenario, long seed, int step,
                                 int episodeTick, Xingsing mob, XingsingObservation observation, boolean[] mask,
                                 XingsingOption teacherAction, XingsingOption policyAction, float reward) {
    }
}
