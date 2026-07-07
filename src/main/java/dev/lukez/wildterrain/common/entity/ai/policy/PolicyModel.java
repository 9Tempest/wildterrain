package dev.lukez.wildterrain.common.entity.ai.policy;

import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingOption;
import java.util.Random;

public interface PolicyModel {
    int inputSize();

    int outputSize();

    float[] logits(float[] observation);

    default XingsingOption select(float[] observation, boolean[] actionMask, Random random) {
        return XingsingOption.byId(MaskedActionSelector.argmax(logits(observation), actionMask));
    }
}
