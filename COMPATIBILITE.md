# Compatibilité MostLight (NeoForge 1.21.1)

## Pourquoi MostLight est compatible avec les mods d'optimisation

- **Aucun mixin dans Minecraft, Sodium ou NeoForge.** Le seul mixin vise Iris. Il est optionnel (`@Pseudo`) et ignoré si Iris n'est pas installé.
- **Uniquement des modèles JSON standard** (pas de rendu personnalisé ni de BlockEntityRenderer). Sodium, Embeddium, ModernFix et ImmediatelyFast les traitent comme des blocs vanilla.
- **Couleurs** : elles passent par les teintes de bloc standard (`BlockColor` / `ItemColor`). Les finitions sont lues dans le block entity, ce qui fonctionne avec le maillage de chunks de Sodium (vérifié).
- **Hitbox et flammes** : elles sont lues depuis un fichier de ressources, sans gros code statique.

## Résultats des tests

Légende :

- ✅ **vérifié en jeu** : la galerie de 1 177 lampes a été construite, la survie et l'éclairage de chaque lampe ont été vérifiés, avec des captures de jour et de nuit.
- 🟡 **chargé sans erreur** : le jeu démarre avec le mod, ressources et modèles chargés, aucune erreur liée à MostLight. Le test complet dans un monde reste à faire.

| Mod | Version testée | Résultat |
|---|---|---|
| Sodium | 0.8.13 | ✅ |
| Iris | 1.8.14-beta.1 | ✅ |
| Complementary Reimagined | r5.9.3 | ✅ (lumière colorée : voir plus bas) |
| ModernFix | 5.27.24 (option `dynamic_resources` activée) | 🟡 |
| FerriteCore | 7.0.3 | 🟡 |
| Sodium Extra | 0.9.4 | 🟡 |
| ImmediatelyFast | 1.6.14 | 🟡 |
| EntityCulling | 1.11.2 | 🟡 |
| Lithium | 0.15.4 | 🟡 |
| MoreCulling (+ Cloth Config) | 1.0.10 | 🟡 |
| Noisium | 2.3.0 | 🟡 |
| ScalableLux (moteur de lumière) | 0.1.0.1 | 🟡 |
| Alternate Current | 1.9.0 | 🟡 |
| Clumps | 19.0.0.1 | 🟡 |
| BadOptimizations | 2.4.1 | 🟡 |
| Particle Core (+ Kotlin for Forge, Fzzy Config) | 0.3.3 | 🟡 |
| C2ME | 0.4.0-alpha.0.122 | 🟡 |
| Distant Horizons | 3.3.3 | 🟡 |
| FastWorkbench (+ Placebo) | 9.1.3 | 🟡 |
| Fast IP Ping | 1.0.12 | 🟡 |
| Ksyxis | 1.4.5 | ⚠️ le monde ne finit pas de charger. Le mod lui-même prévient : « supprimez le mod s'il bloque après ce message ». Le blocage arrive avant que MostLight intervienne. |
| Embeddium | 1.0.15 | non testé (remplace Sodium, les deux ne vont pas ensemble) |
| LambDynamicLights | 4.8.11 | non testable : exige Fabric API sur NeoForge 1.21.1 (Sinytra Connector) |

Pour rejouer le test : mets les mods dans `run-opti/mods/`, puis lance `./gradlew runShowcaseOpti`.

## Lumière colorée avec les shaders

Une lampe colorée éclaire de sa couleur sous Iris. Au chargement du pack, MostLight donne à chaque lampe allumée l'identifiant de matériau qu'utilise le pack pour une source vanilla de la même couleur :

| Couleur de la lampe | Source vanilla de référence |
|---|---|
| Rouge | bloc de redstone |
| Orange | champilampe |
| Jaune | grenouillampe ocre |
| Vert | grenouillampe verdoyante |
| Cyan, bleu clair | lanterne des âmes |
| Bleu, gris | lanterne de mer |
| Violet | obsidienne pleureuse |
| Rose, magenta | grenouillampe nacrée |
| Blanc | barre de l'End |
| Marron | lanterne |
| Noir | améthyste |

Cela marche avec tout pack qui colore la lumière par bloc.

- **Complementary** : son « éclairage coloré » est **désactivé par défaut**. Active-le dans les options du pack : *Colored Lighting*, réglé sur 256 ou plus.
- **Limite** : la teinte de lumière choisie à la clé de décorateur (chaude, froide…) ne change pas la couleur sous shaders. Seule la couleur de la lampe compte.
