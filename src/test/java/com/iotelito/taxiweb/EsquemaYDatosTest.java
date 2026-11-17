package com.iotelito.taxiweb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Los scripts db/schema.sql y db/data.sql: datos de prueba y reglas que garantiza la base. */
@SpringBootTest
@Transactional
class EsquemaYDatosTest {

    private static final String TAXISTA = "(SELECT t.id FROM taxistas t JOIN usuarios u ON u.id = t.usuario_id WHERE u.correo = ?)";
    private static final String SOLICITUD = "(SELECT id FROM solicitudes_taxi WHERE reserva_id = ?)";

    @Autowired
    private JdbcTemplate jdbc;

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private void nuevoServicio(long reservaId, String correoTaxista, String codigoQr) {
        jdbc.update("INSERT INTO servicios_taxi (solicitud_id, taxista_id, estado, hora_aceptacion, codigo_qr, qr_validado) "
                        + "VALUES (" + SOLICITUD + ", " + TAXISTA + ", 'ASIGNADO', CURRENT_TIMESTAMP, ?, FALSE)",
                reservaId, correoTaxista, codigoQr);
    }

    @Test
    void cargaLosDatosDePrueba() {
        assertThat(contar("usuarios")).isEqualTo(11);
        assertThat(contar("taxistas")).isEqualTo(10);
        assertThat(contar("documentos_taxista")).isEqualTo(60);
        assertThat(contar("solicitudes_taxi")).isEqualTo(4);
        assertThat(contar("servicios_taxi")).isEqualTo(3);
        assertThat(contar("ubicaciones_taxista")).isEqualTo(3);
        assertThat(contar("calificaciones_taxista")).isEqualTo(2);
        assertThat(contar("logs_sistema")).isEqualTo(3);
    }

    @Test
    void elResumenDeCadaTaxistaSaleDeSusServicios() {
        Map<String, Object> maria = jdbc.queryForMap("SELECT cantidad_servicios, calificacion FROM taxistas WHERE id = " + TAXISTA, "maria.demo@iotelito.com");
        Map<String, Object> andrea = jdbc.queryForMap("SELECT cantidad_servicios, calificacion FROM taxistas WHERE id = " + TAXISTA, "andrea.demo@iotelito.com");
        Map<String, Object> fernando = jdbc.queryForMap("SELECT cantidad_servicios, calificacion FROM taxistas WHERE id = " + TAXISTA, "fernando.demo@iotelito.com");

        assertThat(maria.get("cantidad_servicios")).isEqualTo(1);
        assertThat((BigDecimal) maria.get("calificacion")).isEqualByComparingTo("5.00");
        assertThat(andrea.get("cantidad_servicios")).isEqualTo(1);
        assertThat((BigDecimal) andrea.get("calificacion")).isEqualByComparingTo("4.00");
        assertThat(fernando.get("cantidad_servicios")).isEqualTo(0);
        assertThat((BigDecimal) fernando.get("calificacion")).isEqualByComparingTo("0");
    }

    @Test
    void unPedidoLoGanaUnSoloTaxista() {
        assertThrows(DataIntegrityViolationException.class,
                () -> nuevoServicio(900003, "maria.demo@iotelito.com", "TX1-prueba-1"));
    }

    @Test
    void unTaxistaNoTieneDosServiciosActivos() {
        assertThrows(DataIntegrityViolationException.class,
                () -> nuevoServicio(900004, "fernando.demo@iotelito.com", "TX1-prueba-2"));
        assertDoesNotThrow(() -> nuevoServicio(900004, "maria.demo@iotelito.com", "TX1-prueba-3"));
    }

    @Test
    void alFinalizarElTaxistaPuedeTomarOtroPedido() {
        jdbc.update("UPDATE servicios_taxi SET estado = 'FINALIZADO', hora_fin = CURRENT_TIMESTAMP WHERE solicitud_id = " + SOLICITUD, 900003);

        assertDoesNotThrow(() -> nuevoServicio(900004, "fernando.demo@iotelito.com", "TX1-prueba-4"));
    }

    @Test
    void laPuntuacionVaDeUnoACinco() {
        String sql = "INSERT INTO calificaciones_taxista (servicio_taxi_id, cliente_uid, taxista_id, puntuacion, fecha) "
                + "SELECT s.id, 'demo-cliente-sofia', s.taxista_id, ?, CURRENT_TIMESTAMP FROM servicios_taxi s WHERE s.solicitud_id = " + SOLICITUD;

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, 6, 900003));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, 0, 900003));
        assertDoesNotThrow(() -> jdbc.update(sql, 5, 900003));
    }

    @Test
    void elMismoDocumentoNoSeRegistraDosVeces() {
        String sql = "INSERT INTO taxistas (usuario_id, tipo_documento, numero_documento, fecha_nacimiento, domicilio, numero_licencia, "
                + "categoria_licencia, fecha_vencimiento_licencia, marca_vehiculo, modelo_vehiculo, anio_vehiculo, color_vehiculo, "
                + "placa_vehiculo, fecha_solicitud) "
                + "SELECT id, ?, '72148965', DATE '1990-01-01', 'Calle 1', ?, 'A-IIb', DATE '2030-01-01', 'Kia', 'Rio', 2020, 'Gris', ?, CURRENT_TIMESTAMP "
                + "FROM usuarios WHERE correo = 'admin@iotelito.com'";

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, "DNI", "Q-PRUEBA-1", "PRB-001"));
    }

    @Test
    void elSuperadminSoloGuardaElHashDeSuContrasena() {
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuarios WHERE correo = 'admin@iotelito.com'", String.class);

        assertThat(hash).startsWith("$2a$10$").hasSize(60);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE rol = 'TAXISTA' AND password_hash IS NOT NULL", Integer.class)).isZero();
    }
}
