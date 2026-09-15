# Plan de implementación — Módulo Administrador de hotel (rol HOTEL_ADMIN)

> Sección **02** del Figma *IoTelito – Mockups* (nodo `36:2`). 13 pantallas, Android nativo Java.
> **Adecuado al material del curso** (`D:\IOT\IOT`) y a la rama de integración del equipo.
>
> **Base:** rama `feat/hotel-admin` creada desde `origin/superadmin` (ya trae login por roles + módulo Superadmin, que es la **plantilla** de este módulo).

---

## 0. Convenciones del material (obligatorias)

Copiadas del módulo **Superadmin** de referencia (`ui/superadmin/*`). No inventar nada fuera de esto:

- **View Binding** en Activities y Fragments (`buildFeatures viewBinding true` ya está activo). Nada de `findViewById`.
- **Entrada por login-por-rol**: `LoginActivity` enruta `HOTEL_ADMIN → HotelAdminMainActivity`. Cuenta demo `admin@iotelito.pe`. (Ya cableado en el andamiaje.)
- **Shell** con `BottomNavigationView` + `replace()` de fragments (como `SuperadminMainActivity`).
- **Listas: inflado manual** de `item_*.xml` en un `LinearLayout` contenedor (patrón de `UsuariosFragment`). ⚠️ El **RecyclerView** llega en el **Lab 4 (29/sep)**: entonces se refactorizan las listas de todos los módulos juntos.
- **Gráficos/medidores: sin librería** → barras estáticas (`<View>` con alto fijo, fondo `@color/io_teal`) y `com.google.android.material.progressindicator.LinearProgressIndicator` (patrón de `fragment_superadmin_reportes.xml`). **No** usar MPAndroidChart ni un custom view.
- **Datos estáticos** en un `HotelAdminSampleData` propio (espejo de `SuperadminSampleData`). Persistencia real (Firebase) recién en Lab 6.
- **Recursos compartidos ya existentes**: colores `io_navy`, `io_divider`, `io_surface`, `io_success_*`, `io_warning_*`, `io_danger_*`; estilos `Widget.IoTelito.Card` / `.Button.Primary` / `.Button.Outline` / `.Input`; iconos `ic_home`, `ic_building`, `ic_bar_chart`, `ic_calendar`, `ic_chat`, `ic_person`, `ic_add`, `ic_edit`, etc. Reutilizar; crear solo lo que falte.
- **Nombres**: paquete `ui.hoteladmin.*`; `activity_hoteladmin_main`, `fragment_hoteladmin_*`, `item_hoteladmin_*`, `hoteladmin_bottom_nav_menu`. Textos siempre en `strings.xml`.

### 0.1 Técnicas del curso por pantalla (diapositivas 1TEL05)

Elementos de UI enseñados (Clase 3.2) y cómo aplican:

- **Tipo de habitación** → `Spinner` con `string-array`/`ArrayAdapter` (el curso lo recomienda para >3 opciones). Alternativa vista en el repo: `MaterialAutoCompleteTextView`.
- **Capacidad adultos/niños** → `EditText` numérico o steppers (`bg_stepper_button`).
- **Disponible / tiene costo / foto principal** → `Switch`/`RadioButton` con `setOnCheckedChangeListener`/`isChecked`.
- **Agregar** (habitación, servicio, foto) → **FAB** (`FloatingActionButton` con `app:srcCompat` + `setOnClickListener`).
- **Eliminar / confirmar cobro** → `MaterialAlertDialogBuilder` (positive/negative).
- **Validación de formularios** → `editText.setError(...)` (Clase 3.2 / guía).
- **Formularios que devuelven dato** (ej. editar → volver a la lista) → `ActivityResultLauncher` (Clase 2.2), o el patrón `onResume()` + refresco del superadmin. Preferir `onResume()` para mantener consistencia con el módulo superadmin.
- **Gráficos de Reportes** → barras estáticas (`<View>` con alto fijo) + `LinearProgressIndicator`, **sin librería** (idéntico a `fragment_superadmin_reportes.xml`).
- **Fotos del hotel** → cuadrícula (mín. 4) con indicador de foto principal; selección real opcional con `ActivityResultContracts.PickVisualMedia` (minSdk 34 lo soporta).
- **Imágenes/íconos** → `Vector Asset` en `drawable`; `ImageView` con `scaleType` para fotos.

### 0.2 Cronograma de labs (del sílabo/plan) — qué toca y cuándo

- **Lab 3 (15 sep):** mockups con navigation, menús y elementos UI → **fase actual del módulo admin.**
- **Lab 4 (29 sep):** **RecyclerView** y sensores → migrar aquí las listas (hoy inflado manual).
- **Lab 5 (20 oct):** Storage local + **notificaciones** (ej. alerta de cobro).
- **Lab 6 (3 nov):** **Firebase** (Auth + Firestore/Realtime) reemplaza los datos en memoria; **Retrofit 2 + Gson** para consumir la **API REST de taxistas** (base 10.0.2.2 en emulador; permiso `INTERNET`).
- Componentes de arquitectura (ViewModel + LiveData/Observer, WorkManager, ExecutorService en `IoTelitoApplication`) se introducen para trabajo en background/estado (útil luego para el seguimiento de taxi en tiempo real).

> **Declaración de IA (obligatoria):** el sílabo exige declarar y citar el uso de IA generativa y adjuntar los *prompts* como anexo (por eso existe la carpeta `Promts` del equipo). Guarda los prompts usados para tu módulo. Política de plagio = nota 0.

---

## 1. Requisitos del Administrador de hotel (del plan de proyecto oficial)

1. Registrar **ubicación** del hotel y **lugares históricos** cercanos.
2. Registrar **fotos** del hotel (**mínimo 4**).
3. Registrar **habitaciones**: tipo (standard, económica, lux…), capacidad (adultos y niños), tamaño (área m²).
4. Registrar **servicios**: nombre, descripción, precio, imágenes, y si tienen costo.
5. Ver **estado del taxista**: asignado / en camino a recoger / llegó al destino.
6. **Alerta de checkout** → cobro de la tarjeta registrada + **cobro adicional por daños** (registrar **monto, motivo y observación**).
7. **Reporte de ingresos** por servicios adicionales, **ordenados de menor a mayor** por monto total.
8. **Chat**: visualizar y responder mensajes de clientes con **reserva activa**.

Estados del servicio de taxi (spec): `SOLICITADO, ASIGNADO, EN_CAMINO, EN_TRASLADO, FINALIZADO`.

---

## 2. Mapa de las 13 pantallas

Bottom-nav: **Hotel · Reservas · Mensajes · Reportes**.

| # | Pantalla Figma | Tipo | Clase (paquete `ui.hoteladmin`) | Plantilla a copiar (superadmin) |
|---|----------------|------|--------------------------------|-------------------------------|
| 1 | adm-dashboard | Tab | `hotel/AdminHotelFragment` | `dashboard/DashboardFragment` |
| 2 | adm-datos-hotel | Activity | `hotel/DatosHotelActivity` | `hoteles/FormularioHotelActivity` |
| 3 | adm-fotos-hotel | Activity | `hotel/FotosHotelActivity` | (galería; usar `item_gallery_photo`) |
| 4 | adm-habitaciones | Activity | `hotel/HabitacionesActivity` | `usuarios/UsuariosFragment` (lista) |
| 5 | adm-formulario-habitacion | Activity | `hotel/FormularioHabitacionActivity` | `FormularioHotelActivity` |
| 6 | adm-servicios | Activity | `hotel/ServiciosActivity` | lista |
| 7 | adm-formulario-servicio | Activity | `hotel/FormularioServicioActivity` | `FormularioHotelActivity` |
| 8 | adm-reservas-checkout | Tab | `reservas/AdminReservasFragment` | `UsuariosFragment` (lista + chips) |
| 9 | adm-cobro-checkout | Activity | `reservas/CobroCheckoutActivity` | `DetalleUsuarioActivity` |
| 10 | adm-estado-taxi | Activity | `reservas/EstadoTaxiActivity` | `DetalleHotelAdminActivity` (mapa estático) |
| 11 | adm-bandeja-mensajes | Tab | `mensajes/AdminMensajesFragment` | `UsuariosFragment` (lista) |
| 12 | adm-chat-cliente | Activity | `mensajes/AdminChatActivity` | `ui/reservas/ChatHotelActivity` (cliente) |
| 13 | adm-reportes | Tab | `reportes/AdminReportesFragment` | `reportes/ReportesFragment` |

Shell: **`HotelAdminMainActivity`** (copia de `SuperadminMainActivity`).

---

## 3. Modelos y datos nuevos (`model/hoteladmin/*` + `data/HotelAdminSampleData`)

POJO `Serializable`, campos `public final` (mutables solo donde se edita en memoria), como los del superadmin.

- **`Habitacion`**: `id, hotelId, tipo, nombreCodigo, areaM2, adultos, ninos, precioNoche, boolean disponible, descripcion, String[] fotos`.
- **`Servicio`**: `id, hotelId, nombre, descripcion, precio, boolean tieneCosto/activo, String[] imagenes`.
- **`ReservaAdmin`**: `id, huespedNombre, huespedDoc, habitacion, rangoFechas, noches, EstadoCheckout estado, List<ItemConsumo> consumos`. `enum EstadoCheckout { PROXIMA, HOSPEDADO, CHECKOUT_PENDIENTE, FINALIZADA }`.
- **`ItemConsumo`**: `concepto, monto` (detalle de consumo y **cobros por daños: monto + motivo + observación**).
- **`ServicioTaxi`**: `huespedNombre, habitacion, origen, destino, conductor, placa, Estado estado`. `enum Estado { SOLICITADO, ASIGNADO, EN_CAMINO, EN_TRASLADO, FINALIZADO }`.
- **`Conversacion`**: `id, clienteNombre, habitacion, ultimoMensaje, hora, noLeidos, inicial`. El chat reutiliza el modelo **`Mensaje`** existente.
- Helper `EstadoCheckoutUi` (texto + colores de pill), análogo a `ui/reservas/EstadoUi`.

`HotelAdminSampleData`: `HABITACIONES` (mín. las del mockup), `SERVICIOS`, `RESERVAS_ADMIN` (cubriendo cada estado), `consumoDe(id)`, `TAXI_ACTIVO`, `CONVERSACIONES` + `chatDe(...)`, `resumenHoy()`, y datos de reportes (ingresos por servicio ordenables). El hotel administrado sale de `SampleData`/`SuperadminSampleData`.

---

## 4. Hitos

- [x] **Hito 0 – Andamiaje** (hecho): `HotelAdminMainActivity` + bottom-nav + 4 fragments placeholder (View Binding) + login `HOTEL_ADMIN →` panel. Compila.
- [ ] **Hito 1 – Modelos y datos**: modelos `model/hoteladmin/*` + `HotelAdminSampleData` + `EstadoCheckoutUi`.
- [x] **Hito 2 – Pestaña Hotel**: dashboard (1) → habitaciones (4) + formulario (5) → servicios (6) + formulario (7) → datos (2) → fotos (3, mín. 4).
- [ ] **Hito 3 – Pestaña Reservas**: lista con chips Próximas/Hospedados/Checkout (8) → cobro/checkout con cobro por daños (9) → estado de taxi (10).
- [ ] **Hito 4 – Pestaña Mensajes**: bandeja (11) → chat (12).
- [ ] **Hito 5 – Pestaña Reportes**: KPIs + ingresos por servicio ordenados de menor a mayor + barras estáticas (13).
- [ ] **Hito 6 – Pulido**: estados vacíos, validaciones, `contentDescription`, revisión contra Figma, sin texto hardcodeado.
- [ ] **Lab 4 (posterior)**: migrar todas las listas a **RecyclerView** (coordinado con el equipo).

Cada hito compila y es demostrable (login como `admin@iotelito.pe`).

---

## 5. A coordinar con el equipo

- La rama parte de `superadmin`; al integrar, coordinar el merge con el líder (Mattew) y evitar tocar firmas de modelos compartidos (`Hotel`, `Reserva`, `Mensaje`, `UserRole`, `LoginActivity`).
- El único cambio fuera del módulo es el enrutamiento `HOTEL_ADMIN` en `LoginActivity` (una línea) y el registro en el manifest.
- Nada se sube a la nube sin autorización explícita del usuario.
