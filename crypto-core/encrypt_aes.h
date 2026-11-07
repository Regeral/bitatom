#ifndef ENCRYPT_AES_H
#define ENCRYPT_AES_H

#include <stddef.h>
#include <stdint.h>

/* ChaCha20-Poly1305 (Monocypher). key = 32 байта, nonce = 24 байта, tag = 16 байт. */
int encrypt_data(const uint8_t *plaintext, size_t plaintext_len,
                 const uint8_t *key, const uint8_t *nonce,
                 uint8_t *out_ciphertext, uint8_t *out_tag);

/* 0 — успех, -1 — неверные параметры, -2 — данные подделаны (tag не совпал). */
int decrypt_data(const uint8_t *ciphertext, size_t ciphertext_len,
                 const uint8_t *key, const uint8_t *nonce,
                 const uint8_t *tag, uint8_t *out_plaintext);

#endif /* ENCRYPT_AES_H */
