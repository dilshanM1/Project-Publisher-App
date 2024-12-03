package com.example.projectapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collections;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.MyViewHolder> {
    static final int REQUEST_IMAGE_PICK = 1;

    private Uri selectedImageUri;
    private ImageView previewImageView;
    private ArrayList<DataClass> dataList;
    private Context context;
    private SharedPreferences sharedPreferences;
    private String userId;
    private String loggedInUserAccountNumber; // Variable to store logged-in user's account number


    public PostAdapter(Context context, ArrayList<DataClass> dataList) {
        this.context = context;
        this.dataList = dataList;
        this.sharedPreferences = context.getSharedPreferences("LIKES", Context.MODE_PRIVATE);
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getLoggedInUserAccountNumber(); // Call method to get logged-in user's account number
        Collections.reverse(dataList); // Reverse the dataList
    }

    public void addNewPost(DataClass newData) {
        dataList.add(0, newData);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DataClass data = dataList.get(dataList.size() - position - 1);

        // Load image using Glide
        Glide.with(context).load(data.getImageURL()).into(holder.recyclerImage);

        // Set caption text
        holder.recyclerCaption.setText(data.getCaption());

        // Set user name text
        holder.nameTextView.setText(data.getUserName());

        // Load profile image using Glide
        Glide.with(context).load(data.getProfileImageURL()).into(holder.profileImageView);

        // Set upload time
        holder.uploadTimeTextView.setText(data.getUploadDateTime());

        holder.fileOpenId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String documentURL = data.getDocumentURL(); // Assume DataClass has a method to get the document URL
                if (documentURL != null && !documentURL.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(documentURL));
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "No document available for this post", Toast.LENGTH_SHORT).show();
                }
            }
        });


//------------------------------------------------------------------------------------------------------------
        // Load like count from the LikeCounts node
        DatabaseReference likeCountsReference = FirebaseDatabase.getInstance().getReference().child("LikeCounts").child(data.getUniquePostNumber());
        likeCountsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int likeCount = dataSnapshot.getValue(Integer.class);
                    holder.likeCountTextView.setText(String.valueOf(likeCount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Set click listener for the like button
        holder.likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLikeAction(data.getUniquePostNumber(), holder.likeCountTextView, holder.likeButton);
            }
        });
//------------------------------------------------------------------------------------------------------------


        // Get the account number associated with the post
        String postAccountNumber = data.getAccountNumber();

        // Check if the post belongs to the logged-in user
        if (postAccountNumber.equals(loggedInUserAccountNumber)) {
            // Show the "More" button
            holder.moreButton.setVisibility(View.VISIBLE);

            // Set click listener for More button
            holder.moreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Show the dialog box for burning the post
                    showMoreDialog(data.getUniquePostNumber(), data.getAccountNumber(), holder.burnCountTextView, data.getUniquePostNumber());
                }
            });
        } else {
            // Hide the "More" button
            holder.moreButton.setVisibility(View.GONE);
        }
//-------------------------------------------------------------------------------------------------

        // Check if the current user has liked the post and set the like button state
        DatabaseReference likesReference = FirebaseDatabase.getInstance().getReference().child("Likes").child(data.getUniquePostNumber());
        likesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(userId)) {
                    // User has liked this post
                    holder.likeButton.setImageResource(R.drawable.baseline_thumb_up_24_blue); // Change to your liked icon
                } else {
                    // User has not liked this post
                    holder.likeButton.setImageResource(R.drawable.baseline_thumb_up_off_alt_24); // Change to your like icon
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle potential errors here
                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

// Set click listener for like button
        holder.likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Play sound effect
                MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.like_sound);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release(); // Release the MediaPlayer resource when the sound is done playing
                    }
                });
                mediaPlayer.start();

                // Handle the like action
                handleLikeAction(data.getUniquePostNumber(), holder.likeCountTextView, holder.likeButton);
            }
        });

//-------------------------------------------------------------------------------------------------
        // Set click listener for burn button
//        holder.burnButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                // Debug log for uniquePostNumber
//                Log.d("DeletePost", "Post uniquePostNumber: " + data.getUniquePostNumber());
//                // Show the dialog box for burning the post
//                showBurnDialog(data.getUniquePostNumber(), data.getAccountNumber(), holder.burnCountTextView);
//                saveSelectedPoints(0);
//            }
//        });

        // Load burn count from Firebase Database
        DatabaseReference burnsReference = FirebaseDatabase.getInstance().getReference().child("Burns").child(data.getUniquePostNumber());
        burnsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Update burn count TextView with actual value
                    int burnCount = dataSnapshot.child("burnCount").getValue(Integer.class);
                    holder.burnCountTextView.setText(String.valueOf(burnCount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle potential errors here
                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

//    private void showBurnDialog(String uniquePostNumber, String accountNumber, TextView burnCountTextView) {
//        // Inflate the layout for the dialog box
//        View dialogView = LayoutInflater.from(context).inflate(R.layout.activity_bottom_navigation_to_burn, null);
//
//        // Create a dialog instance
//        Dialog dialog = new Dialog(context);
//        dialog.setContentView(dialogView);
//
//        Button burnNowButton = dialogView.findViewById(R.id.BurnNowButton);
//        Button point5Button = dialogView.findViewById(R.id.Point5Button);
//        Button point10Button = dialogView.findViewById(R.id.Point10Button);
//        Button point15Button = dialogView.findViewById(R.id.Point15Button);
//        Button point20Button = dialogView.findViewById(R.id.Point20Button);
//        Button point25Button = dialogView.findViewById(R.id.Point25Button);
//        Button point30Button = dialogView.findViewById(R.id.Point30Button);
//
//        // Store the currently selected button
//        final Button[] selectedButton = {null};
//
//        // Set click listener for the "BURN NOW" button in the dialog
//        burnNowButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Perform action when the "BURN NOW" button is clicked
//                burnPoints(uniquePostNumber, accountNumber, burnCountTextView);
//
//                // Dismiss the dialog
//                dialog.dismiss();
//
//            }
//
//        });
//
//        // Update selectedPoints based on the button clicked
//        View.OnClickListener pointButtonClickListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Reset the background of the previously selected button
//                if (selectedButton[0] != null) {
//                    selectedButton[0].setBackgroundResource(R.drawable.rounded_ckground);
//                }
//
//                // Update selectedPoints based on the button clicked
//                Button clickedButton = (Button) v;
//                int selectedPoints = Integer.parseInt(clickedButton.getText().toString().replace("P", ""));
//
//                // Set the new selected button and change its background
//                selectedButton[0] = clickedButton;
//                clickedButton.setBackgroundResource(R.drawable.red_outline);
//
//                // Save selectedPoints for burning
//                saveSelectedPoints(selectedPoints);
//            }
//        };
//
//        point5Button.setOnClickListener(pointButtonClickListener);
//        point10Button.setOnClickListener(pointButtonClickListener);
//        point15Button.setOnClickListener(pointButtonClickListener);
//        point20Button.setOnClickListener(pointButtonClickListener);
//        point25Button.setOnClickListener(pointButtonClickListener);
//        point30Button.setOnClickListener(pointButtonClickListener);
//
//        // Apply custom animation
//        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
//        dialog.getWindow().setGravity(Gravity.BOTTOM);
//        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        // Show the dialog
//        dialog.show();
//    }



    // Method to burn points from user's data location and store them under the corresponding account number in the "Burns" data location
    private void burnPoints(String uniquePostNumber, String accountNumber, TextView burnCountTextView) {
        // Retrieve user's data from the database
        DatabaseReference userDataReference = FirebaseDatabase.getInstance().getReference().child("users").child(userId);

        userDataReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the current point count from the user's data
                    int currentPoints = dataSnapshot.child("points").getValue(Integer.class);

                    // Ensure the user has enough points to burn
                    int selectedPoints = getSelectedPoints();
                    if (currentPoints >= selectedPoints) {
                        // Calculate the new point count after burning points
                        int newPoints = currentPoints - selectedPoints;

                        // Update the user's data with the new point count
                        userDataReference.child("points").setValue(newPoints);

                        // Retrieve the existing burned points from the "Burns" data location
                        DatabaseReference burnsReference = FirebaseDatabase.getInstance().getReference().child("Burns").child(uniquePostNumber);

                        burnsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot burnSnapshot) {
                                int existingBurnPoints = 0;
                                if (burnSnapshot.exists()) {
                                    existingBurnPoints = burnSnapshot.child("burnCount").getValue(Integer.class);
                                }

                                // Add the new burn points to the existing burned points
                                int updatedBurnPoints = existingBurnPoints + selectedPoints;

                                // Store the updated burned points in the "Burns" data location
                                burnsReference.child("burnCount").setValue(updatedBurnPoints);
                                burnsReference.child("accountNumber").setValue(accountNumber);

                                // Display toast message if selectedPoints is greater than 0
                                if (selectedPoints > 0) {
                                    Toast.makeText(context, "Points burned successfully.", Toast.LENGTH_SHORT).show();
                                }

                                // Update the burn count TextView
                                burnCountTextView.setText(String.valueOf(updatedBurnPoints));
                                // Display toast message
                              //  Toast.makeText(context, "Points burned successfully.", Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Handle potential errors here
                                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Handle case where the user does not have enough points to burn
                        // You can display a message to the user indicating insufficient points
                        Toast.makeText(context, "Insufficient points to burn.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle potential errors here
                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to save selected points for burning
    private void saveSelectedPoints(int selectedPoints) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("SELECTED_POINTS", selectedPoints);
        editor.apply();
    }

    // Method to retrieve selected points for burning
    private int getSelectedPoints() {
        return sharedPreferences.getInt("SELECTED_POINTS", 0); // Default value is 5
    }

    // Method to get the user's ID
    private String getUserId() {
        return userId;
    }

    // Method to get the account number of the logged-in user
    private void getLoggedInUserAccountNumber() {
        DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve the account number of the logged-in user
                    loggedInUserAccountNumber = dataSnapshot.child("accountNumber").getValue(String.class);
                    notifyDataSetChanged(); // Notify adapter once account number is fetched
                } else {
                    Log.d("TAG", "User data does not exist");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle potential errors here
                Log.d("TAG", "Error: " + databaseError.getMessage());
            }
        });
    }

    // Method to show "More" dialog
    private void showMoreDialog(String uniquePostNumber, String accountNumber, TextView burnCountTextView, String postUniqueNumber) {
        // Inflate the layout for the dialog box
        View dialogView = LayoutInflater.from(context).inflate(R.layout.more_button_bottom_navigation, null);

        // Create a dialog instance
        Dialog dialog = new Dialog(context);
        dialog.setContentView(dialogView);

        // Find views in dialogView
        TextView deleteTextView = dialogView.findViewById(R.id.deleteTxtId);
        TextView editPostTextView = dialogView.findViewById(R.id.editTxtId);
        TextView cancelTextView = dialogView.findViewById(R.id.cancelTxtId);
        // click listener for deleteTextView
        deleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show delete confirmation dialog
                showDeleteConfirmationDialog(uniquePostNumber, accountNumber, burnCountTextView, postUniqueNumber);
                // Dismiss the dialog after showing the confirmation dialog
                dialog.dismiss();
            }
        });


        // Click listener for editPostTextView
        editPostTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show edit dialog
                showEditDialog(uniquePostNumber);
                dialog.dismiss();
            }
        });

        // Click listener for cancelTextView
        cancelTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Show the dialog
        // Apply custom animation
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }
//-----------------------------------showEditDialog----------------------------------------------------------
    private void showEditDialog(String uniquePostNumber) {
        // Inflate the layout for the edit dialog
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_post, null);

        // Create a dialog instance
        Dialog dialog = new Dialog(context);
        dialog.setContentView(dialogView);

        // Find views in dialogView
        ImageView previewImageView = dialogView.findViewById(R.id.previewImageView);
        EditText editCaption = dialogView.findViewById(R.id.editCaptionEditText);
        Button saveButton = dialogView.findViewById(R.id.saveChangesButton);
        ImageButton selectImageButton = dialogView.findViewById(R.id.selectImageButton);

        // Load current post data
        DatabaseReference postReference = FirebaseDatabase.getInstance().getReference().child("Images").child(uniquePostNumber);
        postReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String currentImageUrl = dataSnapshot.child("imageURL").getValue(String.class);
                    String currentCaption = dataSnapshot.child("caption").getValue(String.class);

                    // Display current image and caption
                    Glide.with(context).load(currentImageUrl).into(previewImageView);
                    editCaption.setText(currentCaption);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Set click listener for saveButton
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newCaption = editCaption.getText().toString().trim();
                if (selectedImageUri != null) {
                    // Upload the image to Firebase Storage and update the post
                    uploadImageAndSavePost(uniquePostNumber, newCaption);
                } else {
                    // Save updated post data without changing the image
                    saveUpdatedPost(uniquePostNumber, null, newCaption);
                }

                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Set click listener for selectImageButton
        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open gallery to select an image
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                ((Activity) context).startActivityForResult(intent, REQUEST_IMAGE_PICK);
            }
        });

        // Show the dialog
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }




    private void uploadImageAndSavePost(String uniquePostNumber, String newCaption) {
        if (selectedImageUri != null) {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Images").child(uniquePostNumber);

            storageReference.putFile(selectedImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String newImageUrl = uri.toString();
                                    Log.d("PostAdapter", "Image URL: " + newImageUrl);
                                    saveUpdatedPost(uniquePostNumber, newImageUrl, newCaption);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("PostAdapter", "Failed to get download URL: " + e.getMessage());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.e("PostAdapter", "selectedImageUri is null");
            Toast.makeText(context, "Image selection failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }


//-----------------------------------saveUpdatedPost----------------------------------------------------------


    private void saveUpdatedPost(String uniquePostNumber, String newImageUrl, String newCaption) {
        DatabaseReference postReference = FirebaseDatabase.getInstance().getReference().child("Images").child(uniquePostNumber);

        if (newImageUrl != null) {
            postReference.child("imageURL").setValue(newImageUrl)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d("PostAdapter", "Image URL updated successfully");
                            } else {
                                Log.e("PostAdapter", "Failed to update image URL: " + task.getException().getMessage());
                            }
                        }
                    });
        }

        postReference.child("caption").setValue(newCaption)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Post updated successfully", Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();
                        } else {
                            Toast.makeText(context, "Failed to update post: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }



//-----------------------------------deletePost----------------------------------------------------------

    // Method to delete the post from the database
    private void deletePost(String uniquePostNumber) {
        if (uniquePostNumber == null || uniquePostNumber.isEmpty()) {

            Toast.makeText(context, "Invalid post number. Cannot delete the post.", Toast.LENGTH_SHORT).show();
            return;
        }



        // Firebase path to the post
        DatabaseReference postReference = FirebaseDatabase.getInstance().getReference().child("Images").child(uniquePostNumber);


        // Adding listener for debugging purposes
        postReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {


                    postReference.removeValue(new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                // Handle error

                                Toast.makeText(context, "Error deleting post: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                // Post deleted successfully

                                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show();

                                // Remove the post from the dataList and notify the adapter
                                for (int i = 0; i < dataList.size(); i++) {
                                    if (dataList.get(i).getUniquePostNumber().equals(uniquePostNumber)) {
                                        dataList.remove(i);
                                        notifyItemRemoved(i);
                                        Log.d("DeletePost", "Removed post from dataList and updated RecyclerView");
                                        break;
                                    }
                                }
                            }
                        }
                    });
                } else {

                    Toast.makeText(context, "Post does not exist.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(context, "Error retrieving post: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Method to show delete confirmation dialog
    private void showDeleteConfirmationDialog(String uniquePostNumber, String accountNumber, TextView burnCountTextView, String postUniqueNumber) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Are you sure you want to delete?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Delete the post
                        deletePost(uniquePostNumber);
                        // Dismiss the dialog
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Dismiss the dialog
                        dialog.dismiss();
                    }
                });
        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

//------------------------------------------------------------------------------------------------------

    // Method to handle like action
    private void handleLikeAction(String uniquePostNumber, TextView likeCountTextView, ImageButton likeButton) {
        DatabaseReference likesReference = FirebaseDatabase.getInstance().getReference().child("Likes").child(uniquePostNumber);
        likesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(userId)) {
                    // User has liked this post, so unlike it
                    likesReference.child(userId).removeValue();
                    likeButton.setImageResource(R.drawable.baseline_thumb_up_off_alt_24); // Change to your like icon
                } else {
                    // User has not liked this post, so like it
                    likesReference.child(userId).setValue(true);
                    likeButton.setImageResource(R.drawable.baseline_thumb_up_24_blue); // Change to your liked icon
                }
                updateLikeCount(uniquePostNumber, likeCountTextView);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLikeCount(String uniquePostNumber, TextView likeCountTextView) {
        DatabaseReference likesReference = FirebaseDatabase.getInstance().getReference().child("Likes").child(uniquePostNumber);
        likesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int likeCount = (int) dataSnapshot.getChildrenCount();

                // Update the like count in the LikeCounts node
                DatabaseReference likeCountsReference = FirebaseDatabase.getInstance().getReference().child("LikeCounts").child(uniquePostNumber);
                likeCountsReference.setValue(likeCount);

                // Update the TextView with the new like count
                likeCountTextView.setText(String.valueOf(likeCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }





//------------------------------------------------------------------------------------------------------








    public static class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView recyclerImage, profileImageView;
        TextView recyclerCaption, nameTextView, uploadTimeTextView, likeCountTextView, burnCountTextView;
        ImageButton likeButton, burnButton, moreButton,fileOpenId;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerImage = itemView.findViewById(R.id.recyclerImage);
            recyclerCaption = itemView.findViewById(R.id.recyclerCaption);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            uploadTimeTextView = itemView.findViewById(R.id.uploadTimeTextView);
            likeCountTextView = itemView.findViewById(R.id.likeCountTextView);
            likeButton = itemView.findViewById(R.id.likeButton);

            fileOpenId = itemView.findViewById(R.id.fileOpenId);

            moreButton = itemView.findViewById(R.id.moreButton);

        }
    }
}
//correct