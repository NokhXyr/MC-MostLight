# Error log

## [2026-10-05 06:50] GameTest mock player always counts as creative

**Context:** exploit tests breaking blocks as a survival player made with `GameTestHelper.makeMockServerPlayerInLevel()`.
**Error:** breaking the upper half of any tall lamp "in survival" dropped nothing.
**Root cause:** the mock player overrides `isCreative()` to always return true, whatever its game mode; `TallLampBlock.playerWillDestroy` used `isCreative()` to remove the lower half without drops.
**Fix:** test the creative ability (`getAbilities().instabuild`), which follows the real game mode. Same behavior in game, correct under the mock.
**Prevention:** in game logic, check `getAbilities().instabuild` for "no drops"; in tests, never trust `isCreative()` of a mock player.

## [2026-10-05 06:55] Sticky piston pulled lamps lost their finish

**Context:** `PistonCarry` keeps lamp data while a piston moves it.
**Error:** a lamp pulled back by a sticky piston came back with its default finish and tone.
**Root cause:** the data was captured at the head of `moveBlocks`, before vanilla removes the piston head; the resolver built then saw the head in the way and failed.
**Fix:** capture where vanilla builds its own `PistonStructureResolver` (mixin `@At("NEW")`), after the head is gone.
**Prevention:** an injection that rebuilds vanilla state must sit where vanilla computes the same thing.

## [2026-10-04 16:30] Lamps fully glowing under the Solas shader

**Context:** colored light under Iris shaders, each lit lamp borrowing the material ID of a vanilla light source of the same color (`compat/IrisLightColors.java`).
**Error:** under Solas, whole lamps glowed (shades, frames, bases), as if made of light.
**Root cause:** a shader pack uses the same block ID for light color and for surface emission. For froglights, lanterns and soul lanterns, Solas makes the whole texture emissive from its color.
**Fix:** use the lit candle of the lamp's color when the pack gives colored candles their own ID; keep the vanilla source otherwise.
**Prevention:** check a borrowed ID in the pack's `lib/pbr` code, not only its light color. Compare packs in one run with `MOSTLIGHT_SHOWCASE_SHADERS`.

## [2026-10-04 16:10] See-through cube lamps hid neighboring faces (X-ray)

**Context:** cube lamps (framed lamp, shoji, neon frame) with open frames or glass.
**Error:** looking through a framed lamp showed the void behind it.
**Root cause:** cube lamps were registered as occluding blocks, so the game culled neighbor faces they did not actually hide.
**Fix:** `noOcclusion()` on every lamp; `CubeLampBlock.skipRendering` hides only the shared face of two lamps of the same model.
**Prevention:** any block whose model is not a full opaque cube must be non-occluding.

## [2026-10-04 17:25] JAR reported the wrong version

**Context:** building 1.2.0 after setting `mod_version` in `gradle.properties`.
**Error:** the JAR's `neoforge.mods.toml` still said `version="1.1.0"`.
**Root cause:** the version was hardcoded in `src/main/resources/META-INF/neoforge.mods.toml`.
**Fix:** `processResources` expands `${version}` from `mod_version`.
**Prevention:** check `META-INF/neoforge.mods.toml` inside the built JAR after a version bump.

## [2026-10-04 15:00] Shader capture loop restarted the game for each pack

**Context:** comparing lamps under several shader packs in the Arcadia modpack.
**Error:** one full game start (several minutes) per pack.
**Root cause:** the pack was only read from `iris.properties` at startup.
**Fix:** `ShowcaseDirector` switches packs in game through Iris (config saved, then `Iris.reload()`, by reflection) and shoots the same series again.
**Prevention:** Iris re-reads its config file on reload: always save the config before reloading.

## [2026-10-04 20:13] Two stress test games running at the same time

**Context:** stopping a stress series (Arcadia, vanilla then MostLight passes) to fix the scenario, then starting a new one.
**Error:** two 16 GB games ran together; the PC slowed down and the measurements of both runs were wrong.
**Root cause:** stopping the background task killed the shell but not the loop's next `gradlew` call, which started the next pass a minute before the new series.
**Fix:** killed every stress game, Gradle launcher, script and Crash Assistant helper; the series script now refuses to start a pass while a java process over 2 GB is running.
**Prevention:** when stopping a test series, kill the script first, then the game, and check that no java process over 2 GB is left before starting anything.

## [2026-10-04 22:01] Stress test stuck on "Saving and pausing game"

**Context:** client stress test without the modpack (runStressClientVanilla) while another Minecraft window was open.
**Error:** the test never started; the log stopped at "Saving and pausing game..." for 18 minutes.
**Root cause:** the window lost focus while the world loaded, the game paused, and the director waits for the player to have ticked 40 times before starting: a paused game never ticks.
**Fix:** the stress and showcase directors turn off pause-on-lost-focus and close the pause screen as soon as the world is loading.
**Prevention:** any automated run that waits on game ticks must disable pausing before waiting.

## [2026-10-05 01:40] Complementary lost its coloured lamp light after the Solas fix

**Context:** refreshing the CurseForge page, whose banners were recorded with Complementary Reimagined (coloured lighting on).
**Error:** with the candle mapping, lamps under Complementary lit in warm white instead of their colour.
**Root cause:** Complementary only colours candles with an extra option (COLORED_CANDLE_LIGHT, off by default); the Solas fix had been checked against Complementary Unbound without coloured lighting, where both mappings looked the same.
**Fix:** `IrisLightColors` checks the active pack: Complementary keeps the vanilla light sources, other packs use the coloured candles.
**Prevention:** check a shader change with each pack's coloured lighting turned on, not only with its defaults.

## [2026-10-05 02:04] Custom finish of a newly placed lamp not shown on clients

**Context:** recording the CurseForge page, a counter of lamps placed with `/setblock` and a `finish` tag.
**Error:** the lamps showed their model's default finish in game.
**Root cause:** the block entity only sends data to clients after a look change (key, dye); a lamp placed with a custom finish, tone or LED setting (item from the finishes tab, `/setblock`, structure) never sent it, so clients kept the defaults until the chunk reloaded.
**Fix:** `LampBlockEntity` marks a custom look for one packet when it is loaded or placed from an item; default lamps still send nothing. GameTest `placedLookReachesClients`.
**Prevention:** when a block entity skips sync packets to save bandwidth, test the first placement as well as later changes.
