#ifndef __GAME_ANNEXE_H__
#define __GAME_ANNEXE_H__

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

#include "game.h"

void print_error_mem(void* p, game g);

bool calculus_direction(cgame c, direction dir, uint line, uint row, uint* line_next, uint* row_next, bool wrap);

constraint convert_to_constraint(char c);

color convert_to_color(char c);

char convert_from_color(color c);

char convert_from_constraint(constraint n);

char* convert(constraint n, color c);

game default_ext();

void genWords(int pos, int len, color* word, unsigned long* count, game g, bool onlyfirst, bool* win);
void printWord(color* word, int len);
#endif