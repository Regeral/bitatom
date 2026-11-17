package ru.team.bitatom.bitatom

import ru.team.bitatom.bitatom.domain.AuthService
import ru.team.bitatom.bitatom.domain.CoreDiagnostics
import ru.team.bitatom.bitatom.domain.CryptoProvider
import ru.team.bitatom.bitatom.domain.EntryRepository
import ru.team.bitatom.bitatom.domain.MasterRepository
import ru.team.bitatom.bitatom.domain.NoOpSyncClient
import ru.team.bitatom.bitatom.domain.PasswordGenerator
import ru.team.bitatom.bitatom.domain.Session
import ru.team.bitatom.bitatom.domain.SyncClient
import ru.team.bitatom.bitatom.domain.VaultService

/**
 * Ручной DI-контейнер. Платформа (desktop/android/ios) передаёт свои реализации
 * CryptoProvider и репозиториев, а сервисы и UI — общие.
 */
class AppContainer(
    val crypto: CryptoProvider,
    val diagnostics: CoreDiagnostics?,
    masterRepository: MasterRepository,
    entryRepository: EntryRepository,
    syncClient: SyncClient = NoOpSyncClient,
    now: () -> Long,
) {
    val session = Session()
    val authService = AuthService(crypto, masterRepository, session)
    val vaultService = VaultService(crypto, entryRepository, session, syncClient, now)
    val passwordGenerator = PasswordGenerator(crypto)
}
