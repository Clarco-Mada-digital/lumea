#!/usr/bin/env bash
# Boucle de développement : compile la variante debug, l'installe sur le
# téléphone branché, la lance, et affiche les erreurs en direct.
#
#   ./dev.sh          compile, installe, lance, suit les logs
#   ./dev.sh -q       compile, installe, lance, sans suivre les logs
#   ./dev.sh --logs   suit les logs d'une app déjà installée
#
# Bien plus rapide qu'un build release : ni R8, ni lint, ni shrink des ressources.
set -euo pipefail

cd "$(dirname "$0")"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$PATH:$ANDROID_HOME/platform-tools"

PKG="net.mada.lumea.debug"
ACT="net.mada.lumea.MainActivity"

follow_logs() {
    echo "--- logs (Ctrl+C pour arrêter) ---"
    local pid
    pid="$(adb shell pidof "$PKG" | tr -d '\r')"
    if [ -n "$pid" ]; then
        adb logcat --pid="$pid" -v brief \
            AndroidRuntime:E System.err:W Lumea:D StrictMode:W '*:S'
    else
        adb logcat -v brief AndroidRuntime:E '*:S'
    fi
}

if [ "${1:-}" = "--logs" ]; then
    follow_logs
    exit 0
fi

if ! adb get-state >/dev/null 2>&1; then
    echo "Aucun téléphone détecté. Branche-le et active le débogage USB." >&2
    exit 1
fi

echo "--- compilation (debug) ---"
./gradlew :app:installDebug

echo "--- lancement ---"
# --user 0 : sur les Samsung avec Dossier sécurisé, le shell vise sinon un autre profil.
adb shell am force-stop "$PKG" || true
adb shell am start --user 0 -n "$PKG/$ACT" >/dev/null

[ "${1:-}" = "-q" ] && exit 0
sleep 2
follow_logs
