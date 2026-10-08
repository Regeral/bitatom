#include <stdio.h>
#include <string.h>
#include <stdint.h>

int encrypt_data(const uint8_t *plaintext, size_t plaintext_len,
                 const uint8_t *key, const uint8_t *nonce,
                 uint8_t *out_ciphertext, uint8_t *out_tag);

int decrypt_data(const uint8_t *ciphertext, size_t ciphertext_len,
                 const uint8_t *key, const uint8_t *nonce,
                 const uint8_t *tag, uint8_t *out_plaintext);

int main() {
    // 1. Исходный пароль пользователя
    const char *password = "VkPass777";
    size_t len = strlen(password);

    // Ключ (32 байта) полученный из мастер-пароля
    uint8_t key[32] = {
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
        0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
        0x17, 0x18, 0x19, 0x20, 0x21, 0x22, 0x23, 0x24,
        0x25, 0x26, 0x27, 0x28, 0x29, 0x30, 0x31, 0x32
    };

    // Случайный Nonce / IV (24 байта для Monocypher)
    uint8_t nonce[24] = {
        0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF, 0x11, 0x22,
        0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99, 0x00,
        0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF, 0x11, 0x22
    };

    uint8_t ciphertext[64] = {0};
    uint8_t tag[16] = {0};
    uint8_t decrypted[64] = {0};

    printf("--- Тест Monocypher ---\n");
    printf("Исходный пароль: %s\n", password);

    // 2. Тестируем шифрование
    if (encrypt_data((const uint8_t*)password, len, key, nonce, ciphertext, tag) == 0) {
        printf("Шифрование успешно!\nЗашифрованные байты (HEX): ");
        for (size_t i = 0; i < len; i++) printf("%02X ", ciphertext[i]);
        
        printf("\nТег подлинности (Tag HEX): ");
        for (int i = 0; i < 16; i++) printf("%02X ", tag[i]);
        printf("\n");
    } else {
        printf("Ошибка при шифровании!\n");
        return 1;
    }

    // 3. Тестируем расшифрование
    int dec_res = decrypt_data(ciphertext, len, key, nonce, tag, decrypted);
    if (dec_res == 0) {
        decrypted[len] = '\0'; // Строка должна заканчиваться нулем
        printf("Расшифрование успешно!\nВосстановленный пароль: %s\n", (char*)decrypted);
    } else {
        printf("Ошибка расшифрования! Код ошибки: %d\n", dec_res);
    }

    return 0;
}
