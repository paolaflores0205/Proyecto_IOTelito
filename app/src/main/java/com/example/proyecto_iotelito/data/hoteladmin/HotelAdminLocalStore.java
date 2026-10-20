package com.example.proyecto_iotelito.data.hoteladmin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.model.Mensaje;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import com.example.proyecto_iotelito.model.hoteladmin.ItemConsumo;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Persistencia local de habitaciones, servicios, checkout, mensajes y galería.
 * El perfil compartido del hotel vive en Room de superadmin. */
public final class HotelAdminLocalStore {
    private static final String PREFS = "hoteladmin_local";
    private static final String PHOTOS_FILE = "hoteladmin_fotos.json";
    private static volatile HotelAdminLocalStore instance;

    private final Context context;
    private final SharedPreferences prefs;
    private final HotelAdminDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Consumer<Exception>> pending = new ArrayList<>();
    private boolean loading;
    private boolean loaded;

    private HotelAdminLocalStore(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        dao = HotelAdminDatabase.get(this.context).dao();
    }

    public static HotelAdminLocalStore get(Context context) {
        if (instance == null) {
            synchronized (HotelAdminLocalStore.class) {
                if (instance == null) instance = new HotelAdminLocalStore(context);
            }
        }
        return instance;
    }

    /** Carga los datos antes de mostrar las pestañas. El callback corre en el hilo UI. */
    public void load(Consumer<Exception> callback) {
        if (loaded) {
            main.post(() -> callback.accept(null));
            return;
        }
        pending.add(callback);
        if (loading) return;
        loading = true;
        io.execute(() -> {
            try {
                if (!prefs.getBoolean("seeded", false)) {
                    List<HabitacionEntity> habitaciones = new ArrayList<>();
                    for (Habitacion h : HotelAdminSampleData.habitaciones()) {
                        habitaciones.add(HabitacionEntity.from(context, h));
                    }
                    List<ServicioEntity> servicios = new ArrayList<>();
                    for (Servicio s : HotelAdminSampleData.servicios()) {
                        servicios.add(ServicioEntity.from(context, s));
                    }
                    HotelAdminDatabase.get(context).runInTransaction(() -> {
                        dao.guardarHabitaciones(habitaciones);
                        dao.guardarServicios(servicios);
                    });
                    prefs.edit().putBoolean("seeded", true).apply();
                }
                List<Habitacion> habitaciones = new ArrayList<>();
                for (HabitacionEntity e : dao.habitaciones()) habitaciones.add(e.toModel(context));
                List<Servicio> servicios = new ArrayList<>();
                for (ServicioEntity e : dao.servicios()) servicios.add(e.toModel(context));
                List<CheckoutEntity> checkouts = dao.checkouts();
                List<Integer> fotos = leerFotosSeguro();
                main.post(() -> {
                    HotelAdminSampleData.habitaciones().clear();
                    HotelAdminSampleData.habitaciones().addAll(habitaciones);
                    HotelAdminSampleData.servicios().clear();
                    HotelAdminSampleData.servicios().addAll(servicios);
                    if (fotos != null && fotos.size() >= 4) {
                        HotelAdminSampleData.FOTOS.clear();
                        HotelAdminSampleData.FOTOS.addAll(fotos);
                    }
                    aplicarCheckouts(checkouts);
                    SuperadminRepository.HotelDetails legacy = legacyProfile();
                    SuperadminRepository.getInstance(context).loadHotelDetailsForAdmin(
                            HotelAdminSampleData.HOTEL_ID, legacy, (hotel, profileError) -> {
                                if (profileError == null) {
                                    HotelAdminSampleData.aplicarHotelCompartido(hotel);
                                    if (legacy != null) prefs.edit()
                                            .putBoolean("profile_migrated_to_superadmin", true).apply();
                                }
                                finishLoad(profileError == null ? null
                                        : new IllegalStateException(profileError));
                            });
                });
            } catch (Exception error) {
                main.post(() -> finishLoad(error));
            }
        });
    }

    private void finishLoad(Exception error) {
        loading = false;
        loaded = error == null;
        List<Consumer<Exception>> callbacks = new ArrayList<>(pending);
        pending.clear();
        for (Consumer<Exception> callback : callbacks) callback.accept(error);
    }

    public void saveHabitacion(Habitacion habitacion, Consumer<Exception> callback) {
        HabitacionEntity entity = HabitacionEntity.from(context, habitacion);
        write(() -> dao.guardarHabitacion(entity), callback);
    }

    public void deleteHabitacion(int id, Consumer<Exception> callback) {
        write(() -> dao.eliminarHabitacion(id), callback);
    }

    public void saveServicio(Servicio servicio, Consumer<Exception> callback) {
        ServicioEntity entity = ServicioEntity.from(context, servicio);
        write(() -> dao.guardarServicio(entity), callback);
    }

    public void deleteServicio(int id, Consumer<Exception> callback) {
        write(() -> dao.eliminarServicio(id), callback);
    }

    public void saveCheckout(ReservaAdmin reserva, Consumer<Exception> callback) {
        CheckoutEntity entity = new CheckoutEntity();
        entity.reservaId = reserva.id;
        entity.estado = reserva.estado.name();
        JSONArray items = new JSONArray();
        try {
            for (ItemConsumo item : reserva.consumos) {
                JSONObject value = new JSONObject();
                value.put("concepto", item.concepto);
                value.put("monto", item.monto);
                value.put("motivo", item.motivo);
                value.put("observacion", item.observacion);
                items.put(value);
            }
        } catch (JSONException error) {
            callback.accept(error);
            return;
        }
        entity.consumosJson = items.toString();
        write(() -> dao.guardarCheckout(entity), callback);
    }

    public void loadMessages(int conversacionId, Consumer<List<Mensaje>> onSuccess,
                             Consumer<Exception> onError) {
        io.execute(() -> {
            try {
                List<Mensaje> messages = new ArrayList<>();
                for (MensajeAdminEntity entity : dao.mensajes(conversacionId)) {
                    String fecha = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(new Date(entity.enviadoEn));
                    messages.add(new Mensaje(entity.texto, false, fecha));
                }
                main.post(() -> onSuccess.accept(messages));
            } catch (Exception error) {
                main.post(() -> onError.accept(error));
            }
        });
    }

    public void saveMessage(int conversacionId, String texto, Consumer<Exception> callback) {
        MensajeAdminEntity entity = new MensajeAdminEntity();
        entity.conversacionId = conversacionId;
        entity.texto = texto;
        entity.enviadoEn = System.currentTimeMillis();
        write(() -> dao.guardarMensaje(entity), callback);
    }

    public void saveFotos(Consumer<Exception> callback) {
        JSONArray photos = new JSONArray();
        for (int photo : HotelAdminSampleData.FOTOS) {
            photos.put(context.getResources().getResourceEntryName(photo));
        }
        String json = photos.toString();
        write(() -> {
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    context.openFileOutput(PHOTOS_FILE, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
                writer.write(json);
            } catch (IOException error) {
                throw new IllegalStateException(error);
            }
        }, callback);
    }

    private void write(Runnable operation, Consumer<Exception> callback) {
        io.execute(() -> {
            Exception error = null;
            try {
                operation.run();
            } catch (Exception failure) {
                error = failure;
            }
            Exception result = error;
            main.post(() -> callback.accept(result));
        });
    }

    private SuperadminRepository.HotelDetails legacyProfile() {
        if (prefs.getBoolean("profile_migrated_to_superadmin", false)
                || !prefs.contains("nombre")) return null;
        return new SuperadminRepository.HotelDetails(
                prefs.getString("nombre", null), prefs.getString("direccion", null),
                prefs.getString("descripcion", null), prefs.getString("atracciones", null),
                prefs.getString("telefono", null), prefs.getString("email", null));
    }

    private List<Integer> leerFotos() throws IOException, JSONException {
        StringBuilder data = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.openFileInput(PHOTOS_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) data.append(line);
        } catch (FileNotFoundException missing) {
            return null;
        }
        JSONArray names = new JSONArray(data.toString());
        List<Integer> photos = new ArrayList<>();
        for (int i = 0; i < names.length(); i++) {
            int resource = context.getResources().getIdentifier(
                    names.getString(i), "drawable", context.getPackageName());
            if (resource != 0) photos.add(resource);
        }
        return photos;
    }

    private List<Integer> leerFotosSeguro() {
        try {
            return leerFotos();
        } catch (IOException | JSONException invalidPhotos) {
            return null;
        }
    }

    private void aplicarCheckouts(List<CheckoutEntity> checkouts) {
        for (CheckoutEntity checkout : checkouts) {
            for (ReservaAdmin reserva : HotelAdminSampleData.reservas()) {
                if (reserva.id != checkout.reservaId) continue;
                try {
                    ReservaAdmin.EstadoCheckout estado =
                            ReservaAdmin.EstadoCheckout.valueOf(checkout.estado);
                    JSONArray items = new JSONArray(checkout.consumosJson);
                    List<ItemConsumo> consumos = new ArrayList<>();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        consumos.add(new ItemConsumo(item.getString("concepto"),
                                item.getDouble("monto"), item.optString("motivo"),
                                item.optString("observacion")));
                    }
                    reserva.estado = estado;
                    reserva.consumos.clear();
                    reserva.consumos.addAll(consumos);
                } catch (JSONException | IllegalArgumentException ignored) {
                    // Se conserva la reserva de ejemplo si un dato guardado es inválido.
                }
                break;
            }
        }
    }
}
