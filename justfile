prism_instance := env_var_or_default("PRISM_INSTANCE", "portable-pocket-cabin-dev")
server_port := env_var_or_default("PPC_PORT", "25566")
server_address := env_var_or_default("PPC_SERVER", "localhost:" + server_port)

# Show the available development commands.
default:
    @just --list

# Build the remapped development mod without running the full server test suite.
build:
    ./gradlew jar

# Run GameTests and the dedicated-server startup test.
test:
    ./gradlew build

# Run client GameTests and capture upgrade UI screenshots.
test-client:
    ./gradlew runClientGameTest

# Rebuild every cabin and vanilla reference screenshot in screenshots/.
screenshots:
    #!/usr/bin/env bash
    set -euo pipefail
    runner=()
    if [[ -z "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; then
        if ! command -v xvfb-run >/dev/null; then
            echo "Screenshot capture needs a graphical display or xvfb-run." >&2
            exit 1
        fi
        runner=(xvfb-run -a)
    fi
    PPC_CAPTURE_ALL=1 "${runner[@]}" ./gradlew runClientGameTest
    python3 scripts/collect-screenshots.py

# Verify that the local development server cannot naturally spawn mobs.
test-dev-config:
    #!/usr/bin/env bash
    set -euo pipefail
    default_compose_config="$(env -u PPC_PORT docker compose config)"
    override_compose_config="$(PPC_PORT=25567 docker compose config)"
    default_server_port="$(env -u PPC_PORT just --evaluate server_port)"
    override_server_port="$(PPC_PORT=25567 just --evaluate server_port)"
    grep -Fq 'DIFFICULTY: peaceful' <<<"$default_compose_config"
    grep -Fq 'SPAWN_ANIMALS: "false"' <<<"$default_compose_config"
    grep -Fq 'SPAWN_MONSTERS: "false"' <<<"$default_compose_config"
    grep -Fq 'SPAWN_NPCS: "false"' <<<"$default_compose_config"
    grep -Fq 'gamerule spawn_mobs false' <<<"$default_compose_config"
    grep -Fq 'published: "25566"' <<<"$default_compose_config"
    grep -Fq 'published: "25567"' <<<"$override_compose_config"
    [[ "$default_server_port" == "25566" ]]
    [[ "$override_server_port" == "25567" ]]

# Stage exactly one local mod jar for Docker and Prism.
stage: build
    #!/usr/bin/env bash
    set -euo pipefail
    mod_version="$(sed -n 's/^mod_version=//p' gradle.properties)"
    source_jar="build/libs/portable-pocket-cabin-${mod_version}.jar"
    test -f "$source_jar"
    mkdir -p build/dev-mods
    install -m 0644 "$source_jar" build/dev-mods/portable-pocket-cabin.jar

# Build and start the Fabric server in the background.
server: stage
    # Keep the bind-mount source owned by the host user instead of Docker.
    mkdir -p run/server
    PPC_PORT="{{ server_port }}" docker compose up -d --wait
    @echo "Server ready at {{ server_address }}"

# Build, sync into a clean Fabric 26.2 Prism instance, and connect to the server.
client instance=prism_instance: stage
    #!/usr/bin/env bash
    set -euo pipefail
    instance_id='{{ instance }}'
    data_root="${XDG_DATA_HOME:-$HOME/.local/share}"
    prism_root="${PRISM_ROOT:-$data_root/PrismLauncher}"
    instance_dir="${PRISM_INSTANCE_DIR:-$prism_root/instances/$instance_id}"
    pack_file="$instance_dir/mmc-pack.json"
    mods_dir="$instance_dir/minecraft/mods"
    if [[ ! -f "$pack_file" ]]; then
        echo "Prism instance '$instance_id' was not found at $instance_dir" >&2
        echo "Create a Fabric 26.2 instance with that ID, or set PRISM_INSTANCE_DIR." >&2
        exit 1
    fi
    if ! grep -q 'net.fabricmc.fabric-loader' "$pack_file"; then
        echo "Prism instance '$instance_id' is not configured for Fabric." >&2
        exit 1
    fi
    mkdir -p "$mods_dir"
    if ! compgen -G "$mods_dir/fabric-api-*.jar" >/dev/null; then
        shopt -s nullglob
        server_api_jars=(run/server/mods/fabric-api-*.jar)
        if [[ ${#server_api_jars[@]} -ne 1 ]]; then
            echo "Fabric API is unavailable; run 'just server' once, then retry." >&2
            exit 1
        fi
        install -m 0644 "${server_api_jars[0]}" "$mods_dir/${server_api_jars[0]##*/}"
    fi
    install -m 0644 build/dev-mods/portable-pocket-cabin.jar "$mods_dir/portable-pocket-cabin.jar"
    "${PRISM_BIN:-prismlauncher}" --launch "$instance_id" --server '{{ server_address }}'

# Start the server, then launch Prism and connect.
dev: server client

# Archive the current development world and start a fresh one.
fresh-world:
    #!/usr/bin/env bash
    set -euo pipefail
    docker compose down
    current_world="run/server/world"
    if [[ -d "$current_world" ]]; then
        backup_root="run/world-backups"
        timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
        archived_world="$backup_root/world-$timestamp"
        if [[ -e "$archived_world" ]]; then
            echo "Refusing to overwrite existing world backup: $archived_world" >&2
            exit 1
        fi
        mkdir -p "$backup_root"
        mv -- "$current_world" "$archived_world"
        echo "Archived previous development world to $archived_world"
    else
        echo "No existing development world found; starting clean."
    fi
    just server

# Follow the Minecraft server logs.
logs:
    docker compose logs -f mc

# Open an RCON console command, e.g. `just cmd "cabin status"`.
cmd command:
    docker compose exec mc rcon-cli {{ quote(command) }}

# Grant a development player operator permissions.
op player:
    docker compose exec mc rcon-cli op {{ quote(player) }}

# Show container and health status.
status:
    docker compose ps

# Gracefully stop the server while preserving its world in run/server.
stop:
    docker compose down
