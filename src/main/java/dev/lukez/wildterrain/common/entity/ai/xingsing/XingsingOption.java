package dev.lukez.wildterrain.common.entity.ai.xingsing;

import java.util.Locale;
import javax.annotation.Nullable;

public enum XingsingOption {
    IDLE_GROOM(0),
    OBSERVE_PLAYER(1),
    APPROACH_PLAYER(2),
    KEEP_PLAY_DISTANCE(3),
    MIRROR_JUMP(4),
    MIRROR_SNEAK(5),
    MIRROR_SPRINT(6),
    PICKUP_ITEM(7),
    RETURN_ITEM(8),
    PLAY_CHASE(9),
    CLIMB_TO_PERCH(10),
    WARN_HOSTILE(11),
    FLEE_TO_TREE(12),
    LEAD_TO_FRUIT(13);

    public static final int COUNT = values().length;

    private final int id;

    XingsingOption(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static XingsingOption byId(int id) {
        for (XingsingOption option : values()) {
            if (option.id == id) {
                return option;
            }
        }
        return IDLE_GROOM;
    }

    @Nullable
    public static XingsingOption byName(String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        for (XingsingOption option : values()) {
            if (option.name().equals(normalized) || option.serializedName().equals(name.trim())) {
                return option;
            }
        }
        return null;
    }
}
