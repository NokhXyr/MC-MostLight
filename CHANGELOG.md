# Changelog

Toutes les versions de MostLight (NeoForge 1.21.1). La plus récente en haut.

## 1.2.0 - 2026-10-04

### Nouveautés
- **Établi de luminaire** : toutes les lampes, interrupteurs et outils se fabriquent ici, en catalogue
  (onglets par catégorie, recherche, aperçu, matériaux avec quantités, choix parmi les 16 couleurs).
  Maj + clic fabrique autant que possible. Plus de recettes à la table de craft.
- **Bandes LED** : jusqu'à 6 bandes par bloc (une par face), en position basse, milieu ou haute, à
  l'horizontale ou à la verticale. Elles se raccordent à leurs voisines comme la redstone : angles
  intérieurs et extérieurs, L, T et croix.
- **Connecteur LED** : relie les bandes voisines et les blocs lumineux en une chaîne qui suit le signal
  redstone d'un seul de ses blocs. La chaîne visée est entourée d'un contour vert visible à travers les murs,
  avec sa taille et son état sous le viseur.
- **Guirlandes** : cordon qui pend avec des ampoules rondes, raccordé comme les bandes LED.
- **Nouvelles lampes** : lampe à engrenages animée (engrenages qui tournent quand elle est allumée),
  tiges lumineuses façon barre de l'End (debout, pendues, couchées au mur ou au sol), ventilateur de
  plafond (1er clic : lumière, 2e clic : ventilateur). 72 modèles en tout.
- **Interrupteurs** simple, variateur et double en 16 couleurs. Clé de décorateur : Maj + molette.
- **Pistons** : les lampes se poussent et se tirent en gardant leur finition, leur teinte, leurs bandes et
  leurs liaisons. Une lampe qui perd son support tombe en objet. Les lampes de 2 blocs de haut se cassent.
- **Create** (facultatif) : les contraptions emportent les lampes attachées au bloc qu'elles déplacent.
- **Lumière colorée sous shaders** (Iris) : chaque lampe allumée éclaire de sa couleur avec les packs qui
  colorent la lumière (Solas, par exemple).
- **Pose sur tout bloc ayant une forme** : escalier à l'envers, barrière, dalle, tête de joueur...
- **JEI** : recettes de l'établi avec les matériaux et leurs quantités.
- **Configuration** (`config/mostlight-common.toml`) : longueur maximale des chaînes LED, limites des
  interrupteurs.
- Logo dans la liste des mods.

### Améliorations
- Modèles retouchés dans Blockbench : abat-jour coniques aux arêtes fines, lustre en cristal, lampe de sel,
  brasero, trépied, lanterne murale, applique murale, éclairage de tableau...
- Translucidité du papier, de la lune, des vitraux, de la lampe à lave et de la lampe de sel claire.
- Onglets créatifs rangés : Outils et interrupteurs, une catégorie de lampes par onglet, puis Finitions et
  teintes. Chaque onglet a sa barre de recherche.
- Petites lampes (murales, suspendues, LED) sans collision : on passe à travers, les projectiles non.
- Hitbox fidèles aux modèles.

### Performances
- Démarrage du jeu 5 fois plus rapide (hitbox précalculées).
- 3 fois moins d'états de bloc (91 040 au lieu de 291 328) : l'allumage et la luminosité tiennent dans une seule
  propriété, et le signal redstone mémorisé passe dans la block entity. Moins de mémoire, démarrage plus court,
  tables des shaders et des mods d'optimisation plus petites. Un monde enregistré avant ce changement rallume ses
  lampes au maximum au premier chargement.
- Allumer, éteindre ou régler une lampe ne réveille plus les blocs voisins (inutile : une lampe n'émet pas de
  redstone). Les observateurs voient toujours le changement. Basculer 10 000 lampes d'un coup est 2,5 fois plus
  rapide, au niveau des ampoules en cuivre vanilla.
- Lampes commandées par la redstone : plus aucun paquet réseau ni recalcul des teintes chez les joueurs quand le
  signal change (le connecteur calcule lui-même si une chaîne est alimentée). Avec 10 000 lampes et des horloges
  redstone : 453 FPS au lieu de 82, et 14,5 ms par tick serveur au lieu de 26,3.
- Moins de données réseau : la finition et la teinte ne sont plus renvoyées à chaque allumage.
- Sauvegarde plus légère : une lampe à la finition et à la teinte de son modèle n'écrit plus ces valeurs.
- Rendu : pièces animées par GeckoLib seulement de près, faces cachées supprimées des modèles (-14 %).
- Tests de charge : 20 joueurs simulés et 10 000 à 37 000 lampes, 20 TPS tenus sans modpack. Dans le modpack
  Arcadia (530 mods), le code de MostLight coûte environ 2 % du temps du serveur avec 20 joueurs actifs.

### Corrections
- **Sodium / Iris** : moitiés hautes des lampes de 2 blocs avec une mauvaise texture, ou invisibles selon
  les mods et les shaders.
- **Sodium** : cadre opaque qui disparaissait derrière le verre.
- **ModernFix** (ressources dynamiques) : bandes LED et lampes animées mal affichées.
- **Solas** : lampes qui brillaient sur toute leur surface (abat-jour, cadre, pied).
- Blocs lumineux (lampe encadrée, shoji, néon...) : on voyait à travers le décor derrière eux.
- Interrupteur double : bascules lumière et ventilateur inversées.
- Brasero : traits au-dessus du feu.
- Bras de l'éclairage de tableau décalé.

### Divers
- Licence : tous droits réservés. Les modpacks peuvent inclure le JAR officiel non modifié, avec crédit.
- Code de conduite, politique de sécurité et guide de contribution (dossier `.github`).
- Fiche du mod en anglais, avec le lien vers le code source sur GitHub.
- La commande `/mostlight showcase` (galerie de test) n'existe plus dans le mod publié.

## 1.1.0 - 2026-10-02

- 66 modèles de lampes (au lieu de 32), chacun en 16 couleurs.
- 10 finitions de cadre et 5 teintes de lumière.
- Clé de décorateur pour changer la finition et la teinte d'une lampe posée.
- Interrupteur, variateur et télécommande à lier aux lampes.
- Textures détaillées en 64 x 64, verre translucide avec ampoule visible, abat-jour lisses.
- Flammes et fumée sur les bougies, flambeaux et braseros.

## 1.0.0 - 2026-10-02

- Première version : 32 lampes en 16 couleurs (plafond, mur, table, sol sur 2 blocs, blocs lumineux).
- Allumage au clic droit ou à la redstone, 4 niveaux de luminosité.
- Recoloration au colorant, lampes immergeables (waterlog).
