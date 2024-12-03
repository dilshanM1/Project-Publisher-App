package com.example.projectapp;

import static android.app.Activity.RESULT_OK;
import static com.example.projectapp.PostAdapter.REQUEST_IMAGE_PICK;

import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import org.w3c.dom.Comment;

import java.util.ArrayList;
import java.util.Collections;

public class HomeFragment extends Fragment {




    private static final String TAG = "HomeFragment";
    private FloatingActionButton fab;
    private RecyclerView recyclerView;
    private ArrayList<DataClass> dataList;
    private PostAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private final DatabaseReference newPostReference = FirebaseDatabase.getInstance().getReference("NewPosts");
    private final DatabaseReference imagesReference = FirebaseDatabase.getInstance().getReference("Images");

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        fab = view.findViewById(R.id.fab);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        dataList = new ArrayList<>();
        adapter = new PostAdapter(requireContext(), dataList);
        recyclerView.setAdapter(adapter);

        loadPosts(); // Load images initially

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                moveNewPostsToImages();
                loadPosts();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(requireContext(), UploadActivity.class);
                startActivity(intent);
                requireActivity().finish();
            }
        });

        return view;
    }
    // Method to refresh or reload the content of the fragment


    public void refresh() {
        // Implement your refresh logic here
        swipeRefreshLayout.setRefreshing(true); // Show the refresh animation
        moveNewPostsToImages(); // Call the method to move new posts to images and refresh
    }
    private void moveNewPostsToImages() {
        newPostReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "NewPosts snapshot exists: " + snapshot.exists());
                if (snapshot.exists()) {
                    ArrayList<DataClass> tempDataList = new ArrayList<>();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                        DataClass dataClass = dataSnapshot.getValue(DataClass.class);
                        if (dataClass != null) {
                            tempDataList.add(dataClass);
                        } else {

                        }
                    }

                    // Sort the list in descending order based on timestamp
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        tempDataList.sort((data1, data2) -> Long.compare(data2.getTimestamp(), data1.getTimestamp()));
                    }

                    // Log the data to be moved
                    Log.d(TAG, "Moving data: " + tempDataList);

                    // Copy sorted data to "Images" location
                    for (DataClass dataClass : tempDataList) {
                        if (dataClass != null && dataClass.getUniquePostNumber() != null) {
                            imagesReference.child(dataClass.getUniquePostNumber()).setValue(dataClass)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Data moved to Images: " + dataClass))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to move data to Images: " + dataClass, e));
                        } else {

                        }
                    }

                    // Clear NewPosts after copying
                    newPostReference.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "NewPosts cleared");
                        } else {
                            Log.e(TAG, "Failed to clear NewPosts", task.getException());
                        }
                        // Refresh the RecyclerView by loading posts from Images
                        loadPosts();
                    });
                } else {
                    Log.d(TAG, "No new posts found in NewPosts");
                    swipeRefreshLayout.setRefreshing(false); // Stop the refresh animation if no new posts
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read NewPosts data", error.toException());
                swipeRefreshLayout.setRefreshing(false); // Stop the refresh animation
            }
        });
    }








    private void loadPosts() {
        // Query Firebase, ordering by timestamp in ascending order (oldest first)
        Query query = imagesReference.orderByChild("timestamp");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dataList.clear(); // Clear the list before adding new data

                // Loop through all posts and add them to dataList
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    DataClass dataClass = dataSnapshot.getValue(DataClass.class);
                    if (dataClass != null) {
                        dataList.add(dataClass);
                    } else {
                        Log.w(TAG, "DataClass is null for DataSnapshot: " + dataSnapshot);
                    }
                }

                // Reverse the list to show the most recent posts first (latest first)
                Collections.reverse(dataList);

                // Notify the adapter that the data has changed
                adapter.notifyDataSetChanged();

                // Optionally, scroll to the top after loading initial posts
                if (!dataList.isEmpty()) {
                    recyclerView.smoothScrollToPosition(0);  // Scroll to the top of the list
                }

                swipeRefreshLayout.setRefreshing(false); // Stop the refresh animation
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read Images data", error.toException());
                swipeRefreshLayout.setRefreshing(false); // Stop the refresh animation
            }
        });
    }


}