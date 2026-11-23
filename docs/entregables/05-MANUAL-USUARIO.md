# Manual de usuario — IoTelito

IoTelito es una app Android para reservar alojamiento en hoteles. Según el rol de la cuenta,
muestra distintas pantallas:

| Rol | Dónde trabaja | Pestañas de la app |
|---|---|---|
| **Cliente** | App | Explorar · Reservas · Mensajes · Perfil |
| **Administrador de hotel** | App | Hotel · Reservas · Mensajes · Reportes |
| **Taxista** | Web (registro) y app | Inicio · Solicitudes · Historial · Perfil |
| **Superadmin** | App y web de taxistas | Inicio · Usuarios · Hoteles · Reportes |

Al iniciar sesión, la app abre automáticamente las pantallas del rol de la cuenta.

> Sugerencia para la entrega: agregar una captura de pantalla bajo cada sección.

---

## 1. Cliente

### Crear la cuenta
1. En la pantalla de inicio elige **Registrarse**.
2. Completa nombres, apellidos, tipo y número de documento (DNI, pasaporte o carné de
   extranjería), fecha de nacimiento (mayor de 18 años), correo, teléfono (9 dígitos), domicilio
   y, si quieres, una foto.
3. La cuenta queda habilitada al instante. Si olvidas la contraseña, usa **¿Olvidaste tu contraseña?**.

### Buscar y reservar
1. En **Explorar**, elige destino, fechas y huéspedes.
2. Abre un hotel para ver fotos, habitaciones, servicios, lugares cercanos y valoraciones.
3. Elige la habitación y registra una **tarjeta de prueba** (el pago es simulado).
4. La reserva aparece en **Reservas**.

No puedes reservar dos estadías cuyas fechas se crucen: la app te muestra el motivo.

### Chatear con el hotel
Desde el detalle de una reserva activa (desde que se confirma hasta el checkout) puedes escribirle
al hotel. Los mensajes quedan guardados con fecha y hora; también aparecen en **Mensajes**.

### Hacer el checkout
1. Desde el **día de salida**, abre la reserva y pide el **checkout**.
2. Califica tu estadía (1 a 5 estrellas) y escribe tus observaciones.
3. El hotel revisa la salida y registra el cobro. Te llega una **notificación** y en la reserva ves
   el comprobante: alojamiento, **servicios consumidos**, cargos por daños (con su motivo) y total.

### Pedir el taxi gratuito al aeropuerto
Disponible si tu alojamiento alcanza el **monto mínimo que define el hotel** (por defecto S/ 1000)
y ya pediste el checkout.

1. En el detalle de la reserva toca **Solicitar taxi de cortesía**.
2. Elige el aeropuerto de destino (solo aparecen los de la ciudad del hotel), la hora, los
   pasajeros y las maletas.
3. Cuando un taxista acepta, ves su nombre, vehículo, placa, valoración y **su ubicación en el mapa**.
   Puedes chatear con él.
4. Al llegar, toca **Ver QR** y muéstraselo al taxista. Si su cámara falla, puede escribir el código
   que aparece debajo del QR.
5. Al finalizar, **califica al taxista**.

Si el servicio de taxis no está disponible, la app te avisa; el resto (reservas, chat con el hotel)
sigue funcionando.

### Perfil
En **Perfil** puedes cambiar tus datos y tu foto, y cerrar sesión.

---

## 2. Administrador de hotel

Su cuenta la crea el superadmin y la asigna a un hotel. Solo ve los datos de ese hotel.

### Pestaña Hotel
- **Datos del hotel:** nombre, dirección, descripción, teléfono, correo y **lugares históricos
  cercanos**. Aquí también se fija el **monto mínimo para el taxi gratuito** (S/; 0 = siempre gratis).
- **Fotos:** galería del hotel, mínimo **4 fotos**.
- **Habitaciones:** tipo (estándar, económica, lux…), capacidad (adultos y niños), área en m², precio
  por noche y descripción. Retirar una habitación la oculta sin borrar su historial.
- **Servicios:** nombre, descripción, precio, unidad (persona, sesión…) e imagen. Se pueden desactivar.

### Pestaña Reservas
- Lista de reservas del hotel con su estado y el **estado del taxi** (asignado, en camino, llegó).
- Cuando un cliente pide el checkout llega una **alerta**. Al abrir el **cobro**:
  1. Marca con **− / +** los **servicios que consumió el huésped** (hasta 5 distintos).
  2. Si hubo daños, activa el cargo adicional e ingresa **monto, motivo y observación** (obligatorios).
  3. Revisa el total y toca **Confirmar cobro simulado**. La reserva queda finalizada y el cliente
     recibe la notificación.

### Pestaña Mensajes
Lee y responde a los clientes que tienen una reserva activa en el hotel.

### Pestaña Reportes
- Ventas del período **diario, mensual o anual** (ingresos, reservas, ocupación y gráfico).
- **Ingresos por servicio adicional, de menor a mayor**, con el servicio que más generó resaltado.

---

## 3. Taxista

### Registrarse (web)
1. Entra a la web de gestión de taxistas, opción **Regístrate como taxista**
   (`https://bkmx72ue4k.execute-api.us-east-1.amazonaws.com/taxistas/registro`).
2. Completa datos personales (nombres, apellidos, documento, nacimiento, correo, teléfono, domicilio),
   licencia, vehículo y placa.
3. Sube tu foto, la del vehículo y tus documentos (DNI, licencia, SOAT y tarjeta de propiedad).
4. Espera la aprobación del superadmin. Al aprobarte te llega un **correo para elegir tu contraseña**.

### Usar la app
1. Inicia sesión con ese correo y contraseña.
2. En **Inicio** activa tu **disponibilidad**.
3. En **Solicitudes** ves los pedidos: hotel de recojo y aeropuerto de destino. Toca uno y
   **acepta**; desde ese momento es tuyo y deja de aparecer a los demás taxistas.
4. La app envía tu ubicación automáticamente durante el viaje y te muestra el mapa.
5. Avanza el viaje: **ASIGNADO → EN CAMINO** (vas al hotel) → **Llegué al hotel** (EN TRASLADO) →
   **Llegué al destino**.
6. Toca **Escanear QR del cliente** (o escribe el código) para **finalizar** el servicio.
7. Puedes chatear con el pasajero desde cualquier pantalla del viaje (ícono de chat junto a su nombre).
8. En **Historial** ves tus viajes terminados y en **Perfil** tu información y tu valoración promedio.

---

## 4. Superadmin

### En la app
- **Inicio:** resumen general, avisos y acceso a los **logs** (eventos del sistema).
- **Usuarios:** busca administradores de hotel, taxistas y clientes; **activa o desactiva** cuentas
  (indicando el motivo). Incluye la lista de taxistas de la web, consultada por la API.
- **Hoteles:** registra hoteles (datos, ubicación en el mapa, fotos) y **asigna su administrador**.
- **Reportes:** reservas por hotel.

### En la web de gestión de taxistas
1. Entra a `https://bkmx72ue4k.execute-api.us-east-1.amazonaws.com/` con el usuario
   `admin@iotelito.com` (la contraseña la entrega el equipo; está en Parameter Store).
2. En **Solicitudes** revisa los datos y los documentos (miniaturas y botón **Ver**).
3. **Aprobar** crea la cuenta del taxista en la app y le envía el correo; **Rechazar** la deja
   inhabilitada. Ambas acciones quedan en el registro de eventos.
4. Busca y filtra taxistas, y consulta su estado y su valoración promedio.
