package com.example.mohochat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;

import com.example.mohochat.R;

public class LetterAvatarGenerator {

    // Material Design color palette for avatars
    private static final int[] COLORS = {
            0xFF9F68FF, // Purple (secondary_accent)
            0xFFFF9F68, // Orange (primary_accent)
            0xFF68FF9F, // Green
            0xFF9F68FF, // Light purple
            0xFFFF6868, // Red
            0xFF68FFFF, // Cyan
            0xFFFFFF68, // Yellow
            0xFFFF68FF, // Magenta
            0xFF68A5FF, // Blue
            0xFFA568FF, // Indigo
            0xFF9CCC65, // Light green
            0xFFFFB74D  // Orange
    };

    /**
     * Generate a circular letter avatar bitmap
     */
    public static Bitmap generateLetterAvatar(Context context, String name, int size) {
        if (name == null || name.trim().isEmpty()) {
            name = "?";
        }

        // Get first letter and make it uppercase
        String letter = name.trim().substring(0, 1).toUpperCase();

        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Choose color based on first letter
        int colorIndex = Math.abs(letter.hashCode()) % COLORS.length;
        int backgroundColor = COLORS[colorIndex];

        // Create circle paint
        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(backgroundColor);

        // Draw circle background
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, backgroundPaint);

        // Create text paint
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Calculate text size (about 40% of the circle size)
        float textSize = size * 0.4f;
        textPaint.setTextSize(textSize);
        textPaint.setFakeBoldText(true);

        // Get text bounds to center vertically
        Rect textBounds = new Rect();
        textPaint.getTextBounds(letter, 0, letter.length(), textBounds);

        // Draw text in center
        float x = radius;
        float y = radius + (textBounds.height() / 2f);
        canvas.drawText(letter, x, y, textPaint);

        return bitmap;
    }

    /**
     * Generate letter avatar with custom color
     */
    public static Bitmap generateLetterAvatar(Context context, String name, int size, int backgroundColor) {
        if (name == null || name.trim().isEmpty()) {
            name = "?";
        }

        String letter = name.trim().substring(0, 1).toUpperCase();

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Create circle paint
        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(backgroundColor);

        // Draw circle background
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, backgroundPaint);

        // Create text paint
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(size * 0.4f);
        textPaint.setFakeBoldText(true);

        // Get text bounds to center vertically
        Rect textBounds = new Rect();
        textPaint.getTextBounds(letter, 0, letter.length(), textBounds);

        // Draw text in center
        float x = radius;
        float y = radius + (textBounds.height() / 2f);
        canvas.drawText(letter, x, y, textPaint);

        return bitmap;
    }

    /**
     * Get a consistent color for a given name
     */
    public static int getColorForName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return COLORS[0];
        }

        String letter = name.trim().substring(0, 1).toUpperCase();
        int colorIndex = Math.abs(letter.hashCode()) % COLORS.length;
        return COLORS[colorIndex];
    }
}