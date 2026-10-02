# Semana 07 – Desarrollo de Aplicaciones Móviles

## Ejercicio 1: GeoCheckIn Seguro

Aplicación Android (Kotlin + Views) para registrar fichajes laborales mediante geolocalización segura.

- Permisos de ubicación en tiempo de ejecución (solo primer plano)
- Validación de la zona de fichaje y detección de ubicaciones simuladas
- Token firmado con HMAC-SHA256 (Android Keystore)
- Almacenamiento cifrado con EncryptedSharedPreferences, excluido de copias de seguridad

| | |
|---|---|
| 📁 Código | [`ejercicio1/`](ejercicio1) — proyecto de Android Studio |
| 📄 Documento | [`ejercicio1.docx`](ejercicio1.docx) |

### Cómo ejecutarlo

1. En Android Studio: **File → Open** y selecciona la carpeta `ejercicio1`.
2. Ejecuta la configuración `app` en un emulador o dispositivo (Android 7.0+).
3. La zona de fichaje está en `MainActivity.kt` (`WORK_ZONE`, radio de 100 m). En el emulador,
   fija la ubicación dentro de la zona desde **Extended controls → Location** o con:
   ```
   adb emu geo fix -77.042793 -12.046374
   ```

Requisitos: Gradle 9.5 (incluido con el wrapper), AGP 9.3.1, compileSdk 37.
