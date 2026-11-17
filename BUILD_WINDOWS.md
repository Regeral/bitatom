# Bitatom MVP — сборка и запуск под Windows

MVP менеджера паролей: общий код на Kotlin Multiplatform (`:core` + `:app:shared`) и Windows-реализация
в `:app:desktopApp`, которая вызывает C-ядро (`crypto-core`) через JNA.

## 1. Что нужно установить (один раз)

| Что | Зачем | Как |
|---|---|---|
| **IntelliJ IDEA** или **Android Studio** | IDE + JDK + Android SDK | Android SDK нужен Gradle'у, потому что модуль `shared` собирается и под Android |
| **JDK 17+** | запуск Gradle | обычно уже есть в IDE; JDK 25 для демона Gradle скачается сам (foojay) |
| **MinGW-w64 gcc (x64)** | сборка C-ядра в DLL | через MSYS2: `pacman -S mingw-w64-ucrt-x86_64-gcc`, затем добавить `C:\msys64\ucrt64\bin` в `PATH` |

Проверка в новом окне `cmd`: `gcc --version` и `java -version` должны работать.
**Важно:** gcc и Java должны быть 64-битными, иначе DLL не загрузится.

Если Gradle пишет `SDK location not found` — создайте в корне проекта файл `local.properties`:
```
sdk.dir=C\:\\Users\\<ваш_пользователь>\\AppData\\Local\\Android\\Sdk
```

## 2. Сборка C-ядра

```bat
cd crypto-core
build_windows.bat
```
Скрипт:
1. собирает `build\crypto_core.dll` из `crypto_api.c` + исходников ядра (`encrypt_aes.c`, `auth_gen.c`, `password_hash.c`, Monocypher);
2. собирает и запускает консольный тест `crypto_test.exe` (`main.c`) — вы увидите сгенерированный пароль, шифртекст и расшифровку;
3. копирует DLL в `app\desktopApp\src\main\resources\win32-x86-64\` — оттуда её автоматически подхватывает JNA.

Вариант через CMake + Visual Studio (MSVC):
```bat
cmake -S crypto-core -B crypto-core\build -A x64
cmake --build crypto-core\build --config Release --target crypto_core_shared
mkdir app\desktopApp\src\main\resources\win32-x86-64
copy crypto-core\build\Release\crypto_core.dll app\desktopApp\src\main\resources\win32-x86-64\
```

## 3. Запуск приложения

Из корня проекта:
```bat
gradlew.bat :app:desktopApp:run
```
или в IDE: Gradle → `app` → `desktopApp` → `Tasks` → `compose desktop` → `run`.

Первая сборка долгая (Gradle скачивает зависимости).

## 4. Установщик MSI (по желанию)

```bat
gradlew.bat :app:desktopApp:packageMsi
```
Результат: `app\desktopApp\build\compose\binaries\main\msi\`. DLL попадает внутрь автоматически (лежит в ресурсах).

## 5. Где хранятся данные

`%APPDATA%\bitatom\vault.db` (SQLite). В БД лежат: соль и verifier мастер-пароля, а у записей — сервис, логин
и **только шифртекст** пароля (ciphertext + nonce + tag). Чтобы начать с нуля (например, забыли мастер-пароль) — удалите этот файл.

## 6. Сценарий демонстрации

1. Первый запуск → создать мастер-пароль (≥ 8 символов) — требование 1.
2. «+ Добавить» → сервис, логин, пароль или «Сгенерировать» → «Сохранить» — 3–6, 10.
3. Поиск по названию сервиса — 7.
4. Выбрать запись → «Показать пароль» (расшифровка в C-ядре); внизу виден шифртекст из БД — 8, 13.
5. «Изменить» / «Удалить» — 9.
6. Вкладка «Генератор»: длина и наборы символов — 11, 12.
7. Вкладка «C-ядро» → «Запустить самопроверку»: загрузка DLL, ГСЧ, `Generate_Password`, SHA-512,
   Argon2id, шифрование/расшифровка, обнаружение подделки шифртекста.
8. «Заблокировать» → снова вход по мастер-паролю — 2; неверный пароль не пускает.

## 7. Частые ошибки

| Ошибка | Причина и решение |
|---|---|
| `Unable to load library 'crypto_core'` | DLL не собрана или не скопирована — запустите `crypto-core\build_windows.bat` |
| `%1 is not a valid Win32 application` | gcc 32-битный, а Java 64-битная — поставьте x64-компилятор (ucrt64/mingw64) |
| `SDK location not found` | см. `local.properties` в разделе 1 |
| `gcc не найден` / `gcc is not recognized` | не добавлен `...\ucrt64\bin` в `PATH`, откройте новое окно `cmd` |

## 8. Как код соответствует диаграммам

| Диаграмма | Где в коде |
|---|---|
| Модели `PasswordEntry`, `EncryptedData`, `MasterRecord`, `GeneratorOptions` | `core/.../domain/Models.kt` |
| `CryptoProvider`, `EntryRepository`, `MasterRepository`, `SyncClient` | `core/.../domain/Interfaces.kt` |
| `Session`, `AuthService`, `VaultService`, `PasswordGenerator` | `core/.../domain/*.kt` |
| `LoginViewModel`, `VaultViewModel`, `GeneratorViewModel` | `app/shared/.../ui/ViewModels.kt` |
| UI: экраны входа, списка, записи, генератора | `app/shared/.../ui/LoginScreen.kt`, `MainScreen.kt` |
| `NativeCryptoProvider` («cinterop C core») | `app/desktopApp/.../desktop/NativeCryptoProvider.kt` (на JVM — через JNA) |
| `SqlEntryRepository`, `SqlMasterRepository` | `app/desktopApp/.../desktop/SqlRepositories.kt` |
| C-ядро: шифрование, вывод ключа, ГСЧ | `crypto-core/crypto_api.c` → `encrypt_aes.c`, `auth_gen.c`, `password_hash.c`, Monocypher |

Примечания:
* На диаграмме компонентов указан AES-GCM, но ядро команды реализует **ChaCha20-Poly1305** (Monocypher) — это тоже AEAD-шифр, его и использует MVP.
  Вывод ключа — **Argon2id** (19 МиБ, 2 прохода), verifier — BLAKE2b с ключом, сравнение в постоянном времени.
* `cinterop` работает только для Kotlin/Native (iOS). Desktop-приложение работает на JVM, поэтому там C-ядро подключено через JNA;
  `crypto.def` обновлён для будущей iOS-сборки.
* Синхронизация с сервером пока заглушка (`NoOpSyncClient`) — следующий спринт.
