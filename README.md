# Crypto Core Module

This repository contains the standalone C crypto core for the password manager application. It handles secure password generation, master password hashing, and streaming data encryption for Kotlin Multiplatform (KMP) integration via `cinterop`.

## 🛡️ Architecture & Cryptography
Instead of heavy corporate libraries like OpenSSL or mbedTLS, this core is powered by **Monocypher** (ChaCha20-Poly1305), which provides:
* **ChaCha20-Poly1305** authenticated encryption (AEAD) for password records.
* **BLAKE2b** (via Monocypher) / **SHA-512** for secure master password hashing.
* **Zero Padding Required:** Streaming mode encrypts data byte-for-byte.
* **Side-Channel Resistance:** Fully protected against microprocessor timing attacks out of the box.

## 📂 Project Structure
* `crypto-core/` — Directory containing all C files, headers, and the KMP configuration.
  * `encrypt_aes.c` — Data encryption/decryption interface logic.
  * `auth_gen.c` — Dynamic password generator utilizing native OS secure RNG (`CryptGenRandom` on Windows, `/dev/urandom` on Mac/Linux).
  * `password_hash.c` — Master password hashing module.
  * `crypto.def` — Kotlin Multiplatform `cinterop` definition file.
  * `CMakeLists.txt` — Build system configuration file for static library compilation.
  * `main.c` — Integration and smoke tests.

## 🛠️ How to Build

### One-Click Compilation (Recommended)
You can compile the crypto core into a static library with a single command using the provided automation script:
```bash
sh build.sh
```

### Manual Compilation via CMake
Alternatively, you can run the compilation steps manually:
```bash
mkdir -p build && cd build
cmake ../crypto-core
cmake --build .
```
After successful compilation, the compiled static library (`libcrypto_core.a` or `.lib`) will be located inside the `build/` directory.

## ⚠️ KMP Integration Notes
* **Nonce (IV) Size:** Must be exactly **24 bytes** (ChaCha20 requirement).
* **Authentication Tag Size:** Exactly **16 bytes**.
* **Memory Management:** The password generator allocates memory inside C using `malloc`. After converting the result to a Kotlin string, **always free the memory** on the Kotlin side using `nativeHeap.free(pointer)`.
