package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.SystemClock;
import android.util.LruCache;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Fotos de hotel en el bucket privado de AWS, mediante la misma API que las fotos de perfil. */
public final class HotelAwsPhotos {
    private static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final long URL_TTL_MS = 90_000;
    private static final Map<String, CachedUrl> URLS = new HashMap<>();
    private static final LruCache<String, byte[]> BYTES = new LruCache<String, byte[]>(16 * 1024 * 1024) {
        @Override protected int sizeOf(String key, byte[] value) { return value.length; }
    };
    private HotelAwsPhotos() { }

    private static final class CachedUrl {
        final String url; final long expires;
        CachedUrl(String url, long expires) { this.url = url; this.expires = expires; }
    }

    /**
     * Sube en orden las fotos locales (file: o content:) y conserva las remotas.
     * hotelId nulo = catálogo del superadmin; si no, carpeta del hotel asignado al administrador.
     */
    public static Task<List<String>> upload(Context context, Integer hotelId, List<String> photos) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException("Inicia sesión nuevamente."));
        String uid = user.getUid();
        Context app = context.getApplicationContext();
        return user.getIdToken(false).onSuccessTask(result -> Tasks.call(IO, () -> {
            List<String> stored = new ArrayList<>();
            for (String value : photos) {
                if (value == null || !(value.startsWith("file:") || value.startsWith("content:"))) {
                    stored.add(value); continue;
                }
                sameSession(uid);
                File file = jpegWithinLimit(app, Uri.parse(value));
                try {
                    JSONObject body = new JSONObject().put("contentType", "image/jpeg").put("sizeBytes", file.length());
                    if (hotelId != null) body.put("hotelId", hotelId.intValue());
                    JSONObject signed = AwsProfilePhotoApi.solicitar("/hotel-photos/upload-url", result.getToken(), body);
                    String reference = signed.getString("reference");
                    if (hotelId == null ? !HotelPhotoPolicy.reference(reference)
                            || !reference.contains("/hotelPhotos/" + HotelPhotoPolicy.CATALOG + "/")
                            : !HotelPhotoPolicy.ofHotel(reference, hotelId))
                        throw new AwsProfilePhotoApi.PhotoException("No se pudo preparar la foto del hotel.");
                    sameSession(uid);
                    AwsProfilePhotoApi.subir(file, signed);
                    stored.add(reference);
                } finally {
                    if (file.getParentFile() != null && "hotel-upload".equals(file.getParentFile().getName())) file.delete();
                }
            }
            sameSession(uid);
            return stored;
        }));
    }

    /** Descarga fuera de la UI; devuelve null si la foto no está disponible. */
    static void load(String reference, java.util.function.Consumer<byte[]> done) {
        byte[] cached;
        synchronized (BYTES) { cached = BYTES.get(reference); }
        if (cached != null) { done.accept(cached); return; }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !HotelPhotoPolicy.reference(reference)) return;
        user.getIdToken(false).addOnSuccessListener(result -> IO.execute(() -> {
            try {
                byte[] bytes;
                try { bytes = AwsProfilePhotoApi.descargar(signedUrl(reference, result.getToken())); }
                catch (AwsProfilePhotoApi.PhotoException expired) {
                    synchronized (URLS) { URLS.remove(reference); }
                    bytes = AwsProfilePhotoApi.descargar(signedUrl(reference, result.getToken()));
                }
                synchronized (BYTES) { BYTES.put(reference, bytes); }
                done.accept(bytes);
            } catch (Exception ignored) { /* Se mantiene la imagen anterior o el marcador. */ }
        }));
    }

    private static String signedUrl(String reference, String token) throws Exception {
        long now = SystemClock.elapsedRealtime();
        synchronized (URLS) {
            CachedUrl cached = URLS.get(reference);
            if (cached != null && cached.expires > now) return cached.url;
        }
        JSONObject signed = AwsProfilePhotoApi.solicitar("/hotel-photos/read-url", token,
                new JSONObject().put("reference", reference));
        String url = signed.getString("url");
        if (!ProfilePhotoPolicy.destinoS3(url)) throw new AwsProfilePhotoApi.PhotoException("Enlace de foto inválido.");
        synchronized (URLS) { URLS.put(reference, new CachedUrl(url, now + URL_TTL_MS)); }
        return url;
    }

    /** Las copias locales ya son JPEG reducidos; si alguna supera el límite de S3 se recomprime. */
    private static File jpegWithinLimit(Context context, Uri uri) throws IOException {
        if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
            File file = new File(uri.getPath());
            if (file.isFile() && file.length() > 0 && file.length() <= ProfilePhotoPolicy.MAX_BYTES) return file;
        }
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null) throw new AwsProfilePhotoApi.PhotoException("Una de las fotos no es válida.");
        File folder = new File(context.getCacheDir(), "hotel-upload");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("No se pudo preparar la foto.");
        File out = new File(folder, UUID.randomUUID() + ".jpg");
        try {
            for (int quality : new int[]{85, 75, 60}) {
                Bitmap scaled = bitmap;
                if (quality == 60 && Math.max(bitmap.getWidth(), bitmap.getHeight()) > 1400) {
                    float factor = 1400f / Math.max(bitmap.getWidth(), bitmap.getHeight());
                    scaled = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.getWidth() * factor),
                            Math.round(bitmap.getHeight() * factor), true);
                }
                try (OutputStream output = new FileOutputStream(out)) {
                    if (!scaled.compress(Bitmap.CompressFormat.JPEG, quality, output))
                        throw new IOException("No se pudo convertir la foto.");
                } finally { if (scaled != bitmap) scaled.recycle(); }
                if (out.length() > 0 && out.length() <= ProfilePhotoPolicy.MAX_BYTES) return out;
            }
        } finally { bitmap.recycle(); }
        out.delete();
        throw new AwsProfilePhotoApi.PhotoException("Una de las fotos es demasiado grande.");
    }

    private static void sameSession(String uid) {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null || !uid.equals(current.getUid()))
            throw new IllegalStateException("La sesión cambió durante la carga. Inicia sesión de nuevo.");
    }

    /** Mensaje legible para la pantalla, sin detalles internos. */
    public static String message(Exception error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof AwsProfilePhotoApi.PhotoException || cause instanceof IllegalStateException)
                return cause.getMessage();
            cause = cause.getCause();
        }
        return null;
    }
}
