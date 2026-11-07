package ru.team.bitatom.bitatom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.team.bitatom.bitatom.ui.GeneratorViewModel
import ru.team.bitatom.bitatom.ui.LoginScreen
import ru.team.bitatom.bitatom.ui.LoginViewModel
import ru.team.bitatom.bitatom.ui.MainScreen
import ru.team.bitatom.bitatom.ui.VaultViewModel

private val BitatomColors = lightColorScheme(
    primary = Color(0xFF2F5BEA),
    secondary = Color(0xFF4A5568),
    error = Color(0xFFC53030),
)

/**
 * Корневой экран. container == null — платформа ещё не подключила C-ядро
 * (Android/iOS в этом MVP) или ядро не загрузилось на Windows.
 */
@Composable
fun App(container: AppContainer? = null, startupError: String? = null) {
    MaterialTheme(colorScheme = BitatomColors) {
        Surface(Modifier.fillMaxSize()) {
            if (container == null) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        startupError ?: "На этой платформе C-ядро пока не подключено. MVP работает на Windows (desktopApp).",
                        color = if (startupError != null) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                }
                return@Surface
            }
            val loginVm = remember { LoginViewModel(container.authService) }
            val vaultVm = remember { VaultViewModel(container.vaultService) }
            val generatorVm = remember { GeneratorViewModel(container.passwordGenerator) }

            if (!loginVm.isUnlocked) {
                LoginScreen(loginVm)
            } else {
                MainScreen(container, loginVm, vaultVm, generatorVm)
            }
        }
    }
}
