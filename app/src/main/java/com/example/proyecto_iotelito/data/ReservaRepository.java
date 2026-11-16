package com.example.proyecto_iotelito.data;

import android.content.Context;

import com.example.proyecto_iotelito.data.local.IoTelitoDatabase;
import com.example.proyecto_iotelito.data.local.ReservaDao;
import com.example.proyecto_iotelito.data.local.ReservaEntity;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.OpcionHabitaciones;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;

/** Punto único de acceso a las reservas locales del cliente. */
public final class ReservaRepository {

    private static volatile ReservaRepository instance;
    private final ReservaDao reservaDao;
    private final IoTelitoDatabase database;

    private ReservaRepository(Context context) {
        database = IoTelitoDatabase.getInstance(context);
        reservaDao = database.reservaDao();
        ClientLocalTasks.execute(() -> database.runInTransaction(this::cargarDatosInicialesSiHaceFalta));
    }

    public static ReservaRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (ReservaRepository.class) {
                if (instance == null) {
                    instance = new ReservaRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public androidx.lifecycle.LiveData<List<Reserva>> observarTodas() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseReservaStore.observarTodas();
        }
        return androidx.lifecycle.Transformations.map(reservaDao.observarTodas(), rows -> {
            List<Reserva> result = new ArrayList<>();
            for (ReservaEntity row : rows) result.add(toModel(row));
            return result;
        });
    }

    @androidx.annotation.WorkerThread
    public List<Reserva> obtenerTodas() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseReservaStore.obtenerTodas();
        }
        List<Reserva> reservas = new ArrayList<>();
        for (ReservaEntity entity : reservaDao.obtenerTodas()) {
            reservas.add(toModel(entity));
        }
        return reservas;
    }

    @androidx.annotation.WorkerThread
    public Reserva buscarPorId(int id) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseReservaStore.buscarPorId(id);
        }
        ReservaEntity entity = reservaDao.buscarPorId(id);
        if (entity != null) {
            return toModel(entity);
        }
        return null;
    }

    public Reserva agregarReserva(Hotel hotel, LocalDate fechaEntrada, LocalDate fechaSalida,
                                   String huespedes, double precioTotal, String codigo,
                                   String habitaciones, String codigosHabitaciones,
                                   List<Integer> roomIds) {
        return agregarReserva(hotel, fechaEntrada, fechaSalida, huespedes, precioTotal, codigo,
                habitaciones, codigosHabitaciones, roomIds, null);
    }

    public Reserva agregarReserva(Hotel hotel, LocalDate fechaEntrada, LocalDate fechaSalida,
                                   String huespedes, double precioTotal, String codigo,
                                   String habitaciones, String codigosHabitaciones,
                                   List<Integer> roomIds, TarjetaSimulada tarjeta) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseReservaStore.agregar(hotel, fechaEntrada, fechaSalida,
                    huespedes, precioTotal, codigo, habitaciones, codigosHabitaciones, roomIds, tarjeta);
        }
        return database.runInTransaction((java.util.concurrent.Callable<Reserva>) () -> {
        Reserva reserva = new Reserva(
                reservaDao.obtenerIdMaximo() + 1,
                hotel.id,
                habitaciones,
                codigosHabitaciones,
                fechaEntrada,
                fechaSalida,
                huespedes,
                precioTotal,
                Reserva.Estado.CONFIRMADA,
                codigo);
        reservaDao.guardar(toEntity(reserva));
        return reserva;
        });
    }

    @androidx.annotation.WorkerThread
    public void solicitarCheckout(int id, int estrellas, String observaciones, TarjetaSimulada tarjeta) {
        FirebaseReservaStore.solicitarCheckout(id, estrellas, observaciones, tarjeta);
    }

    @androidx.annotation.WorkerThread
    public ComprobanteCheckout comprobante(int id) { return FirebaseReservaStore.comprobante(id); }

    public void actualizarEstado(int reservaId, Reserva.Estado estado) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseReservaStore.actualizarEstado(reservaId, estado);
            return;
        }
        reservaDao.actualizarEstado(reservaId, estado.name());
    }

    @androidx.annotation.WorkerThread
    public List<OpcionHabitaciones> filtrarDisponibles(int hotelId, LocalDate entrada,
                                                       LocalDate salida,
                                                       List<OpcionHabitaciones> opciones) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return opciones;
        return FirebaseReservaStore.filtrarDisponibles(hotelId, entrada, salida, opciones);
    }

    @androidx.annotation.WorkerThread
    public List<OpcionHabitaciones> opcionesDisponibles(Hotel hotel, LocalDate entrada,
                                                        LocalDate salida, int adultos, int ninos) {
        if (!hotel.active || !hotel.hasInventory) return java.util.Collections.emptyList();
        List<OpcionHabitaciones> libres = filtrarDisponibles(hotel.id, entrada, salida,
                InventarioCliente.individuales(InventarioCliente.habitaciones(hotel)));
        return InventarioCliente.combinarLibres(libres, adultos, ninos);
    }

    @androidx.annotation.WorkerThread
    public List<Hotel> hotelesDisponibles(Context context, List<Hotel> catalogo,
                                          LocalDate entrada, LocalDate salida, int adultos, int ninos)
            throws InterruptedException {
        List<Hotel> result = new ArrayList<>();
        for (Hotel item : catalogo) {
            if (!item.active || !item.hasInventory) continue;
            Hotel hotel = item.remote ? HotelCatalogRepository.buscar(context, item.id) : item;
            if (hotel == null) continue;
            List<OpcionHabitaciones> opciones = opcionesDisponibles(hotel, entrada, salida, adultos, ninos);
            if (opciones.isEmpty()) continue;
            hotel.precioBusqueda = opciones.get(0).precioPorNoche();
            result.add(hotel);
        }
        return result;
    }

    @androidx.annotation.WorkerThread
    public void asegurarEjemplosPrueba() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) return;
        database.runInTransaction(() -> {
            if (reservaDao.contarPorPrefijoCodigo(ReservasPrueba.PREFIJO_CODIGO) > 0) return;
            List<ReservaEntity> ejemplos = new ArrayList<>();
            for (Reserva reserva : ReservasPrueba.crear(LocalDate.now(), reservaDao.obtenerIdMaximo() + 1)) {
                ejemplos.add(toEntity(reserva));
            }
            reservaDao.guardarTodas(ejemplos);
        });
    }

    private void cargarDatosInicialesSiHaceFalta() {
        if (reservaDao.contar() > 0) {
            return;
        }
        List<ReservaEntity> iniciales = new ArrayList<>();
        for (Reserva reserva : SampleData.RESERVAS) {
            iniciales.add(toEntity(reserva));
        }
        reservaDao.guardarTodas(iniciales);
    }

    private ReservaEntity toEntity(Reserva reserva) {
        ReservaEntity entity = new ReservaEntity();
        entity.id = reserva.id;
        entity.hotelId = reserva.hotelId;
        entity.roomName = reserva.roomName;
        entity.roomNumber = reserva.roomNumber;
        entity.fechaEntrada = reserva.fechaEntrada.toString();
        entity.fechaSalida = reserva.fechaSalida.toString();
        entity.huespedes = reserva.huespedes;
        entity.precioTotal = reserva.precioTotal;
        entity.estado = reserva.estado.name();
        entity.codigo = reserva.codigo;
        return entity;
    }

    private Reserva toModel(ReservaEntity entity) {
        return new Reserva(
                entity.id,
                entity.hotelId,
                entity.roomName,
                entity.roomNumber,
                LocalDate.parse(entity.fechaEntrada),
                LocalDate.parse(entity.fechaSalida),
                entity.huespedes,
                entity.precioTotal,
                Reserva.Estado.valueOf(entity.estado),
                entity.codigo);
    }
}
