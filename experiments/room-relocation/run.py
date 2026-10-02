#!/usr/bin/env python3
"""Run the relocation spike in a disposable copy of this repository."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, help="New directory for the disposable project and logs")
    parser.add_argument("--cache-source", type=Path, default=Path.home() / ".gradle")
    args = parser.parse_args()
    artifact = Path(__file__).resolve().parent
    project = artifact.parent.parent
    target = args.output.resolve() if args.output else Path(tempfile.mkdtemp(prefix="ppc-room-relocation-"))
    if args.output:
        target.mkdir(parents=True, exist_ok=False)
    print(f"Disposable project: {target}", flush=True)
    for name in ("src", "gradle", ".gradle"):
        if (project / name).exists():
            subprocess.run(["cp", "-a", "--reflink=auto", str(project / name), str(target)], check=True)
    for name in ("build.gradle", "settings.gradle", "gradle.properties", "gradlew"):
        shutil.copy2(project / name, target / name)
    cache = target / "gradle-user-home"
    cache.mkdir()
    for name in ("caches", "wrapper"):
        subprocess.run(["cp", "-a", "--reflink=auto", str(args.cache_source / name), str(cache)], check=True)
    source = target / "src"
    package = "java/dev/portablepocketcabin"
    for name, kind in (("RoomRelocationSpike.java", "main"), ("RoomRelocationRestartSpike.java", "main"), ("RoomRelocationSpikeTest.java", "gametest")):
        shutil.copy2(artifact / name, source / kind / package / name)
    manifest = source / "gametest/resources/fabric.mod.json"
    data = json.loads(manifest.read_text())
    data["entrypoints"]["fabric-gametest"] = ["dev.portablepocketcabin.RoomRelocationSpikeTest"]
    manifest.write_text(json.dumps(data, indent=2) + "\n")
    startup = source / "main" / package / "DedicatedServerStartupCheck.java"
    text = startup.read_text()
    marker = "static void onServerStarted(MinecraftServer server) {"
    if text.count(marker) != 1:
        raise RuntimeError("Startup hook changed; adapt the isolated runner before proceeding")
    startup.write_text(text.replace(marker, marker + '\n        if (Boolean.getBoolean("portable-pocket-cabin.relocation-restart")) { RoomRelocationRestartSpike.start(server); return; }'))
    build = target / "build.gradle"
    build.write_text(build.read_text() + '\nloom.runs.startupTest.vmArg "-Dportable-pocket-cabin.relocation-restart=true"\nloom.runs.startupReloadTest.vmArg "-Dportable-pocket-cabin.relocation-restart=true"\n')
    environment = os.environ.copy()
    environment["GRADLE_USER_HOME"] = str(cache)
    for task, log_name in (("runGameTest", "gametest-output.txt"), ("runStartupReloadTest", "restart-output.txt")):
        print(f"Running {task}; log: {target / log_name}", flush=True)
        with (target / log_name).open("w") as output:
            subprocess.run(["./gradlew", "--offline", "--no-daemon", task], cwd=target, env=environment, stdout=output, stderr=subprocess.STDOUT, check=True)
    restart = (target / "restart-output.txt").read_text()
    if "RELOCATION_ABRUPT_RESTART_RECOVERY_PASSED" not in restart:
        raise RuntimeError("Missing restart recovery success marker")
    print("Relocation and abrupt-restart recovery checks passed.")


if __name__ == "__main__":
    main()
