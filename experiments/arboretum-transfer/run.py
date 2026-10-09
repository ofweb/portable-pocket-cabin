#!/usr/bin/env python3
"""Run the arboretum transfer experiment in a disposable project."""
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
    parser.add_argument("--output", type=Path, help="New disposable project directory")
    parser.add_argument("--cache-source", type=Path, default=Path.home() / ".gradle")
    args = parser.parse_args()
    artifact = Path(__file__).resolve().parent
    project = artifact.parent.parent
    target = args.output.resolve() if args.output else Path(tempfile.mkdtemp(prefix="ppc-arboretum-transfer-"))
    if args.output:
        target.mkdir(parents=True, exist_ok=False)
    print(f"Disposable project: {target}", flush=True)
    for name in ("src", "gradle", ".gradle"):
        subprocess.run(["cp", "-a", "--reflink=auto", str(project / name), str(target)], check=True)
    for name in ("build.gradle", "settings.gradle", "gradle.properties", "gradlew"):
        shutil.copy2(project / name, target / name)
    cache = target / "gradle-user-home"
    cache.mkdir()
    for name in ("caches", "wrapper"):
        subprocess.run(["cp", "-a", "--reflink=auto", str(args.cache_source / name), str(cache)], check=True)
    package = target / "src/gametest/java/dev/portablepocketcabin"
    for path in artifact.glob("*.java"):
        shutil.copy2(path, package / path.name)
    resources = target / "src/gametest/resources"
    manifest = resources / "fabric.mod.json"
    data = json.loads(manifest.read_text())
    data["entrypoints"] = {"fabric-client-gametest": ["dev.portablepocketcabin.ArboretumTransferTest"]}
    data["mixins"] = ["arboretum-transfer.mixins.json"]
    manifest.write_text(json.dumps(data, indent=2) + "\n")
    shutil.copy2(artifact / "arboretum-transfer.mixins.json", resources)
    environment = os.environ.copy()
    environment["GRADLE_USER_HOME"] = str(cache)
    environment["ARBORETUM_EVIDENCE"] = str(target / "evidence")
    if environment.get("DISPLAY"):
        environment.pop("WAYLAND_DISPLAY", None)
    log = target / "client-output.txt"
    print(f"Running client experiment; log: {log}", flush=True)
    with log.open("w") as output:
        process = subprocess.Popen(["./gradlew", "--offline", "--no-daemon", "runClientGameTest"], cwd=target,
                                   env=environment, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=300)
        except (subprocess.TimeoutExpired, KeyboardInterrupt):
            os.killpg(process.pid, signal.SIGTERM)
            process.wait(timeout=10)
            raise
    if code:
        print(log.read_text()[-14000:], flush=True)
        raise SystemExit(code)
    report = target / "evidence/results.json"
    if not report.exists():
        raise RuntimeError("Client runner did not produce transfer evidence")
    data = json.loads(report.read_text())
    for case in data["cases"]:
        first = case["frames"][0]
        case["criteria"] = {
            "destination_ready_at_crossing": case["chunk_ready_at_packet"] and case["floor_ready_at_packet"] and first["passage_mesh_ready"],
            "motion_continuous_at_packet": all(abs(case[key]) < 1e-6 for key in (
                "position_translation_error", "interpolation_translation_error", "velocity_error", "yaw_error", "pitch_error")),
            "no_loading_screen_in_sampled_frames": all(frame["screen"] == "none" for frame in case["frames"]),
            "no_later_movement_correction": case["subsequent_position_packets"] == 0 and all(
                tick["step_along_walk"] >= -.01 for tick in case["ticks"]),
        }
    data["findings"] = {
        "relative_translation_preserves_motion_both_directions": all(case["criteria"]["motion_continuous_at_packet"]
            for case in data["cases"] if "relative-translation" in case["name"]),
        "server_preloading_proves_client_readiness": all(case["criteria"]["destination_ready_at_crossing"]
            for case in data["cases"] if abs(case["offset"]) == 4096),
        "actor_visible_across_transfer": all(stage["actor_visible"] for stage in data["multiplayer"]["stages"]),
    }
    data["findings"]["simple_transfer_meets_seamless_requirement"] = all(data["findings"].values())
    data["source_sha256"] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                             for p in artifact.iterdir() if p.suffix in (".java", ".py", ".json") and p.name != "results.json"}
    report.write_text(json.dumps(data, indent=2) + "\n")
    print(f"Experiment complete; evidence: {report}", flush=True)
    print(json.dumps(data["findings"], indent=2), flush=True)


if __name__ == "__main__":
    main()
