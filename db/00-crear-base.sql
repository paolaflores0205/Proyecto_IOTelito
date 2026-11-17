-- IoTelito - Sistema web de gestión de taxistas (MySQL 8)
-- Paso 1 de 3: crea la base. Después ejecutar schema.sql (DDL) y data.sql (DML).
--
-- Recomendado: un usuario propio para la web en lugar de root. Elige tú la contraseña
-- y pásala a la web con la variable de entorno DB_PASSWORD (nunca en el repositorio):
--   CREATE USER IF NOT EXISTS 'taxiweb'@'localhost' IDENTIFIED BY '<contraseña>';
--   GRANT SELECT, INSERT, UPDATE, DELETE ON iotelito.* TO 'taxiweb'@'localhost';

CREATE DATABASE IF NOT EXISTS iotelito
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE iotelito;
