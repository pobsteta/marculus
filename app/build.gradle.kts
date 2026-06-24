import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.pobsteta.marculus"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.pobsteta.marculus"
        minSdk = 26
        targetSdk = 36
        // Version injectée par la CI (-PappVersionName / -PappVersionCode) ; valeurs de repli en local.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "0.1.0"
    }

    // Clé de signature release : depuis l'environnement (CI, secrets) ou les propriétés Gradle.
    // À défaut de keystore, on retombe sur la signature debug (builds locaux sans clé).
    val cheminKeystore = System.getenv("RELEASE_KEYSTORE") ?: (project.findProperty("releaseKeystore") as String?)
    val keystoreExiste = cheminKeystore != null && file(cheminKeystore).exists()

    signingConfigs {
        if (keystoreExiste) {
            create("release") {
                storeFile = file(cheminKeystore!!)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: (project.findProperty("releaseKeystorePassword") as String?)
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: (project.findProperty("releaseKeyAlias") as String?)
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: (project.findProperty("releaseKeyPassword") as String?)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Clé release stable si configurée, sinon repli debug (les mises à jour ne marchent
            // qu'entre APK signés par la MÊME clé release).
            signingConfig = signingConfigs.getByName(if (keystoreExiste) "release" else "debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.osmdroid.android)
    debugImplementation(libs.compose.ui.tooling)
}
