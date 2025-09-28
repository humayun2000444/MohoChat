package com.example.mohochat.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohochat.ChatActivity;
import com.example.mohochat.R;
import com.example.mohochat.models.Contact;
import com.example.mohochat.utils.ImageUtils;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private Context context;
    private ArrayList<Contact> contactsList;
    private Fragment fragment;

    // Random colors for letter avatars
    private static final int[] AVATAR_COLORS = {
        0xFF9F68FF, // Purple
        0xFFFF9F68, // Orange
        0xFF68FF9F, // Green
        0xFF68A5FF, // Blue
        0xFFFF6868, // Red
        0xFF68FFFF, // Cyan
        0xFFFFB74D, // Light Orange
        0xFF9CCC65, // Light Green
        0xFFBA68C8, // Light Purple
        0xFF4FC3F7, // Light Blue
        0xFFFFD54F, // Yellow
        0xFFFF8A65, // Coral
        0xFF81C784, // Medium Green
        0xFF64B5F6, // Medium Blue
        0xFFF06292, // Pink
        0xFFAED581  // Lime Green
    };

    public ContactsAdapter(Context context, ArrayList<Contact> contactsList, Fragment fragment) {
        this.context = context;
        this.contactsList = contactsList;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactsList.get(position);

        // Set contact name - ensure it's never null or empty
        String displayName = contact.getName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "Unknown Contact";
        }
        holder.contactName.setText(displayName);

        // Set phone number - ensure it's never null
        String phoneNumber = contact.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            phoneNumber = "No phone number";
        }
        holder.contactPhone.setText(phoneNumber);

        if (contact.isHasApp()) {
            // Contact has app
            holder.contactStatus.setText("On MohoChat");
            holder.contactStatus.setTextColor(context.getResources().getColor(R.color.primary_accent));
            holder.onlineIndicator.setVisibility(View.VISIBLE);
            holder.inviteButton.setVisibility(View.GONE);

            // Ensure name and phone text are visible for app users
            holder.contactName.setTextColor(context.getResources().getColor(R.color.primary_text));
            holder.contactPhone.setTextColor(context.getResources().getColor(R.color.secondary_text));

            // Load profile picture or letter avatar
            loadContactImage(holder.profileImage, contact);

            // Click to open chat
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("receiverId", contact.getUserId());
                intent.putExtra("receiverName", contact.getName());
                context.startActivity(intent);
            });

        } else {
            // Contact doesn't have app
            holder.contactStatus.setText("Not on MohoChat");
            holder.contactStatus.setTextColor(context.getResources().getColor(R.color.secondary_text));
            holder.onlineIndicator.setVisibility(View.GONE);
            holder.inviteButton.setVisibility(View.VISIBLE);

            // Ensure name and phone text are visible for regular contacts
            holder.contactName.setTextColor(context.getResources().getColor(R.color.primary_text));
            holder.contactPhone.setTextColor(context.getResources().getColor(R.color.secondary_text));

            // Show letter avatar
            loadContactImage(holder.profileImage, contact);

            // Invite button click
            holder.inviteButton.setOnClickListener(v -> {
                // TODO: Implement SMS invitation
                android.widget.Toast.makeText(context,
                    "Invite " + contact.getName() + " to MohoChat",
                    android.widget.Toast.LENGTH_SHORT).show();
            });

            // Click to show contact info
            holder.itemView.setOnClickListener(v -> {
                android.widget.Toast.makeText(context,
                    contact.getName() + "\n" + contact.getPhoneNumber(),
                    android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadContactImage(CircleImageView imageView, Contact contact) {
        // Check if contact has profile picture
        if (contact.isHasApp() && contact.getProfilePic() != null &&
            !contact.getProfilePic().isEmpty() && !"default".equals(contact.getProfilePic())) {

            // Try to load profile picture
            Bitmap profileBitmap = ImageUtils.base64ToBitmap(contact.getProfilePic());
            if (profileBitmap != null) {
                imageView.setImageBitmap(profileBitmap);
                return;
            }
        }

        // Generate letter avatar with random color
        Bitmap letterAvatar = generateLetterAvatar(contact.getName());
        imageView.setImageBitmap(letterAvatar);
    }

    private Bitmap generateLetterAvatar(String name) {
        // Get first letter
        String letter = "?";
        if (name != null && !name.trim().isEmpty()) {
            letter = name.trim().substring(0, 1).toUpperCase();
        }

        // Choose random color based on name hash
        int colorIndex = Math.abs(name != null ? name.hashCode() : 0) % AVATAR_COLORS.length;
        int backgroundColor = AVATAR_COLORS[colorIndex];

        // Create bitmap
        int size = 150; // Size in pixels
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw circle background
        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(backgroundColor);

        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, backgroundPaint);

        // Draw letter
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(size * 0.4f);
        textPaint.setFakeBoldText(true);

        // Center text vertically
        Rect textBounds = new Rect();
        textPaint.getTextBounds(letter, 0, letter.length(), textBounds);

        float x = radius;
        float y = radius + (textBounds.height() / 2f);
        canvas.drawText(letter, x, y, textPaint);

        return bitmap;
    }

    @Override
    public int getItemCount() {
        return contactsList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView contactName, contactPhone, contactStatus;
        View onlineIndicator;
        Button inviteButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            contactName = itemView.findViewById(R.id.contactName);
            contactPhone = itemView.findViewById(R.id.contactPhone);
            contactStatus = itemView.findViewById(R.id.contactStatus);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            inviteButton = itemView.findViewById(R.id.smsButton);
        }
    }
}