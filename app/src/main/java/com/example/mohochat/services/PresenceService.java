package com.example.mohochat.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class PresenceService extends Service {
    private static final String TAG = "PresenceService";
    private static final long ONLINE_TIMEOUT = 2 * 60 * 1000; // 2 minutes in milliseconds

    private DatabaseReference database;
    private FirebaseAuth auth;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private String currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        timeoutHandler = new Handler(Looper.getMainLooper());

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

        Log.d(TAG, "PresenceService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (currentUserId != null) {
            setUserOnline();
            resetTimeout();
        }
        return START_STICKY; // Restart if killed by system
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentUserId != null) {
            setUserOffline();
        }
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
        Log.d(TAG, "PresenceService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is an unbound service
    }

    public void updateUserActivity() {
        if (currentUserId != null) {
            setUserOnline();
            resetTimeout();
        }
    }

    private void setUserOnline() {
        if (currentUserId == null) return;

        long currentTime = System.currentTimeMillis();

        // Update user's online status
        Map<String, Object> updates = new HashMap<>();
        updates.put("user/" + currentUserId + "/isOnline", true);
        updates.put("user/" + currentUserId + "/lastSeen", currentTime);

        // Also update presence node for real-time tracking
        updates.put("presence/" + currentUserId + "/status", "online");
        updates.put("presence/" + currentUserId + "/lastSeen", currentTime);
        updates.put("presence/" + currentUserId + "/timestamp", currentTime);

        database.updateChildren(updates)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "User set as online"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to set user online", e));
    }

    private void setUserOffline() {
        if (currentUserId == null) return;

        long currentTime = System.currentTimeMillis();

        // Update user's offline status
        Map<String, Object> updates = new HashMap<>();
        updates.put("user/" + currentUserId + "/isOnline", false);
        updates.put("user/" + currentUserId + "/lastSeen", currentTime);

        // Also update presence node
        updates.put("presence/" + currentUserId + "/status", "offline");
        updates.put("presence/" + currentUserId + "/lastSeen", currentTime);
        updates.put("presence/" + currentUserId + "/timestamp", currentTime);

        database.updateChildren(updates)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "User set as offline"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to set user offline", e));
    }

    private void resetTimeout() {
        // Remove any existing timeout
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }

        // Create new timeout runnable
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "User inactive for 2 minutes, setting offline");
                setUserOffline();
            }
        };

        // Set new timeout
        timeoutHandler.postDelayed(timeoutRunnable, ONLINE_TIMEOUT);
    }

    // Static method to update activity from other parts of the app
    public static void notifyUserActivity() {
        // This would need to be implemented with a bound service or other mechanism
        // For now, we'll handle this in the activities directly
    }
}