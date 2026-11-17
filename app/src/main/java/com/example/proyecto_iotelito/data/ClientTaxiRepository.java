package com.example.proyecto_iotelito.data;

import android.content.Context;
import androidx.annotation.WorkerThread;
import com.example.proyecto_iotelito.data.taxiapi.TaxiApiException;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.json.JSONObject;

/** Consulta por REST el servicio asociado a cada reserva propia. */
public final class ClientTaxiRepository {
    private ClientTaxiRepository() { }

    @WorkerThread public static List<JSONObject> listar(Context context) {
        AuthenticatedUser user = SessionRepository.obtener(context);
        requireClient(context, user);
        List<JSONObject> taxis = new ArrayList<>();
        for (Reserva reserva : ReservaRepository.getInstance(context).obtenerTodas()) {
            requireClient(context, user);
            JSONObject pedido = TaxiRepository.buscar(context, reserva.id);
            if (pedido != null) taxis.add(pedido);
        }
        requireClient(context, user);
        taxis.sort((a, b) -> Long.compare(b.optLong("creado"), a.optLong("creado")));
        return taxis;
    }

    private static void requireClient(Context context, AuthenticatedUser user) {
        AuthenticatedUser current = SessionRepository.obtener(context);
        if (user == null || current == null || current.getRole() != UserRole.CLIENT
                || !Objects.equals(user.getUid(), current.getUid())
                || !Objects.equals(user.getEmail(), current.getEmail())) {
            throw new TaxiApiException(401, TaxiApiException.Codigo.NO_AUTENTICADO, "Inicia sesión como cliente.");
        }
    }
}
