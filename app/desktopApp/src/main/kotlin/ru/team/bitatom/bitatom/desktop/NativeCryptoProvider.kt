package ru.team.bitatom.bitatom.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import ru.team.bitatom.bitatom.domain.CoreDiagnostics
import ru.team.bitatom.bitatom.domain.CryptoException
import ru.team.bitatom.bitatom.domain.CryptoProvider
import ru.team.bitatom.bitatom.domain.DiagnosticLine
import ru.team.bitatom.bitatom.domain.EncryptedData
import kotlin.time.measureTime

/** Прямое отображение функций crypto_api.h (crypto_core.dll) через JNA. */
@Suppress("FunctionName")
internal interface CryptoCoreLib : Library {
    fun cc_version(): String
    fun cc_random_bytes(out: ByteArray, len: Int): Int
    fun cc_random_int(max: Int): Int
    fun cc_derive_key(pass: ByteArray, passLen: Int, salt: ByteArray, saltLen: Int, memKib: Int, passes: Int, outKey: ByteArray): Int
    fun cc_make_verifier(key: ByteArray, out: ByteArray)
    fun cc_verify32(a: ByteArray, b: ByteArray): Int
    fun cc_encrypt(plain: ByteArray, len: Int, key: ByteArray, nonce: ByteArray, outCipher: ByteArray, outTag: ByteArray): Int
    fun cc_decrypt(cipher: ByteArray, len: Int, key: ByteArray, nonce: ByteArray, tag: ByteArray, outPlain: ByteArray): Int
    fun cc_hash_master_hex(password: ByteArray, outHex: ByteArray): Int
    fun cc_generate_password(length: Int): Pointer?
    fun cc_free(ptr: Pointer?)
    fun cc_wipe(ptr: ByteArray, len: Int)

    companion object {
        /**
         * JNA ищет crypto_core.dll: в -Djna.library.path, в PATH и в ресурсах classpath
         * (src/main/resources/win32-x86-64/crypto_core.dll — туда её кладёт build_windows.bat).
         */
        fun load(): CryptoCoreLib = Native.load("crypto_core", CryptoCoreLib::class.java)
    }
}

/** «cinterop C core» с диаграммы классов — реализация CryptoProvider на C-ядре. */
class NativeCryptoProvider : CryptoProvider, CoreDiagnostics {
    private val lib = CryptoCoreLib.load()

    override val kdfParams = "argon2id;m=$MEM_KIB;t=$PASSES;p=1"
    override val coreVersion: String get() = lib.cc_version()

    override fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { check(lib.cc_random_bytes(it, size), "cc_random_bytes") }

    override fun randomInt(max: Int): Int {
        val r = lib.cc_random_int(max)
        if (r < 0) throw CryptoException("cc_random_int вернул ошибку")
        return r
    }

    override fun generateSalt(): ByteArray = randomBytes(SALT_SIZE)

    override fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val pass = password.encodeToByteArray()
        val key = ByteArray(KEY_SIZE)
        try {
            check(lib.cc_derive_key(pass, pass.size, salt, salt.size, MEM_KIB, PASSES, key), "cc_derive_key")
        } finally {
            pass.fill(0)
        }
        return key
    }

    override fun makeVerifier(key: ByteArray): ByteArray =
        ByteArray(VERIFIER_SIZE).also { lib.cc_make_verifier(key, it) }

    override fun verifiersEqual(a: ByteArray, b: ByteArray): Boolean =
        a.size == 32 && b.size == 32 && lib.cc_verify32(a, b) == 0

    override fun encrypt(plain: String, key: ByteArray): EncryptedData {
        val bytes = plain.encodeToByteArray()
        val nonce = randomBytes(NONCE_SIZE)
        val cipher = ByteArray(bytes.size)
        val tag = ByteArray(TAG_SIZE)
        try {
            check(lib.cc_encrypt(bytes, bytes.size, key, nonce, cipher, tag), "cc_encrypt")
        } finally {
            bytes.fill(0)
        }
        return EncryptedData(cipher, nonce, tag)
    }

    override fun decrypt(data: EncryptedData, key: ByteArray): String {
        val out = ByteArray(data.ciphertext.size)
        val rc = lib.cc_decrypt(data.ciphertext, data.ciphertext.size, key, data.nonce, data.tag, out)
        if (rc == -2) throw CryptoException("Данные повреждены или ключ неверный (tag не совпал)")
        check(rc, "cc_decrypt")
        return out.decodeToString().also { out.fill(0) }
    }

    /** Вызов исходного Generate_Password из auth_gen.c (память освобождается в C через cc_free). */
    fun nativeGeneratePassword(length: Int): String {
        val ptr = lib.cc_generate_password(length) ?: throw CryptoException("Generate_Password вернул NULL")
        try {
            return ptr.getString(0, "UTF-8")
        } finally {
            lib.cc_free(ptr)
        }
    }

    /** Вызов исходного hash_master_password (SHA-512) из password_hash.c. */
    fun nativeHashMaster(password: String): String {
        val input = password.encodeToByteArray() + 0.toByte()
        val out = ByteArray(65)
        check(lib.cc_hash_master_hex(input, out), "cc_hash_master_hex")
        return out.copyOf(64).decodeToString()
    }

    override fun runSelfTest(): List<DiagnosticLine> {
        val result = mutableListOf<DiagnosticLine>()
        fun step(title: String, block: () -> Pair<Boolean, String>) {
            result += try {
                val (ok, details) = block()
                DiagnosticLine(ok, title, details)
            } catch (e: Throwable) {
                DiagnosticLine(false, title, e.message ?: e.toString())
            }
        }
        step("Загрузка crypto_core.dll") { true to coreVersion }
        step("ГСЧ ОС: cc_random_bytes(16)") { true to randomBytes(16).hex() }
        step("Generate_Password(16) из auth_gen.c") { nativeGeneratePassword(16).let { (it.length == 16) to it } }
        step("hash_master_password(\"test\") — SHA-512") {
            nativeHashMaster("test").let { it.startsWith("ee26b0dd4af7e749") to it }
        }
        lateinit var key: ByteArray
        step("Argon2id: deriveKey (m=$MEM_KIB КиБ, t=$PASSES)") {
            val salt = generateSalt()
            val t = measureTime { key = deriveKey("master-пароль", salt) }
            val again = deriveKey("master-пароль", salt)
            key.contentEquals(again) to "key=${key.hex().take(32)}…, время ${t.inWholeMilliseconds} мс, детерминирован"
        }
        val secret = "Пароль от почты: Qw3rty!"
        lateinit var enc: EncryptedData
        step("encrypt_data (ChaCha20-Poly1305)") {
            enc = encrypt(secret, key)
            true to "cipher=${enc.ciphertext.hex()}\nnonce=${enc.nonce.hex()}\ntag=${enc.tag.hex()}"
        }
        step("decrypt_data — восстановление") { decrypt(enc, key).let { (it == secret) to it } }
        step("decrypt_data — подделка шифртекста обнаружена") {
            val bad = enc.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
            val rc = lib.cc_decrypt(bad, bad.size, key, enc.nonce, enc.tag, ByteArray(bad.size))
            (rc == -2) to "код возврата $rc (ожидалось -2)"
        }
        step("verifier + crypto_verify32") {
            val v1 = makeVerifier(key)
            val v2 = makeVerifier(key)
            val other = makeVerifier(randomBytes(32))
            (verifiersEqual(v1, v2) && !verifiersEqual(v1, other)) to "verifier=${v1.hex()}"
        }
        runCatching { key.fill(0) }
        return result
    }

    private fun check(rc: Int, fn: String) {
        if (rc != 0) throw CryptoException("$fn вернул код $rc")
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

    companion object {
        const val KEY_SIZE = 32
        const val NONCE_SIZE = 24
        const val TAG_SIZE = 16
        const val SALT_SIZE = 16
        const val VERIFIER_SIZE = 32
        /** Параметры Argon2id по рекомендации OWASP: 19 МиБ, 2 прохода. */
        const val MEM_KIB = 19456
        const val PASSES = 2
    }
}
