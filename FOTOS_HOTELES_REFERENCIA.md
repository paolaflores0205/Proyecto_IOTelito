# Fotos ilustrativas de hoteles

Se incluyen ocho fotografías de stock de Unsplash, descargadas el 19/10/2026, para probar el carrusel. No son fotos oficiales de los hoteles nombrados en el prototipo ni se afirma que correspondan a sus instalaciones.

Fuente y condiciones: [licencia de Unsplash](https://unsplash.com/license).
Se descargaron desde las URLs de imagen siguientes a 960 × 640, JPEG de calidad 78, para empaquetarlas localmente. No se usa una API de Unsplash ni hay descargas al ejecutar la aplicación.

| Recurso local | Imagen de origen |
| --- | --- |
| hotel_demo_resort.jpg | [Unsplash](https://images.unsplash.com/photo-1566073771259-6a8506099945) |
| hotel_demo_suite.jpg | [Unsplash](https://images.unsplash.com/photo-1611892440504-42a792e24d32) |
| hotel_demo_bedroom.jpg | [Unsplash](https://images.unsplash.com/photo-1566665797739-1674de7a421a) |
| hotel_demo_pool.jpg | [Unsplash](https://images.unsplash.com/photo-1564501049412-61c2a3083791) |
| hotel_demo_terrace.jpg | [Unsplash](https://images.unsplash.com/photo-1571896349842-33c89424de2d) |
| hotel_demo_facade.jpg | [Unsplash](https://images.unsplash.com/photo-1568084680786-a84f91d1153c) |
| hotel_demo_lobby.jpg | [Unsplash](https://images.unsplash.com/photo-1551882547-ff40c63fe5fa) |
| hotel_demo_room.jpg | [Unsplash](https://images.unsplash.com/photo-1582719478250-c89cae4dc85b) |

## Funcionamiento

- Se asignan cuatro fotos variadas a cada uno de los doce hoteles iniciales que no tengan fotos propias.
- Las galerías que ya contienen fotos se conservan completas, sin sustituir su portada.
- Los hoteles nuevos creados por el usuario no reciben estas imágenes automáticamente.
- Las fotos ilustrativas se identifican en el carrusel administrativo y en la galería del cliente.
- Puedes cambiar la portada, añadir fotos y quitar las de ejemplo desde Editar hotel.
- Al borrar todas las fotos y guardar, no reaparecen al reiniciar. galleryInitialized registra que la galería ya fue atendida.
- Las URIs android.resource usan nombres de recursos estables, no los IDs numéricos que pueden cambiar al compilar.
- Las imágenes de respaldo guardadas como IDs numéricos se actualizan a los recursos de la compilación actual, sin sustituir las URIs de fotos propias.
- La base administrativa pasa de versión 3 a 4 agregando únicamente galleryInitialized. No se borran hoteles, fotos propias, reservas ni el historial de reportes.

Ejecuta la app actualizada, entra en Superadmin → Hoteles → un hotel del catálogo y desliza el carrusel. También puedes entrar en Editar hotel o ver el mismo hotel desde cliente. No hace falta borrar datos ni reinstalar.
