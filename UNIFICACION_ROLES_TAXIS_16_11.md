# Unificación final y API real — 17/11/2026

## Estado actual

La rama superadmin incorpora cliente, hotel-admin y taxis Android, incluido origin/feat/hotel-admin 518d572 y su contrato de chat/avisos. Se conservan menú blanco de 64dp, corrección de teclado/espacio inferior, catálogo, validaciones, desplegables, reportes y gestión de taxistas propios de superadmin. La web Spring/MySQL sigue como proyecto separado.

La configuración por defecto de app/build.gradle es:

```properties
taxiApi.url=https://bkmx72ue4k.execute-api.us-east-1.amazonaws.com/api/v1
taxiApi.fake=false
```

No hay que editar local.properties para usarla. Sus valores o las propiedades -P pueden sobrescribir los defaults. La app no recurre al simulador cuando el servidor falla: muestra el error real. No se incluyen contraseñas, tokens ni claves privadas en los cambios.

## Funciones integradas

- Reservas consecutivas, disponibilidad, checkout, servicios/fotos de hoteles y avisos de cliente de la rama actualizada.
- Solicitud de taxi, disponibilidad del conductor, aceptación, estados, ubicación, mapa, QR y calificación mediante REST.
- Mis taxis consulta los viajes propios por API, sin mezclar taxiRequests antiguos de Firestore.
- Chat desde Seguimiento del cliente y desde el viaje del conductor: autorización por participante, UUID para reintentos sin duplicados, hasta 100 mensajes e historial de solo lectura al finalizar.
- Registro de dispositivos y recepción FCM para cliente y conductor mediante las nuevas rutas del backend. Se conservan las comprobaciones de UID y vínculo de sesión antes de mostrar avisos. Las reservas/checkout mantienen su integración actual.
- Usuarios → Gestionar taxistas usa GET /taxistas y PATCH /taxistas/{id}/activo. La aprobación de documentos sigue en la web. El detalle de un usuario taxista conduce a esa gestión; no se cambia solo users.active por una vía separada.

Los archivos temporales de emulador de las ramas originales quedaron fuera de los commits. No se borraron registros antiguos ni se importó SQL en producción.

## Verificación

- Compilación de la app y del APK de instrumentación correctas; 172 pruebas unitarias sin fallos, incluidas permisos de chat, lectura tras finalizar y reintentos.
- TaxiApiEmulatorTest pasó en 53,904 s usando exclusivamente Auth/Firestore demo y API falsa: solicitud, aceptación, mensajes desde conductor y cliente en las pantallas reales, ubicación ficticia del AVD, mapa, QR generado/decodificado y validado mediante entrada manual, calificación y gestión de taxistas por superadmin.
- En el backend real: health HTTP 200; catálogo de 5 aeropuertos HTTP 200; listado de taxistas sin sesión HTTP 401; con la cuenta superadmin facilitada HTTP 200, 11 fichas y 4 aprobadas. Son consultas, no altas ni cambios de estados reales.
- Las rutas nuevas están desplegadas: chat con identidad superadmin devuelve HTTP 403 PROHIBIDO (no es participante); GET sobre la ruta de dispositivos devuelve HTTP 405 porque su método es PUT. Esto confirma las rutas, pero no certifica entrega FCM entre teléfonos.
- La cuenta de conductor facilitada autentica en Firebase, pero GET /taxistas/me devuelve HTTP 404 TAXISTA_NO_REGISTRADO. Necesita su ficha en la nueva base de la web; no se creó una con documentos inventados.
- Los sitios de acceso y registro web responden HTTP 200. No se desplegó código de servidor ni se cambiaron sus credenciales/configuración.

## Cómo probar en el celular

1. Abre IoTelito en Android Studio, conecta y selecciona el A55 y pulsa Run sobre app. No borres sus datos. La compilación predeterminada usa el servidor real y no debe mostrar la franja de simulación.
2. Entra como superadmin y abre Usuarios → Gestionar taxistas: debe consultar las fichas del servidor. Comprueba también hoteles, filtros, validaciones, reportes y menú.
3. Para habilitar el conductor de prueba, registra su ficha con el MISMO correo de Firebase en https://bkmx72ue4k.execute-api.us-east-1.amazonaws.com/taxistas/registro y revísala/apruébala desde el panel web https://bkmx72ue4k.execute-api.us-east-1.amazonaws.com/. Las credenciales web son independientes de las de Android. Completa los datos y documentos reales correspondientes; no hace falta crear otra identidad Firebase si ya existe.
4. Con dos teléfonos y permiso de notificaciones concedido: cliente y taxista deben abrir una vez la sesión en el APK actualizado para registrar el dispositivo. Deja al conductor aprobado/disponible.
5. Usa una reserva del cliente que cumpla el beneficio (estadía desde S/ 1000 y checkout solicitado o finalizado). Solicita el taxi, comprueba el aviso al conductor, acepta, envía mensajes en ambos sentidos, avanza el viaje, consulta mapa/QR, valida y califica. Comprueba avisos con la app en segundo plano.
6. El backend debe conservar el vínculo entre la ficha aprobada de MySQL y el UID de Firebase. Si aparece TAXISTA_NO_REGISTRADO, revisa la ficha/correo de la web; cambiar el menú o reinstalar el APK no crea esa ficha.

## Límites de lo comprobado

La entrega de notificaciones FCM entre teléfonos, el GPS del A55, el escaneo óptico con cámara y el recorrido de un conductor real aprobado requieren esa prueba física. En el AVD el GPS fue inyectado y el QR se validó mediante su contenido. No se fabricaron viajes ni se cambiaron habilitaciones en producción.

Para repetir únicamente las pruebas locales, compila con '-PtaxiApi.fake=true' y usa demo-iotelito-auth en localhost:9099/8180. Ese modo no comparte viajes entre teléfonos y no debe usarse para la demostración del backend real. La prueba Android optativa se omite si la API falsa no está habilitada. En debug FCM se inicia al disponer de una sesión real; los proyectos demo no solicitan tokens FCM, ya que ese servicio no tiene emulador.
