#include "game_annexe.h"

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

#include "game.h"
#include "game_ext.h"
#include "game_struct.h"

void print_error_mem(void* p, game g)
{
  // Fonction qui affiche une erreur d'allocation mémoire si p est NULL et detruit la game
  if (p == NULL) {
    fprintf(stderr, "The memory allocation failed\n");
    game_delete(g);
  }
}

bool calculus_direction(cgame c, direction dir, uint row, uint col, uint* row_next, uint* col_next, bool wrap)
{
  // Fonction qui en fonction de la direction effectue les tests pour vérifier que le coups est possible dans la
  // direction demandé si le coups est impossible return false sinon les coordonnées de la nouvelle case sont stockés
  // dans col_next et row_next et return true si la direction n'existe pas alors return false
  if (wrap == true) {
    switch (dir) {
      case HERE:
        *col_next = col;
        *row_next = row;
        return true;

      case UP:
        if (row == 0) {
          *row_next = c->rows - 1;
        } else {
          *row_next = row - 1;
        }
        *col_next = col;
        return true;

      case DOWN:
        *col_next = col;
        *row_next = (row + 1) % c->rows;
        return true;

      case RIGHT:
        *col_next = (col + 1) % c->cols;
        *row_next = row % c->rows;
        return true;

      case LEFT:
        if (col == 0) {
          *col_next = c->cols - 1;
        } else {
          *col_next = col - 1;
        }
        *row_next = row;
        return true;

      case UP_RIGHT:
        *col_next = (col + 1) % c->cols;
        if (row == 0) {
          *row_next = c->rows - 1;
        } else {
          *row_next = row - 1;
        }
        return true;

      case UP_LEFT:
        if (row == 0) {
          *row_next = c->rows - 1;
        } else {
          *row_next = row - 1;
        }
        if (col == 0) {
          *col_next = c->cols - 1;
        } else {
          *col_next = col - 1;
        }
        return true;

      case DOWN_RIGHT:
        *col_next = (col + 1) % c->cols;
        *row_next = (row + 1) % c->rows;
        return true;

      case DOWN_LEFT:
        *row_next = (row + 1) % c->rows;
        if (col == 0) {
          *col_next = c->cols - 1;
        } else {
          *col_next = col - 1;
        }
        return true;

      default:
        return false;
    }
  } else {
    switch (dir) {
      case HERE:
        *col_next = col;
        *row_next = row;
        return true;

      case UP:
        if (row == 0) {
          return false;
        }
        *row_next = row - 1;
        *col_next = col;
        return true;

      case DOWN:
        if (row == c->rows - 1) {
          return false;
        }
        *col_next = col;
        *row_next = row + 1;
        return true;

      case RIGHT:
        if (col == c->cols - 1) {
          return false;
        }
        *col_next = col + 1;
        *row_next = row;
        return true;

      case LEFT:
        if (col == 0) {
          return false;
        }
        *col_next = col - 1;
        *row_next = row;
        return true;

      case UP_RIGHT:
        if (row == 0 || col == c->cols - 1) {
          return false;
        }
        *row_next = row - 1;
        *col_next = col + 1;
        return true;

      case UP_LEFT:
        if (row == 0 || col == 0) {
          return false;
        }
        *col_next = col - 1;
        *row_next = row - 1;
        return true;

      case DOWN_RIGHT:
        if (row == c->rows - 1 || col == c->cols - 1) {
          return false;
        }
        *col_next = col + 1;
        *row_next = row + 1;
        return true;

      case DOWN_LEFT:
        if (row == c->rows - 1 || col == 0) {
          return false;
        }
        *col_next = col - 1;
        *row_next = row + 1;
        return true;

      default:
        return false;
    }
  }
  return false;
}

constraint convert_to_constraint(char c)
{
  if (c == '-') return -1;
  if (c >= '0' && c <= '9') return c - '0';
  return -2;
}

color convert_to_color(char c)
{
  if (c == 'e') return EMPTY;
  if (c == 'w') return WHITE;
  if (c == 'b') return BLACK;
  return -1;
}

char convert_from_color(color c)
{
  char str_col[3] = {'e', 'w', 'b'};
  return str_col[c];
}

char convert_from_constraint(constraint n)
{
  if (n == -1) return '-';
  if (n >= 0 && n <= 9) return '0' + n;
  return -1;
}

char* convert_tab[33] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "🄋", "➀", "➁", "➂", "➃", "➄", "➅",
                         "➆", "➇", "➈", "⓿", "❶", "❷", "❸", "❹", "❺", "❻", "❼",    "❽", "❾", " ", "□", "■"};

char* convert(constraint n, color c)
{
  if (n == UNCONSTRAINED)
    return convert_tab[c + 30];
  else
    return convert_tab[c * 10 + n];
}

void printWord(color* tab_color, int len)
{
  for (int i = 0; i < len; i++) printf("%d", tab_color[i]);
  printf("\n");
}

void genWords(int pos, int len, color* tab_color, unsigned long* count, game g, bool onlyfirst, bool* win)
{
  if (*win && onlyfirst) {
    return;
  }
  if (pos == len) {
    if (game_won(g)) {
      (*count)++;
      *win = true;
    }
    return;
  }
  for (color color = 1; color <= 2; color++) {
    if (*win && onlyfirst) {
      break;
    }
    tab_color[pos] = color;
    genWords(pos + 1, len, tab_color, count, g, onlyfirst, win);
  }
}