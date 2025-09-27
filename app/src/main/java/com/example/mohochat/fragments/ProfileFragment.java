package com.example.mohochat.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mohochat.R;
import com.example.mohochat.Users;
import com.example.mohochat.login;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private CircleImageView profileImage;
    private TextView userName, userEmail, userAbout, userPhone;
    private TextView editProfile, logout;

    private DatabaseReference database;
    private FirebaseAuth auth;
    private StorageReference storage;
    private Users currentUser;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        loadUserProfile();
        setupClickListeners();
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        userAbout = view.findViewById(R.id.userAbout);
        userPhone = view.findViewById(R.id.userPhone);
        editProfile = view.findViewById(R.id.editProfile);
        logout = view.findViewById(R.id.logout);

        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
    }

    private void setupClickListeners() {
        profileImage.setOnClickListener(v -> selectImage());

        editProfile.setOnClickListener(v -> {
            // TODO: Open edit profile activity
        });

        logout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getContext(), login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        String currentUserId = auth.getCurrentUser().getUid();
        database.child("user").child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(Users.class);
                if (currentUser != null) {
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        userName.setText(currentUser.getUserName());
        userEmail.setText(currentUser.getMail());
        userAbout.setText(currentUser.getAbout());
        userPhone.setText(currentUser.getPhoneNumber());

        if (currentUser.getProfilepic() != null && !currentUser.getProfilepic().isEmpty()) {
            Picasso.get().load(currentUser.getProfilepic()).into(profileImage);
        }
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
            && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadProfileImage(imageUri);
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        String currentUserId = auth.getCurrentUser().getUid();
        StorageReference profileRef = storage.child("profile_images").child(currentUserId + ".jpg");

        profileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                database.child("user").child(currentUserId).child("profilepic").setValue(imageUrl);
                Picasso.get().load(imageUrl).into(profileImage);
                Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
        });
    }
}