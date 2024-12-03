package com.example.projectapp;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText, phoneNumberEditText;
    private TextInputLayout nameInputLayout, emailInputLayout, passwordInputLayout, confirmPasswordInputLayout, phoneNumberInputLayout;
    private Button registerButton, selectImageButton;
    private TextView loginTextView;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private DatabaseReference usersRef; // Realtime Database reference
    private Uri imageUri;

    static final int PICK_IMAGE_REQUEST = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference("profile_images");
        db = FirebaseFirestore.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child("users"); // Realtime Database reference

        nameInputLayout = findViewById(R.id.nameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        phoneNumberInputLayout = findViewById(R.id.phoneNumberInputLayout);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);

        registerButton = findViewById(R.id.buttonRegister);
        selectImageButton = findViewById(R.id.buttonSelectImage);
        loginTextView = findViewById(R.id.loginTextView);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering...");
        progressDialog.setCancelable(false);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });


//-----------------------Check phone number has 10 numbers------------------------------------------

        phoneNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                String phoneNumber = s.toString();
                if (phoneNumber.length() != 10) {
                    phoneNumberInputLayout.setError("Phone number must be 10 digits long");
                } else {
                    phoneNumberInputLayout.setError(null); // Clear the error
                }
            }
        });


//-----------------------to make CAPITAL first letter in Name------------------------------------------
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Handle text change
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    String text = s.toString();
                    String capitalizedText = capitalizeWords(text);
                    if (!text.equals(capitalizedText)) {
                        nameEditText.removeTextChangedListener(this);
                        nameEditText.setText(capitalizedText);
                        nameEditText.setSelection(capitalizedText.length()); // Move cursor to end
                        nameEditText.addTextChangedListener(this);
                    }
                }
            }
        });
    }

    private String capitalizeWords(String text) {
        StringBuilder capitalized = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                capitalized.append(c);
            } else {
                if (capitalizeNext) {
                    capitalized.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    capitalized.append(c);
                }
            }
        }
        return capitalized.toString();
    }



    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
        }
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate random account number
        String accountNumber = generateAccountNumber();

        // Show progress dialog
        progressDialog.show();

        // Check if an image is selected
        if (imageUri != null) {
            // Upload image to Firebase Storage
            StorageReference fileReference = storageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(imageUri));
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            registerUserWithImage(name, email, password, phoneNumber, accountNumber, imageUrl);
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // If no image selected, register user without image
            registerUserWithoutImage(name, email, password, phoneNumber, accountNumber);
        }
    }

    private void registerUserWithoutImage(String name, String email, String password, String phoneNumber, String accountNumber) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendEmailVerification(user); // Send verification email
                            saveUserData(name, email, phoneNumber, accountNumber); // Save user data to Firestore and Realtime Database
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUserWithImage(String name, String email, String password, String phoneNumber, String accountNumber, String imageUrl) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendEmailVerification(user); // Send verification email
                            saveUserDataWithImage(name, email, phoneNumber, accountNumber, imageUrl); // Save user data to Firestore and Realtime Database
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Verification email sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to send verification email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(String name, String email, String phoneNumber, String accountNumber) {
        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phoneNumber", phoneNumber);
        userData.put("accountNumber", accountNumber);
        userData.put("points", 0); // Initialize points to 0

        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save data to Realtime Database
                        saveUserDataToRealtimeDatabase(userId, name, email, phoneNumber, accountNumber);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Failed to register: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserDataWithImage(String name, String email, String phoneNumber, String accountNumber, String imageUrl) {
        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phoneNumber", phoneNumber);
        userData.put("accountNumber", accountNumber);
        userData.put("imageUrl", imageUrl);
        userData.put("points", 0); // Initialize points to 0

        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save data to Realtime Database
                        saveUserDataToRealtimeDatabaseWithImage(userId, name, email, phoneNumber, accountNumber, imageUrl);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Failed to register: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserDataToRealtimeDatabase(String userId, String name, String email, String phoneNumber, String accountNumber) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phoneNumber", phoneNumber);
        userData.put("accountNumber", accountNumber);
        userData.put("points", 0); // Initialize points to 0

        usersRef.child(userId)
                .setValue(userData)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // Registration successful
                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish(); // Close current activity
                    } else {
                        // Registration failed
                        Toast.makeText(RegisterActivity.this, "Failed to register: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserDataToRealtimeDatabaseWithImage(String userId, String name, String email, String phoneNumber, String accountNumber, String imageUrl) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phoneNumber", phoneNumber);
        userData.put("accountNumber", accountNumber);
        userData.put("imageUrl", imageUrl);
        userData.put("points", 0); // Initialize points to 0

        usersRef.child(userId)
                .setValue(userData)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // Registration successful
                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish(); // Close current activity
                    } else {
                        // Registration failed
                        Toast.makeText(RegisterActivity.this, "Failed to register: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            int digit = random.nextInt(10);
            builder.append(digit);
        }
        return builder.toString();
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }
}
