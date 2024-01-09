package id.mncinnovation.mncidentifiersdk.java;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import id.mncinnovation.face_detection.MNCIdentifier;
import id.mncinnovation.face_detection.SelfieWithKtpActivity;
import id.mncinnovation.face_detection.analyzer.DetectionMode;
import id.mncinnovation.mncidentifiersdk.databinding.ActivityMainBinding;
import id.mncinnovation.ocr.CaptureOCRActivity;
import id.mncinnovation.ocr.ScanOCRActivity;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {
    static int LIVENESS_DETECTION_REQUEST_CODE = 101;
    static int CAPTURE_EKTP_REQUEST_CODE = 102;
    static int SCAN_KTP_REQUEST_CODE = 103;
    static int SELFIE_WITH_KTP_REQUEST_CODE = 104;

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        List detectionModes = Arrays.asList(
                DetectionMode.HOLD_STILL);
        int random = 0;
        for (int i = 1; i < 3; i++) {
            random = new Random().nextInt(4);
            if (random == 0) {
                detectionModes.add(i, DetectionMode.TURN_RIGHT);
            } else if (random == 1) {
                detectionModes.add(i, DetectionMode.TURN_LEFT);
            } else if (random == 2) {
                detectionModes.add(i, DetectionMode.SMILE);
            } else {
                detectionModes.add(i, DetectionMode.BLINK);
            }
        }

        MNCIdentifier.setDetectionModeSequence(true, detectionModes);
        MNCIdentifier.setLowMemoryThreshold(50);

        binding.btnScanKtp.setOnClickListener(
                v -> startActivityForResult(new Intent(this, ScanOCRActivity.class), SCAN_KTP_REQUEST_CODE));

        binding.btnCaptureKtp.setOnClickListener(
                v -> startActivityForResult(new Intent(this, CaptureOCRActivity.class), CAPTURE_EKTP_REQUEST_CODE));

        binding.btnLivenessDetection.setOnClickListener(
                v -> startActivityForResult(MNCIdentifier.getLivenessIntent(this), LIVENESS_DETECTION_REQUEST_CODE));

        binding.btnSelfieWKtp
                .setOnClickListener(v -> startActivityForResult(new Intent(this, SelfieWithKtpActivity.class),
                        SELFIE_WITH_KTP_REQUEST_CODE));
    }
}