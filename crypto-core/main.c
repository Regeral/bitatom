#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#define SIZE_OF_PASSWORD 11
char chapters[] = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890";

int main()
{
    srand(time(NULL));
    int i, chapter;
    char password[SIZE_OF_PASSWORD];
    for (i = 0; i < (SIZE_OF_PASSWORD - 1); i++) {
        chapter = rand() % sizeof(chapters);
        password[i] = chapters[chapter];
    }
    password[SIZE_OF_PASSWORD - 1] = '\0';
    printf("Password: %s\n", password);
}
