package com.example.mohochat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;

import com.example.mohochat.adapters.ViewPagerAdapter;
import com.example.mohochat.utils.NotificationHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.widget.Toast;

public class MainActivityNew extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme before setting content view
        SettingsActivity.applyTheme(this);

        setContentView(R.layout.activity_main_new);

        // Check if user is logged in
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(MainActivityNew.this, login.class);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
        setupViewPager();
        setupSearchFunctionality();
        setupMoreOptionsMenu();
        updateUserOnlineStatus(true);

        // Request notification permission and initialize FCM
        NotificationHelper.requestNotificationPermission(this);
        NotificationHelper.initializeFCM();

        // Start message notification service
        Intent notificationServiceIntent = new Intent(this, com.example.mohochat.services.MessageNotificationService.class);
        startService(notificationServiceIntent);
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("CHATS");
                            break;
                        case 1:
                            tab.setText("CONTACTS");
                            break;
                        case 2:
                            tab.setText("PROFILE");
                            break;
                    }
                }).attach();
    }

    private void setupSearchFunctionality() {
        findViewById(R.id.searchIcon).setOnClickListener(v -> {
            // TODO: Implement search functionality
            // For now, we can add a simple toast
            Toast.makeText(this, "Search functionality coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupMoreOptionsMenu() {
        findViewById(R.id.moreOptionsIcon).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_settings) {
                    Intent intent = new Intent(MainActivityNew.this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.action_logout) {
                    // Logout functionality
                    auth.signOut();
                    Intent intent = new Intent(MainActivityNew.this, login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            });

            popup.show();
        });
    }

    private void updateUserOnlineStatus(boolean isOnline) {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                    .child("user").child(userId);

            userRef.child("online").setValue(isOnline);
            if (!isOnline) {
                userRef.child("lastSeen").setValue(System.currentTimeMillis());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateUserOnlineStatus(false);
    }
}