package com.example.chatapp.activities;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

public class FingerprintUtils {

    // ...

    public static boolean isFingerprintAuthAvailable(Context context) {
        FingerprintManager fingerprintManager = context.getSystemService(FingerprintManager.class);
        return fingerprintManager != null && fingerprintManager.isHardwareDetected();
    }


    // ...
}

