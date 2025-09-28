package com.example.mohochat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    // Maximum image size for profile pictures (to keep Base64 string reasonable)
    private static final int MAX_WIDTH = 300;
    private static final int MAX_HEIGHT = 300;
    private static final int QUALITY = 80; // JPEG quality (0-100)

    /**
     * Convert image URI to compressed Base64 string
     */
    public static String convertImageToBase64(Context context, Uri imageUri) {
        try {
            // Get input stream from URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);

            // Convert to bitmap
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

            if (originalBitmap == null) {
                return null;
            }

            // Fix image orientation
            Bitmap rotatedBitmap = fixImageOrientation(context, imageUri, originalBitmap);

            // Compress image
            Bitmap compressedBitmap = compressBitmap(rotatedBitmap);

            // Convert to Base64
            String base64String = bitmapToBase64(compressedBitmap);

            // Clean up
            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle();
            }
            originalBitmap.recycle();
            compressedBitmap.recycle();

            return base64String;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert Base64 string back to Bitmap
     */
    public static Bitmap base64ToBitmap(String base64String) {
        try {
            if (base64String == null || base64String.isEmpty()) {
                return null;
            }

            // Remove data URL prefix if present
            if (base64String.startsWith("data:image")) {
                base64String = base64String.substring(base64String.indexOf(",") + 1);
            }

            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert Bitmap to Base64 string
     */
    private static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * Compress bitmap to reasonable size
     */
    private static Bitmap compressBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Calculate new dimensions
        float ratio = Math.min((float) MAX_WIDTH / width, (float) MAX_HEIGHT / height);

        if (ratio < 1.0f) {
            int newWidth = Math.round(width * ratio);
            int newHeight = Math.round(height * ratio);
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }

        return bitmap;
    }

    /**
     * Fix image orientation based on EXIF data
     */
    private static Bitmap fixImageOrientation(Context context, Uri imageUri, Bitmap bitmap) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            return bitmap;
        }
    }

    /**
     * Get estimated size of Base64 string in KB
     */
    public static long getBase64SizeKB(String base64String) {
        if (base64String == null) return 0;
        return (base64String.length() * 3L) / 4096; // Approximate size in KB
    }
}