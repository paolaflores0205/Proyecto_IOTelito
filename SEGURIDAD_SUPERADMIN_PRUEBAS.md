# Cómo comprobar el acceso de superadmin

## Android y web son aplicaciones distintas

El aviso del compañero sobre `/superadmin/dashboard`, BCrypt, CSRF y MySQL corresponde al servidor web. Ese código no está en este checkout Android. No basta con corregir Android para proteger la web ni con proteger una ruta web para proteger Firestore.

En Android, Firebase Authentication comprueba las credenciales; `SuperadminAccessGuard` oculta el contenido y verifica el perfil remoto al iniciar cada actividad de superadmin. `FirebaseProfileRepository` consulta `users/{uid}` desde el servidor. El perfil debe estar activo, su uid debe coincidir y el rol debe ser SUPERADMIN. Las actividades de superadmin están declaradas con `exported=false`. Los permisos de datos dependen de las reglas del servidor, no solo de ocultar pantallas.

## Prueba en el celular

1. Instala esta compilación con Run desde Android Studio.
2. Inicia sesión como superadmin y comprueba el acceso al panel.
3. Cierra sesión desde el perfil. Pulsa Atrás, vuelve a abrir la app y prueba una notificación anterior de superadmin. No debe mostrar datos ni permitir operaciones de ese rol.
4. Inicia sesión como cliente, administrador o taxista. Repite el intento de abrir una notificación de superadmin. Debe rechazarlo y pedir la cuenta correcta.
5. Comprueba con una cuenta de prueba inactiva, nunca desactivando la cuenta principal de superadmin. No debe obtener acceso.

No confundir sesión persistente con acceso sin credenciales: si Firebase conserva una sesión válida, reabrir la aplicación puede no pedir otra vez la contraseña. La prueba sin sesión comienza cerrando sesión explícitamente. No es necesario borrar todos los datos del celular.

## Prueba de las reglas publicadas

En Firebase Console, Firestore, Reglas, abre el simulador de reglas. Usa lectura (`get`) para no modificar registros:

| Identidad de la prueba | Documento | Esperado |
| --- | --- | --- |
| Sin autenticar | users/UID_DEL_SUPERADMIN | Denegado |
| UID de un cliente activo | users/UID_DEL_SUPERADMIN | Denegado |
| UID de un administrador activo | users/UID_DEL_SUPERADMIN | Denegado |
| UID real de superadmin activo | users/UID_DEL_SUPERADMIN | Permitido |

La lectura del catálogo de hoteles por otros roles activos sí está permitida por diseño. No usar ese caso para concluir que alguien obtuvo permisos de superadmin.

Comprueba que las reglas PUBLICADAS coincidan con el archivo `firestore.rules` del proyecto; las pruebas locales no confirman la configuración remota. No reemplaces reglas por `allow read, write: if true`. No cambies manualmente roles de cuentas reales para probar.

Las pruebas automatizadas `app/src/test/js/firestore_auth_rules.test.cjs` usan solo `demo-iotelito-auth` en localhost:8180. Incluyen falta de sesión, rol incorrecto, usuario inactivo, UID inconsistente, lectura/listado de usuarios, cambios de estado y escalamiento de permisos. Los tokens de prueba del emulador no son credenciales válidas para Firebase real.

## Qué pedir a quien mantiene la web

Confirmar que tanto la ruta del panel como los endpoints de aprobar, rechazar y cambiar disponibilidad exijan autenticación Y rol de superadmin. Un usuario autenticado de otro rol también debe ser rechazado. Probar después de cerrar sesión y desde una ventana privada. Verificar que contraseñas y claves del servidor no estén en el repositorio ni en el APK. La revisión de este proyecto no certifica esa aplicación web.

## Cobros del reporte

La pantalla usa las etiquetas “Comprobantes” y “fecha de cierre de estadía”. El flujo heredado aún guarda `paymentMode=SIMULATED` y no ejecuta un cargo bancario. No se cambió ese dato a REAL ni se añadió una pasarela de pagos por modificar las etiquetas. La conversión a cobros efectivos necesita una integración de pagos y comprobación del resultado en el servidor.
