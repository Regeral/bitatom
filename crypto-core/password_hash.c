#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include "monocypher.h"
#include "monocypher-ed25519.h"

int hash_master_password(const char *password, char *output)
{
    uint8_t hash[64];
    crypto_sha512(hash,password, strlen(password));

    for (int i = 0; i < 32; i++) {
        sprintf(output + i * 2, "%02x", hash[i]);
    }
    output[64] = '\0';
    return 0;
}
