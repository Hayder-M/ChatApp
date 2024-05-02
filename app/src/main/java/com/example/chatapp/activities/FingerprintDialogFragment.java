package com.example.chatapp.activities;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.fragment.app.DialogFragment;

public class FingerprintDialogFragment extends DialogFragment {

    //...

    private OnAuthenticationResultListener onAuthenticationResultListener;

    public interface OnAuthenticationResultListener {
        void onAuthenticationSuccess();

        void onAuthenticationError();
    }

    public void setOnAuthenticationResultListener(OnAuthenticationResultListener listener) {
        onAuthenticationResultListener = listener;
    }


    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
        // L'empreinte digitale est authentifiée avec succès
        if (onAuthenticationResultListener != null) {
            onAuthenticationResultListener.onAuthenticationSuccess();
        }
        dismiss();
    }


    public void onAuthenticationFailed() {
        // L'empreinte digitale n'a pas été reconnue
        if (onAuthenticationResultListener != null) {
            onAuthenticationResultListener.onAuthenticationError();
        }
    }

    //...
}

