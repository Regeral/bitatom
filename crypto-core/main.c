#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <stdlib.h>
#include "GeneratPass.h"

/* Function prototypes for the encryption module */
int encrypt_data(const uint8_t *plaintext, size_t plaintext_len,
                 const uint8_t *key, const uint8_t *nonce,
                 uint8_t *out_ciphertext, uint8_t *out_tag);

int decrypt_data(const uint8_t *ciphertext, size_t ciphertext_len,
                 const uint8_t *key, const uint8_t *nonce,
                 const uint8_t *tag, uint8_t *out_plaintext);

int main() {
    printf("--- CRYPTO CORE INTEGRATION TEST ---\n\n");

    /* 1. Test secure password generation */
    int target_length = 12;
    char *generated_password = Generate_Password(target_length);
    
    if (generated_password == NULL) {
        printf("Error: Password generator returned NULL\n");
        return 1;
    }
    
    size_t len = strlen(generated_password);
    printf("[RNG] Generated password: %s (Length: %zu)\n\n", generated_password, len);

    /* 32-byte encryption key derived from master password */
    uint8_t key[32] = {
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
        0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
        0x17, 0x18, 0x19, 0x20, 0x21, 0x22, 0x23, 0x24,
        0x25, 0x26, 0x27, 0x28, 0x29, 0x30, 0x31, 0x32
    };

    /* 24-byte initialization vector (Nonce) for ChaCha20-Poly1305 */
    uint8_t nonce[24] = {
        0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF, 0x11, 0x22,
        0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99, 0x00,
        0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF, 0x11, 0x22
    };

    uint8_t ciphertext[64] = {0};
    uint8_t tag[16] = {0};
    uint8_t decrypted[64] = {0};

    /* 2. Test data encryption */
    if (encrypt_data((const uint8_t*)generated_password, len, key, nonce, ciphertext, tag) == 0) {
        printf("[Cipher] Encryption successful\n");
        printf("[Cipher] Ciphertext (HEX): ");
        for (size_t i = 0; i < len; i++) printf("%02X ", ciphertext[i]);
        
        printf("\n[Cipher] Authentication Tag (HEX): ");
        for (int i = 0; i < 16; i++) printf("%02X ", tag[i]);
        printf("\n\n");
    } else {
        printf("[Cipher] Encryption failed\n");
        free(generated_password);
        return 1;
    }

    /* 3. Test data decryption and verification */
    int dec_res = decrypt_data(ciphertext, len, key, nonce, tag, decrypted);
    if (dec_res == 0) {
        decrypted[len] = '\0';
        printf("[Cipher] Decryption successful\n");
        printf("[Cipher] Restored plaintext: %s\n", (char*)decrypted);
    } else {
        printf("[Cipher] Decryption failed with code: %d\n", dec_res);
    }

    /* Free dynamically allocated memory from the generator */
    free(generated_password); 

    return 0;
}
