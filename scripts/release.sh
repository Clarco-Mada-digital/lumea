#!/usr/bin/env bash
#
# Publie une nouvelle version de Lumea.
#
#   ./scripts/release.sh 1.1.0
#
# Le script met à jour la version, commite, pose le tag et le pousse. C'est le
# tag qui déclenche GitHub Actions : le build signé et la publication de l'APK
# se font là-bas, pas ici. Rien n'est poussé avant que tout soit vérifié en
# local — un tag poussé se rattrape mal.

set -euo pipefail

cd "$(dirname "$0")/.."

rouge() { printf '\033[31m%s\033[0m\n' "$1"; }
vert()  { printf '\033[32m%s\033[0m\n' "$1"; }
gris()  { printf '\033[90m%s\033[0m\n' "$1"; }

VERSION="${1:-}"
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  rouge "Version attendue au format X.Y.Z — reçu : « ${VERSION:-rien} »"
  echo "Exemple : ./scripts/release.sh 1.1.0"
  exit 1
fi

# --- L'arbre doit être propre : on publie ce qui est commité, rien d'autre.
if [[ -n "$(git status --porcelain)" ]]; then
  rouge "Des modifications ne sont pas commitées."
  git status --short
  exit 1
fi

if git rev-parse "v$VERSION" >/dev/null 2>&1; then
  rouge "Le tag v$VERSION existe déjà."
  exit 1
fi

# --- Le code de version s'incrémente tout seul : deux APK ne peuvent pas
#     porter le même, sinon le Play Store refuse la mise à jour.
CURRENT_CODE=$(grep -oP '(?<=versionCode = )\d+' app/build.gradle.kts)
NEXT_CODE=$((CURRENT_CODE + 1))

gris "Version    : $VERSION"
gris "versionCode: $CURRENT_CODE → $NEXT_CODE"
echo

# --- Vérifications locales avant de graver quoi que ce soit.
vert "→ Tests unitaires"
./gradlew testDebugUnitTest --quiet

vert "→ Document médical à jour"
./gradlew testDebugUnitTest --tests '*MedicalContentExportTest*' --quiet
if [[ -n "$(git status --porcelain -- docs/CONTENU-MEDICAL.md)" ]]; then
  rouge "Le contenu médical a changé sans que le document soit régénéré."
  echo "Il vient de l'être : relis-le, commite-le, puis relance."
  exit 1
fi

vert "→ Build release"
./gradlew assembleRelease --quiet

echo
read -r -p "Publier la version $VERSION ? [o/N] " reponse
[[ "$reponse" =~ ^[oOyY]$ ]] || { gris "Annulé."; exit 0; }

# --- Version, commit, tag.
sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEXT_CODE/" app/build.gradle.kts
sed -i "s/versionName = \"[^\"]*\"/versionName = \"$VERSION\"/" app/build.gradle.kts

git add app/build.gradle.kts
git commit -q -m "Version $VERSION"
git tag -a "v$VERSION" -m "Lumea $VERSION"

git push -q origin HEAD
git push -q origin "v$VERSION"

echo
vert "Version $VERSION publiée."
gris "GitHub Actions construit l'APK signé et crée la release :"
gris "https://github.com/Clarco-Mada-digital/lumea/actions"
