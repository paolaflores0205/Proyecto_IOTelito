package com.example.proyecto_iotelito.data;

import android.content.Context;

import com.example.proyecto_iotelito.data.local.IoTelitoDatabase;
import com.example.proyecto_iotelito.data.local.ReservaDao;
import com.example.proyecto_iotelito.data.local.ReservaEntity;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Punto único de acceso a las reservas locales del cliente. */
public final class ReservaRepository {

    private static volatile ReservaRepository instance;
    private final ReservaDao reservaDao;

    private ReservaRepository(Context context) {
        reservaDao = IoTelitoDatabase.getInstance(context).reservaDao();
        cargarDatosInicialesSiHaceFalta();
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

    public List<Reserva> obtenerTodas() {
        List<Reserva> reservas = new ArrayList<>();
        for (ReservaEntity entity : reservaDao.obtenerTodas()) {
            reservas.add(toModel(entity));
        }
        return reservas;
    }

    public Reserva buscarPorId(int id) {
        ReservaEntity entity = reservaDao.buscarPorId(id);
        if (entity != null) {
            return toModel(entity);
        }
        List<Reserva> reservas = obtenerTodas();
        return reservas.isEmpty() ? null : reservas.get(0);
    }

    public Reserva agregarReserva(Hotel hotel, LocalDate fechaEntrada, LocalDate fechaSalida,
                                   String huespedes, double precioTotal, String codigo,
                                   String habitaciones, String codigosHabitaciones) {
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
    }

    public void actualizarEstado(int reservaId, Reserva.Estado estado) {
        reservaDao.actualizarEstado(reservaId, estado.name());
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
