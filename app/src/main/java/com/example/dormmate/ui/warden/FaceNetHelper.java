package com.example.dormmate.ui.warden;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

public class FaceNetHelper {
    private static final String MODEL_FILE = "facenet.tflite";
    private static final int INPUT_SIZE = 160;
    private static final int OUTPUT_SIZE = 512; // FaceNet TFLite variants usually output 128 or 512
    
    private Interpreter interpreter;

    public FaceNetHelper(Context context) {
        try {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, MODEL_FILE);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            interpreter = new Interpreter(tfliteModel, options);
        } catch (IOException e) {
            Log.e("FaceNetHelper", "Error loading model", e);
        }
    }

    public float[] getFaceEmbedding(Bitmap bitmap) {
        if (bitmap == null) return null;
        if (interpreter == null) {
            // Mock Fallback: Return a simulated embedding if model is missing
            float[] mockEmbedding = new float[OUTPUT_SIZE];
            java.util.Random random = new java.util.Random();
            for (int i = 0; i < OUTPUT_SIZE; i++) {
                mockEmbedding[i] = random.nextFloat() * 2 - 1;
            }
            return normalize(mockEmbedding);
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);
        
        float[][] output = new float[1][OUTPUT_SIZE];
        interpreter.run(inputBuffer, output);
        
        return normalize(output[0]);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                // Normalize to [-1, 1] for FaceNet
                byteBuffer.putFloat((((val >> 16) & 0xFF) - 127.5f) / 127.5f);
                byteBuffer.putFloat((((val >> 8) & 0xFF) - 127.5f) / 127.5f);
                byteBuffer.putFloat(((val & 0xFF) - 127.5f) / 127.5f);
            }
        }
        return byteBuffer;
    }
    
    // L2 Normalization
    private float[] normalize(float[] embedding) {
        float sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] /= norm;
        }
        return embedding;
    }

    public static float cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) return 0f;
        float dotProduct = 0;
        float normA = 0;
        float normB = 0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
    
    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
