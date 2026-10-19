package com.example.proyecto_iotelito.data.superadmin;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.data.superadmin.local.*;
import com.example.proyecto_iotelito.model.superadmin.ActivityLog;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.example.proyecto_iotelito.notification.SuperadminNotifications;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Todas las lecturas síncronas y escrituras ocurren en un único hilo de trabajo. */
public final class SuperadminRepository {
    public interface Callback { void complete(int id, String error); }
    private interface Write { int execute(); }
    private static volatile SuperadminRepository instance;
    private final Context context;
    private final SuperadminDatabase database;
    private final SuperadminDao dao;
    private final ExecutorService worker;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Boolean> ready = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final LiveData<List<ManagedHotel>> hotels;
    private final LiveData<List<AdminUser>> users;
    private final LiveData<List<ActivityLog>> logs;

    private SuperadminRepository(Context context) {
        this(context, SuperadminDatabase.getInstance(context), Executors.newSingleThreadExecutor());
    }

    // La inyección permite probar persistencia en un archivo separado de los datos del usuario.
    SuperadminRepository(Context context, SuperadminDatabase database, ExecutorService worker) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.worker = worker;
        dao = database.dao();
        hotels = Transformations.map(dao.observeHotels(), rows -> {
            List<ManagedHotel> result = new ArrayList<>();
            for (AdminHotelEntity row : rows) result.add(row.toModel());
            return result;
        });
        users = Transformations.map(dao.observeUsers(), rows -> {
            List<AdminUser> result = new ArrayList<>();
            for (AdminUserEntity row : rows) result.add(row.toModel());
            return result;
        });
        logs = Transformations.map(dao.observeLogs(), rows -> {
            List<ActivityLog> result = new ArrayList<>();
            for (AdminLogEntity row : rows) result.add(row.toModel());
            return result;
        });
        worker.execute(() -> {
            try {
                database.runInTransaction(this::seed);
                ready.postValue(true);
            } catch (Exception exception) {
                Log.e("SuperadminStorage", "No se pudo abrir el almacenamiento", exception);
                error.postValue("No se pudo cargar el almacenamiento local. Cierra y vuelve a abrir la aplicación.");
            }
        });
    }

    public static SuperadminRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (SuperadminRepository.class) {
                if (instance == null) instance = new SuperadminRepository(context);
            }
        }
        return instance;
    }

    private void seed() {
        boolean firstRun = dao.countUsers() == 0;
        if (firstRun) {
            for (AdminUser user : SuperadminSampleData.users()) dao.insertUser(AdminUserEntity.from(user));
        }
        if (dao.countHotels() == 0) {
            for (ManagedHotel original : SuperadminSampleData.hotels()) {
                AdminHotelEntity hotel = AdminHotelEntity.from(original);
                hotel.city = SampleData.findById(original.id).city;
                // El catálogo no trae coordenadas: se elegirán realmente en el mapa al editar.
                for (AdminUser user : SuperadminSampleData.users()) {
                    if ("Administrador".equals(user.role) && user.name.equals(hotel.administrator)) {
                        hotel.administratorId = user.id;
                        break;
                    }
                }
                dao.insertHotel(hotel);
            }
        }
        if (firstRun) addLog("Sistema", "Catálogo inicial cargado",
                "Se conservaron los 16 usuarios y 12 hoteles de demostración.");
        for (AdminHotelEntity hotel : dao.hotels()) syncAlert(hotel);
    }

    public LiveData<List<ManagedHotel>> hotels() { return hotels; }
    public LiveData<List<AdminUser>> users() { return users; }
    public LiveData<List<ActivityLog>> logs() { return logs; }
    public LiveData<List<AdminAlertEntity>> alerts() { return dao.observeAlerts(); }
    public LiveData<Boolean> ready() { return ready; }
    public LiveData<String> error() { return error; }
    public LiveData<ManagedHotel> hotel(int id) {
        return Transformations.map(dao.observeHotel(id), row -> row == null ? null : row.toModel());
    }
    public LiveData<AdminUser> user(int id) {
        return Transformations.map(dao.observeUser(id), row -> row == null ? null : row.toModel());
    }

    public void saveHotel(ManagedHotel value, Callback callback) {
        write(() -> {
            if (value.name.trim().isEmpty() || value.address.trim().isEmpty()
                    || value.city.trim().isEmpty() || value.district.trim().isEmpty()
                    || value.latitude == null || value.longitude == null
                    || !Double.isFinite(value.latitude) || !Double.isFinite(value.longitude)
                    || Math.abs(value.latitude) > 90 || Math.abs(value.longitude) > 180) {
                throw new IllegalArgumentException("Completa los datos y selecciona la ubicación del hotel.");
            }
            boolean creating = value.id == 0;
            AdminHotelEntity row = AdminHotelEntity.from(value);
            if (creating) {
                row.administratorId = null;
                row.administrator = "Sin asignar";
                row.id = (int) dao.insertHotel(row);
            } else {
                AdminHotelEntity current = requireHotel(value.id);
                row.administratorId = current.administratorId;
                row.administrator = current.administrator;
                row.rooms = current.rooms;
                row.reservations = current.reservations;
                dao.updateHotel(row);
            }
            syncAlert(row);
            addLog("Hotel", creating ? "Hotel registrado" : "Hotel actualizado", row.name);
            return row.id;
        }, callback);
    }

    public void setHotelActive(int id, boolean active, Callback callback) {
        write(() -> {
            AdminHotelEntity row = requireHotel(id);
            if (row.active != active) {
                row.active = active;
                dao.updateHotel(row);
                syncAlert(row);
                addLog("Hotel", active ? "Hotel activado" : "Hotel desactivado", row.name);
            }
            return id;
        }, callback);
    }

    public void assignAdministrator(int hotelId, int userId, Callback callback) {
        write(() -> {
            AdminHotelEntity hotel = requireHotel(hotelId);
            AdminUserEntity user = dao.user(userId);
            if (user == null || !user.active || !"Administrador".equals(user.role)) {
                throw new IllegalArgumentException("Selecciona un administrador activo.");
            }
            hotel.administratorId = user.id;
            hotel.administrator = user.name;
            dao.updateHotel(hotel);
            syncAlert(hotel);
            addLog("Hotel", "Administrador asignado", user.name + " → " + hotel.name);
            return hotelId;
        }, callback);
    }

    public void setUserActive(int id, boolean active, String reason, Callback callback) {
        write(() -> {
            AdminUserEntity user = dao.user(id);
            if (user == null) throw new IllegalArgumentException("El usuario ya no existe.");
            if (user.active != active) {
                user.active = active;
                dao.updateUser(user);
                addLog(active ? "Usuario" : "Alerta", active ? "Usuario activado" : "Usuario desactivado",
                        user.name + (reason.trim().isEmpty() ? "" : ". Motivo: " + reason.trim()));
            }
            return id;
        }, callback);
    }

    private void write(Write action, Callback callback) {
        worker.execute(() -> {
            try {
                int[] id = {0};
                database.runInTransaction(() -> { id[0] = action.execute(); });
                // Primero se confirma la transacción; luego se publican avisos.
                try {
                    publishOnWorker();
                } catch (Exception notificationError) {
                    // Un aviso fallido no convierte en fallida una escritura ya confirmada.
                    Log.w("SuperadminStorage", "El aviso se reintentará al abrir superadmin", notificationError);
                }
                main.post(() -> callback.complete(id[0], null));
            } catch (Exception exception) {
                Log.e("SuperadminStorage", "Operación no guardada", exception);
                String message = exception instanceof IllegalArgumentException ? exception.getMessage()
                        : "No se pudo guardar. Intenta nuevamente.";
                main.post(() -> callback.complete(0, message));
            }
        });
    }

    private AdminHotelEntity requireHotel(int id) {
        AdminHotelEntity row = dao.hotel(id);
        if (row == null) throw new IllegalArgumentException("El hotel ya no existe.");
        return row;
    }

    private void addLog(String type, String title, String detail) {
        AdminLogEntity log = new AdminLogEntity();
        log.type = type;
        log.title = title;
        log.detail = detail;
        log.createdAt = System.currentTimeMillis();
        dao.insertLog(log);
    }

    private void syncAlert(AdminHotelEntity hotel) {
        String key = "hotel_pending_" + hotel.id;
        if (hotel.active && hotel.administratorId == null) {
            AdminAlertEntity alert = new AdminAlertEntity();
            alert.key = key;
            alert.hotelId = hotel.id;
            alert.title = "Hotel pendiente de administrador";
            alert.message = hotel.name + " necesita un administrador asignado.";
            alert.createdAt = System.currentTimeMillis();
            dao.insertAlert(alert);
            dao.updateAlertMessage(key, alert.message);
        } else {
            dao.deleteAlert(key);
            SuperadminNotifications.cancel(context, key);
        }
    }

    public void publishPendingAlerts() { worker.execute(this::publishOnWorker); }

    private void publishOnWorker() {
        for (AdminAlertEntity alert : dao.pendingAlerts()) {
            AdminHotelEntity hotel = dao.hotel(alert.hotelId);
            if (hotel == null || !hotel.active || hotel.administratorId != null) continue;
            // El nombre puede haber cambiado desde que se creó la alerta.
            alert.message = hotel.name + " necesita un administrador asignado.";
            if (SuperadminNotifications.publish(context, alert)) dao.markNotified(alert.key);
        }
    }

    public void markAlertsRead(int hotelId) {
        worker.execute(() -> {
            dao.markRead(hotelId);
            SuperadminNotifications.cancel(context, "hotel_pending_" + hotelId);
        });
    }
}
