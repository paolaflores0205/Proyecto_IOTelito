package com.example.proyecto_iotelito.data;

import android.content.Context;

import com.example.proyecto_iotelito.data.local.IoTelitoDatabase;
import com.example.proyecto_iotelito.data.local.NotificacionDao;
import com.example.proyecto_iotelito.data.local.NotificacionEntity;
import com.example.proyecto_iotelito.model.NotificacionCliente;

import java.util.ArrayList;
import java.util.List;

/** Acceso a la bandeja persistente de notificaciones del cliente. */
public final class NotificacionRepository {

    private static volatile NotificacionRepository instance;
    private final NotificacionDao dao;

    private NotificacionRepository(Context context) {
        dao = IoTelitoDatabase.getInstance(context).notificacionDao();
    }

    public static NotificacionRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (NotificacionRepository.class) {
                if (instance == null) {
                    instance = new NotificacionRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public List<NotificacionCliente> obtenerTodas() {
        List<NotificacionCliente> resultado = new ArrayList<>();
        for (NotificacionEntity entity : dao.obtenerTodas()) {
            resultado.add(new NotificacionCliente(
                    entity.id,
                    NotificacionCliente.Tipo.valueOf(entity.tipo),
                    entity.titulo,
                    entity.creadaEn,
                    entity.leida,
                    entity.reservaId));
        }
        return resultado;
    }

    public androidx.lifecycle.LiveData<List<NotificacionCliente>> observarTodas() {
        return androidx.lifecycle.Transformations.map(dao.observarTodas(), rows -> {
            List<NotificacionCliente> result = new ArrayList<>();
            for (NotificacionEntity row : rows) result.add(new NotificacionCliente(row.id,
                    NotificacionCliente.Tipo.valueOf(row.tipo), row.titulo, row.creadaEn, row.leida, row.reservaId));
            return result;
        });
    }

    public void guardarSiNoExiste(int id, NotificacionCliente.Tipo tipo,
                                  String titulo, int reservaId) {
        NotificacionEntity entity = new NotificacionEntity();
        entity.id = id;
        entity.tipo = tipo.name();
        entity.titulo = titulo;
        entity.creadaEn = System.currentTimeMillis();
        entity.leida = false;
        entity.reservaId = reservaId;
        dao.guardarSiNoExiste(entity);
    }

    public void marcarLeida(int id) {
        ClientLocalTasks.execute(() -> dao.marcarLeida(id));
    }

    public void marcarTodasLeidas() {
        ClientLocalTasks.execute(() -> dao.marcarTodasLeidas());
    }
}
