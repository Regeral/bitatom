package ru.team.bitatom.bitatom.desktop

import ru.team.bitatom.bitatom.domain.EncryptedData
import ru.team.bitatom.bitatom.domain.EntryRepository
import ru.team.bitatom.bitatom.domain.MasterRecord
import ru.team.bitatom.bitatom.domain.MasterRepository
import ru.team.bitatom.bitatom.domain.PasswordEntry
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/** Локальная БД SQLite (в %APPDATA%\bitatom\vault.db). Пароли лежат только как шифртекст. */
class LocalDatabase(file: File = defaultFile()) : AutoCloseable {
    val connection: Connection

    init {
        file.parentFile?.mkdirs()
        connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
        connection.createStatement().use {
            it.executeUpdate(
                """CREATE TABLE IF NOT EXISTS master(
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    salt BLOB NOT NULL, verifier BLOB NOT NULL, kdf_params TEXT NOT NULL)"""
            )
            it.executeUpdate(
                """CREATE TABLE IF NOT EXISTS entries(
                    id TEXT PRIMARY KEY, service_name TEXT NOT NULL, login TEXT NOT NULL,
                    ciphertext BLOB NOT NULL, nonce BLOB NOT NULL, tag BLOB NOT NULL,
                    created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)"""
            )
        }
    }

    override fun close() = connection.close()

    companion object {
        fun defaultFile(): File {
            val base = System.getenv("APPDATA") ?: System.getProperty("user.home")
            return File(File(base, "bitatom"), "vault.db")
        }
    }
}

class SqlMasterRepository(private val db: LocalDatabase) : MasterRepository {
    override fun save(record: MasterRecord) {
        db.connection.prepareStatement(
            "INSERT OR REPLACE INTO master(id, salt, verifier, kdf_params) VALUES (1, ?, ?, ?)"
        ).use {
            it.setBytes(1, record.salt)
            it.setBytes(2, record.verifier)
            it.setString(3, record.kdfParams)
            it.executeUpdate()
        }
    }

    override fun load(): MasterRecord? =
        db.connection.prepareStatement("SELECT salt, verifier, kdf_params FROM master WHERE id = 1").use { st ->
            st.executeQuery().use { rs ->
                if (rs.next()) MasterRecord(rs.getBytes(1), rs.getBytes(2), rs.getString(3)) else null
            }
        }
}

class SqlEntryRepository(private val db: LocalDatabase) : EntryRepository {
    override fun save(entry: PasswordEntry) {
        db.connection.prepareStatement(
            """INSERT OR REPLACE INTO entries
               (id, service_name, login, ciphertext, nonce, tag, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?)"""
        ).use {
            it.setString(1, entry.id)
            it.setString(2, entry.serviceName)
            it.setString(3, entry.login)
            it.setBytes(4, entry.encryptedPassword.ciphertext)
            it.setBytes(5, entry.encryptedPassword.nonce)
            it.setBytes(6, entry.encryptedPassword.tag)
            it.setLong(7, entry.createdAt)
            it.setLong(8, entry.updatedAt)
            it.executeUpdate()
        }
    }

    override fun findById(id: String): PasswordEntry? =
        db.connection.prepareStatement("SELECT * FROM entries WHERE id = ?").use { st ->
            st.setString(1, id)
            st.executeQuery().use { rs -> if (rs.next()) rs.toEntry() else null }
        }

    override fun findAll(): List<PasswordEntry> =
        db.connection.prepareStatement("SELECT * FROM entries ORDER BY service_name COLLATE NOCASE").use { st ->
            st.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toEntry()) } }
        }

    /** Фильтрация в Kotlin: LIKE в SQLite не умеет регистронезависимо искать кириллицу. */
    override fun searchByService(query: String): List<PasswordEntry> =
        findAll().filter { it.serviceName.contains(query, ignoreCase = true) }

    override fun delete(id: String) {
        db.connection.prepareStatement("DELETE FROM entries WHERE id = ?").use {
            it.setString(1, id)
            it.executeUpdate()
        }
    }

    private fun ResultSet.toEntry() = PasswordEntry(
        id = getString("id"),
        serviceName = getString("service_name"),
        login = getString("login"),
        encryptedPassword = EncryptedData(getBytes("ciphertext"), getBytes("nonce"), getBytes("tag")),
        createdAt = getLong("created_at"),
        updatedAt = getLong("updated_at"),
    )
}
