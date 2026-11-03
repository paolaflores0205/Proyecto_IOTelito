package com.example.proyecto_iotelito.data.superadmin;

import android.net.Uri;
import com.google.android.gms.tasks.*;
import com.google.firebase.storage.*;
import java.util.*;

/** Sube las copias JPEG privadas; Firestore conserva rutas gs sin tokens públicos. */
public final class HotelCloudPhotos {
    private HotelCloudPhotos() { }
    public static Task<List<String>> upload(String uid, List<String> photos, List<StorageReference> uploaded) {
        Task<List<String>> chain = Tasks.forResult(new ArrayList<>());
        for (String value : photos) {
            chain = chain.onSuccessTask(result -> {
                if (value.startsWith("file:") || value.startsWith("content:")) {
                    StorageReference ref = FirebaseStorage.getInstance().getReference()
                            .child("hotelPhotos/" + uid + "/" + UUID.randomUUID() + ".jpg");
                    uploaded.add(ref);
                    return ref.putFile(Uri.parse(value), new StorageMetadata.Builder().setContentType("image/jpeg").build())
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) return Tasks.forException(task.getException());
                                result.add(ref.toString()); return Tasks.forResult(result);
                            });
                }
                result.add(value); return Tasks.forResult(result);
            });
        }
        return chain;
    }
    public static void discard(List<StorageReference> uploaded) {
        for (StorageReference ref : uploaded) ref.delete();
    }
}
