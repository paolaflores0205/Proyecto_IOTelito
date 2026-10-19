# Adaptaciones pendientes del rol cliente

El código del cliente no se modificó en este avance. Tampoco se hizo merge de su rama.
Su base `iotelito_local.db` y la nueva base administrativa `iotelito_superadmin.db` tienen nombres y tablas diferentes, para poder integrar sin reemplazar ni borrar las reservas existentes.

## 1. Llevar Room fuera del hilo principal

En la rama `feature/inicio-cliente-reserva`, `IoTelitoDatabase` usa `allowMainThreadQueries()`.
La clase 7, extra Database, indica usar un repositorio con trabajo en segundo plano.

No basta con borrar esa llamada: cambiar también los consumidores síncronos.

- `ReservaRepository`: consultar, guardar, cambiar estado, sembrar datos y calcular IDs dentro de un hilo de trabajo.
- `NotificacionRepository`: consultas, altas y cambios de leído fuera del hilo de la pantalla.
- Consultas de listas: devolver `LiveData` y observarlo con ViewModel en fragmentos usando `getViewLifecycleOwner()`.
- Guardar reserva: usar un resultado/callback y navegar a ReservaConfirmadaActivity solamente cuando termine correctamente.
- Checkout/cobro: actualizar el estado, confirmar la escritura y luego emitir el aviso. Conservar el aviso en la bandeja aunque no se conceda permiso del sistema.
- Mantener la migración 1 a 2 que ya existe. No usar borrado destructivo para añadir tablas.

## 2. Conectar el catálogo de hoteles

Hoy el cliente consulta `SampleData.HOTELS`; por eso no verá hoteles nuevos ni ediciones persistentes del superadmin.

Punto de acceso ya disponible:

```java
SuperadminRepository.getInstance(requireContext()).hotels()
    .observe(getViewLifecycleOwner(), hoteles -> {
        // Adaptar al modelo/adapter del cliente y refrescar los resultados.
    });
```

Clase: `com.example.proyecto_iotelito.data.superadmin.SuperadminRepository`.
Su constructor inicia la carga en segundo plano. Las listas pueden llegar primero vacías mientras termina la carga; el repositorio expone `ready()` y `error()`.

Para detalles está `hotel(id)`, que devuelve un hotel observable o null si no existe. No reemplazar silenciosamente un ID desconocido por el primer hotel.

Reglas de integración:

- Conservar los IDs del catálogo: las reservas existentes los referencian.
- Reemplazar búsquedas estáticas en Explorar, Resultados, ListaHoteles y DetalleHotel.
- Mostrar únicamente hoteles activos para nuevas búsquedas. Conservar acceso al hotel de una reserva histórica aunque se desactive.
- Mantener precio, habitaciones, capacidad, servicios y valoraciones del modelo del cliente: el modelo administrativo todavía no almacena todo eso.
- Un hotel nuevo tiene cero habitaciones. No inventarle disponibilidad/precios ni permitir reservar hasta que se registren habitaciones y tarifas.
- Si se unifican las bases en el futuro, preparar una migración/copia explícita que conserve reservas y notificaciones. No cambiar el nombre del archivo de Room esperando que los datos se trasladen solos.

## 3. Separar los datos por usuario

Las entidades de reserva y notificación actuales del cliente no contienen el propietario de los datos.
Agregar un identificador de usuario, migrar lo existente y filtrar por la cuenta autenticada.
Las preferencias del perfil tampoco deberían mezclar los datos de distintas cuentas en un único perfil global.
Esto evita mostrar las reservas o avisos de otra cuenta en el mismo teléfono.

## 4. Relacionar estado administrativo y login

La activación/desactivación de usuarios de superadmin ahora se guarda localmente.
El login actual sigue usando sus cuentas de demostración, sin consultar este estado.
Al integrar, relacionar de forma estable cada cuenta con su registro administrativo y comprobar si está activa al autenticar.
No asociar usuarios por posición de la lista ni asumir que los IDs de ejemplo son los del servicio de autenticación futuro.

## 5. Mantener el menú corregido al combinar ramas

Conservar de la rama superadmin:

- La lógica de insets de `IoTelitoApplication`, incluida la ejecución en `onActivityPostCreated`.
- El fondo y dimensiones del menú inferior actuales.
- El catálogo ampliado y los IDs compartidos entre los 12 hoteles iniciales.

Al integrar sus inicializaciones de perfil/canal, añadirlas sobre esa versión; no reemplazar la clase completa por su versión anterior.
Los cambios de superadmin utilizan `SuperadminNotifications` y `SuperadminDatabase`; no cambian la clase de notificaciones del cliente.

## 6. Comprobación conjunta

1. Crear una reserva en cliente y cerrar/abrir la app: sigue presente.
2. Editar un hotel en superadmin: cliente muestra el cambio cuando se conecte al repositorio.
3. Crear un hotel nuevo: aparece en el catálogo pero sin disponibilidad inventada.
4. Denegar notificaciones: no falla la reserva y el aviso se conserva dentro de la app.
5. Conceder permiso: la notificación abre la reserva o el hotel correcto, según el rol.
6. Cambiar de cuenta: no se mezclan reservas, perfiles ni notificaciones.
7. Revisar el menú en el A55 sin recuperar el espacio inferior antiguo.
