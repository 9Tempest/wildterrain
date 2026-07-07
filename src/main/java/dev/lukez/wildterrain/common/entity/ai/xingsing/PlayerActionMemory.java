package dev.lukez.wildterrain.common.entity.ai.xingsing;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.entity.Xingsing;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WildTerrain.MOD_ID)
public final class PlayerActionMemory {
    private static final Map<UUID, Memory> MEMORIES = new ConcurrentHashMap<>();

    private PlayerActionMemory() {
    }

    public static Memory get(Player player) {
        return MEMORIES.computeIfAbsent(player.getUUID(), uuid -> new Memory());
    }

    @Nullable
    public static Memory get(UUID playerUuid) {
        return MEMORIES.get(playerUuid);
    }

    public static void markDroppedItem(Player player, long gameTime) {
        get(player).lastDropTick = gameTime;
    }

    public static void markFedXingsing(Player player, long gameTime) {
        get(player).lastFeedTick = gameTime;
    }

    public static void markLabel(Player player, String optionName, long gameTime) {
        Memory memory = get(player);
        memory.lastLabelTick = gameTime;
        memory.lastLabel = optionName;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        long time = player.level().getGameTime();
        Memory memory = get(player);
        if (player.isSprinting()) {
            memory.lastSprintTick = time;
        }
        if (player.isShiftKeyDown()) {
            memory.lastSneakTick = time;
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            get(player).lastJumpTick = player.level().getGameTime();
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Xingsing && !event.getLevel().isClientSide()) {
            markFedXingsing(event.getEntity(), event.getLevel().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity target = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (target.level().isClientSide) {
            return;
        }
        long time = target.level().getGameTime();
        if (attacker instanceof Player player && target instanceof Xingsing) {
            get(player).lastAttackXingsingTick = time;
        }
        if (target instanceof Player player) {
            get(player).lastHurtTick = time;
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player && event.getLevel() instanceof ServerLevel level) {
            get(player).lastSeenTick = level.getGameTime();
        }
    }

    public static final class Memory {
        private long lastSeenTick = Long.MIN_VALUE / 4;
        private long lastJumpTick = Long.MIN_VALUE / 4;
        private long lastSneakTick = Long.MIN_VALUE / 4;
        private long lastSprintTick = Long.MIN_VALUE / 4;
        private long lastDropTick = Long.MIN_VALUE / 4;
        private long lastFeedTick = Long.MIN_VALUE / 4;
        private long lastAttackXingsingTick = Long.MIN_VALUE / 4;
        private long lastHurtTick = Long.MIN_VALUE / 4;
        private long lastLabelTick = Long.MIN_VALUE / 4;
        private String lastLabel = "";

        public float jumpAgeNorm(long now) {
            return ageNorm(now, lastJumpTick, 60);
        }

        public float sneakAgeNorm(long now) {
            return ageNorm(now, lastSneakTick, 60);
        }

        public float sprintAgeNorm(long now) {
            return ageNorm(now, lastSprintTick, 80);
        }

        public float dropAgeNorm(long now) {
            return ageNorm(now, lastDropTick, 200);
        }

        public float feedAgeNorm(long now) {
            return ageNorm(now, lastFeedTick, 1200);
        }

        public float attackAgeNorm(long now) {
            return ageNorm(now, lastAttackXingsingTick, 1200);
        }

        public float hurtAgeNorm(long now) {
            return ageNorm(now, lastHurtTick, 200);
        }

        public String lastLabel() {
            return lastLabel;
        }

        private static float ageNorm(long now, long tick, int window) {
            if (tick <= Long.MIN_VALUE / 8) {
                return 1.0F;
            }
            long age = Math.max(0L, now - tick);
            return Math.min(1.0F, age / (float) window);
        }
    }
}
