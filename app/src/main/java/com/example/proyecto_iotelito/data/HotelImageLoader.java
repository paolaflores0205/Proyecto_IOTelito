package com.example.proyecto_iotelito.data;

import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.widget.ImageView;
import com.google.firebase.storage.FirebaseStorage;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

/** Descarga fuera de UI y evita entregar una foto a una tarjeta reciclada. */
public final class HotelImageLoader {
    private static final int LIMIT = 5 * 1024 * 1024;
    private HotelImageLoader() { }
    public static void show(ImageView view, String value) {
        Object request = new Object(); view.setTag(com.example.proyecto_iotelito.R.id.hotel_image_request, request);
        if (value == null) return;
        if (value.startsWith("gs://")) {
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(value).getBytes(LIMIT)
                        .addOnSuccessListener(bytes -> decode(view, request, bytes));
            } catch (IllegalArgumentException ignored) { }
        } else if (value.startsWith("https://")) {
            ClientLocalTasks.execute(() -> {
                HttpsURLConnection connection = null;
                try {
                    connection = (HttpsURLConnection) new java.net.URL(value).openConnection();
                    connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
                    connection.setInstanceFollowRedirects(false);
                    if (connection.getResponseCode() != 200 || connection.getContentLengthLong() > LIMIT) return;
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (InputStream input = connection.getInputStream()) {
                        byte[] buffer = new byte[8192]; int n;
                        while ((n = input.read(buffer)) != -1) {
                            if (bytes.size() + n > LIMIT) return; bytes.write(buffer, 0, n);
                        }
                    }
                    decode(view, request, bytes.toByteArray());
                } catch (IOException ignored) { }
                finally { if (connection != null) connection.disconnect(); }
            });
        } else view.setImageURI(Uri.parse(value));
    }
    private static void decode(ImageView view, Object request, byte[] bytes) {
        ClientLocalTasks.execute(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (options.outWidth <= 0 || options.outHeight <= 0) return;
            options.inJustDecodeBounds = false; options.inSampleSize = 1;
            while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1400) options.inSampleSize *= 2;
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (view.getTag(com.example.proyecto_iotelito.R.id.hotel_image_request) == request && bitmap != null) view.setImageBitmap(bitmap);
                else if (bitmap != null) bitmap.recycle();
            });
        });
    }
}
