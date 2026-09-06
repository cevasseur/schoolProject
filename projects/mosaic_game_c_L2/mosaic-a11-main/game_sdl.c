// SDL2 Demo by aurelien.esnard@u-bordeaux.fr

#include "game_sdl.h"

#include <SDL.h>
#include <SDL_image.h>  // required to load transparent texture from PNG
#include <SDL_ttf.h>    // required to use TTF fonts
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

#include "game.h"
#include "game_aux.h"
#include "game_ext.h"
#include "game_tools.h"

#define FONT "../resources/MLP.ttf"
#define FONTSIZE 400
#define UNDO "../resources/undo.png"
#define REDO "../resources/redo.png"
#define SAVE "../resources/save.png"
#define RESTART "../resources/restart.png"
#define SOLVE "../resources/solve.png"
#define BACKGROUND "../resources/background.png"
#define WON "../resources/won.png"
#define NB_BUTTON 5
#define DECALAGE_HAUT 120
#define GRAY 127
#define OPACITE 255

/* **************************************************************** */

struct Env_t {
  SDL_Texture** square;  // Tableau pour les carrés
  SDL_Texture** number;  // Les contraintes
  SDL_Texture** button;  // Les boutons
  game g;
  SDL_Texture* background;
  SDL_Texture* won;
};

int sizeRect(SDL_Window* win, Env* env, int* w, int* h)
{
  /* Permet de retourner la taille d'un côté d'un carré en laissant un peu d'espace au dessus
    pour le "style", de l'espace en dessous pour le message de victoire et les boutons
    et sur les côté pour centrer. Permet aussi de récupérer la taille actuelle de la fenêtre */
  SDL_GetWindowSize(win, w, h);
  int min_size =
      (*w < *h) ? *w - DECALAGE_HAUT : *h - DECALAGE_HAUT;  // Minimum entre la largeur et la hauteur de la fenêtre
  int max_rect = (game_nb_cols(env->g) > game_nb_rows(env->g))
                     ? game_nb_cols(env->g)
                     : game_nb_rows(env->g);  // Maximum entre le nombre de lignes et de colonnes de la game chargée
  return min_size / (max_rect + 2);  // Calcule la taille du côté de un carré (en laissant de la marge sur les bords)
}

void changeRectangleColor(SDL_Window* win, SDL_Renderer* ren, Env* env, int x, int y)
{
  // Pour changer la couleur d'un des carrés lors d'un clic. Passe du EMPTY au WHITE au BLACK au EMPTY
  int w, h;
  int cols = game_nb_cols(env->g);
  if (env != NULL && env->square != NULL) {  // Validité de l'environnement et du tableau de carrés
    if (env->square[y * cols + x] != NULL)
      SDL_DestroyTexture(env->square[y * cols + x]);  // Toujours détruire la texture avant de la changer
    SDL_Surface* surf =
        SDL_CreateRGBSurface(0, sizeRect(win, env, &w, &h), sizeRect(win, env, &w, &h), 32, 0, 0, 0,
                             0);  // Création d'une surface de la taille d'un carré pour travailler dessus
    if (surf == NULL) {           // Vérification de l'existence de la surface
      fprintf(stderr, "CreateSurface: échec\n");
      return;
    }
    // Calcule la prochaine couleur à afficher en fonction de l'indice de la couleur dans son enum
    int clrNext = ((game_get_color(env->g, y, x) + 2) % 3) * GRAY;  // EMPTY(0)->127 / WHITE(1)->255 / BLACK(2)->0
    SDL_FillRect(
        surf, NULL,
        SDL_MapRGBA(surf->format, clrNext, clrNext, clrNext, OPACITE));  // Remplissage de la surface avec la couleur

    env->square[y * cols + x] =
        SDL_CreateTextureFromSurface(ren, surf);  // Convertir la surface en texture pour pouvoir l'afficher ensuite
    game_play_move(env->g, y, x, (game_get_color(env->g, y, x) + 1) % 3);  // Penser à jouer le coup !
    SDL_FreeSurface(surf);  // Toujours penser à free la surface après utilisation
  } else {
    clean(win, ren, env);
  }
}

void refresh(SDL_Window* win, SDL_Renderer* ren, int rows, int cols, Env* env, int size_rect)
{
  /* Permet l'actualisation de la couleur des carrés durant la game */
  for (uint i = 0; i < rows; i++) {
    for (uint j = 0; j < cols; j++) {
      SDL_Surface* surf = SDL_CreateRGBSurface(0, size_rect, size_rect, 32, 0, 0, 0,
                                               0);  // Crée une surface pour mettre la couleur actuelle du carré dessus
      if (surf == NULL) {                           // Si la surface est mal gérée alors on efface tout
        for (uint k = 0; k < (i * cols + j) - 1; k++) {
          if (env->square[k] != NULL) SDL_DestroyTexture(env->square[k]);
          if (env->number[k] != NULL) SDL_DestroyTexture(env->number[k]);
          if (env->button[k] != NULL) SDL_DestroyTexture(env->button[k]);
        }
        free(env->square);
        free(env->number);
        free(env->button);
        printf("Erreur de surface 1: %s\n", SDL_GetError());
        return;
      }
      // Calcul le code RGB en fonction de la couleur
      int codeCouleur = ((game_get_color(env->g, i, j) + 1) % 3) * GRAY;  // EMPTY(0)->127 / WHITE(1)->254 / BLACK(2)->0
      SDL_FillRect(surf, NULL,
                   SDL_MapRGBA(surf->format, codeCouleur, codeCouleur, codeCouleur,
                               OPACITE));                                   // Rempli le carré avec la couleur attendue
      env->square[i * cols + j] = SDL_CreateTextureFromSurface(ren, surf);  // Génère une texture à partir de la surface
      SDL_FreeSurface(surf);  // Toujours penser à free la surface après utilisation
    }
  }
}

/* **************************************************************** */

Env* init(SDL_Window* win, SDL_Renderer* ren, int argc, char* argv[])
{
  Env* env = malloc(sizeof(struct Env_t));  // Réserve de l'espace en mémoire pour l'environnement
  if (env == NULL) return NULL;
  if (argc == 2) {                // Vérifie le nombre d'arguments dans l'éxecutable
    env->g = game_load(argv[1]);  // Charge la game associé au fichier si un est passée en argument
  } else {
    env->g = game_default();  // Charge la game par défaut sinon
  }
  int rows = game_nb_rows(env->g), cols = game_nb_cols(env->g);

  env->square =
      malloc(rows * cols * sizeof(SDL_Texture*));  // Reserve de l'espace pour stocker tous les carrés de la game
  if (env->square == NULL) {
    game_delete(env->g);
    free(env);
    return NULL;
  }
  env->number =
      malloc(rows * cols * sizeof(SDL_Texture*));  // Reserve de l'espace pour stocker toutes les contraintes de la game
  if (env->number == NULL) {
    free(env->square);
    game_delete(env->g);
    free(env);
    return NULL;
  }
  env->button =
      malloc(NB_BUTTON * sizeof(SDL_Texture*));  // Reserve de l'espace pour stocker tous les noutons de la game
  if (env->button == NULL) {
    free(env->square);
    free(env->number);
    game_delete(env->g);
    free(env);
    return NULL;
  }

  int w, h;
  int size_rect = sizeRect(win, env, &w, &h);

  // Içi, on initialise chacune des images des boutons et les autres images
  // On place les images des boutons dans le tableau correspondant
  env->button[0] = IMG_LoadTexture(ren, UNDO);
  if (!env->button[0]) ERROR("IMG_LoadTexture: %s\n", UNDO);

  env->button[1] = IMG_LoadTexture(ren, REDO);
  if (!env->button[1]) ERROR("IMG_LoadTexture: %s\n", REDO);

  env->button[2] = IMG_LoadTexture(ren, SAVE);
  if (!env->button[2]) ERROR("IMG_LoadTexture: %s\n", SAVE);

  env->button[3] = IMG_LoadTexture(ren, RESTART);
  if (!env->button[3]) ERROR("IMG_LoadTexture: %s\n", RESTART);

  env->button[4] = IMG_LoadTexture(ren, SOLVE);
  if (!env->button[4]) ERROR("IMG_LoadTexture: %s\n", SOLVE);

  // On charge à part du tableau l'image de fond et de victoire
  env->background = IMG_LoadTexture(ren, BACKGROUND);
  if (!env->background) ERROR("IMG_LoadTexture: %s\n", BACKGROUND);

  env->won = IMG_LoadTexture(ren, WON);
  if (!env->won) ERROR("IMG_LoadTexture: %s\n", WON);

  // Associe les couleurs au chargement de la game
  refresh(win, ren, cols, rows, env, size_rect);
  return env;
}

/* **************************************************************** */

void render(SDL_Window* win, SDL_Renderer* ren, Env* env)
{
  // Récupère la taille de la fenêtre
  int w, h;
  SDL_RenderCopy(ren, env->background, NULL, NULL);  // Le fond s'affiche tout le temps
  int size_rect = sizeRect(win, env, &w, &h);        // Calcule de la taille du côté d'un carré
  int rows = game_nb_rows(env->g), cols = game_nb_cols(env->g);
  int centre = (w - size_rect * cols) / 2;  // Pour centrer la grille en fonction de la largeur de la game
  for (uint i = 0; i < rows; i++) {
    for (uint j = 0; j < cols; j++) {
      // Permet l'affichage de chacun des carrés de la game à chaque mise à jour de la page
      SDL_Rect rect = {centre + j * size_rect, 93 + i * size_rect, size_rect, size_rect};
      SDL_RenderCopy(ren, env->square[i * cols + j], NULL, &rect);

      // Initialise la police d'écriture
      TTF_Font* font = TTF_OpenFont(FONT, FONTSIZE);
      if (font == NULL) {
        printf("Erreur de police: %s\n", TTF_GetError());
        clean(win, ren, env);
      }
      // Utilise le statut d'une contrainte pour adapter la couleur du texte
      status state = game_get_status(env->g, i, j);
      // ERROR(0)-> R:236, G:4, B:52
      // UNSATISFIED(1)-> R:0, G:128, B:252
      // SATISFIED(2)-> R:96, G:196, B:72
      SDL_Color couleur = {4 * ((1 - state) * 59 + (state == 2) * 83), 4 * (4 + (state * 28) - (state == 2) * 7),
                           4 * (4 + (state * 9) + (state == 1) * 50)};
      char valeur[3];                                            // On garde de l'espace pour un caractère à afficher
      sprintf(valeur, "%d", game_get_constraint(env->g, i, j));  // On convertit la contrainte(int) en caractère
      char* printConstraint = (game_get_constraint(env->g, i, j) != -1) ? valeur : " ";
      SDL_Surface* surface = TTF_RenderText_Solid(font, printConstraint, couleur);  // Crée une surface de travail
      if (surface == NULL) {
        printf("Erreur de surface 2: %s\n", TTF_GetError());
        TTF_CloseFont(font);
        for (uint k = 0; k < i * cols + j; k++) {
          if (env->square[k] != NULL) SDL_DestroyTexture(env->square[k]);
          if (env->number[k] != NULL) SDL_DestroyTexture(env->number[k]);
          if (env->button[k] != NULL) SDL_DestroyTexture(env->button[k]);
        }
        if (env->background != NULL) SDL_DestroyTexture(env->background);
        if (env->won != NULL) SDL_DestroyTexture(env->won);
        free(env->square);
        free(env->number);
        free(env->button);
        game_delete(env->g);
        free(env);
        return;
      }
      SDL_Texture* text = SDL_CreateTextureFromSurface(ren, surface);
      if (text == NULL) {
        printf("Erreur de texture: %s\n", SDL_GetError());
        SDL_FreeSurface(surface);  // Toujours penser à free la surface après utilisation
        TTF_CloseFont(font);       // Ainsi que les polices d'ecriture
        clean(win, ren, env);
      }
      env->number[i * cols + j] = text;
      SDL_FreeSurface(surface);  // Toujours penser à free la surface après utilisation
      TTF_CloseFont(font);       // Ainsi que les polices d'ecriture

      // Permet l'affichage des contraintes dans les carrés associés
      SDL_Rect position = {centre + j * size_rect, 93 + i * size_rect, size_rect, size_rect};
      SDL_RenderCopy(ren, env->number[i * cols + j], NULL, &position);
    }
  }
  // Trace les lignes noires verticales
  SDL_SetRenderDrawColor(ren, 0, 0, 0, SDL_ALPHA_OPAQUE);
  for (uint i = 0; i < cols + 1; i++) {
    SDL_RenderDrawLine(ren, centre + i * size_rect, 93, centre + i * size_rect, size_rect * rows + 93);
  }
  // Trace les lignes noires horizontales
  for (uint i = 0; i < rows + 1; i++) {
    SDL_RenderDrawLine(ren, centre, 93 + i * size_rect, size_rect * cols + centre, i * size_rect + 93);
  }
  // Gère l'afficage des boutons en bas de la page en fonction de la largeur de la fenêtre
  SDL_Rect rectTexture;
  for (uint but = 0; but < NB_BUTTON; but++) {
    SDL_QueryTexture(env->button[but], NULL, NULL, &rectTexture.w, &rectTexture.h);
    rectTexture.x = w * ((but % 4 == 0) * 0.025 + but * 0.2);
    rectTexture.y = size_rect * (rows + 1) + 103;
    rectTexture.w = size_rect + 30;
    rectTexture.h = size_rect + 30;
    SDL_RenderCopy(ren, env->button[but], NULL, &rectTexture);
  }

  // Affiche le message de victoire lorsque la partie est gagnée
  if (game_won(env->g)) {
    SDL_QueryTexture(env->won, NULL, NULL, &rectTexture.w, &rectTexture.h);
    rectTexture.x = w / 2 - (size_rect * 3.5);
    rectTexture.y = h * 0.6;
    rectTexture.w = size_rect * 7;
    rectTexture.h = size_rect * 3;
    SDL_RenderCopy(ren, env->won, NULL, &rectTexture);
  }
}

/* **************************************************************** */

bool process(SDL_Window* win, SDL_Renderer* ren, Env* env, SDL_Event* e)
{
  if (e->type == SDL_QUIT) {
    return true;
  }
  int w, h;
  int rows = game_nb_rows(env->g), cols = game_nb_cols(env->g);
  uint rectSize = sizeRect(win, env, &w, &h);
  int centre = (w - rectSize * cols) / 2;
  if (e->type == SDL_MOUSEBUTTONDOWN) {
    if (e->button.button == SDL_BUTTON_LEFT) {
      int rect_x = (e->button.x - centre) / (rectSize),
          rect_y = (e->button.y - 93) / (rectSize);  // Calcul la position dans le tableau de texture en fonction du
                                                     // clic x et y avec la division entiere par la taille d'un carré
      if (rect_x >= 0 && rect_x < cols && rect_y >= 0 && rect_y < rows && !game_won(env->g)) {
        changeRectangleColor(win, ren, env, rect_x, rect_y);
      } else if (e->button.x > w * 0.025 && e->button.x < w * 0.025 + rectSize + 30 &&
                 e->button.y > rectSize * (rows + 1) + 130 && e->button.y < rectSize * (rows + 1) + 103 + rectSize &&
                 !game_won(env->g)) {
        game_undo(env->g);
        refresh(win, ren, cols, rows, env, rectSize);

      } else if (e->button.x > w * 0.2 && e->button.x < w * 0.2 + rectSize + 30 &&
                 e->button.y > rectSize * (rows + 1) + 130 && e->button.y < rectSize * (rows + 1) + 103 + rectSize &&
                 !game_won(env->g)) {
        game_redo(env->g);
        refresh(win, ren, cols, rows, env, rectSize);

      } else if (e->button.x > w * 0.4 && e->button.x < w * 0.4 + rectSize + 30 &&
                 e->button.y > rectSize * (rows + 1) + 130 && e->button.y < rectSize * (rows + 1) + 103 + rectSize) {
        game_save(env->g, "game.txt");
      } else if (e->button.x > w * 0.6 && e->button.x < w * 0.6 + rectSize + 30 &&
                 e->button.y > rectSize * (rows + 1) + 130 && e->button.y < rectSize * (rows + 1) + 103 + rectSize) {
        game_restart(env->g);
        refresh(win, ren, cols, rows, env, rectSize);
      } else if (e->button.x > w * 0.825 && e->button.x < w * 0.825 + rectSize + 30 &&
                 e->button.y > rectSize * (rows + 1) + 130 && e->button.y < rectSize * (rows + 1) + 103 + rectSize) {
        game_solve(env->g);
        refresh(win, ren, cols, rows, env, rectSize);
      }
    }
  }
  if (e->type == SDL_KEYDOWN) {  // Raccourcis clavier
    switch (e->key.keysym.sym) {
      case SDLK_w:
        game_solve(env->g);
        refresh(win, ren, cols, rows, env, rectSize);
        break;
      case SDLK_r:
        game_restart(env->g);
        refresh(win, ren, cols, rows, env, rectSize);
        break;
      case SDLK_s:
        game_save(env->g, "game.txt");
        break;
      case SDLK_z:
        game_undo(env->g);
        refresh(win, ren, cols, rows, env, rectSize);
        break;
      case SDLK_y:
        game_redo(env->g);
        refresh(win, ren, cols, rows, env, rectSize);
        break;
      default:
        break;
    }
  }
  return false;
}

/* **************************************************************** */

void clean(SDL_Window* win, SDL_Renderer* ren, Env* env)
{
  int rows = game_nb_rows(env->g), cols = game_nb_cols(env->g);
  if (env != NULL) {
    if (env->button != NULL) {
      for (uint i = 0; i < NB_BUTTON; i++) {
        if (env->button[i] != NULL) SDL_DestroyTexture(env->button[i]);
      }
      free(env->button);
    }
    if (env->won != NULL) SDL_DestroyTexture(env->won);
    if (env->background != NULL) SDL_DestroyTexture(env->background);
    if (env->square != NULL || env->number != NULL) {
      for (uint i = 0; i < rows * cols; i++) {
        if (env->square[i] != NULL) SDL_DestroyTexture(env->square[i]);
      }
      free(env->square);
    }
    if (env->number != NULL) {
      for (uint i = 0; i < NB_BUTTON; i++) {
        if (env->number[i] != NULL) SDL_DestroyTexture(env->number[i]);
      }
      free(env->number);
    }
    if (env->g != NULL) game_delete(env->g);
    free(env);
  }
  if (ren != NULL) SDL_DestroyRenderer(ren);
  if (win != NULL) SDL_DestroyWindow(win);
  SDL_Quit();
}

/* **************************************************************** */
