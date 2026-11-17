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

    private final String uid;
    private final NotificacionDao dao;

    private NotificacionRepository(Context context, String uid) {
        this.uid = uid;
        dao = IoTelitoDatabase.getInstance(context).notificacionDao();
    }

    public static NotificacionRepository getInstance(Context context) {
        com.example.proyecto_iotelito.model.auth.AuthenticatedUser user = SessionRepository.obtener(context);
        return forOwner(context, user != null && user.getRole() == com.example.proyecto_iotelito.model.auth.UserRole.CLIENT
                ? user.getUid() : "");
    }

    /** Propietario capturado antes de iniciar una tarea asíncrona o recibir un push. */
    public static NotificacionRepository forOwner(Context context, String uid) {
        return new NotificacionRepository(context.getApplicationContext(), uid == null ? "" : uid);
    }

    public List<NotificacionCliente> obtenerTodas() {
        List<NotificacionCliente> resultado = new ArrayList<>();
        if (uid.isEmpty()) return resultado;
        for (NotificacionEntity entity : dao.obtenerTodas(uid)) {
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
        return androidx.lifecycle.Transformations.map(dao.observarTodas(uid), rows -> {
            List<NotificacionCliente> result = new ArrayList<>();
            if (uid.isEmpty()) return result;
            for (NotificacionEntity row : rows) result.add(new NotificacionCliente(row.id,
                    NotificacionCliente.Tipo.valueOf(row.tipo), row.titulo, row.creadaEn, row.leida, row.reservaId));
            return result;
        });
    }

    public void guardarSiNoExiste(int id, NotificacionCliente.Tipo tipo,
                                  String titulo, int reservaId) {
        if (uid.isEmpty()) return;
        NotificacionEntity entity = new NotificacionEntity();
        entity.ownerUid = uid;
        entity.id = id;
        entity.tipo = tipo.name();
        entity.titulo = titulo;
        entity.creadaEn = System.currentTimeMillis();
        entity.leida = false;
        entity.reservaId = reservaId;
        dao.guardarSiNoExiste(entity);
    }

    public void marcarLeida(int id) {
        ClientLocalTasks.execute(() -> dao.marcarLeida(uid, id));
    }

    public void marcarTodasLeidas() {
        ClientLocalTasks.execute(() -> dao.marcarTodasLeidas(uid));
    }
}
