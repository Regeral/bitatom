import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":app:shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    // Мост к C-ядру (crypto_core.dll) и локальная БД
    implementation(libs.jna)
    implementation(libs.sqlite.jdbc)
}

compose.desktop {
    application {
        mainClass = "ru.team.bitatom.bitatom.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ru.team.bitatom.bitatom"
            packageVersion = "1.0.0"
            // JDBC (SQLite) нужен модуль java.sql в упакованном рантайме
            modules("java.sql")
            windows {
                menuGroup = "Bitatom"
                shortcut = true
            }
        }
    }
}