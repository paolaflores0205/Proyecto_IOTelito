-- Migración aditiva, ejecutar sobre la base de taxistas.
CREATE TABLE IF NOT EXISTS mensajes_taxi (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    servicio_id BIGINT NOT NULL,
    autor_uid VARCHAR(128) NOT NULL,
    mensaje_uuid VARCHAR(36) NOT NULL,
    texto VARCHAR(1000) NOT NULL,
    CONSTRAINT uq_mensaje_taxi UNIQUE (servicio_id, autor_uid, mensaje_uuid),
    CONSTRAINT fk_mensaje_taxi FOREIGN KEY (servicio_id) REFERENCES servicios_taxi(id)
);

CREATE TABLE IF NOT EXISTS dispositivos_taxi (
    uid VARCHAR(128) NOT NULL,
    binding VARCHAR(36) NOT NULL,
    token VARCHAR(4096) NOT NULL,
    ultimo_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uid, binding)
);

CREATE TABLE IF NOT EXISTS avisos_taxi (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reserva_id BIGINT NOT NULL,
    evento VARCHAR(24) NOT NULL,
    uid VARCHAR(128) NOT NULL,
    binding VARCHAR(36) NOT NULL,
    creado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    siguiente_intento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    intentos INT NOT NULL DEFAULT 0,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_aviso_taxi UNIQUE (reserva_id, evento, uid, binding),
    INDEX idx_aviso_pendiente (completado, siguiente_intento)
);
