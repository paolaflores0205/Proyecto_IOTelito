package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.proyecto_iotelito.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Firestore es la fuente del perfil; las preferencias conservan una copia privada por UID. */
public final class PerfilRepository {

    private static final String PREFS = "perfil_cliente";
    private static final String NOMBRE = "nombre";
    private static final String EMAIL = "email";
    private static final String TELEFONO = "telefono";
    private static final String TIPO_DOCUMENTO = "tipo_documento";
    private static final String NUMERO_DOCUMENTO = "numero_documento";
    private static final String FECHA_NACIMIENTO = "fecha_nacimiento";
    private static final String DOMICILIO = "domicilio";

    private PerfilRepository() { }

    private static SharedPreferences preferencias(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String fileName = user == null ? PREFS : PREFS + "_" + user.getUid();
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    public static UserProfile cargar(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        UserProfile valoresIniciales = user == null ? SampleData.PERFIL
                : new UserProfile(user.getDisplayName() == null ? "" : user.getDisplayName(),
                user.getEmail() == null ? "" : user.getEmail(), "", "", "", "", "");
        SharedPreferences prefs = preferencias(context);
        UserProfile profile = new UserProfile(
                prefs.getString(NOMBRE, valoresIniciales.name),
                prefs.getString(EMAIL, valoresIniciales.email),
                prefs.getString(TELEFONO, valoresIniciales.phone),
                prefs.getString(TIPO_DOCUMENTO, valoresIniciales.tipoDocumento),
                prefs.getString(NUMERO_DOCUMENTO, valoresIniciales.numeroDocumento),
                prefs.getString(FECHA_NACIMIENTO, valoresIniciales.fechaNacimiento),
                prefs.getString(DOMICILIO, valoresIniciales.domicilio));
        profile.firstNames = prefs.getString("first_names", profile.name);
        profile.lastNames = prefs.getString("last_names", "");
        profile.photoUrl = prefs.getString("photo_url", "");
        return profile;
    }

    public static void guardar(Context context, UserProfile perfil) {
        preferencias(context)
                .edit()
                .putString("first_names", perfil.firstNames)
                .putString("last_names", perfil.lastNames)
                .putString("photo_url", perfil.photoUrl)
                .putString(NOMBRE, perfil.name)
                .putString(EMAIL, perfil.email)
                .putString(TELEFONO, perfil.phone)
                .putString(TIPO_DOCUMENTO, perfil.tipoDocumento)
                .putString(NUMERO_DOCUMENTO, perfil.numeroDocumento)
                .putString(FECHA_NACIMIENTO, perfil.fechaNacimiento)
                .putString(DOMICILIO, perfil.domicilio)
                .apply();
        aplicarEnMemoria(perfil);
    }

    private static void verificar(String uid) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !uid.equals(user.getUid())) throw new IllegalStateException("La sesión cambió. Vuelve a ingresar.");
    }

    public static com.google.android.gms.tasks.Task<UserProfile> cargarServidor(Context context) {
        FirebaseUser account = FirebaseAuth.getInstance().getCurrentUser();
        if (account == null) return com.google.android.gms.tasks.Tasks.forException(new IllegalStateException("Inicia sesión nuevamente."));
        String uid = account.getUid(); Context app = context.getApplicationContext();
        return com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
                .get(com.google.firebase.firestore.Source.SERVER).continueWith(task -> {
                    if (!task.isSuccessful()) throw new IllegalStateException("No se pudo cargar tu perfil. Revisa la conexión y reintenta.", task.getException());
                    verificar(uid);
                    com.google.firebase.firestore.DocumentSnapshot doc = task.getResult();
                    if (!doc.exists() || !"CLIENT".equals(doc.getString("role")) || !Boolean.TRUE.equals(doc.getBoolean("active"))
                            || !uid.equals(doc.getString("uid"))) throw new IllegalStateException("No se encontró un perfil cliente habilitado.");
                    UserProfile profile = ClientProfileCodec.decode(doc.getData()); guardar(app, profile); return profile;
                });
    }

    public static final class Estado {
        public final UserProfile perfil; public final boolean cargando; public final String error;
        Estado(UserProfile perfil, boolean cargando, String error) { this.perfil = perfil; this.cargando = cargando; this.error = error; }
    }
    public static androidx.lifecycle.LiveData<Estado> observar(Context context) {
        Context app = context.getApplicationContext();
        FirebaseUser account = FirebaseAuth.getInstance().getCurrentUser();
        if (account == null) return new androidx.lifecycle.MutableLiveData<>(new Estado(cargar(app),false,null));
        String uid = account.getUid();
        return new androidx.lifecycle.LiveData<Estado>(new Estado(cargar(app),true,null)) {
            com.google.firebase.firestore.ListenerRegistration listener;
            @Override protected void onActive() {
                listener = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
                        .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE, (doc,error) -> {
                            try {
                                verificar(uid);
                                if (error != null || doc == null || !doc.exists()) throw new IllegalStateException();
                                if (!"CLIENT".equals(doc.getString("role")) || !Boolean.TRUE.equals(doc.getBoolean("active"))) throw new IllegalStateException();
                                if (doc.getMetadata().isFromCache()) { setValue(new Estado(cargar(app),true,null)); return; }
                                UserProfile profile = ClientProfileCodec.decode(doc.getData()); guardar(app,profile);
                                setValue(new Estado(profile,false,null));
                            } catch (RuntimeException failure) { setValue(new Estado(cargar(app),false,"No se pudo actualizar el perfil. Revisa tu conexión o vuelve a ingresar.")); }
                        });
            }
            @Override protected void onInactive() { if (listener != null) listener.remove(); listener = null; }
        };
    }

    public static com.google.android.gms.tasks.Task<UserProfile> guardarRemoto(Context context, String uid,
            String first, String last, String phone, String address, String fotoPreparada) {
        final java.util.Map<String,Object> fields;
        try { verificar(uid); fields = ClientProfileCodec.editables(first,last,phone,address); }
        catch (RuntimeException error) { return com.google.android.gms.tasks.Tasks.forException(error); }
        Context app = context.getApplicationContext();
        return ClientProfilePhotos.subir(uid,fotoPreparada).onSuccessTask(photo -> {
            verificar(uid);
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            com.google.firebase.firestore.DocumentReference ref = db.collection("users").document(uid);
            return db.runTransaction(tx -> {
                verificar(uid);
                com.google.firebase.firestore.DocumentSnapshot before = tx.get(ref);
                if (!before.exists() || !"CLIENT".equals(before.getString("role")) || !Boolean.TRUE.equals(before.getBoolean("active"))
                        || !uid.equals(before.getString("uid"))) throw new IllegalStateException("Tu perfil no está habilitado.");
                java.util.Map<String,Object> updates = new java.util.HashMap<>(fields);
                updates.put("updatedAt",com.google.firebase.firestore.FieldValue.serverTimestamp());
                if (photo != null) updates.put("photoUrl",photo);
                tx.update(ref,updates);
                java.util.Map<String,Object> merged = new java.util.HashMap<>(before.getData()); merged.putAll(updates);
                return ClientProfileCodec.decode(merged);
            });
        }).onSuccessTask(profile -> {
            verificar(uid); guardar(app,profile);
            FirebaseAuth.getInstance().getCurrentUser().updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(profile.name).build());
            return com.google.android.gms.tasks.Tasks.forResult(profile);
        });
    }

    /** Mantiene compatibles las pantallas que todavía consultan SampleData.PERFIL. */
    public static void aplicarEnMemoria(UserProfile perfil) {
        UserProfile actual = SampleData.PERFIL;
        actual.firstNames = perfil.firstNames; actual.lastNames = perfil.lastNames; actual.photoUrl = perfil.photoUrl;
        actual.name = perfil.name;
        actual.email = perfil.email;
        actual.phone = perfil.phone;
        actual.tipoDocumento = perfil.tipoDocumento;
        actual.numeroDocumento = perfil.numeroDocumento;
        actual.fechaNacimiento = perfil.fechaNacimiento;
        actual.domicilio = perfil.domicilio;
    }
}
