import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin/JVM module: all rules (ELO, rewards, lives, hints, skips, budget). No Android imports.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Must match :app so Android can consume these classes (works with any JDK 17+).
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
    testImplementation(libs.junit)
}
