#include <string.h>
#include <stdint.h>
#include <stddef.h>
#include "monocypher.h"

// ============================================================================
// [INTERFACE FUNCTIONS]: CALLED BY KOTLIN / CINTEROP
// ============================================================================

/**
 * Encrypts data using Monocypher (ChaCha20-Poly1305).
 * No padding is required as ChaCha20 is a stream cipher.
 * Ciphertext length will be identical to plaintext length.
 * 
 * @param plaintext      Pointer to the original string/bytes to encrypt
 * @param plaintext_len  Length of the plaintext
 * @param key            32-byte encryption key (derived from master password)
 * @param nonce          24-byte unique initialization vector (IV)
 * @param out_ciphertext Buffer to store the resulting encrypted bytes
 * @param out_tag        16-byte buffer to store the authentication tag
 * @return               0 on success, -1 on invalid parameters
 */
int encrypt_data(const uint8_t *plaintext, size_t plaintext_len,
                 const uint8_t *key, const uint8_t *nonce,
                 uint8_t *out_ciphertext, uint8_t *out_tag) 
{
    if (!plaintext || !key || !nonce || !out_ciphertext || !out_tag) {
        return -1;
    }

    crypto_aead_lock(out_ciphertext, out_tag, key, nonce, 
                     NULL, 0, 
                     plaintext, plaintext_len);
                     
    return 0;
}


/**
 * Decrypts data and verifies its integrity using Monocypher.
 * Automatically checks the authentication tag to ensure data hasn't been tampered with.
 * 
 * @param ciphertext     Pointer to the encrypted bytes
 * @param ciphertext_len Length of the ciphertext
 * @param key            32-byte encryption key
 * @param nonce          24-byte initialization vector (IV)
 * @param tag            16-byte authentication tag
 * @param out_plaintext  Buffer to store the decrypted password string
 * @return               0 on success, -1 on invalid params, -2 on authentication failure
 */
int decrypt_data(const uint8_t *ciphertext, size_t ciphertext_len,
                 const uint8_t *key, const uint8_t *nonce,
                 const uint8_t *tag, uint8_t *out_plaintext) 
{
    if (!ciphertext || !key || !nonce || !tag || !out_plaintext) {
        return -1;
    }


    int result = crypto_aead_unlock(out_plaintext, tag, key, nonce, 
                                    NULL, 0,
                                    ciphertext, ciphertext_len);
    
    if (result != 0) {
        // Securely wipe the buffer on authentication failure
        memset(out_plaintext, 0, ciphertext_len);
        return -2;
    }

    return 0;
}
