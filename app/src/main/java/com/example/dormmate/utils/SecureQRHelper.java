package com.example.dormmate.utils;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Utility for generating and verifying secure, time-sensitive QR tokens.
 * Logic: Encrypts "HOSTEL_ID|ROUNDED_TIMESTAMP" using AES.
 */
public class SecureQRHelper {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String SECRET_KEY = "DormMateSecurKey"; // Exactly 16 chars for AES-128
    private static final String HOSTEL_ID = "GH_HOSTEL_01";
    private static final int REFRESH_INTERVAL_SEC = 10;

    /**
     * Generates an encrypted token based on the current 10-second window.
     */
    public static String generateToken() {
        try {
            long roundedTimestamp = System.currentTimeMillis() / 1000 / REFRESH_INTERVAL_SEC;
            String rawData = HOSTEL_ID + "|" + roundedTimestamp;
            return encrypt(rawData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypts and verifies a token.
     * @return The data if valid, otherwise null.
     */
    public static String verifyAndDecrypt(String encryptedToken) {
        try {
            String decrypted = decrypt(encryptedToken);
            if (decrypted == null) return null;

            String[] parts = decrypted.split("\\|");
            if (parts.length != 2) return null;

            String hostelId = parts[0];
            long tokenTimestamp = Long.parseLong(parts[1]);

            // Check if token is within ±2 intervals (20s) tolerance
            long currentRounded = System.currentTimeMillis() / 1000 / REFRESH_INTERVAL_SEC;
            if (hostelId.equals(HOSTEL_ID) && Math.abs(currentRounded - tokenTimestamp) <= 2) {
                return hostelId;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String encrypt(String data) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encrypted, Base64.DEFAULT).trim();
    }

    private static String decrypt(String encryptedData) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.decode(encryptedData, Base64.DEFAULT);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
