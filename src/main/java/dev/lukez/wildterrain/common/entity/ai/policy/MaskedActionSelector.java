package dev.lukez.wildterrain.common.entity.ai.policy;

public final class MaskedActionSelector {
    private MaskedActionSelector() {
    }

    public static int argmax(float[] logits, boolean[] mask) {
        int best = 0;
        float bestValue = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < logits.length && i < mask.length; i++) {
            float value = mask[i] ? logits[i] : -1.0e9F;
            if (value > bestValue) {
                bestValue = value;
                best = i;
            }
        }
        return best;
    }
}
