#include "game_ext.h"

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

#include "game.h"
#include "game_annexe.h"
#include "game_aux.h"
#include "game_struct.h"

game game_new_ext(uint nb_rows, uint nb_cols, constraint* constraints, color* colors, bool wrapping,
                  neighbourhood neigh)
{
  // Fonction qui crée une game_ext
  if ((nb_rows < 1 || nb_cols < 1) && (wrapping == false || wrapping == true) &&
      (neigh == FULL || neigh == ORTHO || neigh == FULL_EXCLUDE || neigh == ORTHO_EXCLUDE)) {
    // vérfication si les paramètres sont valides
    fprintf(stderr,
            "There is something wrong with your size. Rows: %u, cols: %u\nOr maybe with your wrapping: %d, or "
            "neighbourhood style: %d\n",
            nb_rows, nb_cols, wrapping, neigh);
    exit(EXIT_SUCCESS);
  }
  if (constraints == NULL) {
    fprintf(stderr, "No constraints has been given\n");
    exit(EXIT_SUCCESS);
  }
  game g = malloc(sizeof(struct game_s));
  print_error_mem(g, g);
  g->constraints = malloc(sizeof(constraint) * nb_rows * nb_cols);
  print_error_mem(g->constraints, g);
  g->colors = malloc(sizeof(color) * nb_rows * nb_cols);
  print_error_mem(g->colors, g);
  // Remplissage des tableaux colors et constraints
  for (uint i = 0; i < nb_rows * nb_cols; i++) {
    if (colors == NULL) {
      g->colors[i] = EMPTY;
    } else {
      g->colors[i] = colors[i];
    }
    g->constraints[i] = constraints[i];
  }
  g->rows = nb_rows;
  g->cols = nb_cols;
  g->wrapping = wrapping;
  g->neigh = neigh;
  g->canceled_moves = queue_new();
  g->moves = queue_new();
  return g;
}

game game_new_empty_ext(uint nb_rows, uint nb_cols, bool wrapping, neighbourhood neigh)
{
  // Fonction qui qui crée une game sans contrainte et sans couleur
  game g = malloc(sizeof(struct game_s));
  print_error_mem(g, g);
  g->constraints = malloc(sizeof(constraint) * nb_rows * nb_cols);
  print_error_mem(g->constraints, g);
  g->colors = malloc(sizeof(color) * nb_rows * nb_cols);
  print_error_mem(g->colors, g);
  // Remplissage des tableaux colors et constraints
  for (uint i = 0; i < nb_rows * nb_cols; i++) {
    g->constraints[i] = UNCONSTRAINED;
    g->colors[i] = EMPTY;
  }
  g->size=nb_cols*nb_rows;
  g->rows = nb_rows;
  g->cols = nb_cols;
  g->wrapping = wrapping;
  g->neigh = neigh;
  g->canceled_moves = queue_new();
  g->moves = queue_new();
  return g;
}

// Nombre de rangées
uint game_nb_rows(cgame g)
{
  // Fonction qui retourne le nombre de lignes de la game
  if (g == NULL) {
    fprintf(stderr, "There is something wrong with the cgame g given\n");
    exit(EXIT_SUCCESS);
  }
  return g->rows;
}

// Nombre de colonnes
uint game_nb_cols(cgame g)
{
  // Fonction qui retourne le nombre de colonnes de la game
  if (g == NULL) {
    fprintf(stderr, "There is something wrong with the cgame g given\n");
    exit(EXIT_SUCCESS);
  }
  return g->cols;
}

bool game_is_wrapping(cgame g)
{
  // Fonction qui retourne le champ wrapping de la game
  if (g == NULL) {
    fprintf(stderr, "There is something wrong with the cgame g given\n");
    exit(EXIT_SUCCESS);
  }
  return g->wrapping;
}

// Obtenir le type de voisinnage
neighbourhood game_get_neighbourhood(cgame g)
{
  // Fonction qui retourne le type de voisinage de la game
  if (g == NULL) {
    fprintf(stderr, "There is something wrong with the cgame g given\n");
    exit(EXIT_SUCCESS);
  }
  return g->neigh;
}

// Revenir en arrière
void game_undo(game g)
{
  // Vérifier si le pointeur vers la structure de jeu est null
  if (g == NULL) {
    fprintf(stderr, "Error with game\n");
    exit(EXIT_SUCCESS);
  } else if (!queue_is_empty(g->moves)) {
    // Récupérer les données du dernier mouvement effectué
    int* previous = queue_peek_head(g->moves);
    // Ajouter le mouvement annulé à la pile des mouvements annulés
    queue_push_head(g->canceled_moves, queue_pop_head(g->moves));
    // Appliquer la couleur précédente au jeu
    game_set_color(g, previous[0], previous[1], previous[3]);
  }
}

// Revenir en avant
void game_redo(game g)
{
  // Vérifier si le pointeur vers la structure de jeu est null
  if (g == NULL) {
    fprintf(stderr, "Error with game\n");
    exit(EXIT_SUCCESS);
  }
  if (!queue_is_empty(g->canceled_moves)) {
    // Récupérer les données du dernier mouvement annulé
    int* data = queue_pop_head(g->canceled_moves);
    // Jouer le coup répcupéré
    game_set_color(g, data[0], data[1], data[2]);
    // Ajouter le coup à la pile des coups joués
    queue_push_head(g->moves, data);
  }
}