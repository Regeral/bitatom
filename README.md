# BitAtom

BitAtom is a cross-platform password manager. The user creates a master password once and uses it to unlock a local vault where service credentials are stored, searched, edited and generated. Passwords are never stored in plain text.

The project has three parts:

* **C crypto core** (`crypto-core/`) — encryption, key derivation, master password verification and a secure password generator, built on [Monocypher](https://monocypher.org/).
* **Kotlin Multiplatform application** (`app/`, `core/`) — domain logic, UI (Compose Multiplatform) and platform integrations.
* **Server** (`server/`) — Ktor server skeleton for future synchronization of encrypted records.

## Current status (MVP)

| Platform | Status |
|---|---|
| Windows (desktop) | Fully working: C core via JNA, local SQLite vault, complete UI |
| Android, iOS | Build as UI shells, show a "C core is not connected on this platform" message |
| Linux, macOS (desktop) | Not supported yet: the C core is only wired for Windows |
| Server / sync | Skeleton only, the client uses a no-op `SyncClient` stub |

## Features

* Create a master password and unlock the vault with it
* Add, view, edit and delete entries (service or site name, login, password)
* Search entries by service name
* Generate random passwords with a chosen length and character sets (lowercase, uppercase, digits, symbols)
* Lock the vault; the encryption key is kept in memory only while the vault is unlocked
* Passwords are stored encrypted (ChaCha20-Poly1305)

## Security design

* **Encryption:** ChaCha20-Poly1305 (AEAD), 32-byte key, 24-byte random nonce, 16-byte authentication tag. A tampered ciphertext is rejected on decryption.
* **Key derivation:** Argon2id (19 MiB memory, 2 passes, parallelism 1) with a random 16-byte salt.
* **Master password:** never stored. The database keeps only the salt, the KDF parameters and a verifier (keyed BLAKE2b of the derived key), compared in constant time.
* **Randomness:** the operating system CSPRNG (`CryptGenRandom` on Windows, `/dev/urandom` on macOS and Linux). Uniform sampling without modulo bias.
* **Database:** for every entry only the service name, login and the encrypted password (ciphertext + nonce + tag) are stored.

## Repository structure

```
bitatom/
├── crypto-core/        C core: crypto_api.c/h (FFI layer), encrypt_aes.c, auth_gen.c,
│                       password_hash.c, Monocypher, CMake and Windows build scripts
├── core/               Kotlin domain layer: models, interfaces, Session, AuthService,
│                       VaultService, PasswordGenerator
├── app/
│   ├── shared/         Compose Multiplatform UI, view models, AppContainer (DI)
│   ├── desktopApp/     Windows entry point, NativeCryptoProvider (JNA), SQLite repositories
│   ├── androidApp/     Android shell
│   └── iosApp/         iOS shell (Xcode project)
├── server/             Ktor server skeleton
├── build.sh            Builds the C core as a static library (CMake)
└── gradlew(.bat)       Gradle wrapper
```

## Building and running (Windows)

### 1. Prerequisites

| Tool | Purpose | How to get it |
|---|---|---|
| **Git** | clone the repository | https://git-scm.com |
| **IntelliJ IDEA** or **Android Studio** | IDE, JDK and Android SDK | The `shared` module is also built for Android, so Gradle needs the Android SDK |
| **JDK 17+** | running Gradle | Usually bundled with the IDE. The Gradle daemon toolchain (JDK 25) is downloaded automatically |
| **MinGW-w64 gcc (x64)** | building the C core into a DLL | Install MSYS2, then run `pacman -S mingw-w64-ucrt-x86_64-gcc` and add `C:\msys64\ucrt64\bin` to `PATH` |

Check in a new `cmd` window that `gcc --version` and `java -version` both work. Both must be **64-bit**, otherwise the DLL will not load.

If Gradle reports `SDK location not found`, create `local.properties` in the project root (this file is git-ignored):

```
sdk.dir=C\:\\Users\\<your_user>\\AppData\\Local\\Android\\Sdk
```

### 2. Clone the repository

```bat
git clone https://github.com/Regeral/bitatom.git
cd bitatom
```

### 3. Build the C core

```bat
cd crypto-core
build_windows.bat
```

The script:

1. compiles `build\crypto_core.dll` from `crypto_api.c` and the core sources;
2. builds and runs the console smoke test `crypto_test.exe` (it prints a generated password, the ciphertext and the decrypted text);
3. copies the DLL to `app\desktopApp\src\main\resources\win32-x86-64\`, where JNA picks it up automatically.

The DLL is a build artifact and is **not** committed to the repository. You must run this step after every fresh clone and after any change in `crypto-core/`.

Alternative with CMake and Visual Studio (MSVC), from the project root:

```bat
cmake -S crypto-core -B crypto-core\build -A x64
cmake --build crypto-core\build --config Release --target crypto_core_shared
mkdir app\desktopApp\src\main\resources\win32-x86-64
copy crypto-core\build\Release\crypto_core.dll app\desktopApp\src\main\resources\win32-x86-64\
```

### 4. Run the application

From the project root:

```bat
gradlew.bat :app:desktopApp:run
```

Or in the IDE: Gradle → `app` → `desktopApp` → Tasks → compose desktop → `run`. The first build takes a while because Gradle downloads dependencies.

### 5. Build a Windows installer (optional)

```bat
gradlew.bat :app:desktopApp:packageMsi
```

The installer appears in `app\desktopApp\build\compose\binaries\main\msi\`. The DLL is included automatically, because it lives in the app resources.

### Where the data is stored

`%APPDATA%\bitatom\vault.db` (SQLite). To start from scratch, for example after forgetting the master password, delete this file. There is no master password recovery by design.

### Troubleshooting

| Error | Cause and fix |
|---|---|
| `Unable to load library 'crypto_core'` | The DLL is not built or not copied. Run `crypto-core\build_windows.bat` |
| `%1 is not a valid Win32 application` | A 32-bit gcc with a 64-bit Java. Install an x64 compiler (ucrt64 or mingw64) |
| `SDK location not found` | Create `local.properties` as described in the prerequisites |
| `gcc is not recognized` | The `...\ucrt64\bin` folder is not in `PATH`. Open a new `cmd` window after changing it |
| The app shows "Could not start the C core or database" | The same causes as above, the message contains the exact error |

## Building the C core on its own

The C core can be compiled as a static library on any OS with CMake and a C compiler. This is the base for future Android, iOS, Linux and macOS integrations.

```bash
sh build.sh
```

or manually:

```bash
mkdir -p build && cd build
cmake ../crypto-core
cmake --build .
```

The library (`libcrypto_core.a` or `.lib`) appears in `build/`.

## Integration notes for the C core

* The public FFI layer is `crypto-core/crypto_api.h`. All lengths are `int32_t` so that they do not depend on the size of `size_t` or `long` on different operating systems.
* Nonce size is exactly **24 bytes**, authentication tag size is exactly **16 bytes**.
* The password generator allocates memory in C. Free the returned string with `cc_free`, and use `cc_wipe` for sensitive buffers.
* On the JVM the core is called through JNA (`NativeCryptoProvider`). On iOS it is intended to be called through Kotlin/Native `cinterop` (`crypto.def`).

## Roadmap

* Ktor server API and encrypted synchronization between devices
* C core integration for Android (JNI), iOS (cinterop), Linux and macOS
* Automatic vault lock on inactivity
* CI builds on Windows, Linux and macOS

## License

Apache License 2.0, see [LICENSE](LICENSE).
