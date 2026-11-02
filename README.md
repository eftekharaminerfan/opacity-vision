# Opacity Vision (Fabric, Minecraft Java 26.1.2)

Client-side "x-ray, but adjustable" mod. Instead of deleting every block, you choose how see-through the world is.

## Features
- **Block opacity** 0-100% for normal blocks (0% = classic x-ray, 100% = vanilla).
- **Highlight opacity** for ores / chosen blocks (default 100%, so they stand out).
- **Outlines** around highlighted blocks, on or off, with a choice of colour.
- Highlights: every `*_ore`, plus a list you edit (default: ancient debris, spawner).
- **Inner faces** option: draws faces between two normal blocks for a "foggy depth" look (heavier on FPS).

## Default keys (rebind in Options > Controls > Opacity Vision)
| Key | Action |
|-----|--------|
| X | Toggle Opacity Vision |
| ] / [ | Increase / decrease block opacity (5% steps) |
| O | Toggle outlines |
| L | Add/remove the block you're looking at to/from the highlight list |
| K | Open the settings screen |

Settings are saved to `config/opacityvision.json`.

## Build
Requires **JDK 25**.

1. Open this folder in IntelliJ IDEA (2025.3+) as a Gradle project, or run `gradle wrapper` once and then:
2. `./gradlew build`
3. The mod jar is `build/libs/opacity-vision-1.0.0.jar`. Put it in `.minecraft/mods/` together with Fabric API for 26.1.2 and Fabric Loader 0.19.1+.

Test in the dev client with `./gradlew runClient`.

## Notes
- Client-only; safe to use in singleplayer. On multiplayer servers, x-ray style mods are often against the rules (e.g. Donut SMP and most public servers), so check before using it there.
- Not compatible with mods that replace the terrain renderer without supporting the Fabric Rendering API.
