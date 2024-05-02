package com.example.chatapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivitySignInBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.firebase.ui.auth.AuthUI;
import com.futuremind.recyclerviewfastscroll.Utils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        ImageView passwordVisibilityImageView = findViewById(R.id.eyeImage);
        passwordVisibilityImageView.setImageResource(R.drawable.baseline_visibility_off_24);
        firebaseAuth = FirebaseAuth.getInstance();
        configureGoogleSignIn();
    }
    private void configureGoogleSignIn() {
        // Configurez les options d'authentification avec Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Créez un client GoogleSignIn avec les options configurées
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    private void setListeners() {
        binding.textCreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));

        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignInDetails()) {
                signInWithEmail();
            }
        });

        binding.buttonSignInWithGoogle.setOnClickListener(v -> signInWithGoogle());
    }



    private void signInWithEmail() {
        loading(true);
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        assert user != null;
                        checkIfUserExists(user.getEmail());

                        // Envoyer le code de vérification par e-mail
                        sendVerificationCodeByEmail(email);

                        // Vérifier si l'empreinte digitale est disponible
                        if (FingerprintUtils.isFingerprintAuthAvailable(this)) {
                            showFingerprintAuthentication();
                            // L'empreinte digitale sera gérée dans la méthode showFingerprintAuthentication()
                            startOTPActivity();
                        }
                        } else {
                        loading(false);
                        showToast("Authentication failed. Please check your credentials.");
                    }
                });
    }

    private void showFingerprintAuthentication() {
        FingerprintDialogFragment dialogFragment = new FingerprintDialogFragment();
        dialogFragment.setOnAuthenticationResultListener(new FingerprintDialogFragment.OnAuthenticationResultListener() {
            @Override
            public void onAuthenticationSuccess() {
                // L'empreinte digitale est authentifiée avec succès
                showToast("Fingerprint authentication successful!");
                startOTPActivity();
            }

            @Override
            public void onAuthenticationError() {
                // Erreur d'authentification par empreinte digitale
                showToast("Fingerprint authentication failed. Please enter the verification code received by email.");
            }
        });
        dialogFragment.show(getSupportFragmentManager(), "FingerprintDialogFragment");
    }
    private void startOTPActivity() {
        Intent intent = new Intent(getApplicationContext(), OTP.class);
        startActivity(intent);
        // Finissez l'activité SignInActivity pour qu'elle ne soit pas accessible en arrière-plan après avoir démarré l'activité OTP.
        finish();
    }
    public static String generateVerificationCode() {
        // Génère un code de vérification à six chiffres
        Random random = new Random();
        int code = random.nextInt(900000) + 100000; // Génère un nombre aléatoire entre 100000 et 999999
        return String.valueOf(code);
    }
    private void sendVerificationCodeByEmail(String email) {
        // Générer le code de vérification
        String code = generateVerificationCode();

        // Enregistrer le code dans les préférences (ou dans Firebase Realtime Database)
        preferenceManager.putString(Constants.KEY_VERIFICATION_CODE, code);

        // Envoyer l'e-mail de vérification à l'utilisateur
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // L'e-mail de vérification a été envoyé avec succès
                                showToast("Verification email sent to: " + user.getEmail());
                            } else {
                                // Erreur lors de l'envoi de l'e-mail de vérification
                                showToast("Failed to send verification email");
                            }
                        }
                    });
        }
    }


    private void checkVerificationCode(String verificationCode) {
        // Récupérez le code de vérification envoyé par e-mail et comparez-le avec celui saisi par l'utilisateur
        String sentVerificationCode = preferenceManager.getString(Constants.KEY_VERIFICATION_CODE);

        if (verificationCode.equals(sentVerificationCode)) {
            showToast("Verification successful! You are now signed in.");
            // Marquer l'authentification comme réussie
            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
        } else {
            showToast("Verification failed. Please check the code again.");
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void checkIfUserExists(String email) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // L'utilisateur existe dans la base de données
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, task.getResult().getDocuments().get(0).getId());
                        preferenceManager.putString(Constants.KEY_NAME, task.getResult().getDocuments().get(0).getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, task.getResult().getDocuments().get(0).getString(Constants.KEY_IMAGE));
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        //startPhoneNumberActivity();
                    } else {
                        // L'utilisateur doit d'abord s'inscrire
                        showToast("Please sign up before signing in");
                    }
                    loading(false);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                showToast("Failed to sign in with Google");
                loading(false);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // L'authentification avec Google a réussi
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        assert user != null;

                        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(user.getEmail()).addOnCompleteListener(emailTask -> {
                            if (emailTask.isSuccessful()) {
                                List<String> signInMethods = emailTask.getResult().getSignInMethods();
                                if (signInMethods != null && signInMethods.contains(GoogleAuthProvider.PROVIDER_ID)) {
                                    // L'utilisateur existe dans Firebase Auth et a utilisé l'authentification Google
                                    checkIfUserExists(user.getEmail());
                                    //startPhoneNumberActivity();
                                    sendVerificationCodeByEmail(user.getEmail());
                                    startOTPActivity();

                                } else {
                                    // L'utilisateur existe dans Firebase Auth mais n'a pas utilisé l'authentification Google
                                    showToast("Please sign in with your registered email");
                                    loading(false);
                                }
                            } else {
                                // Erreur lors de la récupération des méthodes d'authentification pour l'e-mail
                                showToast("Failed to sign in. Please try again.");
                                loading(false);
                            }
                        });
                    } else {
                        // L'authentification avec Google a échoué
                        showToast("Failed to sign in with Google");
                        loading(false);
                    }
                });
    }


    public void togglePasswordVisibility(View view) {
        EditText passwordEditText = findViewById(R.id.inputPassword);
        ImageView passwordVisibilityImageView = findViewById(R.id.eyeImage);

        if (passwordEditText.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            // Masquer le mot de passe
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordVisibilityImageView.setImageResource(R.drawable.baseline_visibility_off_24);
        } else {
            // Afficher le mot de passe en texte brut
            passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordVisibilityImageView.setImageResource(R.drawable.baseline_remove_red_eye_24);
        }

        // Déplacer le curseur à la fin du texte
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetails() {
        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else {
            return true;
        }
    }
    /*private void startPhoneNumberActivity() {
        Intent intent = new Intent(getApplicationContext(), phonenumber.class);
        startActivity(intent);
        finish();
    }*/

}