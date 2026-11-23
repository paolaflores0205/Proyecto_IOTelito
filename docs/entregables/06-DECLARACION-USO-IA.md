# Declaración de uso de Inteligencia Artificial generativa — IoTelito

> **BORRADOR.** El sílabo del curso (sección VIII, *Consideraciones adicionales*) exige declarar y
> citar el uso de IA generativa y, si el profesor lo pide, adjuntar los prompts como anexo.
> Cada integrante debe completar y revisar su parte antes de entregar. Lo que no se declare puede
> considerarse una falta a la ética académica.

**Curso:** 1TEL05 – Servicios y Aplicaciones para IoT (2026-2) · **Grupo 1**

## 1. Herramientas usadas

| Herramienta | Proveedor | Quién la usó | Para qué |
|---|---|---|---|
| Claude Code (modelo Claude Opus) | Anthropic | *(completar: integrante)* | Ver sección 2. |
| *(completar: otras IA usadas por el equipo, p. ej. ChatGPT, Gemini, Copilot)* | | | |

## 2. Uso de Claude Code (asistente de programación en el editor)

Se usó como asistente para escribir, revisar y desplegar código bajo la supervisión del integrante,
que revisó, probó y aprobó cada cambio antes de subirlo. Los commits hechos con su ayuda llevan la
línea `Co-Authored-By: Claude` en el historial de Git:

- **App Android** (rama `feat/hotel-admin`): 17 de 111 commits, entre el 15/09/2026 y el 23/11/2026.
- **Web de gestión de taxistas** (rama `feat/taxi-api-web`): 6 de 7 commits.

Trabajos en los que participó:

| Área | Qué hizo la IA | Qué hizo el equipo |
|---|---|---|
| Web de gestión de taxistas | Propuso y escribió el esquema MySQL, la API REST (contrato OpenAPI), la seguridad con tokens de Firebase y el despliegue en AWS (EC2, RDS, API Gateway). | Definió los requisitos, revisó el contrato con el responsable del módulo de taxi en la app, aprobó y probó el despliegue. |
| Automatización de cuentas de taxistas | Implementó la creación de la cuenta en Firebase al aprobar un taxista. | Validó el flujo de registro, aprobación e inicio de sesión. |
| App: módulo de taxi | Agregó el chat en la pantalla de camino al hotel, el mapa real del taxista y el código del QR en texto. | Probó los flujos con dos dispositivos. |
| App: reglas de negocio | Implementó el monto mínimo de taxi por hotel, el registro de servicios consumidos en el checkout y el reporte de ingresos por servicio ordenado. | Revisó los cambios y las pruebas; probó en la app. |
| Reglas de Firestore | Escribió y probó en el emulador reglas nuevas y el retiro de las colecciones antiguas de taxi. | Autorizó cada publicación. |
| Pruebas y datos | Escribió pruebas unitarias y de reglas, y preparó datos de demostración. | Ejecutó los flujos manuales del checklist. |
| Documentación | Redactó borradores de arquitectura, base de datos, OPEX, manuales y este documento. | Revisó, corrigió y completó con capturas. |

## 3. Uso de otras herramientas de IA

*(Cada integrante completa aquí: herramienta, para qué la usó, en qué partes del proyecto y cómo
verificó el resultado.)*

## 4. Verificación

- Todo el código generado o modificado con IA pasó por pruebas automáticas (177 pruebas unitarias
  de la app, pruebas de reglas de Firestore en el emulador y 38 pruebas de la web) y por pruebas
  manuales con el checklist `docs/CHECKLIST_PRUEBAS_FINALES.md`.
- El equipo es responsable del contenido entregado y puede explicar cada parte del sistema.

## 5. Anexo de prompts

Si el profesor lo solicita, se adjuntan las conversaciones con Claude Code (se guardan localmente
como archivos `.jsonl` en la carpeta de proyectos de Claude Code de cada integrante) y, para otras
herramientas, el historial de chats correspondiente.
