# MostLight

Mod NeoForge pour **Minecraft 1.21.1** qui ajoute **512 lampes décoratives** : 32 modèles × 16 couleurs.
Il est compatible avec toutes les versions de NeoForge 21.1.x.

## Fonctionnalités

- **Clic droit** (main vide) : allumer / éteindre
- **Accroupi + clic droit** : changer la luminosité (15 → 12 → 9 → 6)
- **Redstone** : un signal allume la lampe, la fin du signal l'éteint
- **Colorant** : clic droit sur une lampe posée pour la recolorer, en gardant son orientation et son état
- **Craft** : chaque lampe se fabrique en blanc, puis lampe + colorant = n'importe quelle couleur
- Waterloggable, hitbox calquées sur les modèles, parties lumineuses en pleine luminosité (émissives)
- Les lampes suspendues ou murales tombent si on retire leur support
- 5 onglets créatifs : Plafond, Mur, Table, Sol, Blocs

## Les lampes

| Catégorie | Modèles |
|---|---|
| Plafond | Suspension, Lustre, Plafonnier, Lanterne en papier, Lampe cage industrielle, Suspension globe, Lanterne suspendue |
| Mur | Applique, Lanterne murale, Tube néon, Spot mural, Hublot, Flambeau mural |
| Table | Lampe de table, Lampe de bureau, Lampe à lave, Lampe champignon, Lampe de banquier, Lanterne de table, Bougeoir |
| Sol | Lampadaire, Lampadaire arc, Réverbère, Lampe trépied (2 blocs de haut) ; Borne lumineuse, Lanterne de jardin |
| Blocs | Bloc lumineux, Lampe encadrée, Panneau LED, Bande LED, Projecteur, Spot encastré (ces 4 derniers se fixent sur n'importe quelle face) |

Chaque modèle existe dans les 16 couleurs de Minecraft.

## Compiler

```
./gradlew build
```

Le jar se trouve dans `build/libs/`.

- `./gradlew runClient` : lance le jeu avec le mod
- `./gradlew runGameTestServer` : lance les tests automatiques

## Modèles et ressources

Les fichiers Blockbench (`.bbmodel`, textures intégrées) se trouvent dans [`blockbench/`](blockbench/), rangés par catégorie.

Le script [`tools/generate.mjs`](tools/generate.mjs) contient la géométrie de chaque lampe et la recette du rendu. Il génère :

- les modèles Blockbench ;
- les modèles de bloc et d'item, les blockstates, les textures et les traductions (fr/en) ;
- les loot tables, tags et recettes ;
- `LampType.java` et les hitbox (`GeneratedShapes.java`).

Pour ajouter ou modifier une lampe, édite `LAMPS` dans le script, puis lance :

```
node tools/generate.mjs
```

Une lampe ajoutée à `LAMPS` apparaît automatiquement dans les 16 couleurs.

Les zones lumineuses utilisent `neoforge_data` (émissif), que Blockbench n'exporte pas. Tu peux retoucher un modèle dans Blockbench pour visualiser, mais reporte ensuite les cubes dans le script.
