package com.iotelito.taxiweb.api;

import com.iotelito.taxiweb.firebase.MensajeriaFirebase;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class AvisosTaxi {
    record Dispositivo(String binding, String token) {}
    private record Aviso(long id, long reserva, String evento, String uid, String binding, String token, Instant creado) {}

    private static final Logger LOG = LoggerFactory.getLogger(AvisosTaxi.class);
    private static final String TAXISTA_ELEGIBLE = """
            FROM taxistas t JOIN usuarios u ON u.id = t.usuario_id
            WHERE u.firebase_uid = d.uid AND u.activo = TRUE AND t.estado_solicitud = 'APROBADO'
              AND t.disponible = TRUE AND NOT EXISTS (
                  SELECT 1 FROM servicios_taxi s WHERE s.taxista_id = t.id
                  AND s.estado IN ('ASIGNADO', 'EN_CAMINO', 'EN_TRASLADO')
              )
            """;

    private final JdbcTemplate db;
    private final MensajeriaFirebase fcm;

    AvisosTaxi(JdbcTemplate db, MensajeriaFirebase fcm) {
        this.db = db;
        this.fcm = fcm;
    }

    void registrar(String uid, Dispositivo dispositivo) {
        if (dispositivo == null || dispositivo.token() == null || dispositivo.token().length() < 20
                || dispositivo.token().length() > 4096 || dispositivo.token().chars().anyMatch(Character::isWhitespace)) {
            throw ApiException.validacion("El token del dispositivo no es válido.");
        }
        db.update("""
                INSERT INTO dispositivos_taxi (uid, binding, token) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE token = VALUES(token), ultimo_registro = CURRENT_TIMESTAMP
                """, uid, ChatTaxi.uuid(dispositivo.binding()), dispositivo.token());
    }

    void oferta(long reserva) {
        db.update("""
                INSERT IGNORE INTO avisos_taxi (reserva_id, evento, uid, binding)
                SELECT ?, 'OFERTA', d.uid, d.binding FROM dispositivos_taxi d
                WHERE d.ultimo_registro > ? AND EXISTS (SELECT 1
                """ + TAXISTA_ELEGIBLE + ")", reserva, registroVigente());
    }

    void cliente(long reserva, String evento, String uid) {
        db.update("""
                INSERT IGNORE INTO avisos_taxi (reserva_id, evento, uid, binding)
                SELECT ?, ?, uid, binding FROM dispositivos_taxi WHERE uid = ? AND ultimo_registro > ?
                """, reserva, evento, uid, registroVigente());
    }

    private static Timestamp registroVigente() {
        return Timestamp.from(Instant.now().minus(Duration.ofDays(45)));
    }

    @Scheduled(fixedDelay = 5000)
    void despachar() {
        if (!fcm.habilitada()) {
            return;
        }
        var pendientes = db.query("""
                SELECT a.id, a.reserva_id, a.evento, a.uid, a.binding, d.token, a.creado
                FROM avisos_taxi a LEFT JOIN dispositivos_taxi d ON a.uid = d.uid AND a.binding = d.binding
                WHERE a.completado = FALSE AND a.intentos < 5 AND a.siguiente_intento <= CURRENT_TIMESTAMP
                ORDER BY a.id LIMIT 12
                """, (rs, n) -> new Aviso(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getTimestamp(7).toInstant()));
        for (Aviso aviso : pendientes) {
            procesar(aviso);
        }
    }

    private void procesar(Aviso aviso) {
        if (db.update("""
                UPDATE avisos_taxi SET intentos = intentos + 1, siguiente_intento = ?
                WHERE id = ? AND completado = FALSE AND intentos < 5 AND siguiente_intento <= CURRENT_TIMESTAMP
                """, Timestamp.from(Instant.now().plusSeconds(60)), aviso.id()) == 0) {
            return;
        }
        boolean oferta = aviso.evento().equals("OFERTA");
        var vigencia = oferta ? Duration.ofMinutes(15) : Duration.ofDays(1);
        if (aviso.token() == null || aviso.creado().plus(vigencia).isBefore(Instant.now())
                || (oferta && !ofertaVigente(aviso))) {
            completar(aviso);
            return;
        }
        var resultado = fcm.enviar(aviso.token(), datos(aviso), oferta);
        if (resultado == MensajeriaFirebase.Resultado.REINTENTAR) {
            LOG.warn("FCM no confirmó el aviso de taxi {}", aviso.id());
            return;
        }
        if (resultado == MensajeriaFirebase.Resultado.TOKEN_REVOCADO) {
            db.update("DELETE FROM dispositivos_taxi WHERE uid = ? AND binding = ? AND token = ?",
                    aviso.uid(), aviso.binding(), aviso.token());
        }
        completar(aviso);
    }

    private boolean ofertaVigente(Aviso aviso) {
        Integer cantidad = db.queryForObject("""
                SELECT COUNT(*) FROM dispositivos_taxi d WHERE d.uid = ? AND d.binding = ?
                AND EXISTS (SELECT 1 FROM solicitudes_taxi WHERE reserva_id = ? AND estado = 'PENDIENTE')
                AND EXISTS (SELECT 1
                """ + TAXISTA_ELEGIBLE + ")", Integer.class, aviso.uid(), aviso.binding(), aviso.reserva());
        return cantidad != null && cantidad > 0;
    }

    private void completar(Aviso aviso) {
        db.update("UPDATE avisos_taxi SET completado = TRUE WHERE id = ?", aviso.id());
    }

    private static Map<String, String> datos(Aviso aviso) {
        String texto = switch (aviso.evento()) {
            case "OFERTA" -> "Abre la app para revisar el viaje disponible.";
            case "ASIGNADO" -> "Un taxista aceptó tu solicitud.";
            case "EN_CAMINO" -> "Tu taxista va en camino.";
            case "EN_TRASLADO" -> "Tu traslado al aeropuerto comenzó.";
            case "FINALIZADO" -> "Tu traslado al aeropuerto terminó.";
            default -> throw new IllegalArgumentException("Evento de taxi desconocido");
        };
        String idReserva = Long.toString(aviso.reserva());
        return Map.of("uid", aviso.uid(), "binding", aviso.binding(), "type", "TAXI",
                "notificationId", Long.toString(1500000000L + aviso.id() % 500000000),
                "reservationId", idReserva, "taxiId", idReserva,
                "title", aviso.evento().equals("OFERTA") ? "Nuevo viaje disponible" : "Actualización de tu taxi",
                "body", texto, "taxiEvent", aviso.evento());
    }
}
