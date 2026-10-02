plugins {
    alias(libs.plugins.android.application)
}

android {
    // Con AGP 8+ el paquete se declara aquí y no en el atributo package del Manifest
    namespace = "com.example.geocheckin"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.geocheckin"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Servicios de Ubicación de Google Play
    implementation(libs.play.services.location)

    // Jetpack Security (Keystore + EncryptedSharedPreferences)
    // Nota: Google marcó esta librería como obsoleta (ver Conclusiones del documento)
    implementation(libs.androidx.security.crypto)

    // Componentes de Arquitectura
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}
