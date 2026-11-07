package ru.team.bitatom.bitatom

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.team.bitatom.bitatom.desktop.LocalDatabase
import ru.team.bitatom.bitatom.desktop.NativeCryptoProvider
import ru.team.bitatom.bitatom.desktop.SqlEntryRepository
import ru.team.bitatom.bitatom.desktop.SqlMasterRepository

/** Точка входа Windows-версии: собираем зависимости (C-ядро + SQLite) и запускаем общий UI. */
fun main() {
    var database: LocalDatabase? = null
    var startupError: String? = null

    val container: AppContainer? = try {
        val crypto = NativeCryptoProvider()
        val db = LocalDatabase().also { database = it }
        AppContainer(
            crypto = crypto,
            diagnostics = crypto,
            masterRepository = SqlMasterRepository(db),
            entryRepository = SqlEntryRepository(db),
            now = System::currentTimeMillis,
        )
    } catch (e: Throwable) {
        startupError = buildString {
            appendLine("Не удалось запустить C-ядро или базу данных:")
            appendLine(e.message ?: e.toString())
            appendLine()
            append("Соберите ядро: crypto-core\\build_windows.bat (DLL должна оказаться в ")
            append("app\\desktopApp\\src\\main\\resources\\win32-x86-64\\crypto_core.dll)")
        }
        null
    }

    application {
        Window(
            onCloseRequest = {
                container?.session?.lock()
                database?.close()
                exitApplication()
            },
            title = "Bitatom — менеджер паролей",
            state = rememberWindowState(width = 960.dp, height = 720.dp),
        ) {
            App(container, startupError)
        }
    }
}
