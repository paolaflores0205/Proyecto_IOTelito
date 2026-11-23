package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxiapi.*;
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
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public enum Estado {
        SOLICITADO(R.string.cliente_taxi_estado_solicitado),
        ASIGNADO(R.string.cliente_taxi_estado_asignado),
        EN_CAMINO(R.string.taxi_estado_camino),
        FINALIZADO(R.string.cliente_taxi_estado_finalizado);
        public final int texto;

        Estado(int texto) {
            this.texto = texto;
        }

        public boolean permite(Estado siguiente) {
            return (this == SOLICITADO && siguiente == ASIGNADO)
                    || (this == ASIGNADO && siguiente == EN_CAMINO)
                    || (this == EN_CAMINO && siguiente == FINALIZADO);
        }
    }

    private TaxiRepository() {}

    public static <T> void ejecutar(
            Context context, LifecycleOwner owner, Callable<T> tarea, Consumer<T> resultado) {
        Context app = context.getApplicationContext();
        MutableLiveData<Resultado<T>> live = new MutableLiveData<>();
        live.observe(
                owner,
                value -> {
                    live.removeObservers(owner);
                    if (value.error != null) {
                        Log.e("TaxiRepository", "Error de almacenamiento de taxi", value.error);
                        if (context instanceof android.app.Activity)
                            com.example.proyecto_iotelito.ui.taxi.TaxiUi.status(
                                    (android.app.Activity) context, TaxiTasks.message(value.error));
                        else
                            Toast.makeText(app, TaxiTasks.message(value.error), Toast.LENGTH_LONG)
                                    .show();
                    } else {
                        resultado.accept(value.dato);
                    }
                });
        EXECUTOR.execute(
                () -> {
                    try {
                        live.postValue(new Resultado<>(tarea.call(), null));
                    } catch (Exception exception) {
                        live.postValue(new Resultado<>(null, exception));
                    }
                });
    }

    public static LiveData<JSONObject> observar(Context context, int id) {
        Context app = context.getApplicationContext();
        return new MutableLiveData<JSONObject>() {
            final android.os.Handler main =
                    new android.os.Handler(android.os.Looper.getMainLooper());
            boolean active;
            int generation;
            final Runnable tick =
                    new Runnable() {
                        public void run() {
                            if (!active) return;
                            int g = generation;
                            EXECUTOR.execute(
                                    () -> {
                                        try {
                                            JSONObject p = buscar(app, id);
                                            main.post(
                                                    () -> {
                                                        if (active && g == generation) {
                                                            setValue(p);
                                                            main.postDelayed(this, 5000);
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            main.post(
                                                    () -> {
                                                        if (active && g == generation) {
                                                            android.widget.Toast.makeText(
                                                                            app,
                                                                            TaxiTasks.message(e),
                                                                            android.widget.Toast
                                                                                    .LENGTH_LONG)
                                                                    .show();
                                                            main.postDelayed(this, 5000);
                                                        }
                                                    });
                                        }
                                    });
                        }
                    };

            protected void onActive() {
                active = true;
                generation++;
                main.post(tick);
            }

            protected void onInactive() {
                active = false;
                generation++;
                main.removeCallbacks(tick);
            }
        };
    }

    @WorkerThread
    public static JSONObject buscar(Context context, int id) {
        AuthenticatedUser user = SessionRepository.obtener(context);
        if (user == null)
            throw new TaxiApiException(
                    401, TaxiApiException.Codigo.NO_AUTENTICADO, "Inicia sesión nuevamente.");
        try {
            return TaxiCodec.legacy(
                    TaxiApis.get(context).verServicio(id),
                    user.getEmail(),
                    user.getRole() == UserRole.CLIENT);
        } catch (TaxiApiException e) {
            if (e.status == 404) return null;
            throw e;
        }
    }

    @WorkerThread
    public static int crear(
            Context context,
            Reserva reserva,
            String hotel,
            String origen,
            String hora,
            int pasajeros,
            int maletas) {
        String codigo = null;
        try {
            com.example.proyecto_iotelito.model.Hotel h =
                    HotelCatalogRepository.buscar(context, reserva.hotelId);
            org.json.JSONArray a =
                    TaxiApis.get(context).listarAeropuertos().optJSONArray("aeropuertos");
            for (int i = 0; i < a.length(); i++) {
                JSONObject airport = a.optJSONObject(i);
                org.json.JSONArray cities = airport.optJSONArray("ciudadesHotel");
                for (int j = 0; j < cities.length(); j++)
                    if (h.city.trim()
                            .toLowerCase(java.util.Locale.ROOT)
                            .equals(cities.optString(j))) codigo = airport.optString("codigo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw TaxiApiException.noDisponible();
        }
        if (codigo == null)
            throw new TaxiApiException(
                    400,
                    TaxiApiException.Codigo.AEROPUERTO_NO_CORRESPONDE,
                    "No hay aeropuerto disponible para la ciudad del hotel.");
        return crear(context, reserva, codigo, hora, pasajeros, maletas);
    }

    @WorkerThread
    public static int crear(
            Context context,
            Reserva reserva,
            String aeropuerto,
            String hora,
            int pasajeros,
            int maletas) {
        // El monto mínimo lo define cada hotel; se relee del servidor por si el admin lo cambió.
        com.example.proyecto_iotelito.model.Hotel hotelReserva = null;
        try {
            hotelReserva = HotelCatalogRepository.buscar(context, reserva.hotelId);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException sinHotel) {
            Log.w("TaxiRepository", "No se pudo leer el hotel; se usa el monto mínimo por defecto.");
        }
        if (!TaxiBenefitPolicy.puedeSolicitar(reserva, hotelReserva))
            throw new TaxiApiException(409, TaxiApiException.Codigo.ESTADO_INVALIDO,
                    context.getString(R.string.taxi_requisito_estadia, TaxiBenefitPolicy.texto(hotelReserva)));
        AuthenticatedUser user = SessionRepository.obtener(context);
        if (user == null || user.getRole() != UserRole.CLIENT) return -1;
        JSONObject service =
                TaxiApis.get(context)
                        .crearServicio(reserva.id, aeropuerto, hora, pasajeros, maletas);
        int id = Math.toIntExact(service.optLong("id"));
        ClienteNotificationManager.mostrarEstadoTaxi(context, id, reserva.id, Estado.SOLICITADO);
        return id;
    }

    @WorkerThread
    public static int porReserva(Context context, int reservaId) {
        return buscar(context, reservaId) == null ? -1 : reservaId;
    }

    @WorkerThread
    public static List<SolicitudServicio> disponibles(Context context) {
        List<SolicitudServicio> rows = new ArrayList<>();
        org.json.JSONArray a = TaxiApis.get(context).listarDisponibles().optJSONArray("servicios");
        for (int i = 0; i < a.length(); i++)
            rows.add(convertir(TaxiCodec.legacy(a.optJSONObject(i), "", false)));
        return rows;
    }

    public static SolicitudServicio convertir(JSONObject pedido) {
        String nombre = pedido.optString("nombre");
        String detalle =
                pedido.optInt("pasajeros")
                        + " pasajeros · "
                        + pedido.optInt("maletas")
                        + " maletas";
        LocalTime recojo = LocalTime.parse(pedido.optString("hora"));
        LocalTime ahora = LocalTime.now();
        int minutos =
                Math.floorMod(
                        recojo.getHour() * 60
                                + recojo.getMinute()
                                - ahora.getHour() * 60
                                - ahora.getMinute(),
                        24 * 60);
        return new SolicitudServicio(
                pedido.optInt("id"),
                minutos,
                pedido.optString("hotel"),
                pedido.optString("destino"),
                pedido.optString("hora"),
                nombre,
                nombre.isEmpty() ? "C" : nombre.substring(0, 1),
                detalle,
                0);
    }

    @WorkerThread
    public static boolean avanzar(Context context, int id, Estado siguiente) {
        TaxiApi api = TaxiApis.get(context);
        if (siguiente == Estado.ASIGNADO) api.aceptarServicio(id);
        else if (siguiente == Estado.EN_CAMINO) api.avanzarServicio(id, "EN_CAMINO");
        else
            throw new TaxiApiException(
                    409,
                    TaxiApiException.Codigo.ESTADO_INVALIDO,
                    "El servicio requiere validar el QR para finalizar.");
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
