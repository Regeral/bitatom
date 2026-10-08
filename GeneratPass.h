#include <stdio.h>
#include <sodium.h>
#include <string.h>
#include <stdlib.h>

char * Generate_Password(int lenght){

    char * digit = "0123456789";
    int digit_lenght = strlen(digit);

    char * low = "abcdefjhijklmnopqrstuvwxyz";
    int low_lenght = strlen(low);

    char * upp = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    int upp_lenght = strlen(upp);

    char * symbol = "!@#$%&*()_?/|\\№";
    int symbol_lenght = strlen(symbol);

    char * password = malloc(lenght + 1);
    if(password == NULL) return NULL;

    for (int i = 0; i < lenght; i++){

        int char_type = randombytes_uniform (4);

        if(char_type == 0){
            password[i] = digit[ randombytes_uniform (digit_lenght)];
        }

        if(char_type == 1){
            password[i] = low[ randombytes_uniform (low_lenght)];
        }

        if(char_type == 2){
            password[i] = upp[ randombytes_uniform (upp_lenght)];
        }

        if(char_type == 3){
            password[i] = symbol[ randombytes_uniform (symbol_lenght)];
        }
    }
    
    password[lenght] = '\0';

    return password;

    // printf("Password: %s", password);
    // printf("\n    ");

    free(password);

    return 0;

}