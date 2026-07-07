package dev.lukez.wildterrain.common.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.entity.Xingsing;
import dev.lukez.wildterrain.common.entity.ai.xingsing.PlayerActionMemory;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingDecisionLogger;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingOption;
import dev.lukez.wildterrain.core.ModEntities;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WildTerrain.MOD_ID)
public final class WildTerrainAiCommand {
    private static final Map<UUID, Boolean> DEBUG_PLAYERS = new ConcurrentHashMap<>();

    private WildTerrainAiCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("wt_ai")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("xingsing")
                        .then(Commands.literal("record")
                                .then(Commands.literal("start").executes(ctx -> setRecording(ctx.getSource(), true)))
                                .then(Commands.literal("stop").executes(ctx -> setRecording(ctx.getSource(), false))))
                        .then(Commands.literal("debug")
                                .then(Commands.literal("on").executes(ctx -> setDebug(ctx.getSource(), true)))
                                .then(Commands.literal("off").executes(ctx -> setDebug(ctx.getSource(), false))))
                        .then(Commands.literal("label")
                                .then(Commands.argument("option", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                java.util.Arrays.stream(XingsingOption.values())
                                                        .map(XingsingOption::name), builder))
                                        .executes(ctx -> label(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "option")))))
                        .then(Commands.literal("scenario")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{
                                                "mimic_jump", "mimic_sneak", "fetch_item", "hostile_warning",
                                                "trust_recovery", "jungle_pathing"
                                        }, builder))
                                        .executes(ctx -> scenario(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name")))))));
    }

    public static boolean isDebugEnabled(ServerPlayer player) {
        return DEBUG_PLAYERS.getOrDefault(player.getUUID(), false);
    }

    private static int setRecording(CommandSourceStack source, boolean enabled) {
        XingsingDecisionLogger.setRecordingOverride(enabled);
        source.sendSuccess(() -> Component.literal("Xingsing recording " + (enabled ? "enabled" : "disabled")
                + ". Logs stay local under run/wildterrain-ai/logs/xingsing."), true);
        return 1;
    }

    private static int setDebug(CommandSourceStack source, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            DEBUG_PLAYERS.put(player.getUUID(), enabled);
        }
        source.sendSuccess(() -> Component.literal("Xingsing debug " + (enabled ? "enabled" : "disabled") + "."), false);
        return 1;
    }

    private static int label(CommandSourceStack source, String optionName) {
        XingsingOption option = XingsingOption.byName(optionName);
        if (option == null) {
            source.sendFailure(Component.literal("Unknown Xingsing option: " + optionName));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            long time = player.level().getGameTime();
            PlayerActionMemory.markLabel(player, option.name(), time);
            XingsingDecisionLogger.logCorrection(hash(player.getUUID()), option, time);
        }
        source.sendSuccess(() -> Component.literal("Recorded Xingsing label " + option.name() + "."), false);
        return 1;
    }

    private static int scenario(CommandSourceStack source, String rawName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Run Xingsing scenarios as a player in-world."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        Xingsing xingsing = nearestOrSpawnXingsing(level, player);
        String name = rawName.toLowerCase(java.util.Locale.ROOT);
        switch (name) {
            case "fetch_item" -> spawnScenarioItem(level, player);
            case "hostile_warning" -> spawnScenarioZombie(level, player);
            case "trust_recovery" -> {
                xingsing.rewardTrust(0.2F, -0.1F);
                player.getInventory().add(new ItemStack(Items.MELON_SLICE, 4));
            }
            case "mimic_jump", "mimic_sneak", "jungle_pathing" -> xingsing.startAnimation(Xingsing.ANIMATION_OBSERVE, 60);
            default -> {
                source.sendFailure(Component.literal("Unknown Xingsing scenario: " + rawName));
                return 0;
            }
        }
        source.sendSuccess(() -> Component.literal("Prepared Xingsing scenario: " + name + "."), true);
        return 1;
    }

    private static Xingsing nearestOrSpawnXingsing(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(Xingsing.class, player.getBoundingBox().inflate(12.0D))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElseGet(() -> {
                    Xingsing mob = ModEntities.XINGSING.get().spawn(level, player.blockPosition().offset(2, 0, 2),
                            MobSpawnType.COMMAND);
                    if (mob == null) {
                        mob = ModEntities.XINGSING.get().create(level);
                        if (mob != null) {
                            mob.moveTo(player.getX() + 2.0D, player.getY(), player.getZ() + 2.0D,
                                    player.getYRot(), 0.0F);
                            level.addFreshEntity(mob);
                        }
                    }
                    return mob;
                });
    }

    private static void spawnScenarioItem(ServerLevel level, ServerPlayer player) {
        Vec3 pos = player.position().add(player.getLookAngle().normalize().scale(2.5D));
        ItemEntity item = new ItemEntity(level, pos.x, player.getY() + 0.25D, pos.z,
                new ItemStack(Items.MELON_SLICE, 1));
        item.setThrower(player.getUUID());
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        dev.lukez.wildterrain.common.entity.ai.xingsing.DroppedItemOwnershipTracker.recordManualDrop(item, player);
    }

    private static void spawnScenarioZombie(ServerLevel level, ServerPlayer player) {
        Vec3 pos = player.position().add(player.getLookAngle().normalize().scale(8.0D));
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie != null) {
            zombie.moveTo(pos.x, player.getY(), pos.z, player.getYRot() + 180.0F, 0.0F);
            zombie.setTarget(player);
            level.addFreshEntity(zombie);
        }
    }

    private static String hash(UUID uuid) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(uuid.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder("sha256:");
            for (byte b : bytes) {
                out.append(String.format(java.util.Locale.ROOT, "%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "sha256-unavailable";
        }
    }
}
