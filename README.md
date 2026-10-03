# MostLight

Mod NeoForge pour **Minecraft 1.21.1** (toutes versions NeoForge 21.1.x) qui ajoute **66 modèles de lampes** :
plafond, mur, table, sol et blocs lumineux.

Chaque lampe se personnalise sur trois axes :

| Axe | Choix | Comment |
|---|---|---|
| Couleur | 16 couleurs de Minecraft | colorant sur la lampe posée, ou lampe + colorant à l'établi |
| Finition du cadre | acier, fer noir, laiton, cuivre, or, or rose, vert-de-gris, émail blanc, chêne, chêne noir | clic droit avec la **clé de décorateur** |
| Teinte de lumière | auto, colorée, chaude, neutre, froide | accroupi + clic droit avec la clé |

Cela donne 800 combinaisons par modèle, et plus de 50 000 lampes différentes au total.

Les textures font 64×64 :

- métal brossé avec rayures et arêtes usées ;
- lin tissé, tissu plissé, rotin tressé ;
- bois à nœuds, marbre veiné, pierre taillée ;
- cire avec coulures ;
- vitrail Tiffany serti de plomb.

Le verre est translucide : on voit l'ampoule à filament ou la flamme à l'intérieur.

## Contrôles

- **Main vide, clic droit** : allumer / éteindre. **Accroupi + clic droit** : luminosité (15, 12, 9 ou 6).
- **Redstone** : un signal allume la lampe, la fin du signal l'éteint.
- **Interrupteur / Variateur** : avec l'interrupteur en main, fais accroupi + clic droit sur des lampes pour les lier (64 maximum par défaut, réglable), puis pose-le.
  - L'interrupteur allume ou éteint tout le groupe d'un clic.
  - Le variateur change la luminosité du groupe ; accroupi + clic l'allume ou l'éteint.
  - Casser un interrupteur garde ses liaisons dans l'objet.
- **Télécommande** : se lie aux lampes de la même façon. Ensuite, clic droit dans le vide pour allumer / éteindre, accroupi pour la luminosité.
- Bougies, flambeaux et braseros font des flammes et de la fumée quand ils sont allumés.
- Les lampes acceptent l'eau. Elles se posent sur n'importe quel bloc qui a une forme : escalier à l'envers, dalle, barrière, tête de joueur, vitre… Elles tombent si on retire leur support.
- **Bandes LED** : elles se posent en position basse, milieu ou haute de la face, selon l'endroit visé. Accroupi, on les pose à la verticale ; au sol, elles suivent le regard.
  - **Angles** : un même bloc porte jusqu'à 6 bandes, une par face. Dans le coin d'une pièce, la bande du mur nord et celle du mur est se rejoignent dans le même bloc, chacune avec sa propre position.
  - **Chaînes redstone** : avec le **connecteur LED**, clic droit près du bord d'une bande pour la relier à sa voisine de ce côté (accroupi : à travers la face cliquée). Dès qu'une bande de la chaîne reçoit un signal redstone, toute la chaîne s'allume. Les liaisons s'affichent en étincelles vertes.
  - Longueur maximale d'une chaîne : 256 bandes par défaut, réglable.

## Les lampes

| Catégorie | Modèles |
|---|---|
| Plafond | suspension, lustre, lustre en cristal, plafonnier, lanterne en papier, cage industrielle, suspension globe, lanterne suspendue, grappe de suspensions, suspension dôme, suspension anneau, lanterne marocaine, rail de spots, ventilateur lumineux, ampoule Edison |
| Mur | applique, applique double, lanterne murale, tube néon, anneau néon, spot mural, hublot, flambeau, bougeoir mural, éclairage de tableau, applique industrielle, globe mural, applique lumineuse, guirlande |
| Table | lampe de table, lampe de bureau, lampe à lave, lampe champignon, lampe de banquier, lanterne, chandelier, bougie en pot, lampe boule, lampe lune, lampe Tiffany, lampe de sel, lampe à pétrole, lampe shoji |
| Sol | lampadaire, lampadaire arc, réverbère, réverbère double, trépied, torchère, colonne en papier, projecteur de studio et lanterne de pierre (2 blocs de haut) ; borne lumineuse, lanterne de jardin, balise, brasero |
| Blocs | bloc lumineux, lampe encadrée, lampe alvéolée, bloc cadre néon, bloc shoji ; panneau LED, bande LED, projecteur, spot encastré et cristal lumineux (ces 5 derniers se fixent sur n'importe quelle face) |

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

## Performances

Test de charge avec 20 joueurs simultanés et près de 37 000 lampes : 20 TPS tenus, coût proche de blocs lumineux
vanilla, aucune erreur, lumière exacte. Rapport détaillé : [docs/TEST_DE_CHARGE.md](docs/TEST_DE_CHARGE.md)
(`./gradlew runStressServer` et `./gradlew runStressClient` pour le relancer).

## Compiler

```
./gradlew build
```

Le jar se trouve dans `build/libs/`.

- `./gradlew runClient` : lance le jeu
- `./gradlew runGameTestServer` : lance les tests automatiques

## Modèles et ressources

Les fichiers Blockbench (`.bbmodel`, textures intégrées) se trouvent dans [`blockbench/`](blockbench/), rangés par catégorie.

Le script [`tools/generate.mjs`](tools/generate.mjs) contient la géométrie de chaque lampe. Il génère :

- les modèles Blockbench ;
- les modèles de jeu, blockstates, textures et traductions (fr/en) ;
- les loot tables, tags et recettes ;
- `LampType.java`, `LampFinish.java`, les hitbox et les points de flamme.

Pour ajouter ou modifier une lampe, édite `LAMPS` dans le script, puis lance :

```
node tools/generate.mjs
```

Le script refuse deux recettes identiques.

Dans les modèles, les textures sont en niveaux de gris et la couleur vient des teintes :

- `tintindex` 0 : couleur ;
- `tintindex` 1 : diffuseur ;
- `tintindex` 2 : ampoule ;
- `tintindex` 3 : finition.

Les zones lumineuses utilisent `neoforge_data` (émissif), que Blockbench n'exporte pas. Reporte donc dans le script les changements faits dans Blockbench.
