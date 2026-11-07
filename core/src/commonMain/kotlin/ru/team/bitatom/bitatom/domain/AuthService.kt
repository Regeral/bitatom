package ru.team.bitatom.bitatom.domain

/**
 * Модуль авторизации (требования 1–2). Логика соответствует диаграмме последовательности
 * «создание мастер-пароля / вход»: salt → deriveKey → makeVerifier → сравнение verifier.
 */
class AuthService(
    private val crypto: CryptoProvider,
    private val masterRepository: MasterRepository,
    private val session: Session,
) {
    fun isMasterCreated(): Boolean = masterRepository.load() != null

    fun createMasterPassword(password: String): Boolean {
        if (password.length < MIN_MASTER_LENGTH || isMasterCreated()) return false
        val salt = crypto.generateSalt()
        val key = crypto.deriveKey(password, salt)
        val verifier = crypto.makeVerifier(key)
        masterRepository.save(MasterRecord(salt, verifier, crypto.kdfParams))
        session.unlock(key)
        key.fill(0)
        return true
    }

    fun login(password: String): Boolean {
        val record = masterRepository.load() ?: return false
        val key = crypto.deriveKey(password, record.salt)
        val verifier2 = crypto.makeVerifier(key)
        val ok = crypto.verifiersEqual(verifier2, record.verifier)
        if (ok) session.unlock(key)
        key.fill(0)
        return ok
    }

    fun logout() = session.lock()

    companion object {
        const val MIN_MASTER_LENGTH = 8
    }
}
