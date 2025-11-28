# Intu Taxi – App de ejemplo

## Requisitos
- `Android SDK` 24+ (`minSdk` 24), `targetSdk` 36.
- `NDK r27` (`android/app/build.gradle.kts` fija `ndkVersion`).
- Tokens de Mapbox (no se versionan):
  - `MAPBOX_PUBLIC_TOKEN` (pk…)
  - `MAPBOX_DOWNLOADS_TOKEN` (sk…)

## Configuración de tokens
1. Abra `intuv5/local.properties` y añada:
   - `MAPBOX_PUBLIC_TOKEN=pk_...`
   - `MAPBOX_DOWNLOADS_TOKEN=sk_...`
2. Estos valores se usan así:
   - `MAPBOX_PUBLIC_TOKEN` se inyecta como `string` de recursos `mapbox_access_token` en build.
   - `MAPBOX_DOWNLOADS_TOKEN` autentica el repositorio Maven de Mapbox en `settings.gradle.kts`; si está vacío, se omite el repo.

## Seguridad
- `local.properties` está ignorado en `.gitignore`, por lo que los tokens no se subirán a Git.
- No hay secretos en código fuente ni en archivos de recursos del proyecto.
- Use tokens con alcance mínimo en su cuenta Mapbox.

## Builds
- Sincronice Gradle y compile:
  - `./gradlew.bat :app:assembleDebug`
- Si clonan el repo, deberán crear su propio `local.properties` con tokens.

## Funcionalidad
- `Home`: mapa Mapbox Standard a pantalla completa, ubicación del usuario y ScaleBar oculta.
- `Trips`: historial con tarjetas y encabezado.
- `Account`: perfil de cliente con secciones (contacto, pago, lugares, ayuda) y botón `Debug`.
- `Debug`: visualiza logs internos.

## Navegación
- Bottom bar con `Home`, `Trips`, `Account`.
- Botón `Account` siempre regresa a la pantalla de cuenta, limpiando `debug` del back stack.

