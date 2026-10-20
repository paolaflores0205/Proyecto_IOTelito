package com.example.proyecto_iotelito.data;

import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Carga conjunta para pantallas de detalle; nunca sustituye un ID inexistente por otro. */
public final class ClientDataLoader {
    private ClientDataLoader() { }
    public static void hotel(AppCompatActivity activity, int id, Consumer<Hotel> loaded) {
        ClientLocalTasks.run(activity, activity, () -> HotelCatalogRepository.buscar(activity, id), hotel -> {
            if (hotel == null) activity.finish(); else loaded.accept(hotel);
        });
    }
    public static void reserva(AppCompatActivity activity, int id, BiConsumer<Reserva, Hotel> loaded) {
        ReservaRepository repository = ReservaRepository.getInstance(activity);
        ClientLocalTasks.run(activity, activity, () -> {
            Reserva reserva = repository.buscarPorId(id);
            if (reserva == null) return null;
            Hotel hotel = HotelCatalogRepository.buscar(activity, reserva.hotelId);
            return hotel == null ? null : new Data(reserva, hotel);
        }, value -> {
            if (value == null) activity.finish(); else loaded.accept(value.reserva, value.hotel);
        });
    }
    private static class Data {
        final Reserva reserva;
        final Hotel hotel;
        Data(Reserva reserva, Hotel hotel) { this.reserva = reserva; this.hotel = hotel; }
    }
}
