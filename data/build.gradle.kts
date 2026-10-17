import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.pobsteta.marculus.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    // Tests de migration Room sous Robolectric : les schémas exportés servent d'assets de test.
    sourceSets {
        named("test") { assets.directories.add("$projectDir/schemas") }
    }
    testOptions.unitTests.isIncludeAndroidResources = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Schémas Room versionnés : base des tests de migration (MigrationTestHelper).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.terrain.journal)
    implementation(libs.terrain.gpkg)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.geopackage.android)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
