# Changelog

## English

Every release of MostLight (NeoForge 1.21.1). Newest first.

### 1.0.0 - 2026-10-05

First public release.

#### Lamps
- **71 lamp models in 16 colours**: ceiling, wall, table, floor (two blocks tall), light blocks, LED strips and string
  lights. Among them an animated gear lamp (its gears turn while lit), End-rod style light rods (standing, hanging,
  lying on a wall or the floor) and a ceiling fan (first click: light, second click: fan).
- **10 frame finishes and 5 light tones**, applied with the designer wrench (shift + scroll to pick the finish).
- On with a right click or redstone, 4 brightness steps (sneak + right click). A dye recolours a placed lamp.
- Lamps can be waterlogged and stand on any block with a shape: upside-down stairs, fences, slabs, heads...
- Small lamps (wall, hanging, LED) have no collision for players; projectiles still hit them. Hitboxes follow the
  models.
- Translucent paper, moon, stained glass, lava lamp and clear salt lamp; flames and smoke on candles, torches and
  braziers.

#### LED strips and string lights
- Up to 6 strips per block (one per face), low, middle or high, horizontal or vertical. They join their neighbours like
  redstone: inner and outer corners, L, T and cross.
- **LED connector**: links neighbouring strips and light blocks into a chain that follows the redstone signal of any one
  of its blocks. The chain under the cursor is outlined in green through walls, with its size and state.

#### Control
- **Switches**: single, dimmer and double, in 16 colours, and a **remote**. Sneak + right click lamps with one in hand to
  link them before placing it.
- **Lamp workbench**: every lamp, switch and tool is made here, from a catalogue (one tab per category, search,
  preview, materials with amounts, choice of the 16 colours). Shift + click makes as many as possible.

#### Compatibility
- **Pistons** push and pull lamps with their finish, tone, strips and links. A lamp that loses its support drops as an
  item; two-block lamps break.
- **Create** (optional): contraptions carry the lamps attached to the blocks they move.
- **Coloured light under shaders** (Iris): each lit lamp lights up in its colour with packs that colour light, such as
  Solas and Complementary (coloured lighting on).
- **Sodium, Iris, ModernFix**: lamps, strips and animated lamps render correctly.
- **JEI**: workbench recipes with their materials and amounts.

#### For server owners
- **Configuration** (`config/mostlight-common.toml`): longest LED chain, most lamps per switch (up to 1,024) and switch
  range.
- **Diagnostic commands** (operators): `/mostlight inspect` shows everything the mod knows about a lamp or a switch,
  `/mostlight check` lists broken data in the loaded chunks around (missing block entity, two-block lamp cut in half,
  wrong light, dead links), `/mostlight repair` fixes it.
- Tested against duplication and item loss: breaking, explosions, pistons, Create contraptions, workbench and forged
  packets from a modified client.

#### Performance
- One block per lamp model and per switch (74 blocks): the colour is a block property applied to the shared model.
- 91,040 block states: light and brightness share one property, the remembered redstone signal lives in the block
  entity. Less memory, faster start, smaller tables for shaders and optimisation mods.
- Hitboxes computed in advance; model files kept small (3 MB): the middle and high positions of strips and the lit
  version of each lamp are built at load time.
- Switching a lamp does not wake its neighbours, and redstone-driven lamps send nothing over the network when the signal
  changes. With 10,000 lamps on redstone clocks: 453 FPS and 14.5 ms per server tick.
- Saves and network packets only carry what differs from the lamp's defaults. GeckoLib animations only up close.
- Load tests: 20 simulated players and 10,000 to 37,000 lamps hold 20 TPS. In the Arcadia modpack (530 mods), MostLight
  code takes about 2% of server time with 20 active players.

#### Other
- License: all rights reserved. Modpacks may include the official, unmodified JAR, with credit.
- Code of conduct, security policy and contribution guide in `.github`.

## Français

Toutes les versions de MostLight (NeoForge 1.21.1). La plus récente en haut.

### 1.0.0 - 2026-10-05

Première sortie publique.

#### Lampes
- **71 modèles de lampes en 16 couleurs** : plafond, mur, table, sol (sur deux blocs), blocs lumineux, bandes LED et
  guirlandes. Parmi elles une lampe à engrenages animée (ses engrenages tournent quand elle est allumée), des tiges
  lumineuses façon barre de l'End (debout, pendues, couchées au mur ou au sol) et un ventilateur de plafond (1er clic :
  lumière, 2e clic : ventilateur).
- **10 finitions de cadre et 5 teintes de lumière**, appliquées avec la clé de décorateur (Maj + molette pour choisir la
  finition).
- Allumage au clic droit ou à la redstone, 4 niveaux de luminosité (accroupi + clic droit). Un colorant recolore une
  lampe posée.
- Lampes immergeables, posables sur tout bloc ayant une forme : escalier à l'envers, barrière, dalle, tête...
- Petites lampes (murales, suspendues, LED) sans collision pour les joueurs ; les projectiles les touchent. Hitbox
  fidèles aux modèles.
- Papier, lune, vitraux, lampe à lave et lampe de sel claire translucides ; flammes et fumée sur les bougies, flambeaux
  et braseros.

#### Bandes LED et guirlandes
- Jusqu'à 6 bandes par bloc (une par face), en position basse, milieu ou haute, à l'horizontale ou à la verticale.
  Elles se raccordent à leurs voisines comme la redstone : angles intérieurs et extérieurs, L, T et croix.
- **Connecteur LED** : relie les bandes voisines et les blocs lumineux en une chaîne qui suit le signal redstone d'un
  seul de ses blocs. La chaîne visée est entourée d'un contour vert visible à travers les murs, avec sa taille et son
  état.

#### Commande
- **Interrupteurs** simple, variateur et double, en 16 couleurs, et **télécommande**. Accroupi + clic droit sur des
  lampes avec l'objet en main pour les lier avant de le poser.
- **Établi de luminaire** : toutes les lampes, interrupteurs et outils se fabriquent ici, en catalogue (un onglet par
  catégorie, recherche, aperçu, matériaux avec quantités, choix parmi les 16 couleurs). Maj + clic fabrique autant que
  possible.

#### Compatibilité
- **Pistons** : les lampes se poussent et se tirent avec leur finition, leur teinte, leurs bandes et leurs liaisons.
  Une lampe qui perd son support tombe en objet ; les lampes de 2 blocs se cassent.
- **Create** (facultatif) : les contraptions emportent les lampes attachées aux blocs qu'elles déplacent.
- **Lumière colorée sous shaders** (Iris) : chaque lampe allumée éclaire de sa couleur avec les packs qui colorent la
  lumière, comme Solas et Complementary (éclairage coloré activé).
- **Sodium, Iris, ModernFix** : lampes, bandes et lampes animées affichées correctement.
- **JEI** : recettes de l'établi avec leurs matériaux et quantités.

#### Pour les serveurs
- **Configuration** (`config/mostlight-common.toml`) : longueur maximale des chaînes LED, nombre maximal de lampes par
  interrupteur (jusqu'à 1 024) et portée des interrupteurs.
- **Commandes de diagnostic** (opérateurs) : `/mostlight inspect` montre tout ce que le mod sait d'une lampe ou d'un
  interrupteur, `/mostlight check` liste les données cassées dans les chunks chargés autour (block entity manquante,
  lampe de 2 blocs coupée en deux, lumière fausse, liaisons mortes), `/mostlight repair` les corrige.
- Testé contre la duplication et la perte d'objets : casse, explosions, pistons, contraptions de Create, établi et
  paquets forgés par un client modifié.

#### Performances
- Un seul bloc par modèle de lampe et par interrupteur (74 blocs) : la couleur est une propriété du bloc, appliquée au
  modèle commun.
- 91 040 états de bloc : l'allumage et la luminosité tiennent dans une seule propriété, le signal redstone mémorisé est
  dans la block entity. Moins de mémoire, démarrage plus court, tables plus petites pour les shaders et les mods
  d'optimisation.
- Hitbox précalculées ; fichiers de modèles légers (3 Mo) : les positions milieu et haute des bandes et la version
  allumée de chaque lampe sont construites au chargement.
- Basculer une lampe ne réveille pas ses voisins, et les lampes commandées par la redstone n'envoient rien sur le réseau
  quand le signal change. Avec 10 000 lampes sur des horloges redstone : 453 FPS et 14,5 ms par tick serveur.
- Sauvegardes et paquets réseau ne contiennent que ce qui diffère des valeurs par défaut de la lampe. Animations
  GeckoLib seulement de près.
- Tests de charge : 20 joueurs simulés et 10 000 à 37 000 lampes tiennent 20 TPS. Dans le modpack Arcadia (530 mods),
  le code de MostLight coûte environ 2 % du temps du serveur avec 20 joueurs actifs.

#### Divers
- Licence : tous droits réservés. Les modpacks peuvent inclure le JAR officiel non modifié, avec crédit.
- Code de conduite, politique de sécurité et guide de contribution dans `.github`.
