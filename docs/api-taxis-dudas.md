# Observaciones de integración de la app

El contrato api-taxis-v1.yaml se conserva sin modificaciones.

- `TaxistaPerfil` no devuelve documento, domicilio ni licencia. La app no inventará estos campos; el DNI de ejemplo se oculta. Una ampliación requiere acuerdo del equipo.
- Los IDs del contrato son int64; las reservas y la navegación Android actuales usan int. Se conserva esa interfaz y se rechazan valores fuera de rango en el adaptador, sin truncarlos.
- La API falsa vive en memoria y no comparte datos entre dos procesos/teléfonos ni sobrevive al cierre completo. El recorrido se prueba cambiando de cuenta dentro de la misma instalación/proceso. Auth y reservas siguen siendo los del proyecto; el simulador no crea cuentas ni reservas.
- La base configurada en `taxiApi.url` debe incluir `/api/v1`. Solo las pruebas JVM del transporte permiten HTTP en 127.0.0.1; la app usa HTTPS.
- osmdroid 6.1.20 es la última versión publicada y su repositorio está archivado. Se usa por petición explícita del equipo; una sustitución requiere acuerdo.
- Desactivar una cuenta durante un viaje y permitir que lo complete necesita una política de backend consistente con la validación de `users.active`. La app muestra el error recibido; no omite la autorización.
