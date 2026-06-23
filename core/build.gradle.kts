import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    jacoco
}

// Bytecode JVM 17 (Java + Kotlin cohérents) pour rester consommable par les modules Android.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

// Couverture de code (rapports XML/CSV/HTML) — exploitée par la CI pour le badge.
tasks.named<Test>("test") {
    finalizedBy("jacocoTestReport")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn("test")
    // Exclut la table de coefficients EMERGE générée (données, pas de logique à tester) :
    // sa simple initialisation gonflerait artificiellement la couverture.
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude("**/EmergeCoefsKt.class") } }),
    )
    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacoco.xml"))
        csv.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacoco.csv"))
    }
}
