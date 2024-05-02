package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivitySignUpBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;

    private static final int MIN_PASSWORD_LENGTH = 8;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        setListeners();
        ImageView passwordVisibilityImageView1 = findViewById(R.id.eyeImage1);
        passwordVisibilityImageView1.setImageResource(R.drawable.baseline_visibility_off_24);

        ImageView passwordVisibilityImageView2 = findViewById(R.id.eyeImage2);
        passwordVisibilityImageView2.setImageResource(R.drawable.baseline_visibility_off_24);
    }

    private void setListeners() {
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        loading(true);

        String name = binding.inputName.getText().toString().trim();
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String userId = firebaseAuth.getCurrentUser().getUid();
                            saveUserInfo(userId, name, email, encodedImage);
                            loading(false);
                            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                            preferenceManager.putString(Constants.KEY_USER_ID, userId);
                            preferenceManager.putString(Constants.KEY_NAME, name);
                            preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            loading(false);
                            showToast("Sign up failed: " + task.getException().getMessage());
                        }
                    }
                });
    }

    private void saveUserInfo(String userId, String name, String email, String image) {
        Map<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, name);
        user.put(Constants.KEY_EMAIL, email);
        user.put(Constants.KEY_IMAGE, image);

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (!task.isSuccessful()) {
                            showToast("Failed to save user information");
                        }
                    }
                });
    }

    public void togglePasswordVisibility1(View view) {
        EditText passwordEditText = findViewById(R.id.inputPassword);
        ImageView passwordVisibilityImageView = findViewById(R.id.eyeImage1);

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

    public void togglePasswordVisibility2(View view) {
        EditText passwordEditText = findViewById(R.id.inputConfirmPassword);
        ImageView passwordVisibilityImageView = findViewById(R.id.eyeImage2);

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

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private boolean isValidSignUpDetails() {
        if (encodedImage == null) {
            showToast("Select profile image");
            return false;
        } else if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        } else if (!isValidName(binding.inputName.getText().toString())) {
            showToast("Invalid name");
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!isValidEmail(binding.inputEmail.getText().toString())) {
            showToast("Enter valid email");
            return false;
        } else if (isEmailAlreadyExists(binding.inputEmail.getText().toString())) {
            showToast("Email already exists");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Confirm your password");
            return false;
        } else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())) {
            showToast("Password & confirm password must be the same");
            return false;
        } else if (binding.inputPassword.getText().toString().length() < MIN_PASSWORD_LENGTH) {
            showToast("Password must have a minimum length of " + MIN_PASSWORD_LENGTH + " characters");
            return false;
        } else if (!containsDigit(binding.inputPassword.getText().toString())) {
            showToast("Password must contain at least 1 digit");
            return false;
        } else if (!containsSpecialCharacter(binding.inputPassword.getText().toString())) {
            showToast("Password must contain at least 1 special character");
            return false;
        } else if (!isPasswordSafe(binding.inputPassword.getText().toString(), binding.inputName.getText().toString())) {
            showToast("Password cannot contain the username");
            return false;
        } else {
            return true;
        }
    }

    private boolean containsSpecialCharacter(String password) {
        String specialCharacters = "~`!@#$%^&*()-_=+\\|[{]};:'\",<.>/?";
        for (char c : password.toCharArray()) {
            if (specialCharacters.contains(String.valueOf(c))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPasswordSafe(String password, String username) {
        return !password.toLowerCase().contains(username.toLowerCase());
    }

    private boolean containsUpperCaseLetter(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDigit(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }

    private boolean isValidName(String name) {
        // Vérifiez si le nom n'est pas vide et s'il respecte les critères de validation souhaités
        if (name.isEmpty()) {
            return false;
        }

        // Vérifiez si le nom contient uniquement des lettres (majuscules et minuscules) et des espaces
        if (!name.matches("[a-zA-Z\\s]+")) {
            return false;
        }

        // Vérifiez si le nom a une longueur minimale de 2 caractères
        return name.length() >= 2;
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }




    private boolean isEmailAlreadyExists(String email) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        // Vérification de l'email
        Query query = database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .limit(1);

        Task<QuerySnapshot> task = query.get();
        try {
            QuerySnapshot querySnapshot = Tasks.await(task);
            if (!querySnapshot.isEmpty()) {
                // L'email existe déjà dans la base de données
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Gérer l'erreur ici
        }

        return false;
    }
}

