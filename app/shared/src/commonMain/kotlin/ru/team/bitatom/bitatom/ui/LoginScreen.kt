package ru.team.bitatom.bitatom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Экран создания мастер-пароля (первый запуск) и входа (последующие запуски). */
@Composable
fun LoginScreen(vm: LoginViewModel) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val creating = !vm.isMasterCreated

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(Modifier.widthIn(max = 420.dp).padding(24.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bitatom", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (creating) "Первый запуск: придумайте мастер-пароль. Восстановить его нельзя!"
                    else "Введите мастер-пароль, чтобы открыть хранилище",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Мастер-пароль") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (creating) {
                    OutlinedTextField(
                        value = confirm, onValueChange = { confirm = it },
                        label = { Text("Повторите мастер-пароль") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        if (creating) vm.onCreateMaster(password, confirm) else vm.onLogin(password)
                        password = ""; confirm = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (creating) "Создать хранилище" else "Войти") }
                Text(
                    "Ключ выводится из пароля через Argon2id в C-ядре; сам пароль нигде не сохраняется.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
