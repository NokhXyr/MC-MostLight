# Error log

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
