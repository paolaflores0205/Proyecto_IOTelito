# Inventario de validaciones — IoTelito

Revisión campo por campo de la app y de la web de taxistas (23/11/2026). Para cada campo:
qué se validaba, qué faltaba y la regla propuesta. **❓ = decisión del equipo.**

## Decisiones y estado (implementado el 23/11/2026)

| Tema | Decisión | Pantalla | Servidor |
|---|---|---|---|
| Forma de mostrar errores | **Texto rojo debajo de cada campo** en la web y en la app (sin globos del navegador ni "!") | ✅ | — |
| DNI / pasaporte / carné | 8 dígitos / 6–12 / 9–12 alfanuméricos; teclado y máximo según el tipo | ✅ | ✅ web y Firestore |
| Nombres y apellidos | Solo letras (tildes, ñ, apóstrofo, guion), mínimo 2 | ✅ | ✅ web y Firestore |
| Teléfono | 9 dígitos (sin exigir que empiece con 9) | ✅ | ✅ web y Firestore |
| Fecha de nacimiento | No futura y mayor de 18 años | ✅ | ✅ web y Firestore |
| Vencimiento de licencia | Hoy o posterior | ✅ | ✅ web |
| Licencia y categoría | Letra + 8 dígitos; categoría de una lista (A-I a A-IIIc) | ✅ | ✅ web |
| Año del vehículo | **Sin mínimo de antigüedad**: solo se rechazan valores imposibles (antes de 1950 o después del año siguiente) | ✅ | ✅ web |
| Placa | Formato `ABC-123` | ✅ | ✅ web |
| Contraseña (cuentas nuevas) | Mínimo 8 con letras y números | ✅ | — (Firebase Auth) |
| Habitación | 5–1000 m², 1–10 adultos, 0–10 niños, precio > 0 y ≤ S/ 100 000 | ✅ | ✅ Firestore |
| Servicio | Precio de S/ 0 a S/ 10 000 | ✅ | ✅ Firestore |
| Teléfono del hotel | 7 a 15 dígitos | ✅ | ✅ Firestore (al cambiarlo) |
| Taxi | 1–4 pasajeros, 0–6 maletas | ✅ | ✅ API de taxis |

Las tablas siguientes muestran el diagnóstico inicial, campo por campo.

Leyenda de estado: ✅ correcto · ⚠️ incompleto · ❌ sin validar.
"Pantalla" = lo que ve el usuario al escribir. "Servidor" = reglas de Firestore o Java de la web
(lo que no se puede saltar modificando la app).

## Cómo se muestran hoy los errores

| Dónde | Forma | Ejemplo |
|---|---|---|
| **Web de taxistas — al escribir** | **Globo del navegador con "!" amarillo/naranja** (validación nativa HTML: `required`, `pattern`, `type=email`, fechas) y mensajes propios en el mismo globo para el peso de archivos | Registro de taxista |
| Web de taxistas — al enviar | Un solo **recuadro rojo arriba** del formulario con el primer error que encuentra el servidor | "El DNI debe tener 8 dígitos" |
| App — registro de cliente y formulario de hotel (superadmin) | **Texto rojo debajo del campo** (Material `TextInputLayout`) | Registro |
| App — habitaciones, servicios, datos del hotel, cobro | **Ícono "!" rojo con globo** sobre el campo (`EditText.setError`) | "Campo obligatorio" |
| App — otros | Mensaje flotante (Toast) | Tarjeta, checkout |

---

## A. Web de gestión de taxistas — registro (`registro-taxista.html`)

| Campo | Hoy (pantalla / servidor) | Problema | Propuesta |
|---|---|---|---|
| Nombres | obligatorio / — | sin máximo ni formato | 2–100 letras y espacios (acepta tildes, ñ) |
| Apellidos | obligatorio / — | igual | 2–100 letras y espacios |
| Tipo de documento | obligatorio / obligatorio ✅ | — | — |
| Número de documento | patrón fijo 6–12 alfanumérico, máx. 12 / regla por tipo ✅ | ⚠️ en pantalla no cambia según el tipo (DNI deja escribir letras y 12 caracteres) | Según tipo: **DNI 8 dígitos**; pasaporte ❓ 6–12 alfanumérico; carné ❓ 9–12 alfanumérico. Teclado y máximo cambian al elegir el tipo |
| Fecha de nacimiento | obligatoria / — | ❌ acepta fechas futuras y menores de edad | No futura y **mayor de 18 años** ❓ (¿máximo 70–80 años?) |
| Teléfono | obligatorio / — | ❌ acepta cualquier texto | **9 dígitos, empieza con 9** ❓ |
| Correo | formato email / único ✅ | — | — |
| Domicilio | obligatorio / — | sin longitud | 8–255 caracteres |
| Número de licencia | obligatorio / único ✅ | sin formato | ❓ 1 letra + 8 dígitos (formato MTC, ej. Q12345678) o 6–12 alfanumérico |
| Categoría de licencia | texto libre / — | ⚠️ acepta cualquier cosa | Lista: A-I, A-IIa, A-IIb, A-IIIa, A-IIIb, A-IIIc ❓ |
| Vencimiento de licencia | obligatoria / — | ❌ acepta fechas pasadas | **Hoy o posterior** |
| Marca / modelo / color | obligatorios / — | sin longitud | 2–60 caracteres |
| Año del vehículo | obligatorio / — | ❌ acepta 0, 3000, negativos | ❓ entre 2005 y el año siguiente al actual |
| Placa | obligatoria / única ✅ | ❌ sin formato | **`ABC-123`** (3 letras/números + guion + 3 números) ❓ |
| Fotos (conductor, vehículo) | tipo imagen, peso ✅ / ✅ | — | — |
| Documentos (DNI, licencia, SOAT, tarjeta) | obligatorios, peso ✅ / ✅ | — | — |
| Términos | obligatorio / obligatorio ✅ | — | — |

Inicio de sesión del superadmin web: correo y contraseña obligatorios ✅.

## B. App — cliente

| Pantalla · campo | Hoy (pantalla / servidor) | Problema | Propuesta |
|---|---|---|---|
| Registro · Nombres / apellidos | obligatorio / 1–80 y 1–79 caracteres ✅ | ⚠️ acepta números y símbolos; sin máximo en pantalla | Solo letras y espacios; máximo visible |
| Registro · Tipo de documento | lista ✅ / ✅ | — | — |
| Registro · Número de documento | por tipo / por tipo ✅ | ⚠️ **teclado solo numérico** (no se pueden escribir letras de pasaporte o carné) y sin máximo de caracteres | Teclado y máximo según tipo (DNI: 8 dígitos) |
| Registro · Fecha de nacimiento | ≥ 18 años / ≥ 18, no futura ✅ | — | — |
| Registro · Correo | formato / Firebase ✅ | — | — |
| Registro · Contraseña | ≥ 6 caracteres / Firebase ✅ | ❓ ¿pedir mínimo 8 con letra y número? | — |
| Registro · Teléfono | 9 dígitos / 9 dígitos ✅ | ⚠️ sin máximo en el campo | Máximo 9; ❓ que empiece con 9 |
| Registro · Domicilio | ≥ 8 / 8–500 ✅ | — | Máximo visible |
| Editar perfil · mismos campos | usa la misma validación ✅ | ⚠️ errores como mensaje flotante | Texto rojo bajo el campo |
| Nueva contraseña | obligatoria y coincide | ⚠️ no exige el mínimo de 6 | Mínimo igual al registro |
| Reserva · Tarjeta | Luhn, 13–19 dígitos ✅ | — | — |
| Reserva · Vencimiento | MM/AA, **no vencida** ✅ | — | — |
| Reserva · CVV | 3 (4 Amex) ✅ | — | — |
| Reserva · Fechas | entrada desde hoy, salida posterior ✅ / ✅ | — | — |
| Checkout · Comentario | obligatorio / 1–4000 ✅ | — | — |
| Taxi · pasajeros y maletas | / servidor ✅ (≥ 1 y ≥ 0) | ❓ sin máximo | Pasajeros 1–4, maletas 0–6 ❓ |
| Calificar taxista · comentario | máx. 500 ✅ | — | — |

## C. App — administrador de hotel

| Pantalla · campo | Hoy (pantalla / servidor) | Problema | Propuesta |
|---|---|---|---|
| Datos del hotel · Nombre, dirección, correo | obligatorios / longitudes ✅ | — | — |
| Datos del hotel · Teléfono | — / máx. 80 | ❌ acepta letras | 7–15 dígitos (igual que superadmin) |
| Datos del hotel · Monto mínimo taxi | 0–100 000 / 0–100 000 ✅ | — | — |
| Habitación · Código | obligatorio / 1–80 ✅ | — | — |
| Habitación · Área m² | obligatoria / 1–10 000 | ⚠️ en pantalla acepta 0 | 5–1000 m² ❓ |
| Habitación · Adultos | — (si está vacío usa 1) / 1–100 | ⚠️ acepta 0 y 100 en pantalla | 1–10 ❓ |
| Habitación · Niños | — / 0–100 | ⚠️ sin máximo | 0–10 ❓ |
| Habitación · Precio | obligatorio / 0–1 000 000 | ⚠️ acepta S/ 0 | Mayor que 0, máx. 100 000 ❓ |
| Servicio · Nombre | obligatorio / 1–160 ✅ | — | — |
| Servicio · Precio | obligatorio / 0–1 000 000 | ⚠️ — | 0 (gratis) a 10 000 ❓ |
| Cobro · Monto por daños | > 0, 2 decimales / ≤ 1 000 000 ✅ | — | — |
| Cobro · Motivo / observación | obligatorios / ≤ 1000 y ≤ 4000 ✅ | — | — |

## D. App — superadmin

| Pantalla · campo | Hoy (pantalla / servidor) | Problema | Propuesta |
|---|---|---|---|
| Hotel · Nombre, dirección, distrito, ciudad | longitudes y caracteres ✅ / ✅ | — | — |
| Hotel · Teléfono | 7–15 dígitos ✅ | — | — |
| Hotel · Correo | formato ✅ | — | — |
| Hotel · Fotos | 4–8 ✅ / ✅ | — | — |
| Activar/desactivar usuario · Motivo | obligatorio al desactivar ✅ / ✅ | — | — |

## E. Taxista (app)

| Campo | Hoy | Propuesta |
|---|---|---|
| Código del QR escrito a mano | servidor valida el código exacto ✅ | — |
| Chat | 1–1000 caracteres ✅ | — |

---

## Resumen

- **Lo más importante a corregir:** la web de taxistas valida casi todo solo con el navegador; el
  servidor no revisa fechas, teléfono, placa ni año del vehículo. Además, el campo de documento del
  cliente en la app no deja escribir letras (pasaporte y carné).
- **Decisiones ❓ del equipo:** formatos de pasaporte y carné, licencia y su categoría, placa,
  rango de año del vehículo, teléfono que empiece con 9, contraseña más fuerte, límites de
  habitaciones y servicios, pasajeros y maletas del taxi.
- **Forma de mostrar errores:** decidir si la web sigue con los globos del navegador o pasa a
  texto rojo debajo de cada campo, como ya hace el registro de la app.
