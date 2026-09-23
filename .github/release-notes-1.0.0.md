Première version publique de **Lumea** — application de suivi de cycle, de
grossesse et de journal intime, pensée pour Madagascar.

Tout reste sur le téléphone, dans une base chiffrée. Aucune donnée de santé ne
part sur Internet.

## Ce que fait l'application

**Cycle** — anneau de progression, calendrier peint par niveau de risque de
grossesse, prévisions basées sur les cycles passés. Les règles se confirment :
un retard ne disparaît plus tout seul, et chaque confirmation dit si le cycle a
été plus court ou plus long que d'habitude.

**Grossesse** — test expliqué et proposé au bon moment (jamais au premier jour
de retard), semaines d'aménorrhée, terme, et un carnet de suivi adapté à
Madagascar : les 8 consultations du modèle OMS, le VAT, le TPIg dès 13 SA, le
fer et l'acide folique, le déparasitage. Les rendez-vous se posent dans l'agenda
avec un rappel la veille.

**Fiche d'urgence** — groupe sanguin, allergies, traitements, qui appeler, sur
un seul écran. Exportable en PDF pour la consultation.

**Après la naissance** — le suivi ne s'arrête pas à l'accouchement :
consultations postnatales, vaccins du bébé, et la contraception par allaitement
présentée condition par condition. Le retour de couches relance le suivi de
cycle.

**Conseiller** — répond avec vos données, sans connexion et sans rien envoyer.
Ses réponses sont écrites à l'avance : sur du tri médical, mieux vaut un
conseiller qui dit « je ne sais pas » qu'un qui improvise.

**Notes, agenda, journal** — dossiers, tags, recherche, rappels, récurrences,
jours fériés malgaches, habitudes à cocher.

**Assistant IA** — pour réviser et apprendre. Il s'ouvre dans le navigateur, et
une page explique exactement ce qui part et ce qui reste.

## Confidentialité

- Base SQLite chiffrée par SQLCipher, clé dans le Keystore matériel
- Verrou par code et empreinte, verrouillage dès la mise en arrière-plan
- Notes et journées protégeables une par une
- Sauvegarde chiffrée par phrase de passe
- Widget discret par défaut : il n'annonce pas une grossesse sur l'écran
  d'accueil
- Aucune donnée de santé ne quitte le téléphone

## Corrections de cette version

- **Le code PIN à six chiffres devenait impossible à entrer** : les essais
  automatiques du pavé comptaient comme des échecs et déclenchaient le
  verrouillage temporaire.
- Les rendez-vous posés par le carnet de suivi survivaient à l'effacement de la
  grossesse.
- Les réglages (prénom, thème, rappels) étaient absents des sauvegardes.
- Des règles pouvaient être enregistrées dans le futur.
- L'accueil réclamait une consultation déjà faite.

## Installation

Téléchargez le fichier `.apk` ci-dessous et ouvrez-le sur votre téléphone.
Android demandera d'autoriser l'installation depuis cette source.

Android 8.0 minimum.

## ⚠️ Avertissement médical

Le contenu de santé de cette version **n'a pas encore été relu par un soignant
malgache**. Il suit les recommandations de l'OMS reprises par Madagascar, mais
la relecture professionnelle reste à faire — le document est dans
`docs/CONTENU-MEDICAL.md`.

Cette application ne remplace ni une consultation, ni un carnet de santé, ni une
contraception. Les prévisions de cycle sont des estimations.
