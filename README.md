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
- **Interrupteur / Variateur** : avec l'interrupteur en main, fais accroupi + clic droit sur des lampes pour les lier (64 maximum), puis pose-le.
  - L'interrupteur allume ou éteint tout le groupe d'un clic.
  - Le variateur change la luminosité du groupe ; accroupi + clic l'allume ou l'éteint.
  - Casser un interrupteur garde ses liaisons dans l'objet.
- **Télécommande** : se lie aux lampes de la même façon. Ensuite, clic droit dans le vide pour allumer / éteindre, accroupi pour la luminosité.
- Bougies, flambeaux et braseros font des flammes et de la fumée quand ils sont allumés.
- Les lampes acceptent l'eau. Les lampes suspendues et murales tombent si on retire leur support.

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
