#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>

#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
#include <wincrypt.h>
static int get_secure_random(uint32_t *num) {
    HCRYPTPROV hProvider;
    if (!CryptAcquireContext(&hProvider, NULL, NULL, PROV_RSA_FULL, CRYPT_VERIFYCONTEXT)) return -1;
    if (!CryptGenRandom(hProvider, sizeof(uint32_t), (BYTE*)num)) {
        CryptReleaseContext(hProvider, 0);
        return -1;
    }
    CryptReleaseContext(hProvider, 0);
    return 0;
}
#else
static int get_secure_random(uint32_t *num) {
    FILE *f = fopen("/dev/urandom", "rb");
    if (!f) return -1;
    size_t read_bytes = fread(num, 1, sizeof(uint32_t), f);
    fclose(f);
    return (read_bytes == sizeof(uint32_t)) ? 0 : -1;
}
#endif

static uint32_t get_random_uniform(uint32_t max) {
    uint32_t num = 0;
    if (get_secure_random(&num) != 0) {
        return rand() % max;
    }
    return num % max;
}

char * Generate_Password(int length) {
    if (length <= 0) return NULL;

    const char *digit = "0123456789";
    int digit_length = (int)strlen(digit);

    const char *low = "abcdefghijklmnopqrstuvwxyz";
    int low_length = (int)strlen(low);

    const char *upp = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    int upp_length = (int)strlen(upp);

    const char *symbol = "!@#$%&*()_?/|\\";
    int symbol_length = (int)strlen(symbol);

    char *password = malloc(length + 1);
    if (password == NULL) return NULL;

    for (int i = 0; i < length; i++) {
        int char_type = get_random_uniform(4);

        if (char_type == 0) {
            password[i] = digit[get_random_uniform(digit_length)];
        }
        else if (char_type == 1) {
            password[i] = low[get_random_uniform(low_length)];
        }
        else if (char_type == 2) {
            password[i] = upp[get_random_uniform(upp_length)];
        }
        else if (char_type == 3) {
            password[i] = symbol[get_random_uniform(symbol_length)];
        }
    }

    password[length] = '\0';
    return password;
}
