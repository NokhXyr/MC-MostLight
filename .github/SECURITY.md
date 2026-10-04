# Security Policy / Politique de sécurité

## Supported versions / Versions prises en charge

Security fixes are considered for the latest published `1.2.x` release of
MostLight. Older builds are not maintained. This table will change when a
new release line is published.

Les correctifs de sécurité concernent la dernière version publiée de la
branche `1.2.x`. Les anciennes versions ne sont pas maintenues.

## Scope / Portée

This policy covers MostLight's own code, on the server and on the client:
network packets (lamp workbench crafting, switch and remote links, LED
connector), checks on what a player may craft or link, data stored in lamp
block entities, the piston mixin, and the compatibility code for Create and
Iris. Vulnerabilities in Minecraft, NeoForge, GeckoLib, JEI, Create, Iris,
Sodium or another mod should be reported to their respective maintainers.

Cette politique couvre le code propre à MostLight, côté serveur et côté
client : paquets réseau (fabrication à l'établi de luminaire, liaisons des
interrupteurs et de la télécommande, connecteur LED), contrôles de ce qu'un
joueur peut fabriquer ou relier, données enregistrées dans les lampes, mixin
du piston et compatibilité avec Create et Iris. Signalez les failles de
Minecraft, NeoForge, GeckoLib, JEI, Create, Iris, Sodium ou d'un autre mod à
leurs responsables.

## Private reporting / Signalement privé

Do not disclose an exploitable vulnerability in a public issue. Use a
[private GitHub security advisory](https://github.com/NokhXyr/MC-MostLight/security/advisories/new)
and include the affected version, reproduction steps, expected impact, and a
minimal proof of concept. If private advisories are unavailable, open an issue
without exploit details to request a private contact channel. Please allow
time for investigation and a fix before public disclosure. Ordinary bugs,
rendering issues and performance issues can use regular issues.

Ne publiez pas une faille exploitable dans une issue publique. Utilisez un
[avis de sécurité privé GitHub](https://github.com/NokhXyr/MC-MostLight/security/advisories/new)
avec la version concernée, les étapes de reproduction, l'impact et une preuve
de concept minimale. Si ce canal est indisponible, ouvrez une issue sans
détails d'exploitation pour demander un moyen de contact privé. Laissez le
temps d'analyser et de corriger avant toute divulgation publique. Les bugs
ordinaires, problèmes d'affichage et de performances passent par les issues.
