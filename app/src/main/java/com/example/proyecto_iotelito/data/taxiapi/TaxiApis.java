package com.example.proyecto_iotelito.data.taxiapi;

import android.content.Context;
import android.os.Looper;

import com.example.proyecto_iotelito.BuildConfig;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.model.*;
import com.example.proyecto_iotelito.model.auth.*;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.*;

import java.util.concurrent.TimeUnit;

/** Selección de transporte en un único lugar; la API falsa solo existe en debug. */
public final class TaxiApis {
    private static final FakeTaxiApi.Store FAKE_STORE = new FakeTaxiApi.Store();

    private TaxiApis() {}

    public static boolean fake() {
        return BuildConfig.DEBUG && BuildConfig.TAXI_API_FAKE;
    }

    public static TaxiApi get(Context context) {
        AuthenticatedUser user = SessionRepository.obtener(context);
        if (user == null)
            throw new TaxiApiException(
                    401, TaxiApiException.Codigo.NO_AUTENTICADO, "Inicia sesión nuevamente.");
        return create(context.getApplicationContext(), user);
    }

    public static TaxiApi forDriver(FirebaseUser f) {
        return create(
                null,
                new AuthenticatedUser(
                        f.getEmail(),
                        f.getDisplayName() == null ? "Taxista" : f.getDisplayName(),
                        UserRole.TAXISTA,
                        f.getUid()));
    }

    public static TaxiApi forPushDevice(FirebaseUser firebase, UserRole role) {
        return create(null, new AuthenticatedUser(
                firebase.getEmail(), firebase.getDisplayName(), role, firebase.getUid()));
    }

    private static TaxiApi create(Context context, AuthenticatedUser user) {
        try {
            return createChecked(context, user);
        } catch (IllegalArgumentException invalid) {
            return new JsonTaxiApi() {
                protected org.json.JSONObject request(
                        String m, String r, org.json.JSONObject b, boolean a) {
                    throw TaxiApiException.noDisponible();
                }
            };
        }
    }

    private static TaxiApi createChecked(Context context, AuthenticatedUser user) {
        String uid = user.getUid() == null ? "fixture:" + user.getEmail() : user.getUid();
        if (fake())
            return new FakeTaxiApi(
                    FAKE_STORE,
                    () -> {
                        if (user.getUid() != null) {
                            FirebaseUser f = FirebaseAuth.getInstance().getCurrentUser();
                            if (f == null || !uid.equals(f.getUid()))
                                throw new TaxiApiException(
                                        401,
                                        TaxiApiException.Codigo.NO_AUTENTICADO,
                                        "La sesión cambió. Inicia sesión nuevamente.");
                        } else if (context != null) {
                            AuthenticatedUser actual = SessionRepository.obtener(context);
                            if (actual == null || !actual.getEmail().equals(user.getEmail()))
                                throw new TaxiApiException(
                                        401,
                                        TaxiApiException.Codigo.NO_AUTENTICADO,
                                        "La sesión cambió.");
                        }
                        return new FakeTaxiApi.Caller(
                                uid,
                                user.getRole().name(),
                                user.getDisplayName(),
                                user.getEmail(),
                                user.getHotelId());
                    },
                    (id, caller) -> {
                        // Lectura del dominio reservas existente. El simulador no crea ni modifica
                        // reservas.
                        if (context == null) return null;
                        Reserva r =
                                ReservaRepository.getInstance(context)
                                        .buscarPorId(Math.toIntExact(id));
                        if (r == null) return null;
                        Hotel h;
                        try {
                            h = HotelCatalogRepository.buscar(context, r.hotelId);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw TaxiApiException.noDisponible();
                        }
                        if (h == null || !h.active) return null;
                        return new FakeTaxiApi.Booking(
                                caller.uid,
                                caller.name,
                                JsonTaxiApi.json(
                                        "id",
                                        h.id,
                                        "nombre",
                                        h.name,
                                        "direccion",
                                        h.address,
                                        "ciudad",
                                        h.city));
                    });
        if (BuildConfig.TAXI_API_URL.trim().isEmpty())
            return new JsonTaxiApi() {
                protected org.json.JSONObject request(
                        String m, String r, org.json.JSONObject b, boolean a) {
                    throw TaxiApiException.noDisponible();
                }
            };
        return new HttpTaxiApi(
                BuildConfig.TAXI_API_URL,
                (refresh, remaining) -> {
                    if (Looper.myLooper() == Looper.getMainLooper())
                        throw new IllegalStateException("La API requiere un hilo de trabajo.");
                    FirebaseUser f = FirebaseAuth.getInstance().getCurrentUser();
                    if (f == null || !uid.equals(f.getUid()))
                        throw new TaxiApiException(
                                401,
                                TaxiApiException.Codigo.NO_AUTENTICADO,
                                "Inicia sesión nuevamente.");
                    String token =
                            Tasks.await(f.getIdToken(refresh), remaining, TimeUnit.MILLISECONDS)
                                    .getToken();
                    FirebaseUser actual = FirebaseAuth.getInstance().getCurrentUser();
                    if (actual == null || !uid.equals(actual.getUid()))
                        throw new TaxiApiException(
                                401, TaxiApiException.Codigo.NO_AUTENTICADO, "La sesión cambió.");
                    return token;
                });
    }
}
