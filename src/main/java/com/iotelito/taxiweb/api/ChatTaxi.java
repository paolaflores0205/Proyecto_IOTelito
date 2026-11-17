package com.iotelito.taxiweb.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ChatTaxi {
    record NuevoMensaje(String uuid, String texto) {}
    record Mensaje(long id, String texto, boolean mio) {}
    record Conversacion(List<Mensaje> mensajes, boolean permiteEnviar) {}

    private final JdbcTemplate db;

    ChatTaxi(JdbcTemplate db) {
        this.db = db;
    }

    Conversacion leer(long servicio, String uid, boolean permiteEnviar) {
        List<Mensaje> recientes = db.query("""
                SELECT id, texto, autor_uid FROM mensajes_taxi
                WHERE servicio_id = ? ORDER BY id DESC LIMIT 100
                """, (rs, n) -> new Mensaje(rs.getLong(1), rs.getString(2), uid.equals(rs.getString(3))), servicio);
        List<Mensaje> mensajes = new ArrayList<>(recientes);
        Collections.reverse(mensajes);
        return new Conversacion(mensajes, permiteEnviar);
    }

    void enviar(long servicio, String uid, NuevoMensaje mensaje) {
        if (mensaje == null || mensaje.texto() == null || mensaje.texto().isBlank()
                || mensaje.texto().strip().length() > 1000) {
            throw ApiException.validacion("Escribe un mensaje de hasta 1000 caracteres.");
        }
        String uuid = uuid(mensaje.uuid());
        db.update("""
                INSERT INTO mensajes_taxi (servicio_id, autor_uid, mensaje_uuid, texto) VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE mensaje_uuid = mensaje_uuid
                """, servicio, uid, uuid, mensaje.texto().strip());
    }

    static String uuid(String valor) {
        try {
            if (valor != null && valor.length() == 36) {
                return UUID.fromString(valor).toString();
            }
        } catch (IllegalArgumentException ignored) {
            // Se responde con el mismo error para cualquier identificador inválido.
        }
        throw ApiException.validacion("El identificador debe ser un UUID válido.");
    }
}
