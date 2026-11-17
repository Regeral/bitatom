package ru.team.bitatom.bitatom.domain

/**
 * Модели предметной области (по диаграмме классов «Password Entry Management»).
 * Этот модуль (:core) общий для всех платформ и для сервера.
 */

/** Результат шифрования ChaCha20-Poly1305 из C-ядра: шифртекст + nonce (24 байта) + tag (16 байт). */
class EncryptedData(
    val ciphertext: ByteArray,
    val nonce: ByteArray,
    val tag: ByteArray,
)

/** Запись хранилища. Пароль хранится ТОЛЬКО в зашифрованном виде. */
data class PasswordEntry(
    val id: String,
    val serviceName: String,
    val login: String,
    val encryptedPassword: EncryptedData,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Данные мастер-пароля: сам пароль не хранится — только соль и проверочное значение (verifier). */
class MasterRecord(
    val salt: ByteArray,
    val verifier: ByteArray,
    val kdfParams: String,
)

/** Параметры генератора паролей (требования 11–12). */
data class GeneratorOptions(
    val length: Int = 16,
    val useLowercase: Boolean = true,
    val useUppercase: Boolean = true,
    val useDigits: Boolean = true,
    val useSymbols: Boolean = true,
) {
    private val selectedSets: Int
        get() = listOf(useLowercase, useUppercase, useDigits, useSymbols).count { it }

    fun validate(): Boolean =
        length in MIN_LENGTH..MAX_LENGTH && selectedSets > 0 && length >= selectedSets

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 128
    }
}

/** Ошибка: попытка доступа к данным при закрытой сессии. */
class SessionLockedException : IllegalStateException("Хранилище заблокировано: требуется вход")

/** Ошибка C-ядра (код возврата != 0). */
class CryptoException(message: String) : RuntimeException(message)

fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
