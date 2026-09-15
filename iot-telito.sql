-- ============================================================
-- BASE DE DATOS IOTELITO
-- ============================================================

DROP DATABASE IF EXISTS iotelito;

CREATE DATABASE iotelito
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE iotelito;


-- ============================================================
-- 1. USUARIOS
-- Todos los usuarios del sistema
-- ============================================================

CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,

    correo VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,

    telefono VARCHAR(20),

    rol ENUM(
        'CLIENTE',
        'TAXISTA',
        'ADMIN_HOTEL',
        'SUPERADMIN'
    ) NOT NULL,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);


-- ============================================================
-- 2. CLIENTES
-- Datos específicos del huésped
-- ============================================================

CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    usuario_id BIGINT NOT NULL UNIQUE,

    dni VARCHAR(8) UNIQUE,
    fecha_nacimiento DATE,

    foto_perfil VARCHAR(255),

    FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 3. TAXISTAS
-- Datos del formulario web de registro
-- ============================================================

CREATE TABLE taxistas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    usuario_id BIGINT NOT NULL UNIQUE,

    dni VARCHAR(8) NOT NULL UNIQUE,
    fecha_nacimiento DATE NOT NULL,
    domicilio VARCHAR(255) NOT NULL,

    numero_licencia VARCHAR(50) NOT NULL UNIQUE,
    categoria_licencia VARCHAR(20) NOT NULL,
    fecha_vencimiento_licencia DATE NOT NULL,

    marca_vehiculo VARCHAR(60) NOT NULL,
    modelo_vehiculo VARCHAR(60) NOT NULL,
    anio_vehiculo INT NOT NULL,
    color_vehiculo VARCHAR(50) NOT NULL,

    placa_vehiculo VARCHAR(20) NOT NULL UNIQUE,

    estado_solicitud ENUM(
        'PENDIENTE',
        'APROBADO',
        'RECHAZADO'
    ) NOT NULL DEFAULT 'PENDIENTE',

    disponible BOOLEAN NOT NULL DEFAULT FALSE,

    calificacion DECIMAL(3,2) NOT NULL DEFAULT 0.00,

    cantidad_servicios INT NOT NULL DEFAULT 0,

    acepta_terminos BOOLEAN NOT NULL DEFAULT FALSE,

    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 4. DOCUMENTOS DEL TAXISTA
-- ============================================================

CREATE TABLE documentos_taxista (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    taxista_id BIGINT NOT NULL,

    tipo ENUM(
        'FOTO_CONDUCTOR',
        'DNI',
        'LICENCIA',
        'SOAT',
        'TARJETA_PROPIEDAD'
    ) NOT NULL,

    archivo_url VARCHAR(500) NOT NULL,

    estado ENUM(
        'PENDIENTE',
        'APROBADO',
        'RECHAZADO'
    ) NOT NULL DEFAULT 'PENDIENTE',

    observacion VARCHAR(500),

    fecha_subida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (taxista_id)
        REFERENCES taxistas(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 5. HOTELES
-- ============================================================

CREATE TABLE hoteles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    nombre VARCHAR(150) NOT NULL,

    descripcion TEXT,

    direccion VARCHAR(255) NOT NULL,
    distrito VARCHAR(100),
    ciudad VARCHAR(100) DEFAULT 'Lima',

    telefono VARCHAR(20),
    correo VARCHAR(150),

    latitud DECIMAL(10,7),
    longitud DECIMAL(10,7),

    estrellas INT,

    foto_portada VARCHAR(500),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 6. ADMINISTRADORES DE HOTEL
-- ============================================================

CREATE TABLE admin_hoteles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    usuario_id BIGINT NOT NULL UNIQUE,
    hotel_id BIGINT NOT NULL,

    FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE,

    FOREIGN KEY (hotel_id)
        REFERENCES hoteles(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 7. HABITACIONES
-- ============================================================

CREATE TABLE habitaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    hotel_id BIGINT NOT NULL,

    numero VARCHAR(20),

    nombre VARCHAR(100) NOT NULL,

    tipo ENUM(
        'SIMPLE',
        'DOBLE',
        'MATRIMONIAL',
        'SUITE',
        'DELUXE'
    ) NOT NULL,

    descripcion TEXT,

    capacidad INT NOT NULL,

    precio_noche DECIMAL(10,2) NOT NULL,

    disponible BOOLEAN NOT NULL DEFAULT TRUE,

    inteligente BOOLEAN NOT NULL DEFAULT FALSE,

    foto_portada VARCHAR(500),

    FOREIGN KEY (hotel_id)
        REFERENCES hoteles(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 8. FOTOS DE HABITACIÓN
-- ============================================================

CREATE TABLE fotos_habitacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    habitacion_id BIGINT NOT NULL,

    url VARCHAR(500) NOT NULL,

    principal BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (habitacion_id)
        REFERENCES habitaciones(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 9. SERVICIOS DEL HOTEL
-- ============================================================

CREATE TABLE servicios_hotel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    hotel_id BIGINT NOT NULL,

    nombre VARCHAR(100) NOT NULL,

    descripcion VARCHAR(500),

    icono VARCHAR(100),

    activo BOOLEAN DEFAULT TRUE,

    FOREIGN KEY (hotel_id)
        REFERENCES hoteles(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 10. RESERVAS
-- ============================================================

CREATE TABLE reservas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    cliente_id BIGINT NOT NULL,
    habitacion_id BIGINT NOT NULL,

    codigo_reserva VARCHAR(30) NOT NULL UNIQUE,

    fecha_entrada DATE NOT NULL,
    fecha_salida DATE NOT NULL,

    cantidad_huespedes INT NOT NULL,

    precio_total DECIMAL(10,2) NOT NULL,

    estado ENUM(
        'PENDIENTE',
        'CONFIRMADA',
        'CHECK_IN',
        'CHECK_OUT',
        'CANCELADA'
    ) NOT NULL DEFAULT 'PENDIENTE',

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (cliente_id)
        REFERENCES clientes(id),

    FOREIGN KEY (habitacion_id)
        REFERENCES habitaciones(id)
);


-- ============================================================
-- 11. PAGOS
-- ============================================================

CREATE TABLE pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    reserva_id BIGINT NOT NULL,

    monto DECIMAL(10,2) NOT NULL,

    metodo ENUM(
        'TARJETA',
        'YAPE',
        'PLIN',
        'EFECTIVO'
    ) NOT NULL,

    estado ENUM(
        'PENDIENTE',
        'PAGADO',
        'RECHAZADO',
        'REEMBOLSADO'
    ) NOT NULL DEFAULT 'PENDIENTE',

    codigo_operacion VARCHAR(100),

    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (reserva_id)
        REFERENCES reservas(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 12. MENSAJES CLIENTE - HOTEL
-- ============================================================

CREATE TABLE conversaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    cliente_id BIGINT NOT NULL,
    hotel_id BIGINT NOT NULL,

    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (cliente_id)
        REFERENCES clientes(id),

    FOREIGN KEY (hotel_id)
        REFERENCES hoteles(id)
);


CREATE TABLE mensajes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    conversacion_id BIGINT NOT NULL,

    emisor_usuario_id BIGINT NOT NULL,

    mensaje TEXT NOT NULL,

    leido BOOLEAN DEFAULT FALSE,

    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (conversacion_id)
        REFERENCES conversaciones(id)
        ON DELETE CASCADE,

    FOREIGN KEY (emisor_usuario_id)
        REFERENCES usuarios(id)
);


-- ============================================================
-- 13. SOLICITUDES DE TAXI
-- El huésped solicita transporte
-- ============================================================

CREATE TABLE solicitudes_taxi (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    cliente_id BIGINT NOT NULL,
    reserva_id BIGINT,

    hotel_id BIGINT NOT NULL,

    origen VARCHAR(255) NOT NULL,
    destino VARCHAR(255) NOT NULL,

    fecha_servicio DATETIME NOT NULL,

    cantidad_pasajeros INT DEFAULT 1,
    cantidad_equipaje INT DEFAULT 0,

    distancia_km DECIMAL(8,2),
    duracion_estimada_min INT,

    estado ENUM(
        'PENDIENTE',
        'ACEPTADA',
        'RECHAZADA',
        'CANCELADA',
        'FINALIZADA'
    ) NOT NULL DEFAULT 'PENDIENTE',

    fecha_solicitud TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (cliente_id)
        REFERENCES clientes(id),

    FOREIGN KEY (reserva_id)
        REFERENCES reservas(id)
        ON DELETE SET NULL,

    FOREIGN KEY (hotel_id)
        REFERENCES hoteles(id)
);


-- ============================================================
-- 14. SERVICIOS DE TAXI
-- Cuando un taxista acepta una solicitud
-- ============================================================

CREATE TABLE servicios_taxi (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    solicitud_id BIGINT NOT NULL UNIQUE,
    taxista_id BIGINT NOT NULL,

    estado ENUM(
        'ASIGNADO',
        'CAMINO_HOTEL',
        'EN_HOTEL',
        'EN_TRASLADO',
        'EN_DESTINO',
        'FINALIZADO',
        'CANCELADO'
    ) NOT NULL DEFAULT 'ASIGNADO',

    hora_aceptacion DATETIME,
    hora_inicio DATETIME,
    hora_fin DATETIME,

    distancia_real_km DECIMAL(8,2),

    codigo_qr VARCHAR(150),

    qr_validado BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (solicitud_id)
        REFERENCES solicitudes_taxi(id),

    FOREIGN KEY (taxista_id)
        REFERENCES taxistas(id)
);


-- ============================================================
-- 15. CALIFICACIONES TAXISTA
-- ============================================================

CREATE TABLE calificaciones_taxista (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    servicio_taxi_id BIGINT NOT NULL UNIQUE,

    cliente_id BIGINT NOT NULL,
    taxista_id BIGINT NOT NULL,

    puntuacion INT NOT NULL,

    comentario VARCHAR(500),

    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (servicio_taxi_id)
        REFERENCES servicios_taxi(id),

    FOREIGN KEY (cliente_id)
        REFERENCES clientes(id),

    FOREIGN KEY (taxista_id)
        REFERENCES taxistas(id),

    CHECK (puntuacion BETWEEN 1 AND 5)
);


-- ============================================================
-- 16. NOTIFICACIONES
-- ============================================================

CREATE TABLE notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    titulo VARCHAR(150) NOT NULL,

    mensaje VARCHAR(500) NOT NULL,

    tipo ENUM(
        'RESERVA',
        'PAGO',
        'TAXI',
        'MENSAJE',
        'SISTEMA'
    ) NOT NULL,

    leida BOOLEAN DEFAULT FALSE,

    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 17. DISPOSITIVOS IoT
-- ============================================================

CREATE TABLE dispositivos_iot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    habitacion_id BIGINT NOT NULL,

    nombre VARCHAR(100) NOT NULL,

    tipo ENUM(
        'TEMPERATURA',
        'LUZ',
        'CORTINA',
        'CERRADURA'
    ) NOT NULL,

    identificador VARCHAR(150) UNIQUE,

    estado BOOLEAN DEFAULT TRUE,

    FOREIGN KEY (habitacion_id)
        REFERENCES habitaciones(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 18. ESTADO / VALORES DE LOS DISPOSITIVOS IoT
-- ============================================================

CREATE TABLE estados_iot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    dispositivo_id BIGINT NOT NULL,

    valor VARCHAR(100),

    fecha_actualizacion TIMESTAMP
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (dispositivo_id)
        REFERENCES dispositivos_iot(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 19. LOGS DE SUPERADMIN
-- ============================================================

CREATE TABLE logs_sistema (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    usuario_id BIGINT,

    accion VARCHAR(255) NOT NULL,

    detalle TEXT,

    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE SET NULL
);


-- ============================================================
-- ÍNDICES
-- ============================================================

CREATE INDEX idx_usuarios_rol
ON usuarios(rol);

CREATE INDEX idx_reservas_cliente
ON reservas(cliente_id);

CREATE INDEX idx_reservas_estado
ON reservas(estado);

CREATE INDEX idx_solicitud_taxi_estado
ON solicitudes_taxi(estado);

CREATE INDEX idx_taxista_estado
ON taxistas(estado_solicitud);

CREATE INDEX idx_notificacion_usuario
ON notificaciones(usuario_id);


-- ============================================================
-- SUPERADMIN INICIAL
-- Contraseña temporal solo para desarrollo
-- Después se debe almacenar con BCrypt
-- ============================================================

INSERT INTO usuarios (
    nombres,
    apellidos,
    correo,
    password,
    telefono,
    rol
)
VALUES (
    'Super',
    'Administrador',
    'admin@iotelito.com',
    'admin123',
    '999999999',
    'SUPERADMIN'
);


-- ============================================================
-- HOTEL DE PRUEBA
-- ============================================================

INSERT INTO hoteles (
    nombre,
    descripcion,
    direccion,
    distrito,
    ciudad,
    telefono,
    correo,
    estrellas
)
VALUES (
    'Hotel Miraflores Park',
    'Hotel IoTelito de prueba',
    'Av. Malecón de la Reserva 1035',
    'Miraflores',
    'Lima',
    '014000000',
    'contacto@miraflorespark.com',
    5
);


-- ============================================================
-- VERIFICACIÓN
-- ============================================================

SHOW TABLES;