package ru.team.bitatom.bitatom.domain

/**
 * Абстракция криптографии. Реализация для Windows/JVM — NativeCryptoProvider (вызывает C-ядро через JNA),
 * для iOS/Kotlin Native позже — через cinterop (crypto-core/crypto.def).
 */
interface CryptoProvider {
    /** Строка с параметрами KDF, сохраняется в MasterRecord. */
    val kdfParams: String

    fun generateSalt(): ByteArray
    fun deriveKey(password: String, salt: ByteArray): ByteArray
    fun makeVerifier(key: ByteArray): ByteArray
    /** Сравнение в постоянном времени (crypto_verify32). */
    fun verifiersEqual(a: ByteArray, b: ByteArray): Boolean
    fun encrypt(plain: String, key: ByteArray): EncryptedData
    fun decrypt(data: EncryptedData, key: ByteArray): String
    /** Криптостойкое случайное число в диапазоне [0, max). */
    fun randomInt(max: Int): Int
    fun randomBytes(size: Int): ByteArray
}

/** Самопроверка C-ядра — для экрана «C-ядро» в MVP. */
interface CoreDiagnostics {
    val coreVersion: String
    fun runSelfTest(): List<DiagnosticLine>
}

data class DiagnosticLine(val ok: Boolean, val title: String, val details: String)

interface MasterRepository {
    fun save(record: MasterRecord)
    fun load(): MasterRecord?
}

interface EntryRepository {
    fun save(entry: PasswordEntry)
    fun findById(id: String): PasswordEntry?
    fun findAll(): List<PasswordEntry>
    fun searchByService(query: String): List<PasswordEntry>
    fun delete(id: String)
}

/** Клиент синхронизации с удалённым сервером (по HTTPS передаются только зашифрованные записи). */
interface SyncClient {
    fun push(entries: List<PasswordEntry>)
    fun pull(): List<PasswordEntry>
    fun delete(id: String) {}
}

/** Заглушка для MVP: сервер ещё не подключён. */
object NoOpSyncClient : SyncClient {
    override fun push(entries: List<PasswordEntry>) {}
    override fun pull(): List<PasswordEntry> = emptyList()
}
