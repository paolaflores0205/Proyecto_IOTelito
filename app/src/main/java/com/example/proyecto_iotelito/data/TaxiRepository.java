package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.local.cliente.TaxiDao;
import com.example.proyecto_iotelito.data.local.cliente.TaxiDatabase;
import com.example.proyecto_iotelito.data.local.cliente.TaxiEntity;
import com.example.proyecto_iotelito.data.local.cliente.TaxiLegacyMigration;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;
import com.example.proyecto_iotelito.notification.ClienteNotificationManager;
import org.json.JSONObject;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class TaxiRepository {
    public static final String EXTRA_ID = "taxi_solicitud_id";
    private static final String PREFS_ANTERIORES = "solicitudes_taxi";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public enum Estado {
        SOLICITADO(R.string.cliente_taxi_estado_solicitado), ASIGNADO(R.string.cliente_taxi_estado_asignado),
        EN_CAMINO(R.string.taxi_estado_camino), FINALIZADO(R.string.cliente_taxi_estado_finalizado);
        public final int texto;
        Estado(int texto) { this.texto = texto; }
        public boolean permite(Estado siguiente) {
            return (this == SOLICITADO && siguiente == ASIGNADO)
                    || (this == ASIGNADO && siguiente == EN_CAMINO)
                    || (this == EN_CAMINO && siguiente == FINALIZADO);
        }
    }

    private TaxiRepository() { }

    public static <T> void ejecutar(Context context, LifecycleOwner owner,
                                    Callable<T> tarea, Consumer<T> resultado) {
        Context app = context.getApplicationContext();
        MutableLiveData<Resultado<T>> live = new MutableLiveData<>();
        live.observe(owner, value -> {
            live.removeObservers(owner);
            if (value.error != null) {
                Log.e("TaxiRepository", "Error de almacenamiento de taxi", value.error);
                Toast.makeText(app, R.string.taxi_guardar_error, Toast.LENGTH_LONG).show();
            } else {
                resultado.accept(value.dato);
            }
        });
        EXECUTOR.execute(() -> {
            try {
                live.postValue(new Resultado<>(tarea.call(), null));
            } catch (Exception exception) {
                live.postValue(new Resultado<>(null, exception));
            }
        });
    }

    @WorkerThread
    private static synchronized TaxiDao dao(Context context) {
        TaxiDatabase database = TaxiDatabase.getInstance(context);
        TaxiDao dao = database.taxiDao();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_ANTERIORES, Context.MODE_PRIVATE);
        TaxiLegacyMigration.migrar(database, prefs);
        return dao;
    }

    public static LiveData<JSONObject> observar(Context context, int id) {
        return Transformations.map(TaxiDatabase.getInstance(context).taxiDao().observar(id),
                entity -> entity == null ? null : entity.comoJson());
    }

    @WorkerThread
    public static JSONObject buscar(Context context, int id) {
        TaxiEntity entity = dao(context).buscar(id);
        return entity == null ? null : entity.comoJson();
    }

    @WorkerThread
    public static int crear(Context context, Reserva reserva, String hotel, String origen,
                            String hora, int pasajeros, int maletas) {
        AuthenticatedUser user = SessionRepository.obtener(context);
        if (user == null || user.getRole() != UserRole.CLIENT) return -1;
        TaxiDao dao = dao(context);
        TaxiEntity[] guardada = new TaxiEntity[1];
        boolean[] creada = {false};
        TaxiDatabase.getInstance(context).runInTransaction(() -> {
            TaxiEntity existing = dao.porReserva(user.getEmail(), reserva.id);
            if (existing != null) {
                guardada[0] = existing;
                return;
            }
            TaxiEntity entity = new TaxiEntity();
            entity.id = dao.siguienteId();
            entity.reservaId = reserva.id;
            entity.clienteId = user.getEmail();
            entity.nombreCliente = PerfilRepository.cargar(context).name;
            entity.hotel = hotel;
            entity.origen = origen;
            entity.destino = "Aeropuerto Jorge Chávez";
            entity.hora = hora;
            entity.pasajeros = pasajeros;
            entity.maletas = maletas;
            entity.estado = Estado.SOLICITADO.name();
            entity.creado = System.currentTimeMillis();
            entity.taxistaId = "";
            entity.conductor = "";
            if (dao.insertar(entity) < 0) throw new IllegalStateException("No se pudo crear la solicitud");
            guardada[0] = entity;
            creada[0] = true;
        });
        if (creada[0]) ClienteNotificationManager.mostrarEstadoTaxi(context, guardada[0].id, reserva.id, Estado.SOLICITADO);
        return guardada[0].id;
    }

    @WorkerThread
    public static int porReserva(Context context, int reservaId) {
        AuthenticatedUser user = SessionRepository.obtener(context);
        if (user == null || user.getRole() != UserRole.CLIENT) return -1;
        TaxiEntity entity = dao(context).porReserva(user.getEmail(), reservaId);
        return entity == null ? -1 : entity.id;
    }

    @WorkerThread
    public static List<SolicitudServicio> disponibles(Context context) {
        List<SolicitudServicio> resultado = new ArrayList<>();
        AuthenticatedUser user = SessionRepository.obtener(context);
        if (user == null || user.getRole() != UserRole.TAXISTA) return resultado;
        for (TaxiEntity entity : dao(context).disponibles(user.getEmail())) resultado.add(convertir(entity.comoJson()));
        return resultado;
    }

    public static SolicitudServicio convertir(JSONObject pedido) {
        String nombre = pedido.optString("nombre");
        String detalle = pedido.optInt("pasajeros") + " pasajeros · " + pedido.optInt("maletas") + " maletas";
        LocalTime recojo = LocalTime.parse(pedido.optString("hora"));
        LocalTime ahora = LocalTime.now();
        int minutos = Math.floorMod(recojo.getHour() * 60 + recojo.getMinute()
                - ahora.getHour() * 60 - ahora.getMinute(), 24 * 60);
        return new SolicitudServicio(pedido.optInt("id"), minutos, pedido.optString("hotel"),
                pedido.optString("destino"), pedido.optString("hora"), nombre,
                nombre.isEmpty() ? "C" : nombre.substring(0, 1), detalle, 0);
    }

    @WorkerThread
    public static boolean avanzar(Context context, int id, Estado siguiente) {
        AuthenticatedUser user = SessionRepository.obtener(context);
        if (user == null || user.getRole() != UserRole.TAXISTA) return false;
        TaxiDao dao = dao(context);
        TaxiEntity pedido = dao.buscar(id);
        if (pedido == null) return false;
        Estado actual = Estado.valueOf(pedido.estado);
        if (!actual.permite(siguiente)) return false;
        if (dao.avanzar(id, actual.name(), siguiente.name(), user.getEmail(), user.getDisplayName()) != 1) return false;
        ClienteNotificationManager.mostrarEstadoTaxi(context, id, pedido.reservaId, siguiente);
        return true;
    }

    private static final class Resultado<T> {
        final T dato;
        final Exception error;
        Resultado(T dato, Exception error) {
            this.dato = dato;
            this.error = error;
        }
    }
}
