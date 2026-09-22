# Lumea

Application Android de notes, agenda, suivi de cycle, grossesse et journal intime,
pensée pour Madagascar. Tout ce qui touche à tes données est local et chiffré.

**Une seule chose sort du téléphone, et seulement si tu la déclenches** : l'écran
« Assistant IA », qui ouvre la page publique d'un service de chat extérieur pour
réviser ou discuter. Lumea ne lui envoie rien — ni journal, ni cycle, ni grossesse ;
seul ce que tu tapes toi-même dans la conversation part. C'est la raison de la
permission réseau, et l'app l'explique en détail avant le premier usage. Tout le
reste, y compris le conseiller du cycle, fonctionne hors ligne.

## Ce qu'elle fait

**Notes** — dossiers, tags, recherche plein texte, épinglage, favoris, couleurs,
mode liste à cocher, archives. Affichage en grille façon mur de post-its.
Toucher une note l'ouvre **en lecture**, pas en édition : on vient le plus souvent
pour relire. Les cases à cocher restent actionnables depuis la lecture.

**Agenda** — calendrier mensuel, événements avec lieu et notes, journée entière,
rappels par notification, récurrence (quotidienne à annuelle). Les occurrences
d'une série sont calculées à la volée, pas stockées.

**Cycle** — anneau de progression animé (jour du cycle, phases colorées),
calendrier peint par phase (règles enregistrées, règles prévues, fenêtre fertile,
ovulation), prévisions basées sur la moyenne des six derniers cycles, indicateur
de régularité, rappels avant les règles et l'ovulation.

**Assistant de cycle** — pour qui ne connaît pas ses chiffres : trois questions
concrètes (dernières règles, durée, règles précédentes) dont l'app déduit la
longueur du cycle, l'ovulation et les prévisions. « Je ne sais pas » est une
réponse acceptée à chaque étape.

**Journal** — carnet du jour partagé avec le cycle : flux, symptômes, humeur,
énergie, eau, sommeil, texte libre, gratitude. Habitudes à cocher avec séries.
Les journées passées s'ouvrent elles aussi en lecture.

**Grossesse** — test de grossesse expliqué (et pas proposé au premier jour de
retard), semaines d'aménorrhée, terme, carnet de suivi malgache : 8 CPN du modèle
OMS, VAT, TPIg dès 13 SA, fer et acide folique, déparasitage. Rendez-vous posés sur
le calendrier et programmables dans l'agenda. Coordonnées de la sage-femme, avec un
tri des symptômes qui dit quand partir au CSB et quand ne pas téléphoner.

**Après la naissance** — le suivi ne s'arrête pas à l'accouchement : consultations
postnatales, vaccins du bébé, et la contraception par allaitement (MAMA) présentée
condition par condition. Le retour de couches referme la boucle et relance le suivi
de cycle.

**Conseiller** — un moteur de règles local qui répond avec tes vraies données
(« tu es à 2 jours de retard, c'est courant » / « un saignement enceinte se fait
toujours voir »). Aucune donnée n'est envoyée, aucune réponse n'est générée : sur du
tri médical, mieux vaut un conseiller qui dit « je ne sais pas » qu'un qui improvise.

**Accueil** — un écran « Aujourd'hui » qui rassemble la phase du cycle, les
événements du jour, les habitudes et les notes récentes.

**Apparence** — thème clair/sombre/système, 5 palettes ou couleurs dynamiques,
4 tailles de texte, 3 styles de coins, halos colorés, animations et contraste
élevé, tous désactivables.

## Identité visuelle

Style géométrique moderne, décidé avec l'utilisateur plutôt que subi :

- **Polices embarquées** : Outfit (titres, chiffres) et Inter (texte courant),
  toutes deux en licence OFL, livrées dans l'APK. Sans elles, Android impose la
  police système et l'app change d'allure sur chaque téléphone — sur un Samsung,
  une écriture manuscrite sans rapport avec l'identité voulue.
- **Anneau de cycle** dessiné au Canvas, animé, avec les phases en arcs colorés.
- **Cartes héros** en dégradé sombre teinté par la palette : c'est ce qui permet
  aux couleurs vives de l'anneau de ressortir.
- **Barre de navigation flottante** en verre dépoli, le contenu défilant derrière.
- **Illustrations d'écran vide** tracées au Canvas : elles suivent le thème et ne
  pèsent rien dans l'APK.

## Confidentialité

- Base SQLite chiffrée par SQLCipher ; la clé est tirée au hasard à la première
  ouverture et rangée dans le Keystore matériel (`DatabaseKeyProvider`).
- Verrou par code PIN (PBKDF2, 120 000 itérations) et empreinte digitale
  optionnelle (`LockManager`). L'app se verrouille dès qu'elle passe en arrière-plan.
- **Verrou par élément** : une note ou une journée peut être marquée protégée. Elle
  n'affiche alors ni titre ni aperçu dans les listes, et demande le code à
  l'ouverture. Tant qu'elle est verrouillée, le bouton « Modifier » et la bascule
  du verrou disparaissent — sans quoi la protection se contournerait en un geste.
  Le déverrouillage ne vaut que pour la visite en cours.
- `FLAG_SECURE` disponible dans Réglages (« Masquer le contenu ») mais **désactivé
  par défaut** : il noircit aussi la recopie d'écran et les captures, ce qui donne
  l'impression que l'app est cassée — et on ne peut alors plus atteindre le réglage
  pour le désactiver. La vraie protection reste le code PIN.
- Sauvegardes système Android désactivées ; l'export se fait à la main, en JSON,
  vers l'emplacement choisi par l'utilisatrice. **Le fichier exporté n'est pas
  chiffré.**
- Aucune permission réseau n'est déclarée. L'APK final ne demande que :
  notifications, alarme exacte, démarrage, biométrie, vibreur.

## Architecture

Module unique, MVVM sans framework d'injection : un `AppContainer` paresseux
(`di/AppContainer.kt`) porte les dépendances, `containerViewModel { }` les passe
aux ViewModels.

```
data/db        entités Room, DAO, base chiffrée
data/prefs     réglages (DataStore)
data/security  clé de base, verrou PIN
data/repo      dépôts (notes, événements, cycle, journal)
domain/cycle   CycleEngine : phases et prévisions
domain/agenda  Recurrence : développement des séries
notif          AlarmManager, receivers, canaux
backup         export / import JSON
ui/theme       couleurs, dégradés, typographie, formes
ui/components  anneau de cycle, barre flottante, calendrier, illustrations
ui/…           un dossier par écran (Compose + ViewModel)
```

`CycleEngine` et `Recurrence` sont du Kotlin pur, sans dépendance Android : ce
sont les deux morceaux couverts par des tests unitaires (22 cas), et ceux qu'il
faut garder testés si le projet évolue.

## Compiler

Prérequis : JDK 17, SDK Android (plateforme 35, build-tools 35).

```bash
./dev.sh                     # compile, installe, lance, suit les logs
./gradlew assembleDebug      # APK de debug
./gradlew assembleRelease    # APK minifié
./gradlew testDebugUnitTest  # tests unitaires
```

`./dev.sh` est la boucle de développement : `installDebug` prend ~1 à 2 min là où
un build release en prend 15 à 20 (R8, lint, shrink des ressources).

`FLAG_SECURE` est désactivé dans les builds de debug : sinon la recopie d'écran
(scrcpy, Android Studio) n'affiche qu'un rectangle noir et développer à l'aveugle
devient impossible. Il reste actif en release.

L'APK sort dans `app/build/outputs/apk/` (≈16 Mo en release, ≈34 Mo en debug —
l'essentiel est le binaire natif SQLCipher, livré pour quatre architectures).
Installation :

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

`gradle.properties` est réglé pour une machine à ~4 Go de RAM : mémoire bridée et
compilation séquentielle. Sur une machine plus confortable, monter `Xmx` et
remettre `org.gradle.parallel=true` divise le temps de build par deux ou trois.

## Avant une publication

- `app/build.gradle.kts` signe la variante *release* avec la clé de debug pour
  que l'APK soit installable tel quel. Créer un vrai keystore avant le Play Store.
- Les icônes sont des vecteurs générés ici ; un vrai jeu d'icônes ferait la
  différence.
- Seule la couche domaine est testée. Les ViewModels et les écrans ne le sont pas.

## Limites connues

- Les prévisions de cycle sont indicatives. Ce n'est ni un avis médical, ni un
  moyen de contraception, et l'app le dit à l'écran.
- « Fait » n'est pas disponible sur les événements récurrents : cocher une
  occurrence cocherait toute la série.
- Si la permission d'alarme exacte est refusée (Android 12+), les rappels
  tombent dans une fenêtre de dix minutes au lieu d'être à la seconde.
- Un code PIN oublié est définitivement perdu : il n'y a pas de récupération.
- Le verrou par élément masque le contenu dans l'app, mais la protection réelle
  reste le chiffrement de la base : un export JSON contient tout, y compris les
  éléments marqués protégés.
