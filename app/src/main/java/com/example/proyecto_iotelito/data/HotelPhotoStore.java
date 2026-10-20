package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
import java.util.*;

/** Copias privadas reducidas: no dependen de permisos temporales de la galería. */
public final class HotelPhotoStore {
    public static final int MAX_PHOTOS = 8;
    private HotelPhotoStore() {}
    public static List<String> importPhotos(Context context, List<Uri> selected) throws IOException {
        List<String> imported = new ArrayList<>();
        File folder = new File(context.getFilesDir(), "hotel_photos");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("No se pudo crear la galería.");
        try {
            for (Uri uri : selected) {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                    BitmapFactory.decodeStream(input, null, bounds);
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("La foto no es válida.");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1400) options.inSampleSize *= 2;
                Bitmap bitmap;
                try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                    bitmap = BitmapFactory.decodeStream(input, null, options);
                }
                if (bitmap == null) throw new IOException("No se pudo abrir la foto.");
                int orientation = android.media.ExifInterface.ORIENTATION_NORMAL;
                try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                    orientation = new android.media.ExifInterface(input).getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);
                } catch (IOException ignored) { /* Algunas imágenes no contienen EXIF. */ }
                Matrix matrix = new Matrix();
                switch (orientation) {
                    case 2: matrix.setScale(-1, 1); break;
                    case 3: matrix.setRotate(180); break;
                    case 4: matrix.setScale(1, -1); break;
                    case 5: matrix.setRotate(90); matrix.postScale(-1, 1); break;
                    case 6: matrix.setRotate(90); break;
                    case 7: matrix.setRotate(270); matrix.postScale(-1, 1); break;
                    case 8: matrix.setRotate(270); break;
                    default: break;
                }
                if (!matrix.isIdentity()) {
                    Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    if (rotated != bitmap) bitmap.recycle();
                    bitmap = rotated;
                }
                File file = new File(folder, UUID.randomUUID() + ".jpg");
                imported.add(Uri.fromFile(file).toString());
                try (OutputStream output = new FileOutputStream(file)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) throw new IOException("No se pudo guardar la foto.");
                } finally { bitmap.recycle(); }
            }
            return imported;
        } catch (IOException | RuntimeException error) {
            for (String value : imported) new File(Uri.parse(value).getPath()).delete();
            throw error;
        }
    }
}
