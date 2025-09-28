package com.example.mohochat;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohochat.utils.NotificationHelper;

import java.util.List;

public class NotificationSoundActivity extends AppCompatActivity {

    private RecyclerView soundsRecyclerView;
    private NotificationSoundAdapter adapter;
    private List<NotificationHelper.NotificationSound> sounds;
    private int selectedPosition = 0;
    private Ringtone currentRingtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme
        SettingsActivity.applyTheme(this);

        setContentView(R.layout.activity_notification_sound);

        initViews();
        loadSounds();
        setupRecyclerView();
        setupButtons();
    }

    private void initViews() {
        soundsRecyclerView = findViewById(R.id.soundsRecyclerView);
    }

    private void loadSounds() {
        sounds = NotificationHelper.getAvailableNotificationSounds(this);

        // Find currently selected sound
        String currentSoundName = NotificationHelper.getNotificationSoundName(this);
        for (int i = 0; i < sounds.size(); i++) {
            if (sounds.get(i).getName().equals(currentSoundName)) {
                selectedPosition = i;
                break;
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new NotificationSoundAdapter();
        soundsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        soundsRecyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        findViewById(R.id.saveButton).setOnClickListener(v -> {
            if (selectedPosition >= 0 && selectedPosition < sounds.size()) {
                NotificationHelper.NotificationSound selectedSound = sounds.get(selectedPosition);
                NotificationHelper.setNotificationSound(this, selectedSound.getUri(), selectedSound.getName());
            }
            finish();
        });
    }

    private void playSound(Uri soundUri) {
        stopCurrentSound();
        currentRingtone = RingtoneManager.getRingtone(this, soundUri);
        if (currentRingtone != null) {
            currentRingtone.play();
        }
    }

    private void stopCurrentSound() {
        if (currentRingtone != null && currentRingtone.isPlaying()) {
            currentRingtone.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCurrentSound();
    }

    private class NotificationSoundAdapter extends RecyclerView.Adapter<NotificationSoundAdapter.SoundViewHolder> {

        @NonNull
        @Override
        public SoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification_sound, parent, false);
            return new SoundViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SoundViewHolder holder, int position) {
            NotificationHelper.NotificationSound sound = sounds.get(position);

            holder.soundName.setText(sound.getName());
            holder.radioButton.setChecked(position == selectedPosition);

            holder.itemView.setOnClickListener(v -> {
                int oldPosition = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
            });

            holder.playButton.setOnClickListener(v -> {
                playSound(sound.getUri());
            });
        }

        @Override
        public int getItemCount() {
            return sounds.size();
        }

        class SoundViewHolder extends RecyclerView.ViewHolder {
            RadioButton radioButton;
            TextView soundName;
            ImageView playButton;

            SoundViewHolder(@NonNull View itemView) {
                super(itemView);
                radioButton = itemView.findViewById(R.id.radioButton);
                soundName = itemView.findViewById(R.id.soundName);
                playButton = itemView.findViewById(R.id.playButton);
            }
        }
    }
}