# Mejoras de superadmin: reportes, fotos, ubicación y alertas

> Actualización posterior: los hoteles iniciales ahora tienen galerías de fotos ilustrativas locales. La versión actual de la base administrativa es 4. Consulta FOTOS_HOTELES_REFERENCIA.md para las fuentes y la carga que conserva fotos propias.

## Actualización: historial amplio y foto centrada

Reportes ahora tiene el interruptor **Incluir historial de demostración**, activado inicialmente. Combina tus reservas locales con una tabla separada de ejemplos, sin añadir operaciones ficticias a Mis reservas, checkout, taxi o notificaciones del cliente. Desactívalo para consultar únicamente las reservas locales existentes.

Con el catálogo inicial de 12 hoteles y el año de instalación 2026 se generan **28 418 reservas simuladas**, con llegadas distribuidas durante 2025 y 2026. Varían por hotel, mes, duración, cantidad de habitaciones, tarifa y estado (confirmadas, completadas o canceladas). La generación evita ocupar una misma habitación dos veces la misma noche. Los códigos empiezan por DEMO-RPT- y los IDs de estos ejemplos son negativos; no comparten tabla ni IDs con reservas del cliente.

El historial se crea al entrar por primera vez a Reportes y se guarda en Room en segundo plano. No se vuelve a insertar si ya existe. El período generado queda guardado: cambiar la fecha del teléfono no desplaza ni reemplaza silenciosamente esos registros. Los hoteles sin habitaciones no reciben reservas simuladas.

La base administrativa pasa a versión 3: la migración 1→2 conserva la galería y 2→3 agrega report_demo_bookings sin modificar hoteles, usuarios, fotos ni la base del cliente. Actualiza ejecutando la app normalmente; no borres sus datos ni la desinstales.

Para comprobar los filtros, entra en Reportes, deja el historial activado y alterna hotel, Diario/Mensual/Anual y Cambiar fecha. Un período fuera de los años generados puede seguir mostrando cero; eso es correcto. El texto superior indica cuántos registros simulados y locales forman la fuente.

En Registrar/Editar hotel, la imagen de referencia y cualquier foto única quedan centradas horizontalmente. Si hay varias fotos, se conserva el carrusel desplazable desde el inicio.

Las secciones siguientes describen la implementación anterior; el historial de demostración y la versión 3 son las ampliaciones de esta actualización.

## 1. Reportes conectados a datos locales

Antes, el selector de hotel no tenía efecto y las tarjetas y el ranking mostraban cifras escritas en el XML. Ahora ReportesFragment observa los hoteles administrativos y las reservas guardadas con Room.

ReportCalculator aplica simultáneamente hotel, período y fecha. El desplegable incluye todo el catálogo persistido, no solo dos hoteles de ejemplo.

- Diario: el día elegido. Gráfico de llegadas, salidas y reservas en estadía.
- Mensual: el mes de la fecha elegida. Llegadas no canceladas agrupadas por tramos de días: 1–7, 8–14, etc.
- Anual: el año de la fecha elegida. Llegadas no canceladas por cada uno de los doce meses.
- El título cambia entre Reservas del día, del mes y del año.
- El ranking usa el mismo hotel/período/fecha. Si eliges un hotel, solo aparece ese hotel.

El botón Cambiar fecha abre un MaterialDatePicker. Es importante elegir una fecha con reservas: un filtro sin datos muestra cero, no números inventados.

### Qué significa cada indicador

Reservas con llegada: reservas cuya entrada cae en el rango, incluyendo canceladas. No representa fecha de creación, porque ese dato aún no se registra.

Cancelaciones: de esas llegadas, cuántas están canceladas; el porcentaje usa el mismo denominador.

Ocupación estimada: noches-habitación de reservas no canceladas que coinciden con el período / habitaciones del hotel por días del período. La fecha de salida no cuenta como una noche más. Las combinaciones de habitaciones se cuentan por sus códigos separados por comas. Es una estimación local: todavía no hay un inventario histórico de disponibilidad, bloqueos y capacidad.

Importe de estadías: precio total prorrateado por las noches que caen dentro del período, excluyendo cancelaciones. No se presenta como dinero efectivamente cobrado ni como una contabilidad real.

Los hoteles sin inventario muestran Sin inventario en el ranking. Se conservan los contadores iniciales del dashboard, pero no se usan como fuente del reporte porque no incluyen fechas.

## 2. Fotos y carrusel local

En Registrar/Editar hotel:

1. Pulsa Añadir fotos y elige imágenes del selector de documentos del teléfono.
2. Puedes añadir hasta ocho fotos, en varias selecciones.
3. Desliza horizontalmente para verlas.
4. Usa Usar portada para colocar una foto primero; Quitar retira la foto del formulario.
5. Pulsa Guardar para persistir los cambios del hotel.

La primera imagen se usa como portada de la lista. El detalle administrativo muestra el carrusel completo. También se transmite la galería al detalle del cliente y la portada a sus tarjetas.

HotelPhotoStore crea copias JPEG reducidas dentro de files/hotel_photos de la aplicación. Respeta la orientación EXIF y no necesita acceso general a todas las fotos. No depende de que continúe disponible el permiso temporal del selector ni de que la foto original permanezca en la galería del teléfono.

La copia se realiza en segundo plano. SuperadminViewModel retiene el resultado de la importación durante una rotación, para que la nueva actividad reciba las fotos. Los archivos solo se asocian al hotel al guardar. Cancelar el formulario no altera la galería guardada; las copias importadas y no usadas pueden quedar en el almacenamiento privado hasta una limpieza futura.

AdminHotelEntity guarda las rutas en photosJson. La base administrativa pasa de versión 1 a 2 mediante ALTER TABLE: agrega únicamente esa columna y conserva hoteles, usuarios, eventos y alertas. No desinstales la app ni borres sus datos para actualizarla.

Las fotos son locales a esta instalación; no se suben ni se sincronizan con otros teléfonos.

## 3. Selección de ubicación y búsqueda de dirección

1. Abre el mapa desde el formulario.
2. Escribe una dirección con distrito y ciudad; pulsa Buscar dirección.
3. Elige uno de los resultados y revisa el punto del mapa.
4. También puedes mover el mapa o tocar una calle para colocar el cursor.
5. Espera a que aparezca la dirección y pulsa confirmar.
6. Al volver, Dirección, Distrito y Ciudad se reemplazan por los datos encontrados. Las coordenadas se guardan internamente, no se muestran como descripción al usuario.

Usar mi ubicación centra el selector en la ubicación disponible del teléfono para que puedas revisar y confirmar la entrada del hotel; no guarda automáticamente un punto sin revisión.

Se conserva el mapa OpenStreetMap/Leaflet que ya tenía el proyecto. No se añadió un SDK de Google Maps ni una API de pago. La búsqueda y la conversión de punto a dirección usan el Geocoder asíncrono de Android.

El servicio puede no devolver un distrito o ciudad, o estar temporalmente no disponible. En ese caso no se conserva silenciosamente el distrito de otro hotel: se deja el campo pendiente, editable, para completarlo. Si no se obtiene una dirección, se muestra el error y no se permite confirmar ese resultado; prueba otra calle o verifica internet.

Referencia oficial: [Geocoder de Android](https://developer.android.com/reference/android/location/Geocoder). El proveedor no garantiza disponibilidad o precisión: revisa siempre el resultado antes de guardar.

## 4. Alertas con filtros

Desde Inicio:

- Usuarios inactivos abre Usuarios con el chip Inactivo seleccionado.
- Hoteles sin administrador abre Hoteles con el chip Sin administrador asignado seleccionado.

Los filtros se ven y pueden retirarse. Se pueden combinar con la búsqueda; en hoteles también con distrito. Al asignar un administrador, el hotel deja de cumplir el filtro y desaparece de esa lista.

SuperadminMainActivity transporta alert_filter como argumento de navegación. Cada fragmento aplica el filtro inicial correspondiente, sin cambiar la altura, el fondo ni la lógica de insets del menú inferior.

## 5. Verificación

Se añadieron tres pruebas unitarias del cálculo: filtros combinados, límites de fecha/estadías entre meses y períodos sin datos.

Se añadieron tres pruebas instrumentadas: navegación filtrada y ranking, importación/persistencia de fotos y migración que conserva un hotel, y disponibilidad del buscador del mapa. La migración se prueba con una base temporal; no borra la base del usuario.

La búsqueda real depende de internet y del proveedor Geocoder. La prueba automatizada de la pantalla no demuestra por sí sola que el proveedor encuentre todas las direcciones: comprueba manualmente una dirección conocida y luego mueve el punto para verificar los campos.

No se hicieron cambios al tamaño ni al fondo del menú inferior. No se hizo push.
