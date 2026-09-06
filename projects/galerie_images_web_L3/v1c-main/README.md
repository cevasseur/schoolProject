# Groupe V1C : #

## Readme ##
1. [English](#english-version)
    - [Generate the project](#project-generation)
    - [About](#about-the-project)
2. [Français](#version-francaise)
    - [Générer le projet](#generation-du-projet)
    - [A propos](#a-propos)
3. [Authors](#authors)
***
***
#### English version ####
## Project generation ##
- **First of all :** You need to check that you have the required packages. *If you have any doubts* enter the commands :
    - `cd frontend` then `npm install`
- **How to :** Build the entire project from the root (V1C) with the command :
    - `mvn clean install`
    - If you don't want to run the tests during the process, type : `mvn clean install -DskipTests`
- **Launch the project :** From the root, run the server :
    - `mvn --projects backend spring-boot:run`
- **View the project :** To open the project webpage, go to the following URL : 
    - [localhost:8181](https://localhost:8181)

***
#### About the project ####
The project has benn tested on Crémi machines (Debian) and Ubuntu 22.04.
The client side runs on Google Chrome (Version 134.0.6998.89 (Build officiel) (64 bits))

***
***
#### Version francaise ####
## Génération du projet ##
- **Avant tout :** Il est nécessaire de vérifier que vous avez bien les packages requis. *Si vous avez un doute* entrez les commandes :
    - `cd frontend` puis `npm install`
- **Comment faire :** Générez l'intégralité du projet depuis la racine (V1C) avec la commande :
    - (Optionnel) Si vous n'êtes pas sur les pc du Crémi, veuillez changer le fichier suivant : backend/src/main/resources/application.properties en remplaçant jdbc:postgresql://pgsql:5432/mguillorit
    par 
    jdbc:postgresql://127.0.0.1:5432/mguillorit
    et lancer la commande suivante depuis un terminal :
    ssh idnum@ssh.emi.u-bordeaux.fr -L 5432:pgsql:5432 (remplacez idnum par votre identifiant)
    - `mvn clean install`
    - Si vous ne souhaitez pas lancer les tests en même temps, tapez alors : `mvn clean install -DskipTests`
- **Lancer le projet :** Depuis la racine, lancez le serveur :
    - `mvn --projects backend spring-boot:run`
- **Voir le projet :** Pour ouvrir la page web du projet, rendez-vous sur l'url suivante : 
    - [localhost:8181](https://localhost:8181)
***

#### A propos ####
Le projet a été testé sur les machines du Crémi (Debian) et sur Ubuntu 22.04.
Le côté client fonctionne sur Google Chrome (Version 134.0.6998.89 (Build officiel) (64 bits))
***

### Authors ###
- Joris Douillet
- Mathys Guillorit
- Cengiz Vasseur