---
name: minecraft-ui
description: Design, implement, or review Minecraft/Fabric screens and container interfaces in this repository, including widgets, slots, tooltips, layout, rendering, and UI screenshots. Exclude gameplay logic with no UI component.
---

# Minecraft UI

Build interfaces that feel like Minecraft. Prefer vanilla interaction conventions and visual elements.

## Inspect before designing

Read the affected screen, menu, layout, rendering code, and relevant feature brief. For cabin upgrades, start with these files under `src/main/java/dev/portablepocketcabin/`:

- `CabinUpgradeScreen.java`: rendering, widgets, tooltips, and client interaction.
- `CabinUpgradeLayout.java`: shared geometry.
- `CabinUpgradeMenu.java`: slots, synchronized state, permissions, and actions.

Check `gradle.properties` and `build.gradle` for the current Minecraft version, Fabric dependencies, and test configuration. Inspect matching local Minecraft source and assets when available. Consult current official documentation when an unfamiliar API cannot be verified locally. Do not copy rendering APIs from another Minecraft version.

Choose the closest vanilla references for the interaction:

- Inventory/container screens: player inventory and slots.
- Stonecutter: choosing between alternatives.
- Smithing: inputs, requirements, and results.
- Beacon: upgrade selection and state.
- Furnace screens: progress and status.
- Creative inventory: categories and tabs.

Prefer focused references over inventing a visual language. Use source, assets, or rendered references rather than memory when available.

Determine what the player chooses, which states must be visible, which actions are possible, and which information is primary. Move secondary explanation into tooltips. Settle these interactions before choosing rendering details.

## Visual rules

Prefer Minecraft's existing widgets, textures, sprites, item rendering, fonts, colors, and interaction conventions. Compose vanilla elements before adding custom drawing. Do not approximate an available vanilla element with rectangles, gradients, borders, or bevels. Use custom assets only where vanilla elements cannot express the required interaction.

Avoid Unicode symbols as icon substitutes unless Minecraft uses that symbol in the same context. Prefer items, sprites, or short text. Use item icons when the item communicates meaning better than a label.

Use Minecraft's GUI pixel grid. Item icons are normally 16×16 GUI pixels and inventory slot cells are 18×18. Preserve the normal player inventory and hotbar geometry. Align related elements to shared anchors rather than tuning each coordinate independently.

Keep selected, unavailable, completed, partially funded, and blocked states distinguishable without a tooltip. Use more than color alone where states would otherwise be ambiguous. Preserve Minecraft's existing conventions for these states.

## Layout and implementation

Define shared dimensions and anchors in the layout class. Keep screen rendering and menu slot positions consistent. Plan screen bounds, navigation, primary content, actions/status, and player inventory before placing individual elements. Include tabs and controls extending outside the main panel when checking available space.

Avoid enlarging the GUI merely to simplify layout. Remove unnecessary information or use tooltips first. Check the layout at the GUI scales and window sizes it needs to support.

Keep visual code separate from menu and game-state logic where practical. Preserve server authority, permissions, slot behavior, and confirmation semantics unless the task explicitly changes them. Do not duplicate Minecraft rendering behavior merely for cosmetic control.

## Deterministic states and capture

For new screens or significant visual changes, provide reproducible fixtures for states that materially change appearance. Upgrade interfaces commonly need missing materials, partial funding, ready to install, installed, blocked by prerequisites, and the maximum normal number of categories and requirements. Include confirmation and permission states when they change visible controls. Avoid dependence on an existing player save.

Run `just screenshots` to regenerate every cabin screen, exterior view, and vanilla reference. The command saves captures in the top-level `screenshots/` folder, grouped by subject, and generates `screenshots/README.md` as an index. Keep review screenshots there and link to them from experiment notes. Do not move them into experiments. The command uses Xvfb when no display is configured.

Run `just test-client` or `./gradlew runClientGameTest` for client tests alone. Raw captures are saved under `build/run/clientGameTest/screenshots/` and can be cleared by later tests. Client tests run separately from `build`.

Extend `src/gametest/java/dev/portablepocketcabin/PortablePocketCabinClientGameTest.java` for upgrade UI fixtures. It creates a fresh world and cabin, fixes material attunement, resolution, GUI scale, and cursor position, and captures missing materials, partial funding, readiness, and confirmation through the real synchronized menu. Server tests in `PortablePocketCabinGameTest.java` cover menu and gameplay behavior; they do not verify rendered appearance. Recheck `build.gradle` if the test configuration changes.

Use a relevant client GameTest or UI fixture to produce deterministic screenshots. If no capture fixture exists, add the smallest fixture needed for the UI change and verify its APIs against the installed Fabric version. Keep resolution, GUI scale, language, state, and mouse position fixed. Stabilize time-dependent content where relevant. Capture tooltips and hover states separately when they matter.

If client execution or capture is blocked, finish the checks that are available and report the blocker. Do not substitute a mockup for a rendered screenshot or claim visual verification. This skill does not require building a screenshot harness for a review-only request.

## Visual verification and review

A UI change is not visually verified after it compiles. Open the rendered screenshots with the local image viewing tool. Compare them with the chosen vanilla references. Inspect alignment, spacing, clipping, readability, hierarchy, widget consistency, density, selected/disabled states, and unnecessary custom rendering.

Fix meaningful visible problems and capture the affected states again. Stop when the review finds no meaningful remaining problems; do not iterate on tiny differences without a concrete reason. Compilation, behavior tests, and screenshot comparisons cannot replace inspection.

When reviewing existing UI, inspect a screenshot of the state under review before proposing cosmetic changes. Confirm that it represents the current implementation. Describe concrete locations and effects, such as: "The action buttons compete with the requirements and appear disconnected from the selected upgrade." Treat the rendered result as the authority for visual issues. If no screenshot is available, report that limitation and scope findings to code or behavior.

For accepted, stable UI states, prefer persistent screenshot comparison tests where supported. Use fuzzy comparison rather than exact framebuffer equality. Do not update a baseline until the changed rendering has been inspected. Regression comparisons protect accepted layouts but do not replace visual review during redesign.

## Completion

Report the vanilla references used, rendered states inspected, screenshot paths, relevant checks, and remaining visual compromises or verification blockers. Do not declare visual correctness without inspecting rendered screenshots, or mark UI work complete solely because tests pass.
