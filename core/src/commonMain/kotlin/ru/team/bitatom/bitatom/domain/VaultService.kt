package ru.team.bitatom.bitatom.domain

/** Модуль хранилища записей (требования 3–9, 13). Все операции требуют открытой сессии. */
class VaultService(
    private val crypto: CryptoProvider,
    private val repository: EntryRepository,
    private val session: Session,
    private val syncClient: SyncClient = NoOpSyncClient,
    private val now: () -> Long,
) {
    fun addEntry(serviceName: String, login: String, password: String): PasswordEntry {
        require(serviceName.isNotBlank()) { "Укажите название сервиса" }
        require(password.isNotEmpty()) { "Пароль не может быть пустым" }
        val key = session.getKey()
        val time = now()
        val entry = PasswordEntry(
            id = crypto.randomBytes(16).toHex(),
            serviceName = serviceName.trim(),
            login = login.trim(),
            encryptedPassword = crypto.encrypt(password, key),
            createdAt = time,
            updatedAt = time,
        )
        repository.save(entry)
        syncClient.push(listOf(entry))
        return entry
    }

    fun getEntry(id: String): PasswordEntry? {
        session.getKey()
        return repository.findById(id)
    }

    /** Расшифровка пароля выбранной записи — только при открытой сессии. */
    fun getDecryptedPassword(id: String): String {
        if (!session.isUnlocked) throw SessionLockedException()
        val key = session.getKey()
        val entry = repository.findById(id) ?: throw NoSuchElementException("Запись не найдена")
        return crypto.decrypt(entry.encryptedPassword, key)
    }

    /** Изменение записи. Если newPassword == null — пароль остаётся прежним. */
    fun updateEntry(entry: PasswordEntry, newPassword: String?): PasswordEntry {
        require(entry.serviceName.isNotBlank()) { "Укажите название сервиса" }
        val key = session.getKey()
        val updated = entry.copy(
            serviceName = entry.serviceName.trim(),
            login = entry.login.trim(),
            encryptedPassword = if (newPassword.isNullOrEmpty()) entry.encryptedPassword
            else crypto.encrypt(newPassword, key),
            updatedAt = now(),
        )
        repository.save(updated)
        syncClient.push(listOf(updated))
        return updated
    }

    fun deleteEntry(id: String) {
        session.getKey()
        repository.delete(id)
        syncClient.delete(id)
    }

    /** Поиск по названию сервиса. Пароли в результатах остаются зашифрованными. */
    fun search(query: String): List<PasswordEntry> {
        session.getKey()
        return if (query.isBlank()) repository.findAll() else repository.searchByService(query.trim())
    }
}
