package dev.portablepocketcabin;

import dev.portablepocketcabin.CabinCorridorSnapshot;
import dev.portablepocketcabin.CabinExterior;
import dev.portablepocketcabin.CabinGreenhouse;
import dev.portablepocketcabin.CabinGreenhouseGrowth;
import dev.portablepocketcabin.CabinGreenhouseState;
import dev.portablepocketcabin.CabinIngredientFallbacks;
import dev.portablepocketcabin.CabinPalette;
import dev.portablepocketcabin.CabinRegistry;
import dev.portablepocketcabin.CabinUpgradeCatalog;
import dev.portablepocketcabin.CabinUpgradeDefinitions;
import dev.portablepocketcabin.CabinUpgradeState;
import dev.portablepocketcabin.WorldAttunement;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class CabinGreenhouseGameTest {

    @GameTest
    public void sizesPatternsCostsAndLegacyState(GameTestHelper helper) {
        var registry = new CabinRegistry();
        var owner = UUID.randomUUID();
        var cabin = registry.create(owner);
        registry.beginDeployment(
            cabin.uuid(),
            owner,
            new CabinExterior(
                Level.OVERWORLD,
                new BlockPos(0, 100, 0),
                Direction.NORTH
            )
        );
        registry.markInteriorGenerated(cabin.uuid());
        registry.finishDeployment(cabin.uuid());
        cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 3, 5, 21);
        helper.assertTrue(
            CabinUpgradeCatalog.groups(
                cabin,
                new WorldAttunement(
                    1,
                    CabinPalette.DEFAULT.walls().profileId()
                ),
                CabinUpgradeDefinitions.current()
            )
                .stream()
                .noneMatch(g -> g.title().equals("Greenhouse")),
            "A book must reveal greenhouse purchases"
        );
        registry.updateUpgradeState(
            cabin.uuid(),
            cabin.upgrades().withGreenhouse(CabinGreenhouseState.EMPTY.reveal())
        );
        var ops = helper
            .getLevel()
            .registryAccess()
            .createSerializationContext(NbtOps.INSTANCE);
        Map<
            BlockPos,
            net.minecraft.world.level.block.state.BlockState
        > previous = Map.of();
        int[] spots = { 24, 48, 112, 200 };
        for (int level = 1; level <= 4; level++) {
            cabin = registry.find(cabin.uuid()).orElseThrow();
            var defaults = CabinGreenhouse.defaults(cabin, level);
            helper.assertTrue(
                defaults
                    .values()
                    .stream()
                    .filter(state -> state.is(Blocks.FARMLAND))
                    .count() == spots[level - 1],
                "Exact planting capacity"
            );
            for (var entry : previous.entrySet())
                if (
                    entry.getValue().is(Blocks.FARMLAND) ||
                    entry.getValue().is(Blocks.STONE_BRICK_SLAB)
                ) helper.assertTrue(
                    defaults.get(entry.getKey()).equals(entry.getValue()),
                    "Beds and paths remain beds and paths"
                );
            previous = defaults;
            var requirements = CabinUpgradeCatalog.greenhouseRequirements(
                level
            );
            helper.assertTrue(
                requirements.size() == level + 4 &&
                    requirements.get(0).count() ==
                        new int[] { 16, 32, 48, 64 }[level - 1],
                "Glass and botanical costs match every purchase"
            );
            var offer = CabinUpgradeCatalog.offer(
                cabin,
                CabinUpgradeState.Target.greenhouse(level),
                new WorldAttunement(
                    1,
                    CabinPalette.DEFAULT.walls().profileId()
                ),
                CabinUpgradeDefinitions.current()
            ).orElseThrow();
            helper.assertTrue(
                offer.targetSize() == level,
                "Only the next size is offered"
            );
            var stacks = requirements
                .stream()
                .map(r ->
                    new ItemStack(
                        BuiltInRegistries.ITEM.getOptional(
                            r.itemId()
                        ).orElseThrow(),
                        r.count()
                    )
                )
                .toList();
            var install = new CabinUpgradeState.Installation(
                UUID.randomUUID(),
                offer.target(),
                level - 1,
                level
            );
            registry.updateUpgradeState(
                cabin.uuid(),
                cabin
                    .upgrades()
                    .withFund(
                        new CabinUpgradeState.Fund(
                            offer.target(),
                            requirements,
                            stacks
                        )
                    )
                    .withInstallation(install)
            );
            registry = CabinRegistry.CODEC.parse(
                ops,
                CabinRegistry.CODEC.encodeStart(ops, registry).getOrThrow()
            ).getOrThrow();
            cabin = registry.completeUpgradeInstallation(
                cabin.uuid(),
                install.operationId()
            );
            helper.assertTrue(
                cabin.progression().rooms().size() == 1 &&
                    cabin.upgrades().greenhouse().level() == level,
                "Restart preserves one room and consumes one purchase"
            );
            var space = CabinGreenhouse.space(cabin, level);
            helper.assertTrue(
                space.maximum().getY() - 1 == CabinGreenhouse.HEIGHTS[level],
                "Clear height excludes the gardening layer"
            );
        }
        helper.assertTrue(
            CabinIngredientFallbacks.resolve(
                Identifier.parse("biomesoplenty:lavender")
            ).equals(Identifier.withDefaultNamespace("allium")),
            "Missing botanical mods use the shared vanilla fallback"
        );
        var tag =
            (net.minecraft.nbt.CompoundTag) CabinUpgradeState.CODEC.encodeStart(
                ops,
                cabin.upgrades()
            ).getOrThrow();
        tag.remove("greenhouse");
        helper.assertTrue(
            CabinUpgradeState.CODEC.parse(ops, tag)
                .getOrThrow()
                .greenhouse()
                .equals(CabinGreenhouseState.EMPTY),
            "Old saves need no greenhouse field"
        );
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void catchUpRespectsMaturityShadeAndElapsedTime(
        GameTestHelper helper
    ) {
        var level = helper.getLevel();
        BlockPos lit = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos shaded = lit.east(3);
        level.setBlock(
            lit.below(),
            Blocks.FARMLAND.defaultBlockState().setValue(
                FarmlandBlock.MOISTURE,
                7
            ),
            CabinCorridorSnapshot.FLAGS
        );
        level.setBlock(
            lit,
            Blocks.WHEAT.defaultBlockState(),
            CabinCorridorSnapshot.FLAGS
        );
        level.setBlock(
            shaded.below(),
            Blocks.FARMLAND.defaultBlockState(),
            CabinCorridorSnapshot.FLAGS
        );
        level.setBlock(
            shaded,
            Blocks.WHEAT.defaultBlockState(),
            CabinCorridorSnapshot.FLAGS
        );
        for (var pos : BlockPos.betweenClosed(
            shaded.offset(-1, -2, -1),
            shaded.offset(1, 2, 1)
        ))
            if (
                !pos.equals(shaded) &&
                !pos.equals(shaded.above()) &&
                !pos.equals(shaded.below())
            ) level.setBlock(
                pos,
                Blocks.STONE.defaultBlockState(),
                CabinCorridorSnapshot.FLAGS
            );
        helper.runAfterDelay(20, () -> {
            level.setBlock(
                lit,
                Blocks.WHEAT.defaultBlockState(),
                CabinCorridorSnapshot.FLAGS
            );
            CabinGreenhouseGrowth.catchUp(level, Set.of(lit), 0, 42);
            helper.assertTrue(
                level.getBlockState(lit).getValue(CropBlock.AGE) == 0,
                "No elapsed time means no growth"
            );
            CabinGreenhouseGrowth.catchUp(level, Set.of(lit), 4096L * 8192, 42);
            helper.assertTrue(
                level.getBlockState(lit).getValue(CropBlock.AGE) == 7,
                "Catch-up stops at mature wheat"
            );
            helper.assertTrue(
                !CabinGreenhouseGrowth.canProgress(level.getBlockState(lit)),
                "Mature crops cannot harvest or replant"
            );
            level.setBlock(
                shaded,
                Blocks.WHEAT.defaultBlockState(),
                CabinCorridorSnapshot.FLAGS
            );
            helper.assertTrue(
                level.getRawBrightness(shaded, 0) < 9,
                "The shade fixture blocks crop lighting"
            );
            CabinGreenhouseGrowth.catchUp(
                level,
                Set.of(shaded),
                4096L * 8192,
                42
            );
            helper.assertTrue(
                level.getBlockState(shaded).getValue(CropBlock.AGE) == 0,
                "Shaded crops cannot catch up"
            );
            helper.succeed();
        });
    }
}
