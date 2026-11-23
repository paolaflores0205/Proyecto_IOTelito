# Checklist de pruebas finales — IoTelito

Lista para probar el sistema completo antes de la presentación final (14/12/2026).
Cada punto sale del *Plan de proyecto* del curso. Marca `[x]` cuando funcione y anota
en **Errores encontrados** lo que falle (pantalla, pasos y qué esperabas).

**Antes de empezar**
- [ ] Laboratorio de AWS de Mariana encendido (web de taxistas y base MySQL).
- [ ] Laboratorio con la API de fotos encendido (fotos de perfil y de hoteles).
- [ ] App instalada desde la rama `feat/hotel-admin` (última versión).
- [ ] Emulador con ubicación puesta en la ciudad del hotel (Extended controls → Location).
- [ ] Dos dispositivos para el flujo de taxi (cliente y taxista con cuentas distintas).

---

## 1. Cliente

**Cuenta**
- [ ] Se registra solo (nombres, apellidos, tipo y número de documento, nacimiento, correo, teléfono, domicilio, foto) y queda habilitado.
- [ ] No acepta datos inválidos (documento, menor de 18, teléfono de 9 dígitos, domicilio corto).
- [ ] Inicia sesión, recupera contraseña y cierra sesión.
- [ ] Edita su perfil y su foto (se guarda en la nube).

**Hoteles y reservas**
- [ ] Ve hoteles, fotos, servicios, lugares cercanos y valoraciones.
- [ ] Busca por fechas y huéspedes; solo aparecen habitaciones disponibles.
- [ ] Reserva con tarjeta simulada; la reserva aparece en "Mis reservas".
- [ ] No puede reservar dos estadías que se crucen: el mensaje dice el motivo real.
- [ ] Dos clientes no pueden reservar la misma habitación en fechas que se crucen.
- [ ] Ve el historial de sus reservas (próximas, en curso, pasadas).

**Chat con el hotel**
- [ ] Con reserva activa puede escribir al hotel; los mensajes muestran fecha y hora y persisten.
- [ ] Después del checkout ya no puede enviar mensajes.

**Checkout**
- [ ] Desde el día de salida puede pedir el checkout.
- [ ] Debe dejar una valoración (1–5) y observaciones.
- [ ] Recibe la notificación cuando el hotel cobra.
- [ ] El comprobante muestra alojamiento, servicios consumidos, cargo por daños y total.

**Taxi gratuito**
- [ ] Solo se ofrece si el alojamiento alcanza el monto mínimo **de ese hotel**.
- [ ] Elige el aeropuerto de destino (solo los de la ciudad del hotel).
- [ ] Ve los datos del taxista, vehículo, valoración y ubicación en el mapa.
- [ ] Chatea con el taxista.
- [ ] Muestra el QR (y el código en texto) al llegar.
- [ ] Califica al taxista al finalizar.
- [ ] Con la web de taxis apagada, la app avisa "servicio de taxis no disponible" y lo demás sigue funcionando.

## 2. Administrador de hotel

- [ ] Inicia sesión y solo ve su hotel.
- [ ] Edita datos del hotel, ubicación y lugares históricos cercanos.
- [ ] Configura el **monto mínimo para el taxi gratuito**.
- [ ] Sube fotos del hotel (mínimo 4).
- [ ] Registra habitaciones (tipo, capacidad adultos/niños, área m², precio).
- [ ] Registra servicios (nombre, descripción, precio, imagen; con o sin costo).
- [ ] Ve las reservas de su hotel y el estado del taxi (asignado, en camino, llegó).
- [ ] Recibe la alerta de checkout de un cliente.
- [ ] Cobra el checkout: marca **servicios consumidos** y, si hubo daños, monto + motivo + observación.
- [ ] Reportes diario, mensual y anual de ventas.
- [ ] Reporte de **ingresos por servicio, de menor a mayor**.
- [ ] Ve y responde los mensajes de clientes con reserva activa.

## 3. Taxista

**Web de gestión de taxistas** (`/taxistas/registro`)
- [ ] Se registra con datos personales, vehículo, placa y fotos/documentos.
- [ ] Si usa un correo que ya es de un cliente de la app, el panel no deja aprobarlo y explica el motivo.

**App (después de ser aprobado)**
- [ ] Inicia sesión con la cuenta creada al aprobarlo (correo para elegir contraseña).
- [ ] Ve los pedidos disponibles con hotel y destino.
- [ ] Acepta un pedido; otro taxista ya no puede aceptarlo.
- [ ] Ve su ubicación en el mapa; la ubicación se envía sola durante el servicio.
- [ ] Cambia el estado: ASIGNADO → EN CAMINO → EN TRASLADO.
- [ ] Finaliza escaneando el QR del cliente (o escribiendo el código).
- [ ] Chatea con el pasajero en todas las pantallas del viaje.

## 4. Superadmin

**App**
- [ ] Registra hoteles y les asigna un administrador.
- [ ] Activa y desactiva administradores de hotel, taxistas y clientes.
- [ ] Ve el reporte de reservas por hotel.
- [ ] Ve los logs de eventos.
- [ ] Ve la lista de taxistas de la web (por la API).

**Web de gestión de taxistas** (`/`)
- [ ] Inicia sesión (contraseña en Parameter Store: `/iotelito/taxiweb/admin-password`).
- [ ] Ve solicitudes pendientes con miniaturas de documentos y botón "Ver".
- [ ] Aprueba (se crea la cuenta en la app y llega el correo) y rechaza.
- [ ] Busca y filtra taxistas; ve su valoración promedio.

## 5. Requisitos de calidad

- [ ] Ninguna contraseña se guarda en texto plano (Firebase Auth y BCrypt en la web).
- [ ] Cada rol solo hace lo que le corresponde (probar abrir pantallas de otro rol).
- [ ] Un cliente solo ve sus reservas, chats, pagos y taxis.
- [ ] Se registran eventos relevantes (logs del superadmin y de la web).
- [ ] La app nunca se conecta directo a MySQL: todo lo de taxis va por la API REST.

---

## Pruebas automáticas (al 23/11/2026)

| Conjunto | Resultado |
|---|---|
| App: pruebas unitarias (`./gradlew testDebugUnitTest`) | 177 pasan |
| Reglas de Firestore en el emulador (`app/src/test/js`) | 10 conjuntos pasan |
| Web de taxistas (`./mvnw test` en la rama `feat/taxi-api-web`) | 38 pasan |
| Servicios desplegados (web, API de taxis, API de fotos) | responden; sin token, la API rechaza (401) |

## Errores encontrados

| Fecha | Rol | Pantalla | Qué pasó | Responsable | Estado |
|---|---|---|---|---|---|
| | | | | | |
