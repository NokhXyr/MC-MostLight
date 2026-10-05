# MostLight

Mod NeoForge pour **Minecraft 1.21.1** (toutes versions NeoForge 21.1.x) qui ajoute **71 modèles de lampes** :
plafond, mur, table, sol et blocs lumineux.

Chaque lampe se personnalise sur trois axes :

| Axe | Choix | Comment |
|---|---|---|
| Couleur | 16 couleurs de Minecraft | couleur choisie à l'établi de luminaire, ou colorant sur la lampe posée |
| Finition du cadre | acier, fer noir, laiton, cuivre, or, or rose, vert-de-gris, émail blanc, chêne, chêne noir | **clé de décorateur** : Maj + molette pour choisir la finition, clic droit pour l'appliquer |
| Teinte de lumière | auto, colorée, chaude, neutre, froide | accroupi + clic droit avec la clé |

Cela donne 800 combinaisons par modèle, et plus de 50 000 lampes différentes au total.

Les textures font 64×64 :

- métal brossé avec rayures et arêtes usées ;
- lin tissé, tissu plissé, rotin tressé ;
- bois à nœuds, marbre veiné, pierre taillée ;
- cire avec coulures ;
- vitrail Tiffany serti de plomb.

Le verre est translucide : on voit l'ampoule à filament ou la flamme à l'intérieur. Le papier, la lune, les vitraux (Tiffany, marocaine), la lampe à lave et la lampe de sel claire laissent aussi passer un peu la lumière.

## Contrôles

- **Établi de luminaire** : un catalogue, comme l'établi de Refurbished ou de Decocraft. Tous les objets du mod y sont rangés par onglets (plafond, mur, table, sol, blocs, interrupteurs, outils), avec une recherche. On choisit l'objet et sa couleur (palette de 16 couleurs : un colorant en plus, sauf pour le blanc). Les matériaux nécessaires s'affichent en vert s'ils sont dans l'inventaire, en rouge s'il en manque. « Fabriquer » les prend dans l'inventaire ; Maj + clic en fabrique autant que possible, une pile au maximum. Aucune recette du mod ne se fait à l'établi vanilla ; l'établi de luminaire, lui, s'y fabrique (poudre de glowstone, 2 lingots de fer, un établi et 2 planches).

- **Main vide, clic droit** : allumer / éteindre. **Accroupi + clic droit** : luminosité (15, 12, 9 ou 6).
- **Redstone** : un signal allume la lampe, la fin du signal l'éteint.
- **Interrupteurs** : simple, variateur et double, chacun en 16 couleurs (choisies à l'établi de luminaire). Avec l'interrupteur en main, avec l'interrupteur en main, accroupi + clic droit sur des lampes pour les lier (64 maximum par défaut, réglable), puis pose-le.
  - L'interrupteur allume ou éteint tout le groupe d'un clic.
  - Le variateur change la luminosité du groupe ; accroupi + clic l'allume ou l'éteint.
  - L'interrupteur double : vu de face, bascule gauche = lumière, bascule droite = ventilateurs des ventilateurs lumineux liés.
  - Casser un interrupteur garde ses liaisons dans l'objet.
- **Télécommande** : se lie aux lampes de la même façon. Ensuite, clic droit dans le vide pour allumer / éteindre, accroupi pour la luminosité.
- **Ventilateur lumineux** : 1er clic = lumière, 2e clic = le ventilateur tourne, 3e clic = tout éteint.
- Bougies, flambeaux et braseros font des flammes et de la fumée quand ils sont allumés.
- Les lampes acceptent l'eau. Elles se posent sur n'importe quel bloc qui a une forme : escalier à l'envers, dalle, barrière, tête de joueur, vitre… Elles tombent si on retire leur support.
- **Pistons** : les pistons poussent et tirent les lampes, qui gardent leur finition, leur teinte, leurs bandes et leurs liaisons. Une lampe qui arrive sans support tombe en objet. Les lampes de 2 blocs de haut se cassent quand on les pousse (un piston n'en déplacerait qu'une moitié). Avec Create, les contraptions emportent les lampes attachées au bloc qu’elles déplacent.
- **Bandes LED** : elles se posent en position basse, milieu ou haute de la face, selon l'endroit visé. Accroupi, on les pose à la verticale ; au sol, elles suivent le regard.
  - **Raccords** : un même bloc porte jusqu'à 6 bandes, une par face. Les bandes voisines se raccordent toutes seules, comme de la redstone : coin d'une pièce (une bande s'arrête contre l'autre), tour d'un pilier (angle extérieur), en L, en T ou en croix sur un même mur.
  - **Chaînes redstone** : avec le **connecteur LED**, clic droit près du bord d'une bande pour la relier à sa voisine de ce côté (accroupi : à travers la face cliquée). Dès qu'un bloc de la chaîne reçoit un signal redstone, toute la chaîne s'allume. Les **blocs lumineux** pleins (bloc lumineux, lampe encadrée, alvéolée, cadre néon, bloc shoji) se relient de la même façon, entre eux et avec les bandes.
  - Connecteur en main, en visant une bande ou un bloc lumineux : toute sa chaîne est entourée d'un contour vert, visible en transparence derrière les murs, et les voisins qu'on peut encore relier ont un contour rouge pâle. Sous le viseur s'affichent la taille de la chaîne et si elle est alimentée.
  - Longueur maximale d'une chaîne : 256 bandes par défaut, réglable.
- **Guirlandes** : même système que les bandes LED (plusieurs par bloc, raccords, positions, chaînes redstone). Au mur, le cordon pend entre ses attaches avec des ampoules rondes.
- **Tiges lumineuses** (tige, tiges jumelles, colonne lumineuse) : au mur, couchées à l'horizontale (accroupi : à la verticale) ; au sol debout, au plafond pendues (accroupi : couchées dans l'axe du regard).

## Les lampes

| Catégorie | Modèles |
|---|---|
| Plafond | suspension, lustre, lustre en cristal, plafonnier, lanterne en papier, cage industrielle, suspension globe, lanterne suspendue, grappe de suspensions, suspension dôme, suspension anneau, lanterne marocaine, rail de spots, ventilateur lumineux, ampoule Edison |
| Mur | applique, applique double, lanterne murale, tube néon, anneau néon, spot mural, hublot, flambeau, bougeoir mural, éclairage de tableau, applique industrielle, globe mural, applique lumineuse, guirlande |
| Table | lampe à engrenages (engrenages animés quand elle est allumée, plus vite à pleine puissance), lampe de table, lampe de bureau, lampe à lave, lampe champignon, lampe de banquier, lanterne, chandelier, bougie en pot, lampe boule, lampe lune, lampe Tiffany, lampe de sel, lampe de sel claire, lampe à pétrole, lampe shoji |
| Sol | lampadaire, lampadaire arc, réverbère, réverbère double, trépied, torchère, colonne en papier, projecteur de studio et lanterne de pierre (2 blocs de haut) ; borne lumineuse, lanterne de jardin, balise, brasero |
| Blocs | bloc lumineux, lampe encadrée, lampe alvéolée, bloc cadre néon, bloc shoji ; panneau LED, bande LED, projecteur, spot encastré, cristal lumineux, tige lumineuse, tiges jumelles et pilier lumineux (ces 8 derniers, style barre de l'End, se fixent sur n'importe quelle face) |

## Galerie

- En jeu : `/mostlight showcase` (opérateur) construit au sud de toi une galerie de toutes les lampes, de leurs finitions et teintes, avec un interrupteur et un variateur déjà liés.
- En dev : `./gradlew runShowcase` crée un monde plat, construit la galerie et vérifie que chaque lampe tient et éclaire. Il prend des captures de jour et de nuit (`run/screenshots/`), musique coupée, puis se ferme. Le monde reste dans `run/saves/mostlight_showcase`.

## Shaders

Testé avec Sodium 0.8.13, Iris 1.8.14-beta.1 et Complementary Reimagined r5.9.3. Les 1 177 lampes de la galerie passent la vérification, et rendent correctement de jour comme de nuit :

- parties lumineuses ;
- verre translucide ;
- couleurs, finitions et teintes.

Pour refaire le test :

1. Dépose les jars de Sodium et d'Iris (NeoForge 1.21.1) dans `run-shaders/mods/`.
2. Dépose un pack de shaders dans `run-shaders/shaderpacks/`.
3. Lance `./gradlew runShowcaseShaders`.

Les captures arrivent dans `run-shaders/screenshots/`.

Voir aussi [COMPATIBILITE.md](COMPATIBILITE.md) : mods d'optimisation et lumière colorée sous shaders.

## Configuration

Le fichier `config/mostlight-common.toml` est créé au premier lancement :

| Réglage | Par défaut | Rôle |
|---|---|---|
| `leds.maxChainLength` | 256 | Nombre maximal de bandes LED dans une chaîne redstone (1 à 4096) |
| `switches.maxLinks` | 64 | Nombre maximal de lampes liées à un interrupteur ou une télécommande (1 à 1024) |
| `switches.range` | 64 | Distance maximale en blocs entre un interrupteur et ses lampes (8 à 1024) |

## Commandes de diagnostic

Réservées aux opérateurs (niveau 2). Elles ne lisent que les chunks déjà chargés et ne modifient que les blocs MostLight.

| Commande | Rôle |
|---|---|
| `/mostlight inspect [position]` | Tout ce que le mod sait de la lampe ou de l'interrupteur visé : état, lumière émise et mesurée, finition, teinte, mémoire redstone, bandes LED, chaîne, liaisons, problèmes détectés |
| `/mostlight check [rayon]` | Liste les problèmes dans les chunks autour (rayon en chunks, 4 par défaut, 32 au plus), avec des coordonnées cliquables pour s'y téléporter |
| `/mostlight repair [rayon]` | Corrige ces problèmes et l'écrit dans les logs du serveur |

Problèmes détectés : block entity manquante, de mauvais type ou restée sans son bloc, lampe de 2 blocs à qui il manque une
moitié (remise si la place est libre, sinon la lampe tombe en objet), lumière plus faible que celle de la lampe (recalculée),
liaison LED vers un bloc qui ne la rend pas, interrupteur lié à une lampe disparue.

## Performances

Test de charge avec 20 joueurs simultanés et près de 37 000 lampes : 20 TPS tenus, coût proche de blocs lumineux
vanilla, aucune erreur, lumière exacte (`./gradlew runStressServer` et `./gradlew runStressClient` pour le relancer).

## Dépendances

- NeoForge 21.1.x (Minecraft 1.21.1).
- [GeckoLib](https://modrinth.com/mod/geckolib) 4.9 ou plus, obligatoire : il anime les pièces mobiles (engrenages, lampe à lave, pales du ventilateur), seulement de près : de loin elles restent dans le modèle précuit. Les autres lampes sont des modèles précuits, sans GeckoLib, pour rester rapides.

- [JEI](https://modrinth.com/mod/jei), facultatif : recettes, variantes de finition et fiches d'explication de chaque lampe, outil et interrupteur.
- [Create](https://modrinth.com/mod/create), facultatif : les contraptions emportent les lampes avec le bloc qui les porte (sans colle), et le canon à schémas garde leur finition et leur teinte.

## Compiler

```
./gradlew build
```

Le jar se trouve dans `build/libs/`.

- `./gradlew runClient` : lance le jeu
- `./gradlew runGameTestServer` : lance les tests automatiques, dont les tests de triche (`ExploitTests` : duplication,
  perte d'objets, paquets forgés par un client modifié)
- `./gradlew runGameTestServerCreate` : tests des contraptions de Create (`CreateExploitTests`). Create n'est pas une
  dépendance : déposer son JAR dans `run-gametest-create/mods` avant

## Modèles et ressources

Les modèles, blockstates, textures, traductions, recettes, hitbox et la liste des lampes (`LampType.java`) sont
produits par des outils internes à partir de fichiers Blockbench, qui ne sont pas publiés dans ce dépôt. Pour proposer
un changement de modèle ou de texture, ouvrez une issue.

Dans les modèles, les textures sont en niveaux de gris et la couleur vient des teintes :

- `tintindex` 0 : couleur ;
- `tintindex` 1 : diffuseur ;
- `tintindex` 2 : ampoule ;
- `tintindex` 3 : finition ;
- `tintindex` 4 à 8 : pièces animées (mêmes teintes), retirées du modèle précuit quand GeckoLib les anime.

Les bandes et guirlandes sont des morceaux de ligne (`<bande>_seg/`) dessinés dans un repère local ; le jeu les tourne
vers la face voulue et choisit les morceaux d'après les bandes voisines.

## Licence

MostLight est **tous droits réservés** : le code est visible mais pas libre. Les modpacks peuvent inclure le JAR
officiel non modifié, avec crédit. Détails dans [LICENSE](LICENSE).

- [Contribuer](.github/CONTRIBUTING.md)
- [Code de conduite](.github/CODE_OF_CONDUCT.md)
- [Signaler une faille de sécurité](.github/SECURITY.md)
- [Changelog](CHANGELOG.md)
