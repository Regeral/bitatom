#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include "monocypher.h"

int hash_master_password(const char *password, char *output)
{
    uint8_t hash[32];
    crypto_sha256(hash, password, strlen(password));

    for (int i = 0; i < 32; i++) {
        sprintf(output + i * 2, "%02x", hash[i]);
    }
    output[64] = '\0';
    return 0;
}
