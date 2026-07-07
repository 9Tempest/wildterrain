package dev.lukez.wildterrain.common.entity.ai.policy;

import dev.lukez.wildterrain.WildTerrain;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

public final class PolicyResourceLoader {
    private static final ResourceLocation XINGSING_POLICY =
            new ResourceLocation(WildTerrain.MOD_ID, "policies/xingsing_policy_v1.json");

    private PolicyResourceLoader() {
    }

    public static Optional<TinyMlpPolicy> loadXingsingPolicy(MinecraftServer server) {
        Optional<Resource> resource = server.getResourceManager().getResource(XINGSING_POLICY);
        if (resource.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(TinyMlpPolicy.fromJson(resource.get().open()));
        } catch (IOException | RuntimeException ex) {
            WildTerrain.LOGGER.warn("Could not load Xingsing policy {}, falling back to teacher",
                    XINGSING_POLICY, ex);
            return Optional.empty();
        }
    }
}
