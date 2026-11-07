#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "crypto_api.h"
#include "encrypt_aes.h"
#include "GeneratPass.h"
#include "password_hash.h"
#include "monocypher.h"

#if defined(_WIN32)
  #include <windows.h>
  #include <wincrypt.h>
#endif

static const uint8_t VERIFIER_CONTEXT[] = "bitatom-verifier";

CC_API const char *cc_version(void)
{
    return "bitatom crypto-core 0.2 | Monocypher 4: ChaCha20-Poly1305, Argon2id, BLAKE2b, SHA-512";
}

CC_API int cc_random_bytes(uint8_t *out, int32_t len)
{
    if (!out || len <= 0) return -1;
#if defined(_WIN32)
    HCRYPTPROV prov;
    if (!CryptAcquireContext(&prov, NULL, NULL, PROV_RSA_FULL, CRYPT_VERIFYCONTEXT)) return -1;
    BOOL ok = CryptGenRandom(prov, (DWORD)len, (BYTE *)out);
    CryptReleaseContext(prov, 0);
    return ok ? 0 : -1;
#else
    FILE *f = fopen("/dev/urandom", "rb");
    if (!f) return -1;
    size_t n = fread(out, 1, (size_t)len, f);
    fclose(f);
    return n == (size_t)len ? 0 : -1;
#endif
}

CC_API int32_t cc_random_int(int32_t max)
{
    if (max <= 0) return -1;
    uint32_t limit = UINT32_MAX - (UINT32_MAX % (uint32_t)max); /* отбрасываем «хвост» — нет смещения */
    uint32_t r;
    do {
        if (cc_random_bytes((uint8_t *)&r, sizeof r) != 0) return -1;
    } while (r >= limit);
    return (int32_t)(r % (uint32_t)max);
}

CC_API int cc_derive_key(const uint8_t *pass, int32_t pass_len,
                         const uint8_t *salt, int32_t salt_len,
                         uint32_t mem_kib, uint32_t passes,
                         uint8_t out_key[CC_KEY_SIZE])
{
    if (!pass || pass_len <= 0 || !salt || salt_len < 8 || !out_key || mem_kib < 8 || passes < 1) return -1;
    void *work_area = malloc((size_t)mem_kib * 1024);
    if (!work_area) return -2;

    crypto_argon2_config config = { CRYPTO_ARGON2_ID, mem_kib, passes, 1 };
    crypto_argon2_inputs inputs = { pass, salt, (uint32_t)pass_len, (uint32_t)salt_len };
    crypto_argon2(out_key, CC_KEY_SIZE, work_area, config, inputs, crypto_argon2_no_extras);

    crypto_wipe(work_area, (size_t)mem_kib * 1024);
    free(work_area);
    return 0;
}

CC_API void cc_make_verifier(const uint8_t key[CC_KEY_SIZE], uint8_t out[CC_VERIFIER_SIZE])
{
    crypto_blake2b_keyed(out, CC_VERIFIER_SIZE, key, CC_KEY_SIZE,
                         VERIFIER_CONTEXT, sizeof(VERIFIER_CONTEXT) - 1);
}

CC_API int cc_verify32(const uint8_t a[32], const uint8_t b[32])
{
    return crypto_verify32(a, b);
}

CC_API int cc_encrypt(const uint8_t *plain, int32_t len,
                      const uint8_t key[CC_KEY_SIZE], const uint8_t nonce[CC_NONCE_SIZE],
                      uint8_t *out_cipher, uint8_t out_tag[CC_TAG_SIZE])
{
    if (len <= 0) return -1;
    return encrypt_data(plain, (size_t)len, key, nonce, out_cipher, out_tag);
}

CC_API int cc_decrypt(const uint8_t *cipher, int32_t len,
                      const uint8_t key[CC_KEY_SIZE], const uint8_t nonce[CC_NONCE_SIZE],
                      const uint8_t tag[CC_TAG_SIZE], uint8_t *out_plain)
{
    if (len <= 0) return -1;
    return decrypt_data(cipher, (size_t)len, key, nonce, tag, out_plain);
}

CC_API int cc_hash_master_hex(const char *password, char out_hex[65])
{
    if (!password || !out_hex) return -1;
    return hash_master_password(password, out_hex);
}

CC_API char *cc_generate_password(int32_t length)
{
    return Generate_Password((int)length);
}

CC_API void cc_free(void *ptr) { free(ptr); }

CC_API void cc_wipe(void *ptr, int32_t len)
{
    if (ptr && len > 0) crypto_wipe(ptr, (size_t)len);
}
