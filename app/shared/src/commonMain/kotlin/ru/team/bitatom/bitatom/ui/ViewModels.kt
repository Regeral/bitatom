package ru.team.bitatom.bitatom.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.team.bitatom.bitatom.domain.AuthService
import ru.team.bitatom.bitatom.domain.GeneratorOptions
import ru.team.bitatom.bitatom.domain.PasswordEntry
import ru.team.bitatom.bitatom.domain.PasswordGenerator
import ru.team.bitatom.bitatom.domain.VaultService

/** Presentation-слой (по диаграмме классов): LoginViewModel, VaultViewModel, GeneratorViewModel. */

class LoginViewModel(private val auth: AuthService) {
    var isMasterCreated by mutableStateOf(auth.isMasterCreated())
        private set
    var isUnlocked by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun onCreateMaster(password: String, confirm: String) {
        error = when {
            password.length < AuthService.MIN_MASTER_LENGTH ->
                "Мастер-пароль должен быть не короче ${AuthService.MIN_MASTER_LENGTH} символов"
            password != confirm -> "Пароли не совпадают"
            !auth.createMasterPassword(password) -> "Не удалось создать мастер-пароль"
            else -> null
        }
        if (error == null) { isMasterCreated = true; isUnlocked = true }
    }

    fun onLogin(password: String) {
        val ok = runCatching { auth.login(password) }.getOrElse { error = it.message; return }
        error = if (ok) null else "Неверный мастер-пароль"
        isUnlocked = ok
    }

    fun onLogout() {
        auth.logout()
        isUnlocked = false
    }
}

class VaultViewModel(private val vault: VaultService) {
    var query by mutableStateOf("")
        private set
    var entries by mutableStateOf<List<PasswordEntry>>(emptyList())
        private set
    var selected by mutableStateOf<PasswordEntry?>(null)
        private set
    var revealedPassword by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private inline fun safe(block: () -> Unit) {
        error = null
        runCatching(block).onFailure { error = it.message ?: it.toString() }
    }

    fun refresh() = safe { entries = vault.search(query) }

    fun onSearch(q: String) { query = q; refresh() }

    fun onSelect(entry: PasswordEntry?) { selected = entry; revealedPassword = null }

    fun onShow(id: String) = safe { revealedPassword = vault.getDecryptedPassword(id) }

    fun onHide() { revealedPassword = null }

    fun onAdd(service: String, login: String, password: String): Boolean {
        var ok = false
        safe { selected = vault.addEntry(service, login, password); revealedPassword = null; ok = true }
        refresh()
        return ok
    }

    fun onEdit(entry: PasswordEntry, service: String, login: String, newPassword: String): Boolean {
        var ok = false
        safe {
            selected = vault.updateEntry(entry.copy(serviceName = service, login = login), newPassword.ifEmpty { null })
            revealedPassword = null
            ok = true
        }
        refresh()
        return ok
    }

    fun onDelete(id: String) {
        safe { vault.deleteEntry(id); selected = null; revealedPassword = null }
        refresh()
    }
}

class GeneratorViewModel(private val generator: PasswordGenerator) {
    var options by mutableStateOf(GeneratorOptions())
        private set
    var result by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun onOptionsChange(newOptions: GeneratorOptions) { options = newOptions }

    fun onGenerate(): String? {
        if (!options.validate()) {
            error = "Выберите хотя бы один набор символов; длина ${GeneratorOptions.MIN_LENGTH}–${GeneratorOptions.MAX_LENGTH}"
            return null
        }
        error = null
        result = generator.generate(options)
        return result
    }
}
