#ifndef CRYPTO_API_H
#define CRYPTO_API_H

/*
 * Публичный API C-ядра для приложения (FFI-слой).
 * Windows/JVM: вызывается из Kotlin через JNA (crypto_core.dll).
 * Kotlin/Native (iOS): через cinterop, см. crypto.def.
 * Все длины — int32_t, чтобы не зависеть от размера size_t/long на разных ОС.
 */

#include <stdint.h>

#if defined(_WIN32)
  #define CC_API __declspec(dllexport)
#else
  #define CC_API __attribute__((visibility("default")))
#endif

#define CC_KEY_SIZE      32
#define CC_NONCE_SIZE    24
#define CC_TAG_SIZE      16
#define CC_SALT_SIZE     16
#define CC_VERIFIER_SIZE 32

#ifdef __cplusplus
extern "C" {
#endif

CC_API const char *cc_version(void);

/* Криптостойкий ГСЧ ОС. 0 — успех. */
CC_API int     cc_random_bytes(uint8_t *out, int32_t len);
/* Равномерное случайное число в [0, max) без смещения по модулю. -1 при ошибке. */
CC_API int32_t cc_random_int(int32_t max);

/* Вывод ключа из мастер-пароля: Argon2id (Monocypher). mem_kib — память в КиБ, passes — число проходов. */
CC_API int  cc_derive_key(const uint8_t *pass, int32_t pass_len,
                          const uint8_t *salt, int32_t salt_len,
                          uint32_t mem_kib, uint32_t passes,
                          uint8_t out_key[CC_KEY_SIZE]);
/* verifier = BLAKE2b-keyed(key, "bitatom-verifier") — хранится вместо пароля. */
CC_API void cc_make_verifier(const uint8_t key[CC_KEY_SIZE], uint8_t out[CC_VERIFIER_SIZE]);
/* Сравнение 32 байт в постоянном времени: 0 — равны, -1 — нет. */
CC_API int  cc_verify32(const uint8_t a[32], const uint8_t b[32]);

/* Обёртки над encrypt_data / decrypt_data. */
CC_API int  cc_encrypt(const uint8_t *plain, int32_t len,
                       const uint8_t key[CC_KEY_SIZE], const uint8_t nonce[CC_NONCE_SIZE],
                       uint8_t *out_cipher, uint8_t out_tag[CC_TAG_SIZE]);
CC_API int  cc_decrypt(const uint8_t *cipher, int32_t len,
                       const uint8_t key[CC_KEY_SIZE], const uint8_t nonce[CC_NONCE_SIZE],
                       const uint8_t tag[CC_TAG_SIZE], uint8_t *out_plain);

/* Обёртка над hash_master_password (SHA-512, hex 64 символа + '\0'). */
CC_API int   cc_hash_master_hex(const char *password, char out_hex[65]);
/* Обёртка над Generate_Password. Результат освобождать через cc_free! */
CC_API char *cc_generate_password(int32_t length);
CC_API void  cc_free(void *ptr);
CC_API void  cc_wipe(void *ptr, int32_t len);

#ifdef __cplusplus
}
#endif

#endif /* CRYPTO_API_H */
