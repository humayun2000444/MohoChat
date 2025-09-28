package com.example.mohochat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.example.mohochat.R;

public class ProfileImageLoader {

    /**
     * Load profile image into ImageView
     * Handles both Base64 images and default image
     */
    public static void loadProfileImage(ImageView imageView, String imageData) {
        if (imageData == null || imageData.isEmpty() || "default".equals(imageData)) {
            // Load default image
            imageView.setImageResource(R.drawable.ic_launcher_foreground);
        } else if (imageData.startsWith("http")) {
            // Legacy URL-based images (for backward compatibility)
            // You can use Picasso or Glide here if needed
            imageView.setImageResource(R.drawable.ic_launcher_foreground);
        } else {
            // Assume it's Base64 image
            Bitmap bitmap = ImageUtils.base64ToBitmap(imageData);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                // Fallback to default image
                imageView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
    }

    /**
     * Load profile image with custom default resource
     */
    public static void loadProfileImage(ImageView imageView, String imageData, int defaultResourceId) {
        if (imageData == null || imageData.isEmpty() || "default".equals(imageData)) {
            imageView.setImageResource(defaultResourceId);
        } else if (imageData.startsWith("http")) {
            // Legacy URL-based images
            imageView.setImageResource(defaultResourceId);
        } else {
            // Base64 image
            Bitmap bitmap = ImageUtils.base64ToBitmap(imageData);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(defaultResourceId);
            }
        }
    }

    /**
     * Load profile image with letter avatar fallback
     * Shows first letter of name if no profile image is available
     */
    public static void loadProfileImageWithLetterFallback(Context context, ImageView imageView, String imageData, String userName) {
        if (imageData == null || imageData.isEmpty() || "default".equals(imageData)) {
            // Generate letter avatar
            int size = (int) (48 * context.getResources().getDisplayMetrics().density); // 48dp in pixels
            Bitmap letterAvatar = LetterAvatarGenerator.generateLetterAvatar(context, userName, size);
            imageView.setImageBitmap(letterAvatar);
        } else if (imageData.startsWith("http")) {
            // Legacy URL-based images - fallback to letter avatar
            int size = (int) (48 * context.getResources().getDisplayMetrics().density);
            Bitmap letterAvatar = LetterAvatarGenerator.generateLetterAvatar(context, userName, size);
            imageView.setImageBitmap(letterAvatar);
        } else {
            // Base64 image
            Bitmap bitmap = ImageUtils.base64ToBitmap(imageData);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                // Fallback to letter avatar
                int size = (int) (48 * context.getResources().getDisplayMetrics().density);
                Bitmap letterAvatar = LetterAvatarGenerator.generateLetterAvatar(context, userName, size);
                imageView.setImageBitmap(letterAvatar);
            }
        }
    }

    /**
     * Load profile image with letter avatar fallback and custom size
     */
    public static void loadProfileImageWithLetterFallback(Context context, ImageView imageView, String imageData, String userName, int sizeDp) {
        if (imageData == null || imageData.isEmpty() || "default".equals(imageData)) {
            // Generate letter avatar
            int size = (int) (sizeDp * context.getResources().getDisplayMetrics().density);
            Bitmap letterAvatar = LetterAvatarGenerator.generateLetterAvatar(context, userName, size);
            imageView.setImageBitmap(letterAvatar);
        } else if (imageData.startsWith("http")) {
            // Legacy URL-based images - fallback to letter avatar
            int size = (int) (sizeDp * context.getResources().getDisplayMetrics().density);
            Bitmap letterAvatar = LetterAvatarGenerator.generateLetterAvatar(context, userName, size);
            imageView.setImageBitmap(letterAvatar);
        } else {
            // Base64 image
            Bitmap bitmap = ImageUtils.base64ToBitmap(imageData);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                // Fallback to letter avatar
                int size = (int) (sizeDp * context.getResources().getDisplayMetrics().density);
                Bitmap letterAvatar = LetterAvatarGenerator.generateLetterAvatar(context, userName, size);
                imageView.setImageBitmap(letterAvatar);
            }
        }
    }
}