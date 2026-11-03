package com.example.proyecto_iotelito.data.superadmin;

import com.google.firebase.firestore.*;
import java.util.*;

/** Evento en la misma transacción que la entidad; no incluye credenciales ni información bancaria. */
public final class FirebaseAdminEvents {
    private FirebaseAdminEvents() { }
    public static void append(Transaction tx, FirebaseFirestore db, String uid, String action, String collection, String id) {
        Map<String, Object> data = new HashMap<>();
        data.put("actorUid", uid); data.put("actorRole", "SUPERADMIN");
        data.put("action", action); data.put("entityType", collection); data.put("entityId", id);
        data.put("createdAt", FieldValue.serverTimestamp());
        tx.set(db.collection("logs").document(), data);
    }
}
