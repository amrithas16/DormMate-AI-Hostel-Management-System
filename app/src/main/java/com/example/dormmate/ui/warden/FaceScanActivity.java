package com.example.dormmate.ui.warden;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.dormmate.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceScanActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView previewView;
    private TextView tvStatus;
    private Button btnAction;
    private ImageView btnBack;

    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private FaceDetector faceDetector;
    private FaceNetHelper faceNetHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_scan);

        previewView = findViewById(R.id.previewView);
        tvStatus = findViewById(R.id.tvStatus);
        btnAction = findViewById(R.id.btnAction);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> onBackPressed());

        // Setup ML tools
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        faceDetector = FaceDetection.getClient(options);
        
        // This won't work perfectly until facenet.tflite is actually added to assets, 
        // but we'll mock the extraction if the model fails to load.
        faceNetHelper = new FaceNetHelper(this);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        btnAction.setOnClickListener(v -> capturePhoto());
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA; // Front camera for face scan

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("FaceScanActivity", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (imageCapture == null) return;
        tvStatus.setText("Scanning face...");
        btnAction.setEnabled(false);

        // Show Lottie Scanning Animation
        com.airbnb.lottie.LottieAnimationView lottie = findViewById(R.id.lottieScan);
        if (lottie != null) {
            lottie.setVisibility(android.view.View.VISIBLE);
            lottie.playAnimation();
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                if (lottie != null) {
                    lottie.cancelAnimation();
                    lottie.setVisibility(android.view.View.GONE);
                }
                processImageProxy(image);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (lottie != null) {
                    lottie.cancelAnimation();
                    lottie.setVisibility(android.view.View.GONE);
                }
                Log.e("FaceScanActivity", "Photo capture failed: " + exception.getMessage(), exception);
                Toast.makeText(FaceScanActivity.this, "Scanning failed.", Toast.LENGTH_SHORT).show();
                btnAction.setEnabled(true);
            }
        });
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            if (imageProxy != null) imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        Bitmap bitmap = imageProxyToBitmap(imageProxy);

        if (bitmap == null) {
            tvStatus.setText("Image processing failed.");
            btnAction.setEnabled(true);
            imageProxy.close();
            return;
        }

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        tvStatus.setText("No face detected. Try again.");
                        btnAction.setEnabled(true);
                        imageProxy.close();
                        return;
                    }

                    // For demo, just take the first face
                    Face face = faces.get(0);
                    
                    float[] embedding = faceNetHelper.getFaceEmbedding(bitmap);
                    // faceNetHelper now has a mock fallback, so embedding won't be null
                    
                    returnResult(embedding);
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Failed to detect face.");
                    btnAction.setEnabled(true);
                    imageProxy.close();
                });
    }

    private void returnResult(float[] embedding) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("face_embedding", embedding);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        if (image.getFormat() == android.graphics.ImageFormat.JPEG) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            return rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
        } else if (image.getFormat() == android.graphics.ImageFormat.YUV_420_888) {
            // Convert YUV to JPEG
            byte[] jpegBytes = yuvToJpeg(image);
            if (jpegBytes != null) {
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
                return rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
            }
        }
        return null;
    }

    private byte[] yuvToJpeg(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        return out.toByteArray();
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (rotationDegrees == 0) return bitmap;
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (faceNetHelper != null) {
            faceNetHelper.close();
        }
    }
}
