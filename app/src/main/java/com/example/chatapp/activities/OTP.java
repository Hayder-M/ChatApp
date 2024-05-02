package com.example.chatapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.utilities.AndroidUtils;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OTP extends AppCompatActivity {
    String phoneNumber;
    Long timeoutSeconds = 60L;
    String verificationCode;
    PhoneAuthProvider.ForceResendingToken resendingToken;
    EditText otpInput;
    Button nextbtn;
    ProgressBar progressBar;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.otp);

        //otpInput = findViewById(R.id.otpET);
        nextbtn = findViewById(R.id.otpB);
        progressBar = findViewById(R.id.otpPB);
        preferenceManager = new PreferenceManager(getApplicationContext());
        phoneNumber = getIntent().getStringExtra("phone");
        //sendOtp(phoneNumber, false);

        nextbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = otpInput.getText().toString().trim();
                if (!code.isEmpty()) {
                    verifyOtpCode(code);
                } else {
                    Toast.makeText(OTP.this, "Please enter the verification code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void verifyOtpCode(String code) {
        // Récupérez le code de vérification envoyé par e-mail
        String sentVerificationCode = preferenceManager.getString(Constants.KEY_VERIFICATION_CODE);
        if (code.equals(sentVerificationCode)) {
            showToast("Verification successful! You are now signed in.");
            // Marquer l'authentification comme réussie
            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
            // Ouvrir MainActivity si la vérification réussit
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            // Terminez l'activité OTP pour qu'elle ne soit pas accessible en arrière-plan après avoir ouvert MainActivity.
            finish();
        } else {
            showToast("Verification failed. Please check the code again.");
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode, code);
        signIn(credential);
    }

    /*void sendOtp(String phoneNumber, boolean isResend) {
        setInProgress(true);
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signIn(phoneAuthCredential);
                        setInProgress(false);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        AndroidUtils.showToast(getApplicationContext(), "OTP verification failed");
                        setInProgress(false);
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        verificationCode = s;
                        resendingToken = forceResendingToken;
                        AndroidUtils.showToast(getApplicationContext(), "OTP sent successfully");
                        setInProgress(false);
                    }
                };

        PhoneAuthOptions.Builder builder =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks);

        if (isResend) {
            PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(resendingToken).build());
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build());
        }
    }*/


    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            nextbtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            nextbtn.setVisibility(View.VISIBLE);
        }
    }

    void signIn(PhoneAuthCredential phoneAuthCredential) {
        setInProgress(true);
        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                setInProgress(false);
                if (task.isSuccessful()) {
                    Intent intent = new Intent(OTP.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    AndroidUtils.showToast(getApplicationContext(), "OTP verification failed");
                }
            }
        });
    }
}