package com.example.projectapp;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class UploadActivity extends AppCompatActivity {

    private ImageView uploadImage, uploadDocument;
    private EditText uploadCaption;
    private Uri imageUrl, documentUrl;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private AlertDialog progressDialog;
    private String[] fileUrls = new String[2]; // Array to hold image and document URLs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        uploadImage = findViewById(R.id.uploadImage);
        uploadDocument = findViewById(R.id.uploadDocument);
        uploadCaption = findViewById(R.id.uploadCaption);
        findViewById(R.id.uploadButton).setOnClickListener(v -> uploadToFirebase());

        databaseReference = FirebaseDatabase.getInstance().getReference("NewPosts");
        storageReference = FirebaseStorage.getInstance().getReference();

        // Image Picker
        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        imageUrl = result.getData().getData();
                        uploadImage.setImageURI(imageUrl);
                    } else {
                        Toast.makeText(this, "No Image Selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Document Picker
        ActivityResultLauncher<Intent> documentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        documentUrl = result.getData().getData();
                        Toast.makeText(this, "Document Selected Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No Document Selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        uploadImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        uploadDocument.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            documentPickerLauncher.launch(intent);
        });

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(UploadActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void uploadToFirebase() {
        if (imageUrl == null && documentUrl == null) {
            Toast.makeText(this, "Please select an image or document", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userName = snapshot.child("name").getValue(String.class);
                    String profileImageUrl = snapshot.child("imageUrl").getValue(String.class);
                    String accountNumber = snapshot.child("accountNumber").getValue(String.class);

                    String uniquePostNumber = accountNumber + "_" + System.currentTimeMillis();
                    ArrayList<String> likedUsers = new ArrayList<>();
                    String caption = uploadCaption.getText().toString();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
                    String currentDateAndTime = sdf.format(new Date());

                    // Start uploading files
                    if (imageUrl != null) {
                        uploadFile(imageUrl, "image", uri -> {
                            fileUrls[0] = uri.toString(); // Convert Uri to String and save image URL
                            finalizePost(userName, profileImageUrl, currentDateAndTime, accountNumber, uniquePostNumber, likedUsers, caption);
                        });
                    }
                    if (documentUrl != null) {
                        uploadFile(documentUrl, "document", uri -> {
                            fileUrls[1] = uri.toString(); // Convert Uri to String and save document URL
                            finalizePost(userName, profileImageUrl, currentDateAndTime, accountNumber, uniquePostNumber, likedUsers, caption);
                        });
                    }

                } else {
                    dismissProgressDialog();
                    Toast.makeText(UploadActivity.this, "User details not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dismissProgressDialog();
                Toast.makeText(UploadActivity.this, "Error retrieving user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadFile(Uri uri, String type, OnSuccessListener<Uri> onSuccess) {
        String fileName = System.currentTimeMillis() + "_" + type + "." + getFileExtension(uri);
        StorageReference fileRef = storageReference.child(fileName);

        fileRef.putFile(uri).addOnSuccessListener(taskSnapshot ->
                fileRef.getDownloadUrl().addOnSuccessListener(onSuccess)
        ).addOnFailureListener(e -> {
            dismissProgressDialog();
            Toast.makeText(this, type + " upload failed", Toast.LENGTH_SHORT).show();
        });
    }


    private void finalizePost(String userName, String profileImageUrl, String currentDateAndTime, String accountNumber, String uniquePostNumber, ArrayList<String> likedUsers, String caption) {
        if (fileUrls[0] != null && fileUrls[1] != null) { // Ensure both URLs are available
            DataClass dataClass = new DataClass(
                    fileUrls[0], // Image URL
                    fileUrls[1], // Document URL
                    caption,
                    userName,
                    profileImageUrl,
                    currentDateAndTime,
                    accountNumber,
                    uniquePostNumber,
                    likedUsers,
                    System.currentTimeMillis()
            );

            databaseReference.child(uniquePostNumber).setValue(dataClass).addOnSuccessListener(aVoid -> {
                dismissProgressDialog();
                Toast.makeText(this, "Post uploaded successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }).addOnFailureListener(e -> {
                dismissProgressDialog();
                Toast.makeText(this, "Failed to save post", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.progress_dialog_to_upload_image, null);
        builder.setView(view).setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
