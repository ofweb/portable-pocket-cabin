# Cabin book installation

Minecraft 26.2 client GameTests capture the shared Books tab at 1280×800 with GUI scale 3. The fixtures use Survival mode and the real synchronized menu.

The captures show the empty input, each book's preview, hover text, an already installed book, the tab after all books are installed, and resident access. Vanilla inventory slots and the [smithing reference](../crafting-ui/screenshots/vanilla-10-smithing-empty.png) guide the interface.

To repeat the captures:

```sh
PPC_TEST_BOOKS=1 ./gradlew runClientGameTest
```

New captures appear in `build/run/clientGameTest/screenshots/`. The [screenshots](screenshots) here preserve the reviewed states when later tests clear build output. They are visual references, not regression baselines.
