#!/usr/bin/env python3
"""Verify private runtime dimensions in a disposable copy of the project."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    artifact = Path(__file__).resolve().parent
    project = artifact.parent.parent
    target = args.output.resolve() if args.output else Path(tempfile.mkdtemp(prefix="ppc-arboretum-worlds-"))
    if args.output:
        target.mkdir(parents=True, exist_ok=False)
    print(f"Disposable project: {target}", flush=True)
    for name in ("src", "gradle", ".gradle"):
        subprocess.run(["cp", "-a", "--reflink=auto", str(project / name), str(target)], check=True)
    for name in ("build.gradle", "settings.gradle", "gradle.properties", "gradlew"):
        shutil.copy2(project / name, target / name)
    package = target / "src/main/java/dev/portablepocketcabin"
    for name in ("ArboretumWorldHost.java", "ArboretumWorldServerAccess.java"):
        shutil.copy2(artifact / name, package / name)
    initializer = package / "PortablePocketCabin.java"
    initializer.write_text(initializer.read_text().replace("CabinItems.register();", "ArboretumWorldHost.register();\n\t\tCabinItems.register();"))
    resources = target / "src/main/resources"
    (resources / "arboretum-worlds.mixins.json").write_text(json.dumps({
        "required": True, "package": "dev.portablepocketcabin.worldtest", "compatibilityLevel": "JAVA_25",
        "mixins": ["ArboretumWorldServerAccess"], "injectors": {"defaultRequire": 1}}))
    manifest = resources / "fabric.mod.json"
    data = json.loads(manifest.read_text())
    data["mixins"].append("arboretum-worlds.mixins.json")
    manifest.write_text(json.dumps(data, indent=2))
    tests = target / "src/gametest/java/dev/portablepocketcabin"
    previous = project / "experiments/arboretum-transfer"
    for name in ("ArboretumTransferProbe.java", "ArboretumHiddenWindowMixin.java"):
        shutil.copy2(previous / name, tests / name)
    shutil.copy2(artifact / "ArboretumWorldTest.java", tests / "ArboretumWorldTest.java")
    text = (previous / "ArboretumMultiplayerTest.java").read_text()
    text = text.replace("ArboretumTransferTest.build(level, SOURCE);", "ArboretumWorldHost.platform(level, SOURCE, net.minecraft.world.level.block.Blocks.OAK_PLANKS);")
    text = text.replace("ArboretumTransferTest.build(level, FAR);", """var a = ArboretumWorldHost.load(instance, ArboretumWorldHost.A);
                var b = ArboretumWorldHost.load(instance, ArboretumWorldHost.B);
                ArboretumWorldHost.platform(a, FAR, net.minecraft.world.level.block.Blocks.OAK_PLANKS);
                ArboretumWorldHost.platform(b, FAR, net.minecraft.world.level.block.Blocks.BIRCH_PLANKS);
                if (!a.getBlockState(FAR).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS)
                    || !b.getBlockState(FAR).is(net.minecraft.world.level.block.Blocks.BIRCH_PLANKS))
                    throw new AssertionError("Worlds share terrain");
                for (var destination : java.util.Map.of(a, FAR, level, SOURCE).entrySet()) {
                    var blocked = destination.getValue().offset(-3, 1, 4);
                    destination.getKey().setBlockAndUpdate(blocked, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    var safe = SafeDestinationResolver.search(destination.getKey(), blocked,
                        connection.getServerPlayer()).orElseThrow();
                    if (safe.level() != destination.getKey() || safe.feet().equals(blocked)
                        || !safe.teleport(connection.getServerPlayer(), 90))
                        throw new AssertionError("Blocked arrival did not use a safe nearby destination");
                    destination.getKey().setBlockAndUpdate(blocked, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }""")
    text = text.replace("instance.getLevel(PocketDimension.LEVEL_KEY), center.getX()",
        "instance.getLevel(center.equals(FAR) ? ArboretumWorldHost.A : PocketDimension.LEVEL_KEY), center.getX()")
    text = text.replace('data.put("phase", name);', 'data.put("phase", name);\n                data.put("dimension", client.level.dimension().identifier().toString());')
    text = text.replace('moveActor(server, connection, SOURCE);', '''server.runOnServer(instance -> connection.getServerPlayer().teleportTo(
                instance.getLevel(ArboretumWorldHost.B), FAR.getX() - 2.5, 64, FAR.getZ() + 4.5, Set.of(), 90, 0, false));
            stages.add(stage(context, observer, "actor-second-world", false));
            server.runOnServer(instance -> instance.getPlayerList().getPlayers().stream()
                .filter(p -> !p.getUUID().equals(actor)).findFirst().orElseThrow().teleportTo(
                    instance.getLevel(ArboretumWorldHost.B), FAR.getX() - 5.5, 64, FAR.getZ() + 4.5, Set.of(), -90, 0, false));
            stages.add(stage(context, observer, "together-second-world", true));
            moveActor(server, connection, SOURCE);''')
    (tests / "ArboretumMultiplayerTest.java").write_text(text)
    test_resources = target / "src/gametest/resources"
    manifest = test_resources / "fabric.mod.json"
    data = json.loads(manifest.read_text())
    data["entrypoints"] = {"fabric-client-gametest": ["dev.portablepocketcabin.ArboretumWorldTest"]}
    data["mixins"] = ["arboretum-hidden.mixins.json"]
    manifest.write_text(json.dumps(data, indent=2))
    (test_resources / "arboretum-hidden.mixins.json").write_text(json.dumps({
        "required": True, "package": "dev.portablepocketcabin.transfer.mixin", "compatibilityLevel": "JAVA_25",
        "client": ["ArboretumHiddenWindowMixin"], "injectors": {"defaultRequire": 1}}))
    run = target / "run"
    run.mkdir(parents=True)
    (run / "eula.txt").write_text("eula=true\n")
    (run / "server.properties").write_text("server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\n")
    environment = os.environ.copy()
    environment.pop("WAYLAND_DISPLAY", None)
    environment["ARBORETUM_EVIDENCE"] = str(target / "evidence")
    for task, phase, marker in (
        ("runServer", "seed", "ARBORETUM_WORLD_SEED_PASSED"),
        ("runServer", "verify", "ARBORETUM_WORLD_RESTART_PASSED"),
        ("runClientGameTest", "", "ARBORETUM_WORLD_CLIENT_PASSED"),
    ):
        if phase:
            environment["ARBORETUM_WORLD_PHASE"] = phase
        else:
            environment.pop("ARBORETUM_WORLD_PHASE", None)
        log = target / f"{task}-{phase or 'client'}.log"
        print(f"Running {task}; log: {log}", flush=True)
        with log.open("w") as output:
            command = ["./gradlew", "--offline", "--no-daemon", task]
            if phase:
                command.append("--args=nogui")
            process = subprocess.Popen(command, cwd=target, env=environment, stdout=output,
                stderr=subprocess.STDOUT, start_new_session=True)
            try:
                code = process.wait(timeout=360)
            except (subprocess.TimeoutExpired, KeyboardInterrupt):
                os.killpg(process.pid, signal.SIGTERM)
                process.wait(timeout=10)
                raise
        if code or marker not in log.read_text():
            print(log.read_text()[-9000:], flush=True)
            raise SystemExit(code or 1)
        print(marker, flush=True)
    report = target / "evidence/results.json"
    results = json.loads(report.read_text())
    results["server_checks"] = {"isolated_save_paths": True, "unload_reload": True,
        "restart_terrain_and_chest_inventory": True}
    results["blocked_arrival_search_both_directions"] = True
    results["source_sha256"] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
        for p in artifact.iterdir() if p.suffix in (".java", ".py")}
    results["reused_source_sha256"] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
        for p in previous.iterdir() if p.name in ("ArboretumMultiplayerTest.java",
            "ArboretumTransferProbe.java", "ArboretumHiddenWindowMixin.java")}
    report.write_text(json.dumps(results, indent=2) + "\n")


if __name__ == "__main__":
    main()
