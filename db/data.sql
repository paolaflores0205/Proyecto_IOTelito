-- IoTelito - Sistema web de gestión de taxistas
-- DML de prueba (paso 3 de 3, después de schema.sql). Ids generados por la base:
-- las relaciones se resuelven por correo, documento o reserva, nunca por ids fijos.
-- reserva_id, hotel_id y cliente_uid son ids de ejemplo de Firestore.

-- Superadmin del panel web. Contraseña de desarrollo: admin123 (aquí solo su hash BCrypt).
-- Al desplegar, la web la reemplaza con la variable de entorno IOTELITO_ADMIN_PASSWORD.
INSERT INTO usuarios (nombres, apellidos, correo, password_hash, telefono, rol, firebase_uid, activo, fecha_creacion, fecha_actualizacion) VALUES
('Super', 'Administrador', 'admin@iotelito.com', '$2a$10$n2.JowdbbrEN0pghi7HWKevzzJenON7tWxj/HmZG4tOG.s/iUz9Uq', '999999999', 'SUPERADMIN', NULL, TRUE, '2026-10-15 08:00:00', '2026-10-15 08:00:00');

-- Cuentas de los taxistas: sin contraseña web. Los aprobados tienen su cuenta de Firebase.
INSERT INTO usuarios (nombres, apellidos, correo, password_hash, telefono, rol, firebase_uid, activo, fecha_creacion, fecha_actualizacion) VALUES
('Andrea',   'Cordova Rios',    'andrea.demo@iotelito.com',   NULL, '987222333', 'TAXISTA', 'demo-uid-andrea',   TRUE,  '2026-10-18 08:40:00', '2026-10-19 10:00:00'),
('Maria',    'Silva Torres',    'maria.demo@iotelito.com',    NULL, '987111222', 'TAXISTA', 'demo-uid-maria',    TRUE,  '2026-10-20 10:00:00', '2026-10-21 09:30:00'),
('Luis',     'Ruiz Vargas',     'luis.demo@iotelito.com',     NULL, '987333444', 'TAXISTA', NULL,                FALSE, '2026-10-22 11:30:00', '2026-10-23 15:00:00'),
('Fernando', 'Salazar Vega',    'fernando.demo@iotelito.com', NULL, '987444555', 'TAXISTA', 'demo-uid-fernando', TRUE,  '2026-10-25 12:10:00', '2026-10-26 11:20:00'),
('Rosa',     'Huaman Castillo', 'rosa.demo@iotelito.com',     NULL, '987888999', 'TAXISTA', 'demo-uid-rosa',     TRUE,  '2026-10-28 17:05:00', '2026-10-29 08:15:00'),
('Jorge',    'Navarro Soto',    'jorge.demo@iotelito.com',    NULL, '987000111', 'TAXISTA', NULL,                FALSE, '2026-10-30 09:50:00', '2026-10-31 16:40:00'),
('Carlos',   'Mendoza Quispe',  'carlos.demo@iotelito.com',   NULL, '987654321', 'TAXISTA', NULL,                FALSE, '2026-11-02 09:15:00', '2026-11-02 09:15:00'),
('Patricia', 'Luna Vargas',     'patricia.demo@iotelito.com', NULL, '987555666', 'TAXISTA', NULL,                FALSE, '2026-11-05 14:20:00', '2026-11-05 14:20:00'),
('Diego',    'Ramos Paredes',   'diego.demo@iotelito.com',    NULL, '987777888', 'TAXISTA', NULL,                FALSE, '2026-11-08 16:45:00', '2026-11-08 16:45:00'),
('Valeria',  'Mejia Flores',    'valeria.demo@iotelito.com',  NULL, '987121212', 'TAXISTA', NULL,                FALSE, '2026-11-12 19:30:00', '2026-11-12 19:30:00');

-- Fichas de taxista: 4 aprobados (Rosa sin disponibilidad), 2 rechazados y 4 pendientes.
-- calificacion y cantidad_servicios se recalculan al final desde los servicios.
INSERT INTO taxistas (usuario_id, tipo_documento, numero_documento, fecha_nacimiento, domicilio, numero_licencia, categoria_licencia, fecha_vencimiento_licencia, marca_vehiculo, modelo_vehiculo, anio_vehiculo, color_vehiculo, placa_vehiculo, estado_solicitud, disponible, acepta_terminos, fecha_solicitud) VALUES
((SELECT id FROM usuarios WHERE correo = 'andrea.demo@iotelito.com'),   'DNI',                '72148965',  '1990-03-12', 'Av. El Sol 245, Cusco',            'Q72148965', 'A-IIb', '2028-05-10', 'Honda',      'City',     2021, 'Plata',  'HIL-310', 'APROBADO',  TRUE,  TRUE, '2026-10-18 08:40:00'),
((SELECT id FROM usuarios WHERE correo = 'maria.demo@iotelito.com'),    'DNI',                '73569421',  '1992-04-15', 'Av. Larco 123, Miraflores, Lima',  'Q73569421', 'A-IIb', '2028-08-18', 'Nissan',     'Sentra',   2021, 'Gris',   'XYZ-987', 'APROBADO',  TRUE,  TRUE, '2026-10-20 10:00:00'),
((SELECT id FROM usuarios WHERE correo = 'luis.demo@iotelito.com'),     'DNI',                '70324598',  '1988-11-02', 'Jr. Ica 410, Lima',                'Q70324598', 'A-IIa', '2026-12-01', 'Kia',        'Cerato',   2020, 'Blanco', 'MNO-456', 'RECHAZADO', FALSE, TRUE, '2026-10-22 11:30:00'),
((SELECT id FROM usuarios WHERE correo = 'fernando.demo@iotelito.com'), 'DNI',                '73456789',  '1985-07-21', 'Av. Brasil 1550, Jesús María',     'Q73456789', 'A-IIb', '2029-01-30', 'Chevrolet',  'Onix',     2020, 'Rojo',   'LIM-204', 'APROBADO',  TRUE,  TRUE, '2026-10-25 12:10:00'),
((SELECT id FROM usuarios WHERE correo = 'rosa.demo@iotelito.com'),     'DNI',                '76985214',  '1994-09-09', 'Calle Los Pinos 88, Surco',        'Q76985214', 'A-IIb', '2027-06-15', 'Kia',        'Rio',      2019, 'Azul',   'HOT-118', 'APROBADO',  FALSE, TRUE, '2026-10-28 17:05:00'),
((SELECT id FROM usuarios WHERE correo = 'jorge.demo@iotelito.com'),    'DNI',                '71896325',  '1983-01-27', 'Av. La Cultura 1020, Cusco',       'Q71896325', 'A-IIa', '2026-11-30', 'Volkswagen', 'Virtus',   2022, 'Negro',  'CUS-620', 'RECHAZADO', FALSE, TRUE, '2026-10-30 09:50:00'),
((SELECT id FROM usuarios WHERE correo = 'carlos.demo@iotelito.com'),   'DNI',                '71234567',  '1991-05-30', 'Av. Arequipa 3010, San Isidro',    'Q71234567', 'A-IIb', '2028-03-22', 'Toyota',     'Corolla',  2022, 'Gris',   'ABC-123', 'PENDIENTE', FALSE, TRUE, '2026-11-02 09:15:00'),
((SELECT id FROM usuarios WHERE correo = 'patricia.demo@iotelito.com'), 'CARNET_EXTRANJERIA', '001234567',  '1993-12-04', 'Av. Javier Prado 760, San Isidro', 'Q75842136', 'A-IIb', '2029-04-11', 'Hyundai',    'Elantra',  2023, 'Blanco', 'KLP-789', 'PENDIENTE', FALSE, TRUE, '2026-11-05 14:20:00'),
((SELECT id FROM usuarios WHERE correo = 'diego.demo@iotelito.com'),    'DNI',                '74681293',  '1996-02-18', 'Jr. Junín 230, Trujillo',          'Q74681293', 'A-IIb', '2028-10-05', 'Toyota',     'Yaris',    2022, 'Plata',  'TAX-452', 'PENDIENTE', FALSE, TRUE, '2026-11-08 16:45:00'),
((SELECT id FROM usuarios WHERE correo = 'valeria.demo@iotelito.com'),  'PASAPORTE',          'PE4321698',  '1997-08-25', 'Calle Mercaderes 115, Arequipa',   'Q74321698', 'A-IIb', '2029-09-19', 'Mazda',      '2 Sedan',  2023, 'Rojo',   'MIR-907', 'PENDIENTE', FALSE, TRUE, '2026-11-12 19:30:00');

-- Los 6 documentos de cada taxista. El estado de cada documento sigue al de su solicitud;
-- a los rechazados se les observa el SOAT.
INSERT INTO documentos_taxista (taxista_id, tipo, archivo_url, content_type, tamano_bytes, estado, observacion, fecha_subida)
SELECT t.id,
       d.tipo,
       CONCAT('taxistaDocs/', t.id, '/', LOWER(d.tipo), '.jpg'),
       'image/jpeg',
       d.tamano,
       CASE t.estado_solicitud WHEN 'APROBADO' THEN 'APROBADO' WHEN 'RECHAZADO' THEN 'RECHAZADO' ELSE 'PENDIENTE' END,
       CASE WHEN t.estado_solicitud = 'RECHAZADO' AND d.tipo = 'SOAT' THEN 'SOAT vencido' END,
       t.fecha_solicitud
FROM taxistas t
CROSS JOIN (
    SELECT 'FOTO_CONDUCTOR' AS tipo, 180000 AS tamano
    UNION ALL SELECT 'FOTO_VEHICULO', 240000
    UNION ALL SELECT 'DOCUMENTO_IDENTIDAD', 150000
    UNION ALL SELECT 'LICENCIA', 150000
    UNION ALL SELECT 'SOAT', 120000
    UNION ALL SELECT 'TARJETA_PROPIEDAD', 130000
) d;

-- Cuatro pedidos: dos finalizados, uno en camino y uno esperando taxista.
INSERT INTO solicitudes_taxi (reserva_id, cliente_uid, cliente_nombre, hotel_id, hotel_nombre, hotel_ciudad, origen, aeropuerto_codigo, destino, hora_recojo, cantidad_pasajeros, cantidad_equipaje, distancia_km, duracion_estimada_min, estado, fecha_solicitud) VALUES
(900001, 'demo-cliente-ana',   'Ana García',   1, 'Hotel Miraflores Park', 'Lima',  'Av. Malecón de la Reserva 1035, Miraflores', 'LIM', 'Aeropuerto Internacional Jorge Chávez',             '10:30', 2, 2, 18.20, 35,   'FINALIZADA', '2026-11-10 09:40:00'),
(900002, 'demo-cliente-luis',  'Luis Paredes', 2, 'Hotel Andino Cusco',    'Cusco', 'Calle Saphi 123, Cusco',                     'CUZ', 'Aeropuerto Internacional Alejandro Velasco Astete', '06:15', 1, 1, 5.10,  15,   'FINALIZADA', '2026-11-12 05:30:00'),
(900003, 'demo-cliente-sofia', 'Sofía Ramos',  1, 'Hotel Miraflores Park', 'Lima',  'Av. Malecón de la Reserva 1035, Miraflores', 'LIM', 'Aeropuerto Internacional Jorge Chávez',             '18:00', 3, 3, 18.20, 40,   'ACEPTADA',   '2026-11-16 17:05:00'),
(900004, 'demo-cliente-diego', 'Diego Salas',  1, 'Hotel Miraflores Park', 'Lima',  'Av. Malecón de la Reserva 1035, Miraflores', 'LIM', 'Aeropuerto Internacional Jorge Chávez',             '21:30', 1, 0, NULL,  NULL, 'PENDIENTE',  '2026-11-16 19:20:00');

INSERT INTO servicios_taxi (solicitud_id, taxista_id, estado, hora_aceptacion, hora_inicio, hora_traslado, hora_fin, distancia_real_km, codigo_qr, qr_validado) VALUES
((SELECT id FROM solicitudes_taxi WHERE reserva_id = 900001),
 (SELECT t.id FROM taxistas t JOIN usuarios u ON u.id = t.usuario_id WHERE u.correo = 'maria.demo@iotelito.com'),
 'FINALIZADO', '2026-11-10 09:45:00', '2026-11-10 10:05:00', '2026-11-10 10:32:00', '2026-11-10 11:08:00', 18.60, 'TX1-demo-900001', TRUE),
((SELECT id FROM solicitudes_taxi WHERE reserva_id = 900002),
 (SELECT t.id FROM taxistas t JOIN usuarios u ON u.id = t.usuario_id WHERE u.correo = 'andrea.demo@iotelito.com'),
 'FINALIZADO', '2026-11-12 05:35:00', '2026-11-12 05:50:00', '2026-11-12 06:16:00', '2026-11-12 06:33:00', 5.30, 'TX1-demo-900002', TRUE),
((SELECT id FROM solicitudes_taxi WHERE reserva_id = 900003),
 (SELECT t.id FROM taxistas t JOIN usuarios u ON u.id = t.usuario_id WHERE u.correo = 'fernando.demo@iotelito.com'),
 'EN_CAMINO', '2026-11-16 17:10:00', '2026-11-16 17:35:00', NULL, NULL, NULL, 'TX1-demo-900003', FALSE);

-- Últimas ubicaciones del servicio en camino (Miraflores).
INSERT INTO ubicaciones_taxista (servicio_id, taxista_id, latitud, longitud, precision_m, registrada_en)
SELECT s.id, s.taxista_id, p.latitud, p.longitud, p.precision_m, p.registrada_en
FROM servicios_taxi s
JOIN solicitudes_taxi st ON st.id = s.solicitud_id
CROSS JOIN (
    SELECT -12.1046 AS latitud, -77.0353 AS longitud, 9.0 AS precision_m, CAST('2026-11-16 17:36:00' AS DATETIME) AS registrada_en
    UNION ALL SELECT -12.1089, -77.0362, 7.5, CAST('2026-11-16 17:36:15' AS DATETIME)
    UNION ALL SELECT -12.1131, -77.0379, 8.0, CAST('2026-11-16 17:36:30' AS DATETIME)
) p
WHERE st.reserva_id = 900003;

INSERT INTO calificaciones_taxista (servicio_taxi_id, cliente_uid, taxista_id, puntuacion, comentario, fecha)
SELECT s.id, st.cliente_uid, s.taxista_id, c.puntuacion, c.comentario, c.fecha
FROM servicios_taxi s
JOIN solicitudes_taxi st ON st.id = s.solicitud_id
JOIN (
    SELECT 900001 AS reserva_id, 5 AS puntuacion, 'Muy puntual y amable.' AS comentario, CAST('2026-11-10 11:20:00' AS DATETIME) AS fecha
    UNION ALL SELECT 900002, 4, 'Buen servicio, auto limpio.', CAST('2026-11-12 06:45:00' AS DATETIME)
) c ON c.reserva_id = st.reserva_id;

-- Resumen de cada taxista calculado desde sus servicios y calificaciones.
UPDATE taxistas t
SET cantidad_servicios = (SELECT COUNT(*) FROM servicios_taxi s
                          WHERE s.taxista_id = t.id AND s.estado = 'FINALIZADO'),
    calificacion = COALESCE((SELECT ROUND(AVG(CAST(c.puntuacion AS DECIMAL(5,2))), 2)
                             FROM calificaciones_taxista c WHERE c.taxista_id = t.id), 0);

INSERT INTO logs_sistema (usuario_id, accion, detalle, fecha)
SELECT u.id, l.accion, l.detalle, l.fecha
FROM usuarios u
CROSS JOIN (
    SELECT 'APROBAR_TAXISTA' AS accion, 'Andrea Cordova Rios (HIL-310)' AS detalle, CAST('2026-10-19 10:00:00' AS DATETIME) AS fecha
    UNION ALL SELECT 'APROBAR_TAXISTA', 'Maria Silva Torres (XYZ-987)', CAST('2026-10-21 09:30:00' AS DATETIME)
    UNION ALL SELECT 'RECHAZAR_TAXISTA', 'Luis Ruiz Vargas (MNO-456): SOAT vencido', CAST('2026-10-23 15:00:00' AS DATETIME)
) l
WHERE u.correo = 'admin@iotelito.com';
