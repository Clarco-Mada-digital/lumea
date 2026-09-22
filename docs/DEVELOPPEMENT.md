# Lumea — développement et publication

## Mettre en route

```bash
git clone https://github.com/Clarco-Mada-digital/lumea.git
cd lumea
./gradlew assembleDebug
```

Le fichier `local.properties` est créé par Android Studio ; il n'est pas dans le
dépôt.

## Vérifier

```bash
./gradlew testDebugUnitTest          # 62 tests, sans appareil
./gradlew connectedDebugAndroidTest  # 12 tests, téléphone branché requis
```

Les tests sur appareil couvrent les migrations de base et les composants
d'interface. Ils ne tournent pas en intégration continue : ils demandent un
téléphone, et un émulateur multiplierait le temps de build sans gain réel.

## Publier une version

```bash
./scripts/release.sh 1.1.0
```

Le script vérifie l'arbre de travail, lance les tests, régénère le document
médical, construit la release, demande confirmation, puis pose et pousse le tag.
C'est le tag qui déclenche GitHub Actions : **le build publié est construit par
l'action, pas sur la machine de dev.**

### Secrets à configurer une seule fois

Dans le dépôt GitHub, `Settings → Secrets and variables → Actions` :

| Secret | Contenu |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 lumea-release.jks` |
| `KEYSTORE_PASSWORD` | mot de passe du keystore |
| `KEY_ALIAS` | `lumea` |
| `KEY_PASSWORD` | mot de passe de la clé |

```bash
base64 -w0 lumea-release.jks | xclip -selection clipboard
```

Sans ces secrets, l'action **échoue volontairement**. Un APK signé avec la clé de
debug ne pourrait jamais être mis à jour par un APK signé correctement : les
utilisatrices devraient désinstaller et perdraient leurs données.

### ⚠️ La clé de signature

`lumea-release.jks` et `keystore.properties` ne sont pas dans le dépôt, et ne
doivent jamais y entrer. **Ils n'existent que sur la machine de développement.**

Perdre ce fichier signifie ne plus jamais pouvoir mettre à jour l'application :
il faudrait republier sous un autre nom de paquet, et toutes les utilisatrices
perdraient leurs données. Garde une copie chiffrée ailleurs — clé USB, coffre de
mots de passe, autre machine.

Empreinte attendue de la clé :

```
SHA-256: 77:88:4D:85:B9:3C:21:B6:56:AB:D5:15:B1:30:93:B0:D9:6C:07:B8:F1:E3:04:BE:67:C6:33:71:95:C8:A3:DC
```

## Le contenu médical

Tout ce que l'app affiche en matière de santé est rassemblé dans
[`docs/CONTENU-MEDICAL.md`](CONTENU-MEDICAL.md), généré depuis le code :

```bash
./gradlew testDebugUnitTest --tests '*MedicalContentExportTest*'
```

Il ne se modifie pas à la main — les corrections vont dans les sources
(`domain/pregnancy/`, `domain/advisor/`, `domain/learn/`), puis le document se
régénère. L'intégration continue refuse une poussée si le contenu a changé sans
que le document suive.

**Ce document doit être relu par une sage-femme ou un médecin malgache avant
toute diffusion.** C'est la seule condition de publication qui ne soit pas
technique.

## Organisation du code

```
data/          base chiffrée (Room + SQLCipher), réglages (DataStore), sauvegarde
domain/        règles métier, sans Android
  cycle/       moteur de cycle, prévisions, niveaux de risque
  pregnancy/   suivi de grossesse et d'après-naissance, contenu malgache
  advisor/     conseiller local : reconnaissance de sujet et réponses
  learn/       leçons éducatives
ui/            écrans Compose, un dossier par domaine
widget/        widget d'écran d'accueil
export/        génération du carnet en PDF
notif/         rappels et vérification quotidienne
```

Le domaine ne dépend pas d'Android : c'est ce qui rend les 62 tests unitaires
possibles sans appareil.
