package com.example.proyecto_iotelito.data;

import android.content.Context;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.data.superadmin.local.AdminHotelEntity;
import com.example.proyecto_iotelito.data.superadmin.local.SuperadminDatabase;
import com.example.proyecto_iotelito.model.Attraction;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import java.util.ArrayList;
import java.util.List;

/** Une los datos administrativos persistidos con las tarifas y servicios del cliente. */
public final class HotelCatalogRepository {
    private HotelCatalogRepository() { }
    public static LiveData<List<Hotel>> observar(Context context) {
        return Transformations.map(SuperadminRepository.getInstance(context).hotels(), rows -> {
            List<Hotel> result = new ArrayList<>();
            for (ManagedHotel row : rows) result.add(combinar(row));
            return result;
        });
    }
    @WorkerThread public static Hotel buscar(Context context, int id) throws InterruptedException {
        SuperadminRepository.getInstance(context).awaitInitialization();
        AdminHotelEntity row = SuperadminDatabase.getInstance(context).dao().hotel(id);
        return row == null ? null : combinar(row.toModel());
    }
    public static Hotel combinar(ManagedHotel row) {
        Hotel base = null;
        for (Hotel candidate : SampleData.HOTELS) if (candidate.id == row.id) { base = candidate; break; }
        Hotel hotel = new Hotel(row.id, row.city, row.name, row.address,
                base == null ? 0 : base.rating, base == null ? 0 : base.reviews,
                row.description == null || row.description.isEmpty()
                        ? (base == null ? "Hotel registrado. Habitaciones y servicios pendientes de configurar." : base.description)
                        : row.description,
                base == null ? 0 : base.pricePerNight,
                base == null ? new String[0] : base.services,
                base == null ? new Attraction[0] : base.attractions,
                base == null ? "" : base.roomName, base == null ? "" : base.roomCapacity,
                base == null ? "" : base.roomSize, base == null ? new String[0] : base.roomFeatures);
        hotel.active = row.active;
        hotel.hasInventory = row.rooms > 0 && !CatalogoHabitaciones.disponiblesPorHotel(row.id).isEmpty();
        hotel.latitude = row.latitude;
        hotel.longitude = row.longitude;
        hotel.contactPhone = row.phone;
        hotel.contactEmail = row.email;
        hotel.photos = new ArrayList<>(row.photos);
        return hotel;
    }
}
