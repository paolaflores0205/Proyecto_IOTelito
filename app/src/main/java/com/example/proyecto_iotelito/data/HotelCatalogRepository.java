package com.example.proyecto_iotelito.data;

import android.content.Context;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.hoteladmin.HotelAdminFirestoreCodec;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.data.superadmin.local.AdminHotelEntity;
import com.example.proyecto_iotelito.data.superadmin.local.SuperadminDatabase;
import com.example.proyecto_iotelito.model.Attraction;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Comparte catálogo e inventario de Firestore con sesión; conserva ejemplos para uso local sin sesión. */
public final class HotelCatalogRepository {
    private HotelCatalogRepository() { }
    public static LiveData<List<Hotel>> observar(Context context) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            String[] lastError = {null};
            return Transformations.map(new com.example.proyecto_iotelito.data.superadmin.FirebaseHotelsRepository(context, false).observe(), state -> {
                if (state.error != null && !state.error.equals(lastError[0])
                        && ConexionCliente.disponible(context))
                    android.widget.Toast.makeText(context.getApplicationContext(), state.error, android.widget.Toast.LENGTH_LONG).show();
                lastError[0] = state.error;
                List<Hotel> result = new ArrayList<>();
                for (ManagedHotel row : state.hotels) result.add(combinar(row));
                return result;
            });
        }
        return Transformations.map(SuperadminRepository.getInstance(context).hotels(), rows -> {
            List<Hotel> result = new ArrayList<>();
            for (ManagedHotel row : rows) result.add(combinar(row));
            return result;
        });
    }
    @WorkerThread public static Hotel buscar(Context context, int id) throws InterruptedException {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            com.example.proyecto_iotelito.data.superadmin.FirebaseHotelsRepository remote =
                    new com.example.proyecto_iotelito.data.superadmin.FirebaseHotelsRepository(context, false);
            ManagedHotel row = remote.find(id);
            if (row == null) return null;
            Hotel hotel = combinar(row);
            hotel.inventory = remote.rooms(id);
            hotel.hasInventory = row.hasInventory && !hotel.inventory.isEmpty();
            return hotel;
        }
        SuperadminRepository.getInstance(context).awaitInitialization();
        AdminHotelEntity row = SuperadminDatabase.getInstance(context).dao().hotel(id);
        return row == null ? null : combinar(row.toModel());
    }

    @WorkerThread public static List<Servicio> serviciosDisponibles(int hotelId) throws InterruptedException {
        List<Servicio> services = new ArrayList<>();
        try {
            for (DocumentSnapshot doc : Tasks.await(FirebaseFirestore.getInstance()
                    .collection("hotels").document(String.valueOf(hotelId)).collection("services")
                    .whereEqualTo("activo", true).get(Source.SERVER))) {
                if (Boolean.TRUE.equals(doc.getBoolean("archived"))) continue;
                try {
                    services.add(HotelAdminFirestoreCodec.service(doc.getId(), hotelId, doc.getData(),
                            R.drawable.superadmin_hotel_sol));
                } catch (IllegalArgumentException malformed) {
                    android.util.Log.w("HotelCatalog", "Servicio de hotel con datos inválidos.");
                }
            }
        } catch (ExecutionException error) {
            throw new IllegalStateException("No se pudieron cargar los servicios del hotel.", error);
        }
        return services;
    }

    public static Hotel combinar(ManagedHotel row) {
        if (row.remote) {
            Hotel remote = new Hotel(row.id, row.city, row.name, row.address, row.rating, row.reviews,
                    row.description, row.pricePerNight, row.services.toArray(new String[0]),
                    attractions(row.attractions, new Attraction[0]), "Habitaciones del hotel", "", "", new String[0]);
            remote.remote = true; remote.active = row.active; remote.hasInventory = row.hasInventory;
            remote.latitude = row.latitude; remote.longitude = row.longitude;
            remote.contactPhone = row.phone; remote.contactEmail = row.email;
            remote.photos = new ArrayList<>(row.photos);
            remote.taxiMinimo = row.taxiMinAmount;
            return remote;
        }
        Hotel base = null;
        for (Hotel candidate : SampleData.HOTELS) if (candidate.id == row.id) { base = candidate; break; }
        Hotel hotel = new Hotel(row.id, row.city, row.name, row.address,
                base == null ? 0 : base.rating, base == null ? 0 : base.reviews,
                row.description == null || row.description.isEmpty()
                        ? (base == null ? "Hotel registrado. Habitaciones y servicios pendientes de configurar." : base.description)
                        : row.description,
                base == null ? 0 : base.pricePerNight,
                base == null ? new String[0] : base.services,
                attractions(row.attractions, base == null ? new Attraction[0] : base.attractions),
                base == null ? "" : base.roomName, base == null ? "" : base.roomCapacity,
                base == null ? "" : base.roomSize, base == null ? new String[0] : base.roomFeatures);
        hotel.active = row.active;
        hotel.hasInventory = row.rooms > 0 && !CatalogoHabitaciones.disponiblesPorHotel(row.id).isEmpty();
        hotel.latitude = row.latitude;
        hotel.longitude = row.longitude;
        hotel.contactPhone = row.phone;
        hotel.contactEmail = row.email;
        hotel.photos = new ArrayList<>(row.photos);
        hotel.taxiMinimo = row.taxiMinAmount;
        return hotel;
    }

    private static Attraction[] attractions(String text, Attraction[] fallback) {
        if (text == null) return fallback;
        List<Attraction> parsed = new ArrayList<>();
        for (String entry : text.split(",")) {
            String value = entry.trim();
            if (value.isEmpty()) continue;
            int distanceStart = value.lastIndexOf('(');
            if (distanceStart > 0 && value.endsWith(")")) {
                parsed.add(new Attraction(value.substring(0, distanceStart).trim(),
                        value.substring(distanceStart + 1, value.length() - 1).trim()));
            } else {
                parsed.add(new Attraction(value, ""));
            }
        }
        return parsed.toArray(new Attraction[0]);
    }
}
