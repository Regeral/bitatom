package ru.team.bitatom.bitatom.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.team.bitatom.bitatom.AppContainer
import ru.team.bitatom.bitatom.domain.DiagnosticLine
import ru.team.bitatom.bitatom.domain.GeneratorOptions
import ru.team.bitatom.bitatom.domain.PasswordEntry
import ru.team.bitatom.bitatom.domain.toHex

private enum class Tab(val title: String) { VAULT("Хранилище"), GENERATOR("Генератор"), CORE("C-ядро") }

@Composable
fun MainScreen(container: AppContainer, loginVm: LoginViewModel, vaultVm: VaultViewModel, generatorVm: GeneratorViewModel) {
    var tab by remember { mutableStateOf(Tab.VAULT) }
    LaunchedEffect(Unit) { vaultVm.refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bitatom", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Tab.entries.forEach { t ->
                if (t == tab) Button(onClick = {}) { Text(t.title) }
                else OutlinedButton(onClick = { tab = t }) { Text(t.title) }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vaultVm.onSelect(null); loginVm.onLogout() }) { Text("Заблокировать") }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        when (tab) {
            Tab.VAULT -> VaultTab(vaultVm, generatorVm)
            Tab.GENERATOR -> GeneratorTab(generatorVm)
            Tab.CORE -> CoreTab(container)
        }
    }
}

// ---------------------------------------------------------------- Хранилище

@Composable
private fun VaultTab(vm: VaultViewModel, generatorVm: GeneratorViewModel) {
    var editing by remember { mutableStateOf<PasswordEntry?>(null) }
    var adding by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vm.query, onValueChange = vm::onSearch, singleLine = true,
                    label = { Text("Поиск по сервису") }, modifier = Modifier.weight(1f),
                )
                Button(onClick = { adding = true; editing = null; vm.onSelect(null) }) { Text("+ Добавить") }
            }
            if (vm.entries.isEmpty()) Text("Записей нет", color = Color.Gray)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(vm.entries, key = { it.id }) { entry ->
                    val isSelected = vm.selected?.id == entry.id
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { adding = false; editing = null; vm.onSelect(entry) },
                        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else CardDefaults.cardColors(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.serviceName, style = MaterialTheme.typography.titleMedium)
                            Text(entry.login.ifEmpty { "—" }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Column(Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.padding(4.dp)) }
            val selected = vm.selected
            when {
                adding -> EntryEditor(null, generatorVm, onCancel = { adding = false }) { s, l, p ->
                    if (vm.onAdd(s, l, p)) adding = false
                }
                editing != null -> EntryEditor(editing, generatorVm, onCancel = { editing = null }) { s, l, p ->
                    if (vm.onEdit(editing!!, s, l, p)) editing = null
                }
                selected != null -> EntryDetails(selected, vm, onEdit = { editing = selected })
                else -> Text("Выберите запись слева или добавьте новую", color = Color.Gray)
            }
        }
    }
}

@Composable
private fun EntryDetails(entry: PasswordEntry, vm: VaultViewModel, onEdit: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.serviceName, style = MaterialTheme.typography.headlineSmall)
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Логин: ${entry.login.ifEmpty { "—" }}")
                    Text("Пароль: ${vm.revealedPassword ?: "••••••••••"}", fontFamily = FontFamily.Monospace)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vm.revealedPassword == null) Button(onClick = { vm.onShow(entry.id) }) { Text("Показать пароль") }
                else OutlinedButton(onClick = vm::onHide) { Text("Скрыть") }
                OutlinedButton(onClick = onEdit) { Text("Изменить") }
                TextButton(onClick = { confirmDelete = true }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            }
            HorizontalDivider()
            Text("Как запись хранится в БД (ChaCha20-Poly1305):", style = MaterialTheme.typography.labelLarge)
            SelectionContainer {
                Text(
                    "ciphertext = ${entry.encryptedPassword.ciphertext.toHex()}\n" +
                        "nonce      = ${entry.encryptedPassword.nonce.toHex()}\n" +
                        "tag        = ${entry.encryptedPassword.tag.toHex()}",
                    fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить запись?") },
            text = { Text("«${entry.serviceName}» будет удалена без возможности восстановления.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.onDelete(entry.id) }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun EntryEditor(
    entry: PasswordEntry?,
    generatorVm: GeneratorViewModel,
    onCancel: () -> Unit,
    onSave: (service: String, login: String, password: String) -> Unit,
) {
    var service by remember(entry) { mutableStateOf(entry?.serviceName ?: "") }
    var login by remember(entry) { mutableStateOf(entry?.login ?: "") }
    var password by remember(entry) { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (entry == null) "Новая запись" else "Изменение записи", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(service, { service = it }, label = { Text("Сервис или сайт") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(login, { login = it }, label = { Text("Логин") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                password, { password = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(if (entry == null) "Пароль" else "Новый пароль (пусто — не менять)") },
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { generatorVm.onGenerate()?.let { password = it; visible = true } }) {
                    Text("Сгенерировать (${generatorVm.options.length} симв.)")
                }
                TextButton(onClick = { visible = !visible }) { Text(if (visible) "Скрыть" else "Показать") }
            }
            generatorVm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(service, login, password) }) { Text("Сохранить") }
                TextButton(onClick = onCancel) { Text("Отмена") }
            }
        }
    }
}

// ---------------------------------------------------------------- Генератор

@Composable
private fun GeneratorTab(vm: GeneratorViewModel) {
    val o = vm.options
    Column(Modifier.fillMaxWidth(0.7f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Длина пароля: ${o.length}", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = o.length.toFloat(),
            onValueChange = { vm.onOptionsChange(o.copy(length = it.toInt())) },
            valueRange = GeneratorOptions.MIN_LENGTH.toFloat()..64f,
        )
        OptionRow("Строчные буквы (a–z)", o.useLowercase) { vm.onOptionsChange(o.copy(useLowercase = it)) }
        OptionRow("Заглавные буквы (A–Z)", o.useUppercase) { vm.onOptionsChange(o.copy(useUppercase = it)) }
        OptionRow("Цифры (0–9)", o.useDigits) { vm.onOptionsChange(o.copy(useDigits = it)) }
        OptionRow("Спецсимволы (!@#…)", o.useSymbols) { vm.onOptionsChange(o.copy(useSymbols = it)) }
        Button(onClick = { vm.onGenerate() }) { Text("Сгенерировать") }
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (vm.result.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(vm.result, Modifier.padding(16.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
                }
            }
            Text("Случайность: cc_random_int() из C-ядра (системный криптостойкий ГСЧ).", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OptionRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onChange(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(text)
    }
}

// ---------------------------------------------------------------- C-ядро

@Composable
private fun CoreTab(container: AppContainer) {
    val diagnostics = container.diagnostics
    var lines by remember { mutableStateOf<List<DiagnosticLine>>(emptyList()) }
    var sample by remember { mutableStateOf("Привет, Bitatom!") }
    var sampleResult by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Версия ядра: ${diagnostics?.coreVersion ?: "диагностика недоступна"}", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { lines = diagnostics?.runSelfTest().orEmpty() }, enabled = diagnostics != null) {
            Text("Запустить самопроверку C-ядра")
        }
        lines.forEach { line ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        (if (line.ok) "✔ " else "✘ ") + line.title,
                        color = if (line.ok) Color(0xFF2F855A) else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SelectionContainer { Text(line.details, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Песочница: шифрование ключом текущей сессии", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(sample, { sample = it }, label = { Text("Открытый текст") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            sampleResult = runCatching {
                val key = container.session.getKey()
                val enc = container.crypto.encrypt(sample, key)
                val dec = container.crypto.decrypt(enc, key)
                "ciphertext = ${enc.ciphertext.toHex()}\nnonce      = ${enc.nonce.toHex()}\n" +
                    "tag        = ${enc.tag.toHex()}\ndecrypted  = $dec"
            }.getOrElse { "Ошибка: ${it.message}" }
        }) { Text("Зашифровать и расшифровать") }
        if (sampleResult.isNotEmpty()) {
            SelectionContainer { Text(sampleResult, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
