#include "game.h"

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

#include "game_annexe.h"
#include "game_aux.h"
#include "game_ext.h"
#include "game_struct.h"

game game_new(constraint* constr, color* col)
{
  // Fonction qui instancie une nouvelle game avec des contraintes et des couleurs données

  // Allocation dynamique de la game
  game g = malloc(sizeof(struct game_s));
  print_error_mem(g, g);
  // Allocation dynamique du tab de contraintes
  g->constraints = malloc(sizeof(constraint) * DEFAULT_SIZE * DEFAULT_SIZE);
  print_error_mem(g->constraints, g);
  // Allocation dynamique du tab de couleur
  g->colors = malloc(sizeof(color) * DEFAULT_SIZE * DEFAULT_SIZE);
  print_error_mem(g->colors, g);

  for (int square_in_tab = 0; square_in_tab < DEFAULT_SIZE * DEFAULT_SIZE; square_in_tab++) {
    if (col == NULL) {
      g->constraints[square_in_tab] = constr[square_in_tab];
      g->colors[square_in_tab] = EMPTY;
    } else {
      g->constraints[square_in_tab] = constr[square_in_tab];
      g->colors[square_in_tab] = col[square_in_tab];
    }
  }
  g->size = DEFAULT_SIZE * DEFAULT_SIZE;
  g->rows = DEFAULT_SIZE;
  g->cols = DEFAULT_SIZE;
  g->wrapping = false;
  g->canceled_moves = queue_new();
  g->moves = queue_new();
  g->neigh = FULL;
  return g;
}

game game_new_empty(void)
{
  // Allocation d'une nouvelle partie vide avec des contraintes non définies et des cases sans couleur

  // Allocation dynamique d'une nouvelle structure de jeu
  game g = malloc(sizeof(struct game_s));
  print_error_mem(g, g);

  // Allocation dynamique du tableau de contraintes pour la taille par défaut
  g->constraints = malloc(sizeof(constraint) * DEFAULT_SIZE * DEFAULT_SIZE);
  print_error_mem(g->constraints, g);

  // Allocation dynamique du tableau de couleurs pour la taille par défaut
  g->colors = malloc(sizeof(color) * DEFAULT_SIZE * DEFAULT_SIZE);
  print_error_mem(g->colors, g);

  // Initialisation des tableaux de contraintes et de couleurs à des valeurs par défaut (UNCONSTRAINED et EMPTY)
  for (int i = 0; i < DEFAULT_SIZE * DEFAULT_SIZE; i++) {
    g->constraints[i] = UNCONSTRAINED;
    g->colors[i] = EMPTY;
  }
  g->cols = DEFAULT_SIZE;
  g->rows = DEFAULT_SIZE;
  g->size = DEFAULT_SIZE * DEFAULT_SIZE;
  g->canceled_moves = queue_new();
  g->moves = queue_new();
  g->wrapping = false;
  g->neigh = FULL;
  return g;
}

// Copier une partie et en retourne une identique
game game_copy(cgame g)
{
  // Allocation dynamique d'une structure game_s
  game gCopy = malloc(sizeof(struct game_s));
  if (gCopy == NULL) {
    print_error_mem(gCopy, gCopy);
  }
  gCopy->size = g->size;
  gCopy->rows = g->rows;
  gCopy->cols = g->cols;
  // Allocation dynamique d'un tableau de contraintes
  gCopy->constraints = malloc(sizeof(constraint) * gCopy->rows * gCopy->cols);
  if (gCopy->constraints == NULL) {
    print_error_mem(gCopy, gCopy);
  }
  // Allocation dynamique d'un tableau de couleurs
  gCopy->colors = malloc(sizeof(color) * gCopy->rows * gCopy->cols);
  if (gCopy->colors == NULL) {
    print_error_mem(gCopy, gCopy);
  }
  // On y associe les valeurs de g dans sa copie
  for (uint i = 0; i < gCopy->rows * gCopy->cols; i++) {
    gCopy->constraints[i] = g->constraints[i];
    gCopy->colors[i] = g->colors[i];
  }
  gCopy->canceled_moves = queue_new();
  gCopy->moves = queue_new();
  gCopy->wrapping = g->wrapping;
  gCopy->neigh = g->neigh;
  return gCopy;
}

bool game_equal(cgame g1, cgame g2)
{
  // Fonction qui vérifie si deux games sont égale en comparant les contraintes et les couleurs de chaque case une à une
  if (g1 == NULL || g2 == NULL) {
    fprintf(stderr, "g1 or g2 or both are NULL\n");
    return false;
  }
  if (g1->cols != g2->cols || g1->rows != g2->rows || g1->neigh != g2->neigh || g1->wrapping != g2->wrapping) {
    fprintf(stderr, "There is/are something different between g1 and g2\n");
    return false;
  }
  // Vérfication que chaque couleurs et chaque contraintes sont les mêmes une à une dans les deux games
  // sinon return false
  for (int square_in_tab = 0; square_in_tab < g1->cols * g1->rows; square_in_tab++) {
    if (g1->colors[square_in_tab] != g2->colors[square_in_tab] ||
        g1->constraints[square_in_tab] != g2->constraints[square_in_tab]) {
      return false;
    }
  }
  return true;
}

void game_delete(game g)  // Suppression d'une partie existante et libération de la mémoire associée
{
  // Vérification du paramètre
  if (g == NULL) {
    fprintf(stderr, "Bad arguments game_delete\n");
    exit(EXIT_FAILURE);
  }
  if (g->constraints != NULL) {
    free(g->constraints);
    g->constraints = NULL;
  }
  if (g->colors != NULL) {
    free(g->colors);
    g->colors = NULL;
  }
  if (g->moves != NULL) {
    queue_free_full(g->moves, &free);
  }
  if (g->canceled_moves != NULL) {
    queue_free_full(g->canceled_moves, &free);
  }
  free(g);
  g = NULL;
}

// Changer une contrainte
void game_set_constraint(game g, uint row, uint col, constraint n)
{
  // Vérification des coordonnées
  if ((row < 0) || (row >= g->rows) || (col < 0) || (col >= g->cols)) {
    fprintf(stderr, "There is something wrong with the given coordinates set cons\n");
    exit(EXIT_FAILURE);
  }
  // Vérifiaction que n est bien une valeur valide
  else if ((n < MIN_CONSTRAINT) || (n > MAX_CONSTRAINT)) {
    fprintf(stderr, "There is something wrong with the given constraint\n");
    exit(EXIT_FAILURE);
  }
  // Verification du type du neighbourhood pour comparer si la nouvelle contrainte est valide
  if (game_get_neighbourhood(g) == FULL_EXCLUDE) {
    if (n < 9) {
      uint posInTab = row * g->cols + col;
      g->constraints[posInTab] = n;
    } else {
      fprintf(stderr, "The constraint is too big for FULL_EXCLUDE\n");
      game_delete(g);
      exit(EXIT_FAILURE);
    }
  }
  if (game_get_neighbourhood(g) == ORTHO_EXCLUDE) {
    if (n < 5) {
      uint posInTab = row * g->cols + col;
      g->constraints[posInTab] = n;
    } else {
      fprintf(stderr, "The constraint is too big for ORTHO_EXCLUDE\n");
      game_delete(g);
      exit(EXIT_FAILURE);
    }
  }
  if (game_get_neighbourhood(g) == ORTHO) {
    if (n < 6) {
      uint posInTab = row * g->cols + col;
      g->constraints[posInTab] = n;
    } else {
      fprintf(stderr, "The constraint is too big for ORTHO\n");
      game_delete(g);
      exit(EXIT_FAILURE);
    }
  }
  if (game_get_neighbourhood(g) == FULL) {
    if (n <= MAX_CONSTRAINT) {
      uint posInTab = row * g->cols + col;
      g->constraints[posInTab] = n;
    } else {
      fprintf(stderr, "The constraint is too big for FULL\n");
      game_delete(g);
      exit(EXIT_FAILURE);
    }
  }
}

void game_set_color(game g, uint row, uint col, color c)
{
  // Fonction qui met à jour la couleur de la case (col,row) avec la nouvelle couleur
  // si les coordonnées et la couleur sont valide

  if (g == NULL) {
    fprintf(stderr, "The game is NULL\n");
    exit(EXIT_FAILURE);
  }
  if (g->colors == NULL) {
    fprintf(stderr, "The tab of colors is NULL\n");
    exit(EXIT_FAILURE);
  }
  // Vérification si la couleur donnée est valide et si les coordonnées sont valides aussi
  if (row < 0 || row >= g->rows || col < 0 || col >= g->cols || !(c == WHITE || c == BLACK || c == EMPTY)) {
    fprintf(stderr, "Invalid coordinates or color\n");
    exit(EXIT_FAILURE);
  }
  // Couleur de la case mise à jour
  g->colors[row * g->cols + col] = c;
}

// Récupération de la contrainte d'une case spécifique dans une partie donnée
constraint game_get_constraint(cgame g, uint row, uint col)
{
  // Vérifications des paramètres
  if (g == NULL || g->constraints == NULL || row >= g->rows || col >= g->cols) {
    fprintf(stderr, "Bad arguments get constraint\n");
    exit(EXIT_FAILURE);
  }
  return (g->constraints[row * g->cols + col]);
}

// Obtenir la couleur d'une case de coordonnées (i, j)
color game_get_color(cgame g, uint row, uint col)
{
  // Vérification de la validité des coordonnées
  if ((col < 0) || (col > g->cols - 1) || (row < 0) || (row > g->rows - 1)) {
    fprintf(stderr, "There is something wrong with the given coordinates get col\n");
    exit(EXIT_FAILURE);
  }
  uint posInTab = row * g->cols + col;
  return g->colors[posInTab];
}

bool game_get_next_square(cgame g, uint row, uint col, direction dir, uint* row_next, uint* col_next)
{
  // Fonction qui vérifie si la case ou nous voulons jouer (col,row)+direction est valide si valide les coordonnées
  // sont stockés dans col_next et row_next
  if (g == NULL) {
    fprintf(stderr, "Game is NULL\n");
    exit(EXIT_FAILURE);
  }
  if (col < 0 || col >= g->cols || row < 0 || row >= g->rows) {
    fprintf(stderr, "wrong arguments for position (row,col)\n");
    exit(EXIT_FAILURE);
  }
  // Test en fonction de la direction si le carré ou l'ont veut aller est valide ou pas,si valide les coordonnées de la
  // prochaine case sont stockés dans col_next et row_next
  return calculus_direction(g, dir, row, col, row_next, col_next, g->wrapping);
}

status game_get_status(cgame g, uint row, uint col)  // Obtention du statut actuel d'une case spécifique dans la partie
{
  // Vérification des paramètres
  if (g == NULL || col >= g->cols || row >= g->rows) {
    fprintf(stderr, "Bad arguments get status\n");
    exit(EXIT_FAILURE);

    // Calcul du status de la case demandée
  }
  if ((game_nb_neighbors(g, row, col, BLACK) == game_get_constraint(g, row, col) &&
       game_nb_neighbors(g, row, col, EMPTY) == 0) ||
      (game_get_constraint(g, row, col) == -1 && game_nb_neighbors(g, row, col, EMPTY) == 0)) {
    return SATISFIED;
  } else if ((game_nb_neighbors(g, row, col, BLACK) > game_get_constraint(g, row, col) &&
              game_get_constraint(g, row, col) != -1) ||
             (game_nb_neighbors(g, row, col, WHITE) >
                  (game_nb_neighbors(g, row, col, WHITE) + game_nb_neighbors(g, row, col, BLACK) +
                   game_nb_neighbors(g, row, col, EMPTY)) -
                      game_get_constraint(g, row, col) &&
              game_get_constraint(g, row, col) != -1)) {
    return ERROR;
  } else {
    return UNSATISFIED;
  }
}

// Compteur du nombre de carrés voisins de couleurs noir
int game_nb_neighbors(cgame g, uint rows, uint cols, color c)
{
  // On recupere les directions
  direction dir[9] = {HERE, UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT};
  // Si c n'est pas une couleur existante
  if (c != EMPTY && c != WHITE && c != BLACK) {
    fprintf(stderr, "There is something wrong with the given color\n");
    exit(EXIT_FAILURE);
  }
  // Vérification des coordonnées
  else if ((rows < 0) || (rows >= g->rows) || (cols < 0) || (cols >= g->cols)) {
    fprintf(stderr, "There is something wrong with the given coordinates nb neigh\n");
    exit(EXIT_FAILURE);
  }
  neighbourhood neighType = game_get_neighbourhood(g);
  uint compteur = 0, limit = 9;
  uint pi, pj;
  bool check = true;
  if (neighType == ORTHO || neighType == ORTHO_EXCLUDE) {
    // Limitation des directions jusqu'à RIGHT
    limit = 5;
  }
  for (uint i = 0; i < limit; i++) {
    if ((neighType == FULL_EXCLUDE || neighType == ORTHO_EXCLUDE) && check == true) {
      // Verification unique grace à check que le neighbourhood est exclude, pour ne pas compter le premier
      check = false;
    } else if (game_get_next_square(g, rows, cols, dir[i], &pi, &pj)) {
      if (game_get_color(g, pi, pj) == c) {
        compteur++;
      }
    }
  }
  return compteur;
}

void game_play_move(game g, uint row, uint col, color c)
{
  // Fonction qui joue la couleur c à la case (col,row)
  if (g == NULL || g->constraints == NULL || !(c == WHITE || c == BLACK || c == EMPTY) || col > g->cols ||
      row > g->rows || row < 0 || col < 0) {
    fprintf(stderr, "%d, %d, %d, %d, %d, %d, %d",g == NULL , !(c == WHITE || c == BLACK || c == EMPTY) , col > g->cols ,
      row > g->rows , row < 0 , col < 0, g->constraints == NULL);
    fprintf(stderr, "Game or (i,j) or color is/are not valid\n");
    exit(EXIT_FAILURE);
  }
  int* tab = malloc(sizeof(int) * 4);
  if (tab == NULL) {
    fprintf(stderr, "memory allocation problem\n");
    exit(EXIT_FAILURE);
  }
  // On empile dans la pile moves les informations du coups jouer ainsi que la couleur de la case avant de jouer
  tab[0] = row;
  tab[1] = col;
  tab[2] = c;
  tab[3] = game_get_color(g, row, col);
  queue_push_head(g->moves, tab);
  // Couleur de la case (line,row) mise a jour
  g->colors[row * g->cols + col] = c;
  queue_clear_full(g->canceled_moves, &free);
}

bool game_won(cgame g)  // Vérifie si la partie est gagnée (toutes les cases ont des contraintes satisfaites)
{
  // Vérifiaction des paramètres
  if (g == NULL) {
    fprintf(stderr, "Bad arguments game_won");
    exit(EXIT_FAILURE);
  }

  // Vérification de chaque case pour voir si toutes les contraintes sont satisfaites
  for (int i = 0; i < g->rows; i++) {
    for (int j = 0; j < g->cols; j++) {
      if (game_get_status(g, i, j) != SATISFIED) {
        return false;
      }
    }
  }
  return true;
}

// Recommencer une partie
void game_restart(game g)
{
  // Vérifiaction que la structure g est valide
  if ((g == NULL) || (g->size <= 0) || (g->constraints == NULL) || (g->colors == NULL)) {
    fprintf(stderr, "There is something wrong with the given game\n");
    exit(EXIT_FAILURE);
  }
  // On vide toutes les couleurs de la partie en cours
  for (uint i = 0; i < g->rows; i++) {
    for (uint j = 0; j < g->cols; j++) {
      game_play_move(g, i, j, EMPTY);
    }
  }
  if (g->moves != NULL) {
    queue_clear_full(g->moves, &free);
  }
  if (g->canceled_moves != NULL) {
    queue_clear_full(g->canceled_moves, &free);
  }
}