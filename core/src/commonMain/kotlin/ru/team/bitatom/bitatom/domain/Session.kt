package ru.team.bitatom.bitatom.domain

/** Сессия: держит ключ шифрования в памяти, пока хранилище открыто. */
class Session {
    private var key: ByteArray? = null

    val isUnlocked: Boolean get() = key != null

    fun unlock(key: ByteArray) {
        lock()
        this.key = key.copyOf()
    }

    /** Блокировка: ключ затирается нулями. */
    fun lock() {
        key?.fill(0)
        key = null
    }

    fun getKey(): ByteArray = key ?: throw SessionLockedException()
}
