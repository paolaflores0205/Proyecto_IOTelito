-- IoTelito - Sistema web de gestión de taxistas
-- DDL para MySQL 8 (paso 2 de 3, después de 00-crear-base.sql).
--
-- Solo vive aquí lo del taxi. Hoteles, habitaciones, reservas, checkout, chat y
-- notificaciones son de la app móvil y están en Firestore (NoSQL), como pide el plan.
-- Por eso las columnas reserva_id, hotel_id y cliente_uid guardan ids de Firestore
-- sin llave foránea.
-- La web usa spring.jpa.hibernate.ddl-auto=validate: este script es la fuente de verdad.

DROP TABLE IF EXISTS avisos_taxi;
DROP TABLE IF EXISTS dispositivos_taxi;
DROP TABLE IF EXISTS mensajes_taxi;
DROP TABLE IF EXISTS calificaciones_taxista;
DROP TABLE IF EXISTS ubicaciones_taxista;
DROP TABLE IF EXISTS servicios_taxi;
DROP TABLE IF EXISTS solicitudes_taxi;
DROP TABLE IF EXISTS documentos_taxista;
DROP TABLE IF EXISTS logs_sistema;
DROP TABLE IF EXISTS taxistas;
DROP TABLE IF EXISTS usuarios;

-- Cuentas del sistema web: el superadmin entra al panel con contraseña (BCrypt).
-- Un taxista no tiene contraseña aquí: entra a la app con su cuenta de Firebase,
-- que la web crea al aprobarlo y enlaza por firebase_uid.
CREATE TABLE usuarios (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    nombres              VARCHAR(100) NOT NULL,
    apellidos            VARCHAR(100) NOT NULL,
    correo               VARCHAR(150) NOT NULL,
    password_hash        VARCHAR(100) NULL,
    telefono             VARCHAR(20)  NULL,
    rol                  ENUM('SUPERADMIN','TAXISTA') NOT NULL,
    firebase_uid         VARCHAR(128) NULL,
    activo               BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion       DATETIME     NOT NULL,
    fecha_actualizacion  DATETIME     NOT NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_correo UNIQUE (correo),
    CONSTRAINT uq_usuarios_firebase_uid UNIQUE (firebase_uid)
);

CREATE TABLE taxistas (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id                  BIGINT       NOT NULL,
    tipo_documento              ENUM('DNI','PASAPORTE','CARNET_EXTRANJERIA') NOT NULL,
    numero_documento            VARCHAR(12)  NOT NULL,
    fecha_nacimiento            DATE         NOT NULL,
    domicilio                   VARCHAR(255) NOT NULL,
    numero_licencia             VARCHAR(50)  NOT NULL,
    categoria_licencia          VARCHAR(20)  NOT NULL,
    fecha_vencimiento_licencia  DATE         NOT NULL,
    marca_vehiculo              VARCHAR(60)  NOT NULL,
    modelo_vehiculo             VARCHAR(60)  NOT NULL,
    anio_vehiculo               INT          NOT NULL,
    color_vehiculo              VARCHAR(50)  NOT NULL,
    placa_vehiculo              VARCHAR(20)  NOT NULL,
    estado_solicitud            ENUM('PENDIENTE','APROBADO','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
    disponible                  BOOLEAN      NOT NULL DEFAULT FALSE,
    calificacion                DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    cantidad_servicios          INT          NOT NULL DEFAULT 0,
    acepta_terminos             BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_solicitud             DATETIME     NOT NULL,
    CONSTRAINT pk_taxistas PRIMARY KEY (id),
    CONSTRAINT uq_taxistas_usuario UNIQUE (usuario_id),
    CONSTRAINT uq_taxistas_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT uq_taxistas_licencia UNIQUE (numero_licencia),
    CONSTRAINT uq_taxistas_placa UNIQUE (placa_vehiculo),
    CONSTRAINT ck_taxistas_calificacion CHECK (calificacion BETWEEN 0 AND 5),
    CONSTRAINT ck_taxistas_servicios CHECK (cantidad_servicios >= 0),
    CONSTRAINT fk_taxistas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_taxistas_estado ON taxistas (estado_solicitud);

-- archivo_url guarda la clave del objeto en el bucket privado de S3, no una URL pública.
CREATE TABLE documentos_taxista (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    taxista_id    BIGINT       NOT NULL,
    tipo          ENUM('FOTO_CONDUCTOR','FOTO_VEHICULO','DOCUMENTO_IDENTIDAD','LICENCIA','SOAT','TARJETA_PROPIEDAD') NOT NULL,
    archivo_url   VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    tamano_bytes  INT          NOT NULL,
    estado        ENUM('PENDIENTE','APROBADO','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
    observacion   VARCHAR(500) NULL,
    fecha_subida  DATETIME     NOT NULL,
    CONSTRAINT pk_documentos_taxista PRIMARY KEY (id),
    CONSTRAINT uq_documentos_taxista_tipo UNIQUE (taxista_id, tipo),
    CONSTRAINT ck_documentos_taxista_tamano CHECK (tamano_bytes > 0),
    CONSTRAINT fk_documentos_taxista FOREIGN KEY (taxista_id) REFERENCES taxistas (id) ON DELETE CASCADE
);

-- Pedido del cliente. Una reserva tiene como máximo un pedido; reserva_id es también el
-- id público del servicio en la API. PENDIENTE equivale al estado SOLICITADO de la app.
CREATE TABLE solicitudes_taxi (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    reserva_id             BIGINT       NOT NULL,
    cliente_uid            VARCHAR(128) NOT NULL,
    cliente_nombre         VARCHAR(160) NOT NULL,
    hotel_id               BIGINT       NOT NULL,
    hotel_nombre           VARCHAR(160) NOT NULL,
    hotel_ciudad           VARCHAR(120) NOT NULL,
    origen                 VARCHAR(500) NOT NULL,
    aeropuerto_codigo      ENUM('LIM','CUZ','AQP','TRU','PIO') NOT NULL,
    destino                VARCHAR(255) NOT NULL,
    hora_recojo            VARCHAR(5)   NOT NULL,
    cantidad_pasajeros     INT          NOT NULL,
    cantidad_equipaje      INT          NOT NULL DEFAULT 0,
    distancia_km           DECIMAL(8,2) NULL,
    duracion_estimada_min  INT          NULL,
    estado                 ENUM('PENDIENTE','ACEPTADA','RECHAZADA','CANCELADA','FINALIZADA') NOT NULL DEFAULT 'PENDIENTE',
    fecha_solicitud        DATETIME     NOT NULL,
    CONSTRAINT pk_solicitudes_taxi PRIMARY KEY (id),
    CONSTRAINT uq_solicitudes_taxi_reserva UNIQUE (reserva_id),
    CONSTRAINT ck_solicitudes_taxi_pasajeros CHECK (cantidad_pasajeros >= 1),
    CONSTRAINT ck_solicitudes_taxi_equipaje CHECK (cantidad_equipaje >= 0)
);

CREATE INDEX idx_solicitudes_taxi_estado ON solicitudes_taxi (estado, hora_recojo);

-- Servicio: existe desde que un taxista acepta el pedido.
-- UNIQUE (solicitud_id): un pedido lo gana un solo taxista.
-- taxista_activo_id solo tiene valor mientras el servicio está en curso; su índice único
-- impide que un taxista tenga dos servicios activos a la vez.
CREATE TABLE servicios_taxi (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    solicitud_id       BIGINT       NOT NULL,
    taxista_id         BIGINT       NOT NULL,
    estado             ENUM('ASIGNADO','EN_CAMINO','EN_TRASLADO','FINALIZADO','CANCELADO') NOT NULL DEFAULT 'ASIGNADO',
    hora_aceptacion    DATETIME     NOT NULL,
    hora_inicio        DATETIME     NULL,
    hora_traslado      DATETIME     NULL,
    hora_fin           DATETIME     NULL,
    distancia_real_km  DECIMAL(8,2) NULL,
    codigo_qr          VARCHAR(64)  NOT NULL,
    qr_validado        BOOLEAN      NOT NULL DEFAULT FALSE,
    taxista_activo_id  BIGINT GENERATED ALWAYS AS (
        CASE WHEN estado IN ('ASIGNADO','EN_CAMINO','EN_TRASLADO') THEN taxista_id END
    ),
    CONSTRAINT pk_servicios_taxi PRIMARY KEY (id),
    CONSTRAINT uq_servicios_taxi_solicitud UNIQUE (solicitud_id),
    CONSTRAINT uq_servicios_taxi_qr UNIQUE (codigo_qr),
    CONSTRAINT uq_servicios_taxi_activo UNIQUE (taxista_activo_id),
    CONSTRAINT fk_servicios_taxi_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes_taxi (id),
    CONSTRAINT fk_servicios_taxi_taxista FOREIGN KEY (taxista_id) REFERENCES taxistas (id)
);

CREATE INDEX idx_servicios_taxi_taxista ON servicios_taxi (taxista_id, estado);

-- La app del taxista envía su ubicación cada 15 s durante un servicio activo.
CREATE TABLE ubicaciones_taxista (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    servicio_id    BIGINT   NOT NULL,
    taxista_id     BIGINT   NOT NULL,
    latitud        DOUBLE   NOT NULL,
    longitud       DOUBLE   NOT NULL,
    precision_m    DOUBLE   NULL,
    registrada_en  DATETIME NOT NULL,
    CONSTRAINT pk_ubicaciones_taxista PRIMARY KEY (id),
    CONSTRAINT ck_ubicaciones_latitud CHECK (latitud BETWEEN -90 AND 90),
    CONSTRAINT ck_ubicaciones_longitud CHECK (longitud BETWEEN -180 AND 180),
    CONSTRAINT fk_ubicaciones_servicio FOREIGN KEY (servicio_id) REFERENCES servicios_taxi (id) ON DELETE CASCADE,
    CONSTRAINT fk_ubicaciones_taxista FOREIGN KEY (taxista_id) REFERENCES taxistas (id)
);

CREATE INDEX idx_ubicaciones_servicio ON ubicaciones_taxista (servicio_id, registrada_en);

CREATE TABLE calificaciones_taxista (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    servicio_taxi_id  BIGINT       NOT NULL,
    cliente_uid       VARCHAR(128) NOT NULL,
    taxista_id        BIGINT       NOT NULL,
    puntuacion        INT          NOT NULL,
    comentario        VARCHAR(500) NULL,
    fecha             DATETIME     NOT NULL,
    CONSTRAINT pk_calificaciones_taxista PRIMARY KEY (id),
    CONSTRAINT uq_calificaciones_servicio UNIQUE (servicio_taxi_id),
    CONSTRAINT ck_calificaciones_puntuacion CHECK (puntuacion BETWEEN 1 AND 5),
    CONSTRAINT fk_calificaciones_servicio FOREIGN KEY (servicio_taxi_id) REFERENCES servicios_taxi (id),
    CONSTRAINT fk_calificaciones_taxista FOREIGN KEY (taxista_id) REFERENCES taxistas (id)
);

-- Eventos relevantes de la web: aprobaciones, rechazos, activaciones.
CREATE TABLE logs_sistema (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id  BIGINT        NULL,
    accion      VARCHAR(255)  NOT NULL,
    detalle     VARCHAR(2000) NULL,
    fecha       DATETIME      NOT NULL,
    CONSTRAINT pk_logs_sistema PRIMARY KEY (id),
    CONSTRAINT fk_logs_sistema_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE SET NULL
);

CREATE INDEX idx_logs_sistema_fecha ON logs_sistema (fecha);
