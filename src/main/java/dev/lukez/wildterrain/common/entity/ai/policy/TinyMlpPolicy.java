package dev.lukez.wildterrain.common.entity.ai.policy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingObservation;
import dev.lukez.wildterrain.common.entity.ai.xingsing.XingsingOption;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class TinyMlpPolicy implements PolicyModel {
    private final int inputSize;
    private final int outputSize;
    private final List<Layer> layers;

    private TinyMlpPolicy(int inputSize, int outputSize, List<Layer> layers) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.layers = layers;
    }

    public static TinyMlpPolicy fromJson(InputStream inputStream) throws IOException {
        JsonObject root = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .getAsJsonObject();
        if (root.get("schema_version").getAsInt() != 1) {
            throw new IOException("Unsupported policy schema version");
        }
        if (root.get("obs_spec_version").getAsInt() != XingsingObservation.SPEC_VERSION) {
            throw new IOException("Xingsing observation spec mismatch");
        }
        int numActions = root.get("num_actions").getAsInt();
        if (numActions != XingsingOption.COUNT) {
            throw new IOException("Xingsing action count mismatch");
        }
        List<Layer> layers = new ArrayList<>();
        int inferredInput = -1;
        int inferredOutput = -1;
        JsonArray layerJson = root.getAsJsonArray("layers");
        for (JsonElement element : layerJson) {
            JsonObject layer = element.getAsJsonObject();
            String type = layer.get("type").getAsString();
            if ("linear".equals(type)) {
                int in = layer.get("in").getAsInt();
                int out = layer.get("out").getAsInt();
                float[] weights = decodeFloats(layer.get("weights").getAsString(), in * out);
                float[] bias = decodeFloats(layer.get("bias").getAsString(), out);
                layers.add(new LinearLayer(in, out, weights, bias));
                if (inferredInput < 0) {
                    inferredInput = in;
                }
                inferredOutput = out;
            } else if ("relu".equals(type)) {
                layers.add(new ReluLayer());
            } else {
                throw new IOException("Unsupported policy layer: " + type);
            }
        }
        return new TinyMlpPolicy(inferredInput, inferredOutput, layers);
    }

    @Override
    public int inputSize() {
        return inputSize;
    }

    @Override
    public int outputSize() {
        return outputSize;
    }

    @Override
    public float[] logits(float[] observation) {
        float[] activations = observation;
        for (Layer layer : layers) {
            activations = layer.forward(activations);
        }
        return activations;
    }

    private static float[] decodeFloats(String base64, int expected) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        if (bytes.length != expected * Float.BYTES) {
            throw new IOException("Policy tensor size mismatch");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] values = new float[expected];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.getFloat();
        }
        return values;
    }

    private interface Layer {
        float[] forward(float[] input);
    }

    private record LinearLayer(int in, int out, float[] weights, float[] bias) implements Layer {
        @Override
        public float[] forward(float[] input) {
            if (input.length != in) {
                throw new IllegalArgumentException("Expected " + in + " policy inputs, got " + input.length);
            }
            float[] output = new float[out];
            for (int row = 0; row < out; row++) {
                float sum = bias[row];
                int offset = row * in;
                for (int col = 0; col < in; col++) {
                    sum += weights[offset + col] * input[col];
                }
                output[row] = sum;
            }
            return output;
        }
    }

    private static final class ReluLayer implements Layer {
        @Override
        public float[] forward(float[] input) {
            float[] output = new float[input.length];
            for (int i = 0; i < input.length; i++) {
                output[i] = Math.max(0.0F, input[i]);
            }
            return output;
        }
    }
}
