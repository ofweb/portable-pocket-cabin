prism_instance := env_var_or_default("PRISM_INSTANCE", "portable-pocket-cabin-dev")
server_port := env_var_or_default("PPC_PORT", "25565")
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
