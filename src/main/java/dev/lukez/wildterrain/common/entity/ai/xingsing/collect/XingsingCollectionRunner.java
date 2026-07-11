package dev.lukez.wildterrain.common.entity.ai.xingsing.collect;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.config.WildTerrainConfig;
import dev.lukez.wildterrain.common.entity.Xingsing;
import dev.lukez.wildterrain.common.entity.ai.xingsing.DroppedItemOwnershipTracker;
import dev.lukez.wildterrain.common.entity.ai.xingsing.PlayerActionMemory;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingActionMaskBuilder;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingObservation;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingObservationBuilder;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingOption;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingRuleTeacher;
import dev.lukez.wildterrain.core.ModEntities;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WildTerrain.MOD_ID)
public final class XingsingCollectionRunner {
    private static final int DEFAULT_EPISODES = 24;
    private static final int DEFAULT_TICKS = 240;
    private static final int MIN_TICKS = 60;
    private static final int MAX_TICKS = 6000;
    private static final int MAX_EPISODES = 5000;
    @Nullable
    private static RunState activeRun;

    private XingsingCollectionRunner() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        RunState run = activeRun;
        if (run == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            run.beforeServerTick();
        } else if (event.phase == TickEvent.Phase.END) {
            run.afterServerTick();
            if (!run.isRunning()) {
                activeRun = null;
            }
        }
    }

    public static CommandResult start(ServerPlayer player, String scenarioName, int episodes,
                                      int ticksPerEpisode, String modeName) {
        if (activeRun != null) {
            return CommandResult.failure("Xingsing collection is already running. Use /wt_ai xingsing collect status.");
        }
        XingsingCollectionScenario requestedScenario = XingsingCollectionScenario.byName(scenarioName);
        if (requestedScenario == null) {
            return CommandResult.failure("Unknown Xingsing collection scenario: " + scenarioName);
        }
        WildTerrainConfig.XingsingAiMode mode = parseMode(modeName);
        if (mode == null || mode == WildTerrainConfig.XingsingAiMode.DISABLED) {
            return CommandResult.failure("Collection mode must be teacher or model.");
        }
        int safeEpisodes = Mth.clamp(episodes, 1, MAX_EPISODES);
        int safeTicks = Mth.clamp(ticksPerEpisode, MIN_TICKS, MAX_TICKS);
        try {
            activeRun = new RunState(player, requestedScenario, safeEpisodes, safeTicks, mode);
            return CommandResult.success("Started Xingsing collection " + activeRun.statusLine());
        } catch (IOException ex) {
            WildTerrain.LOGGER.warn("Failed to start Xingsing collection", ex);
            return CommandResult.failure("Failed to start Xingsing collection. Check latest.log.");
        }
    }

    public static CommandResult startDefault(ServerPlayer player) {
        return start(player, XingsingCollectionScenario.COVERAGE.id(), DEFAULT_EPISODES, DEFAULT_TICKS, "teacher");
    }

    public static CommandResult stop() {
        RunState run = activeRun;
        if (run == null) {
            return CommandResult.failure("No Xingsing collection run is active.");
        }
        run.finish("stopped_by_command");
        activeRun = null;
        return CommandResult.success("Stopped Xingsing collection. Run dir: " + run.runDir());
    }

    public static CommandResult status() {
        RunState run = activeRun;
        if (run == null) {
            return CommandResult.success("No Xingsing collection run is active.");
        }
        return CommandResult.success("Xingsing collection " + run.statusLine());
    }

    public static String[] scenarioIds() {
        return XingsingCollectionScenario.ids();
    }

    private static WildTerrainConfig.XingsingAiMode parseMode(String modeName) {
        try {
            return WildTerrainConfig.XingsingAiMode.valueOf(modeName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record CommandResult(boolean success, String message) {
        public static CommandResult success(String message) {
            return new CommandResult(true, message);
        }

        public static CommandResult failure(String message) {
            return new CommandResult(false, message);
        }
    }

    private static final class RunState {
        private final ServerPlayer player;
        private final XingsingCollectionScenario requestedScenario;
        private final int requestedEpisodes;
        private final int ticksPerEpisode;
        private final WildTerrainConfig.XingsingAiMode requestedMode;
        private final WildTerrainConfig.XingsingAiMode previousMode;
        private final boolean previousAllowModelInference;
        private final XingsingEpisodeLogger logger;
        private final XingsingObservationBuilder observationBuilder = new XingsingObservationBuilder();
        private final XingsingActionMaskBuilder maskBuilder = new XingsingActionMaskBuilder();
        private final XingsingRuleTeacher teacher = new XingsingRuleTeacher();
        private final List<Entity> spawnedEntities = new ArrayList<>();
        private final Map<BlockPos, BlockState> restoredBlocks = new LinkedHashMap<>();
        private boolean running = true;
        private int episodeIndex;
        private int episodesCompleted;
        private int episodeTick;
        private int step;
        private long episodeSeed;
        private String episodeId = "";
        private XingsingCollectionScenario episodeScenario = XingsingCollectionScenario.SETTLE_NEAR_PLAYER;
        @Nullable
        private Xingsing mob;
        @Nullable
        private BufferedWriter episodeWriter;
        @Nullable
        private XingsingEpisodeLogger.DecisionSample pendingSample;

        private RunState(ServerPlayer player, XingsingCollectionScenario requestedScenario, int requestedEpisodes,
                         int ticksPerEpisode, WildTerrainConfig.XingsingAiMode requestedMode) throws IOException {
            this.player = player;
            this.requestedScenario = requestedScenario;
            this.requestedEpisodes = requestedEpisodes;
            this.ticksPerEpisode = ticksPerEpisode;
            this.requestedMode = requestedMode;
            this.previousMode = WildTerrainConfig.xingsingAiMode();
            this.previousAllowModelInference = WildTerrainConfig.xingsingAllowModelInference();
            this.logger = XingsingEpisodeLogger.open(requestedScenario, requestedMode.name().toLowerCase(Locale.ROOT),
                    requestedEpisodes, ticksPerEpisode, player.getUUID());
            WildTerrainConfig.setXingsingAiMode(requestedMode.name().toLowerCase(Locale.ROOT));
            WildTerrainConfig.setXingsingAllowModelInference(requestedMode == WildTerrainConfig.XingsingAiMode.MODEL);
            startEpisode();
        }

        private void beforeServerTick() {
            if (!running || mob == null) {
                return;
            }
            if (!player.isAlive() || player.hasDisconnected()) {
                finish("collector_player_unavailable");
                return;
            }
            applyStimulus();
        }

        private void afterServerTick() {
            if (!running || mob == null) {
                return;
            }
            if (episodeTick % WildTerrainConfig.xingsingDecisionIntervalTicks() == 0) {
                captureDecision();
            }
            episodeTick++;
            if (episodeTick >= ticksPerEpisode) {
                finishEpisode();
                episodeIndex++;
                if (episodeIndex >= requestedEpisodes) {
                    finish("complete");
                } else {
                    startEpisode();
                }
            }
        }

        private boolean isRunning() {
            return running;
        }

        private String statusLine() {
            return "run_id=" + logger.runId()
                    + " scenario=" + requestedScenario.id()
                    + " episode=" + Math.min(episodeIndex + 1, requestedEpisodes) + "/" + requestedEpisodes
                    + " tick=" + episodeTick + "/" + ticksPerEpisode
                    + " transitions=" + logger.transitionsWritten()
                    + " dir=" + logger.runDir();
        }

        private String runDir() {
            return logger.runDir().toString();
        }

        private void startEpisode() {
            cleanupEpisodeWorld();
            pendingSample = null;
            episodeTick = 0;
            step = 0;
            episodeScenario = requestedScenario.isCoverage()
                    ? XingsingCollectionScenario.coverageScenario(episodeIndex)
                    : requestedScenario;
            episodeSeed = System.currentTimeMillis() ^ player.getUUID().getLeastSignificantBits() ^ episodeIndex;
            episodeId = logger.runId() + "-ep-" + String.format(Locale.ROOT, "%04d", episodeIndex);
            try {
                episodeWriter = logger.openEpisode(episodeId);
            } catch (IOException ex) {
                WildTerrain.LOGGER.warn("Failed to open Xingsing episode log", ex);
                finish("episode_log_open_failed");
                return;
            }
            mob = spawnXingsing();
            if (mob == null) {
                finish("xingsing_spawn_failed");
                return;
            }
            setupScenario();
            logger.writeManifest(episodesCompleted, "running");
        }

        private void finishEpisode() {
            if (pendingSample != null && episodeWriter != null) {
                try {
                    logger.writeTransition(episodeWriter, pendingSample, null, null, true);
                } catch (IOException ex) {
                    WildTerrain.LOGGER.warn("Failed to finalize Xingsing episode transition", ex);
                }
            }
            pendingSample = null;
            closeEpisodeWriter();
            cleanupEpisodeWorld();
            episodesCompleted = Math.max(episodesCompleted, episodeIndex + 1);
            logger.writeManifest(episodesCompleted, "running");
        }

        private void finish(String status) {
            if (!running) {
                return;
            }
            running = false;
            if (episodeWriter != null || pendingSample != null || mob != null) {
                finishEpisode();
            }
            WildTerrainConfig.setXingsingAiMode(previousMode.name().toLowerCase(Locale.ROOT));
            WildTerrainConfig.setXingsingAllowModelInference(previousAllowModelInference);
            logger.writeManifest(Math.min(episodesCompleted, requestedEpisodes), status);
            logger.close();
            WildTerrain.LOGGER.info("Xingsing collection {}. {}", status, statusLine());
        }

        private void closeEpisodeWriter() {
            if (episodeWriter == null) {
                return;
            }
            try {
                episodeWriter.close();
            } catch (IOException ex) {
                WildTerrain.LOGGER.warn("Failed to close Xingsing episode log", ex);
            } finally {
                episodeWriter = null;
            }
        }

        @Nullable
        private Xingsing spawnXingsing() {
            ServerLevel level = player.serverLevel();
            BlockPos pos = player.blockPosition().offset(2 + (episodeIndex % 3), 0, 2);
            Xingsing spawned = ModEntities.XINGSING.get().spawn(level, pos, MobSpawnType.COMMAND);
            if (spawned == null) {
                spawned = ModEntities.XINGSING.get().create(level);
                if (spawned != null) {
                    spawned.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
                    level.addFreshEntity(spawned);
                }
            }
            if (spawned != null) {
                spawned.rewardTrust(0.35F, 0.05F);
                spawned.startAnimation(Xingsing.ANIMATION_OBSERVE, 40);
                spawnedEntities.add(spawned);
            }
            return spawned;
        }

        private void setupScenario() {
            if (mob == null) {
                return;
            }
            long now = player.level().getGameTime();
            switch (episodeScenario) {
                case MIMIC_JUMP -> PlayerActionMemory.markJumped(player, now);
                case MIMIC_SNEAK -> PlayerActionMemory.markSneaking(player, now);
                case MIMIC_SPRINT -> PlayerActionMemory.markSprinting(player, now);
                case FETCH_ITEM -> spawnScenarioItem();
                case RETURN_ITEM -> mob.setCarriedItem(new ItemStack(Items.MELON_SLICE), player.getUUID());
                case HOSTILE_WARNING -> spawnScenarioZombie(false);
                case FLEE_TO_TREE -> {
                    buildTemporaryCanopy();
                    mob.setHealth(Math.min(mob.getHealth(), 3.0F));
                    spawnScenarioZombie(true);
                }
                case CLIMB_TO_PERCH -> buildTemporaryCanopy();
                case PLAY_CHASE -> mob.rewardTrust(0.35F, 0.1F);
                case SETTLE_NEAR_PLAYER, COVERAGE -> {
                }
            }
        }

        private void applyStimulus() {
            if (mob == null || mob.isRemoved()) {
                finish("xingsing_removed");
                return;
            }
            long now = player.level().getGameTime();
            switch (episodeScenario) {
                case MIMIC_JUMP -> {
                    if (episodeTick % 28 < 8) {
                        PlayerActionMemory.markJumped(player, now);
                    }
                }
                case MIMIC_SNEAK -> PlayerActionMemory.markSneaking(player, now);
                case MIMIC_SPRINT -> PlayerActionMemory.markSprinting(player, now);
                case FETCH_ITEM -> {
                    if (episodeTick % 100 == 20 && !mob.hasCarriedItem()) {
                        spawnScenarioItem();
                    }
                }
                case HOSTILE_WARNING -> {
                    if (episodeTick % 120 == 20) {
                        spawnScenarioZombie(false);
                    }
                }
                case FLEE_TO_TREE -> {
                    mob.setHealth(Math.min(mob.getHealth(), 3.0F));
                    if (episodeTick % 120 == 20) {
                        spawnScenarioZombie(true);
                    }
                }
                case PLAY_CHASE -> mob.rewardTrust(0.02F, 0.0F);
                case SETTLE_NEAR_PLAYER, RETURN_ITEM, CLIMB_TO_PERCH, COVERAGE -> {
                }
            }
        }

        private void captureDecision() {
            if (mob == null || episodeWriter == null) {
                return;
            }
            XingsingObservation observation = observationBuilder.build(mob);
            boolean[] mask = maskBuilder.build(mob, observation);
            XingsingOption teacherAction = teacher.select(observation, mask);
            XingsingOption policyAction = mob.getCurrentOption();
            if (policyAction == null) {
                policyAction = XingsingOption.IDLE_GROOM;
            }
            float reward = episodeScenario.reward(observation, teacherAction, policyAction);
            XingsingEpisodeLogger.DecisionSample sample = new XingsingEpisodeLogger.DecisionSample(episodeId,
                    episodeScenario, episodeSeed, step++, episodeTick, mob, observation, mask,
                    teacherAction, policyAction, reward);
            if (pendingSample != null) {
                try {
                    logger.writeTransition(episodeWriter, pendingSample, observation, mask, false);
                } catch (IOException ex) {
                    WildTerrain.LOGGER.warn("Failed to write Xingsing transition", ex);
                    finish("transition_write_failed");
                    return;
                }
            }
            pendingSample = sample;
        }

        private void spawnScenarioItem() {
            ServerLevel level = player.serverLevel();
            Vec3 pos = player.position().add(player.getLookAngle().normalize().scale(2.5D));
            ItemEntity item = new ItemEntity(level, pos.x, player.getY() + 0.25D, pos.z,
                    new ItemStack(Items.MELON_SLICE, 1));
            item.setThrower(player.getUUID());
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
            spawnedEntities.add(item);
            DroppedItemOwnershipTracker.recordManualDrop(item, player);
        }

        private void spawnScenarioZombie(boolean targetXingsing) {
            ServerLevel level = player.serverLevel();
            LivingTarget target = targetXingsing && mob != null ? new LivingTarget(mob) : new LivingTarget(player);
            Vec3 fromTarget = target.position();
            Vec3 look = player.getLookAngle();
            if (look.lengthSqr() < 0.01D) {
                look = new Vec3(1.0D, 0.0D, 0.0D);
            }
            Vec3 pos = fromTarget.add(look.normalize().scale(4.5D));
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie != null) {
                zombie.moveTo(pos.x, fromTarget.y, pos.z, player.getYRot() + 180.0F, 0.0F);
                zombie.setTarget(target.entity());
                level.addFreshEntity(zombie);
                spawnedEntities.add(zombie);
            }
        }

        private void buildTemporaryCanopy() {
            if (mob == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockPos base = mob.blockPosition().offset(1, 0, 1);
            for (int y = 0; y <= 4; y++) {
                placeTemp(level, base.above(y), Blocks.OAK_LOG.defaultBlockState());
            }
            BlockPos crown = base.above(4);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 3) {
                        placeTemp(level, crown.offset(dx, 0, dz), Blocks.OAK_LEAVES.defaultBlockState());
                    }
                }
            }
            placeTemp(level, crown.above(), Blocks.OAK_LEAVES.defaultBlockState());
        }

        private void placeTemp(ServerLevel level, BlockPos pos, BlockState state) {
            if (!restoredBlocks.containsKey(pos)) {
                restoredBlocks.put(pos.immutable(), level.getBlockState(pos));
            }
            level.setBlock(pos, state, 3);
        }

        private void cleanupEpisodeWorld() {
            for (Entity entity : spawnedEntities) {
                if (entity != null && !entity.isRemoved()) {
                    entity.discard();
                }
            }
            spawnedEntities.clear();
            ServerLevel level = player.serverLevel();
            for (Map.Entry<BlockPos, BlockState> entry : restoredBlocks.entrySet()) {
                level.setBlock(entry.getKey(), entry.getValue(), 3);
            }
            restoredBlocks.clear();
            mob = null;
            closeEpisodeWriter();
        }
    }

    private record LivingTarget(net.minecraft.world.entity.LivingEntity entity) {
        private Vec3 position() {
            return entity.position();
        }
    }
}
