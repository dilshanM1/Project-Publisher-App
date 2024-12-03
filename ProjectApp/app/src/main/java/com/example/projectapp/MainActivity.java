package com.example.projectapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    BottomNavigationView bottomNavigationView;
    HomeFragment homeFragment;
    ProfileFragment profileFragment;
    SettingFragment settingFragment;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Initialize fragments
        homeFragment = new HomeFragment();
        settingFragment = new SettingFragment();
        profileFragment = new ProfileFragment();



        // Set default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.home); // Set default selection
        }

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.flFragment);
                if (currentFragment instanceof HomeFragment) {
                    // Do nothing to prevent exiting the app
                } else {
                    // Navigate to the HomeFragment when the back button is pressed while on other fragments
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.flFragment, homeFragment)
                            .commit();
                    bottomNavigationView.setSelectedItemId(R.id.home);
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.home) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.flFragment);
            if (currentFragment instanceof HomeFragment) {
                // If already on HomeFragment, refresh its content
                ((HomeFragment) currentFragment).refresh(); // Call refresh method in HomeFragment
            } else {
                // Replace fragment if not already on HomeFragment
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.flFragment, homeFragment)
                        .commit();
            }
            return true;
        } else if (item.getItemId() == R.id.person) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, profileFragment)
                    .commit();
            return true;
        } else if (item.getItemId() == R.id.settings) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, settingFragment)
                    .commit();
            return true;
        }


        return false;
    }
}
