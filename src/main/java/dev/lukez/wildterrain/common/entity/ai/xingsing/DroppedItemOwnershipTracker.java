package dev.lukez.wildterrain.common.entity.ai.xingsing;

import dev.lukez.wildterrain.WildTerrain;
import dev.lukez.wildterrain.common.config.WildTerrainConfig;
import dev.lukez.wildterrain.common.entity.Xingsing;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WildTerrain.MOD_ID)
public final class DroppedItemOwnershipTracker {
    private static final int ITEM_ELIGIBLE_TICKS = 20 * 45;
    private static final Map<UUID, Entry> ITEMS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> RESERVATIONS = new ConcurrentHashMap<>();

    private DroppedItemOwnershipTracker() {
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity item = event.getEntity();
        Player player = event.getPlayer();
        if (item.level().isClientSide) {
            return;
        }
        recordManualDrop(item, player);
    }

    public static void recordManualDrop(ItemEntity item, Player player) {
        long time = item.level().getGameTime();
        ITEMS.put(item.getUUID(), new Entry(player.getUUID(), time));
        PlayerActionMemory.markDroppedItem(player, time);
    }

    public static Optional<Entry> get(ItemEntity item) {
        return Optional.ofNullable(ITEMS.get(item.getUUID()));
    }

    public static boolean isEligible(ItemEntity item, Xingsing mob, long now) {
        Entry entry = ITEMS.get(item.getUUID());
        if (entry == null && !WildTerrainConfig.xingsingAllowFetchDeathDrops()) {
            return false;
        }
        if (entry != null && now - entry.dropTick() > ITEM_ELIGIBLE_TICKS) {
            ITEMS.remove(item.getUUID());
            RESERVATIONS.remove(item.getUUID());
            return false;
        }
        if (item.getItem().isEmpty() || item.isRemoved() || !isSafeItemPosition(item.level(), item.blockPosition())) {
            return false;
        }
        UUID reservedBy = RESERVATIONS.get(item.getUUID());
        return reservedBy == null || reservedBy.equals(mob.getUUID());
    }

    public static boolean reserve(ItemEntity item, Xingsing mob) {
        UUID reservedBy = RESERVATIONS.get(item.getUUID());
        if (reservedBy != null && !reservedBy.equals(mob.getUUID())) {
            return false;
        }
        RESERVATIONS.put(item.getUUID(), mob.getUUID());
        return true;
    }

    public static void release(ItemEntity item) {
        RESERVATIONS.remove(item.getUUID());
    }

    public static void remove(ItemEntity item) {
        ITEMS.remove(item.getUUID());
        RESERVATIONS.remove(item.getUUID());
    }

    @Nullable
    public static UUID ownerOf(ItemEntity item) {
        Entry entry = ITEMS.get(item.getUUID());
        return entry == null ? null : entry.ownerUuid();
    }

    public static boolean isSafeItemPosition(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        return !state.is(Blocks.LAVA)
                && !state.is(Blocks.FIRE)
                && !(state.getBlock() instanceof FireBlock)
                && !(below.getBlock() instanceof CactusBlock)
                && pos.getY() > level.getMinBuildHeight() + 2;
    }

    public record Entry(UUID ownerUuid, long dropTick) {
    }
}
