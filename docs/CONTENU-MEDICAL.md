# Lumea — contenu médical à relire

> **Document généré automatiquement depuis le code.** Ne pas le modifier
> à la main : les corrections doivent être reportées dans les sources
> (`domain/pregnancy/`, `domain/advisor/`, `domain/learn/`), puis le
> document régénéré. Il reflète donc toujours ce que l'application
> affiche réellement.

## À l'attention du relecteur

Lumea est une application de suivi de cycle, de grossesse et d'après-
naissance destinée à un public jeune, à Madagascar. Le contenu suit les
recommandations de l'OMS reprises par Madagascar (8 contacts prénatals,
TPIg dès 13 SA, VAT, fer et acide folique).

**Ce qui mérite votre attention en priorité :**

1. Les seuils de tri d'urgence (section « Quand appeler, quand partir »).
2. Le calendrier des CPN, vaccins et doses de TPIg.
3. Les conditions de la MAMA et ce qui est annoncé comme protecteur.
4. Tout ce qui nomme un médicament, une dose ou un délai.
5. La formulation sur l'interruption de grossesse, au regard de la loi.

---

---

# Avertissements affichés dans l'application

**Écran grossesse :** Ces informations suivent les recommandations de l'OMS reprises par Madagascar (8 contacts prénatals, TPIg dès 13 SA, VAT). Elles sont éducatives et ne remplacent pas la consultation au CSB. Le calcul des semaines part de tes dernières règles ; si une échographie donne un autre terme, c'est elle qui fait foi.

**Écran cycle :** Aucun jour n'est « sans risque ». Les prévisions de Lumea sont des estimations basées sur tes cycles passés : elles ne remplacent pas une contraception.

---

# Test de grossesse — à partir de quand

L'application ne propose aucun test avant **5 jours**
de retard, et ne le conseille vraiment qu'à partir de
**7 jours**.

### À 0 jour(s) de retard — « Ce n'est pas encore le moment »

Un retard de un à quatre jours arrive à presque tout le monde, et plusieurs fois par an : fatigue, stress, sommeil décalé, un peu plus de sport. Un test serait techniquement fiable, mais il n'y a pas de raison d'en faire un maintenant. Laisse passer quelques jours.

### À 3 jour(s) de retard — « Ce n'est pas encore le moment »

Un retard de un à quatre jours arrive à presque tout le monde, et plusieurs fois par an : fatigue, stress, sommeil décalé, un peu plus de sport. Un test serait techniquement fiable, mais il n'y a pas de raison d'en faire un maintenant. Laisse passer quelques jours.

### À 5 jour(s) de retard — « Tu peux, si tu veux être fixée »

À 5 jours de retard, un test urinaire est fiable. Rien ne presse pour autant : attendre d'arriver à une semaine pleine évite de le faire pour rien. Si l'attente te pèse plus que le test, fais-le.

### À 8 jour(s) de retard — « Un test a du sens maintenant »

Une semaine pleine de retard, c'est le moment. Fais-le avec les premières urines du matin : elles sont plus concentrées, le résultat est plus net. S'il est négatif et que les règles ne viennent toujours pas, refais-en un dans 3 à 5 jours.

### À 25 jour(s) de retard — « Un test, puis un avis médical »

Plus de trois semaines de retard : fais un test, et prends rendez-vous quel que soit le résultat. Un retard qui dure avec un test négatif mérite d'être expliqué — thyroïde, ovaires polykystiques, autre chose. Ça se diagnostique et ça se traite.

### Mode d'emploi affiché

- **Où s'en procurer** — En pharmacie, en supermarché ou en ligne, sans ordonnance et sans justification d'âge. Les tests les moins chers détectent la même hormone que les plus chers.
- **Quand le faire** — Avec les premières urines du matin, plus concentrées. À n'importe quelle heure si tu ne peux pas attendre, mais évite de boire beaucoup avant.
- **Comment le lire** — Respecte le temps d'attente indiqué sur la notice, ni avant ni longtemps après. Une deuxième ligne, même très pâle, est un résultat positif.
- **En cas de doute** — Une prise de sang en laboratoire dose l'hormone précisément et détecte une grossesse plus tôt qu'un test urinaire. Elle se fait sans ordonnance, et le résultat n'est pas discutable.

### Causes d'un test négatif malgré le retard

- **Un test fait trop tôt** — C'est la cause numéro un. Refais-en un dans 3 à 5 jours si les règles ne sont toujours pas là.
- **Le stress et la fatigue** — Examens, deuil, déménagement, gros coup de stress : le cycle est l'une des premières choses qui se dérègle.
- **Un changement de poids ou de sport** — Une perte de poids rapide ou un entraînement intense peut suspendre l'ovulation, donc les règles.
- **Une cause médicale** — Thyroïde, syndrome des ovaires polykystiques, certains médicaments. Rien de dramatique, mais ça se diagnostique et ça se traite.
- **Un changement de contraception** — Démarrer, arrêter ou changer de contraception décale souvent le cycle pendant quelques mois.

---

# Carnet de suivi prénatal

### Consultations prénatales

**1ʳᵉ CPN — avant 12 SA**  _(0 à 12 SA — code `CPN1`)_

La plus importante de toutes. On y confirme la grossesse, on pèse, on prend la tension, on fait le groupe sanguin, le dépistage du VIH et de la syphilis, et on ouvre le carnet de santé mère-enfant. Y aller tôt est ce qui prédit le mieux le fait de faire toutes les consultations ensuite.

**Contact de 13 à 16 SA — début du TPIg**  _(13 à 16 SA — code `CPN_TPI`)_

Un contact ajouté par l'OMS et suivi à Madagascar, précisément pour démarrer la prévention du paludisme dès le début du 2ᵉ trimestre.

**2ᵉ CPN — vers 20 SA**  _(18 à 23 SA — code `CPN2`)_

Contrôle de la tension, du poids, de la hauteur utérine et des urines (albumine, sucre). C'est aussi le moment de la 2ᵉ dose de TPIg.

**3ᵉ CPN — vers 26 SA**  _(24 à 28 SA — code `CPN3`)_

Suivi de la croissance et dépistage de l'anémie, fréquente à ce stade. 3ᵉ dose de TPIg si ce n'est pas déjà fait.

**4ᵉ CPN — vers 30 SA**  _(29 à 32 SA — code `CPN4`)_

On vérifie la position du bébé, la tension et les signes de pré-éclampsie. C'est le seuil que moins d'une femme sur deux atteint dans certains districts : ne t'arrête pas là.

**5ᵉ CPN — vers 34 SA**  _(33 à 35 SA — code `CPN5`)_

Suivi rapproché. On commence à parler du lieu d'accouchement et du plan d'urgence : comment tu iras au CSB, avec qui, et avec quel argent.

**6ᵉ CPN — vers 36 SA**  _(36 à 37 SA — code `CPN6`)_

Position définitive du bébé, dernier dépistage de l'anémie, préparation de la trousse d'accouchement.

**7ᵉ CPN — vers 38 SA**  _(38 à 39 SA — code `CPN7`)_

Surveillance de fin de grossesse. Les mouvements du bébé et la tension sont les deux choses à signaler sans attendre.

**8ᵉ CPN — vers 40 SA**  _(40 à 42 SA — code `CPN8`)_

Dernier contact avant le terme. Un accouchement à terme va de 37 à 42 SA : dépasser la date prévue de quelques jours est normal, mais le suivi continue.

### Vaccination

**VAT 1 — à la 1ʳᵉ CPN**  _(0 à 14 SA — code `VAT1`)_

Première dose d'anatoxine tétanique, sauf si ton carnet montre que tu es déjà à jour. Elle prépare la protection, elle ne suffit pas seule.

**VAT 2 — au moins 4 semaines après VAT 1**  _(8 à 28 SA — code `VAT2`)_

C'est la dose qui compte vraiment : elle protège le bébé du tétanos à la naissance. À faire au moins 90 jours (environ 3 mois) avant l'accouchement pour que la protection passe au nouveau-né.

**VAT 3 — 6 mois après VAT 2**  _(26 à 42 SA — code `VAT3`)_

Souvent après l'accouchement. Elle porte la protection à environ 5 ans. Les doses VAT 4 et VAT 5, un an d'intervalle chacune, complètent le schéma et protègent alors pour toute la période de procréation.

### Paludisme

**Dormir sous moustiquaire imprégnée**  _(0 à 42 SA — code `MILD`)_

Toutes les nuits, toute la grossesse, et ensuite avec le bébé. C'est la protection la plus simple et la plus efficace. Les MILD sont distribuées gratuitement à la CPN et lors des campagnes.

**TPIg — 1ʳᵉ dose, dès 13 SA**  _(13 à 17 SA — code `TPI1`)_

Trois comprimés de sulfadoxine-pyriméthamine en une seule prise, avalés devant le soignant. Jamais avant 13 SA. C'est gratuit à la CPN.

**TPIg — 2ᵉ dose, un mois après**  _(17 à 24 SA — code `TPI2`)_

Une prise par mois, à chaque CPN. Deux doses valent mieux qu'une, trois valent mieux que deux.

**TPIg — 3ᵉ dose**  _(24 à 32 SA — code `TPI3`)_

Le minimum recommandé est de trois doses. Il n'y a pas de maximum tant que l'espacement d'un mois est respecté jusqu'à l'accouchement.

**TPIg — doses suivantes**  _(32 à 42 SA — code `TPI4`)_

Continue une prise par mois jusqu'à l'accouchement. Chaque dose supplémentaire réduit encore le risque d'anémie et de petit poids.

### Suppléments et déparasitage

**Fer et acide folique, tous les jours**  _(0 à 42 SA — code `FAF`)_

30 à 60 mg de fer et de l'acide folique chaque jour, toute la grossesse. L'anémie est la complication la plus fréquente ici, et elle augmente le risque d'hémorragie à l'accouchement. Les comprimés sont fournis à la CPN : demande-les si on ne t'en donne pas.

**Déparasitage, à partir du 4ᵉ mois**  _(16 à 30 SA — code `DEPARASITAGE`)_

Un comprimé antiparasitaire après le 1ᵉʳ trimestre. Les vers intestinaux entretiennent l'anémie : traiter les parasites fait remonter le fer.

### Accouchement

**Préparer le plan d'accouchement**  _(30 à 38 SA — code `PLAN_URGENCE`)_

Trois questions à régler avant 36 SA : dans quel CSB ou maternité tu accouches, comment tu y vas la nuit, et qui t'accompagne. La plupart des décès maternels arrivent faute d'avoir pu partir à temps.

**Accoucher avec un soignant qualifié**  _(37 à 42 SA — code `ACCOUCHEMENT_ASSISTE`)_

Sage-femme, infirmier ou médecin, au CSB-II ou à la maternité. C'est ce qui change le plus les chances en cas d'hémorragie — la première cause de décès maternel, et celle qui va le plus vite.

**Consultations après la naissance**  _(40 à 42 SA — code `CPON`)_

Dans les 24 heures, puis vers le 3ᵉ jour, vers le 7ᵉ jour et à 6 semaines. On y surveille les saignements, l'infection, l'allaitement, et on y parle de contraception si tu le souhaites.

---

# Étapes affichées en résumé

**1ʳᵉ CPN — le plus tôt possible, avant 12 SA**  _(0 à 12 SA)_

Au CSB, gratuitement. On confirme la grossesse, on ouvre le carnet de santé mère-enfant, on fait le groupe sanguin et les dépistages, et on commence le fer. Y aller tôt est ce qui prédit le mieux le fait d'aller jusqu'au bout du suivi.

**Début du traitement préventif du paludisme**  _(13 à 17 SA)_

À partir de 13 SA, jamais avant : trois comprimés de sulfadoxine-pyriméthamine en une prise, avalés devant le soignant, puis une fois par mois. Et une moustiquaire imprégnée toutes les nuits.

**2ᵉ et 3ᵉ CPN — vers 20 et 26 SA**  _(18 à 25 SA)_

Tension, poids, hauteur utérine, urines, et les doses suivantes de TPIg. C'est aussi le moment du déparasitage et, si elle est disponible, de l'échographie du 2ᵉ trimestre.

**4ᵉ CPN — vers 30 SA**  _(26 à 32 SA)_

Le seuil que moins d'une femme sur deux atteint dans certains districts. C'est pourtant là que se dépistent l'anémie de fin de grossesse et les premiers signes de pré-éclampsie.

**5ᵉ et 6ᵉ CPN — préparer l'accouchement**  _(33 à 37 SA)_

Vers 34 et 36 SA. Trois questions à régler : dans quel CSB tu accouches, comment tu y vas la nuit, et qui t'accompagne. La plupart des décès maternels arrivent faute d'être partie à temps.

**7ᵉ et 8ᵉ CPN — jusqu'au terme**  _(38 à 42 SA)_

Vers 38 et 40 SA. Un accouchement à terme va de 37 à 42 SA. Accoucher auprès d'un soignant qualifié est ce qui change le plus les chances en cas d'hémorragie.

---

# Alimentation

### Recommandé

- **Fer et acide folique (FAF), tous les jours** — Les comprimés FAF sont fournis gratuitement à la CPN : 30 à 60 mg de fer et de l'acide folique par jour, toute la grossesse. Demande-les si on ne t'en donne pas. L'acide folique protège le cerveau et la colonne vertébrale du bébé pendant les tout premiers mois.
- **Fer — la priorité numéro un ici** — L'anémie est la complication la plus fréquente à Madagascar, et elle augmente le risque d'hémorragie à l'accouchement. Brèdes (anamamy, anantsonga), feuilles de manioc, lentilles, haricots, viande, abats, petits poissons entiers. Un fruit acide ou du citron au repas aide à absorber le fer ; le thé et le café pris en mangeant le bloquent.
- **Ce qui se trouve facilement et qui compte** — Les feuilles de moringa (ananambo) sont parmi les aliments les plus riches en fer, en calcium et en vitamine A. Ajoute aussi l'arachide, le haricot, l'œuf bien cuit, la patate douce à chair orange et les fruits de saison.
- **Calcium et iode** — Petits poissons mangés avec les arêtes, brèdes, lait bouilli. Pour l'iode, qui participe au développement du cerveau du bébé : utilise du sel iodé et mange du poisson.
- **Fibres et eau** — La constipation est l'un des désagréments les plus fréquents. Fruits, légumes, céréales complètes, et 1,5 à 2 litres d'eau par jour.

### À éviter

- **Alcool : zéro, vraiment zéro** — Il n'existe aucune dose sans risque, à aucun moment de la grossesse. L'alcool passe directement au bébé et touche son cerveau. C'est la première cause évitable de handicap mental à la naissance.
- **Tabac, cannabis, vape** — Ils réduisent l'oxygène qui arrive au bébé : prématurité, petit poids. Arrêter, même tard, améliore les choses immédiatement. Demande de l'aide plutôt que de culpabiliser : Tabac info service, 39 89.
- **Viande, poisson et lait crus ou mal cuits** — Tout ce qui est bien cuit ne pose pas de problème. Évite la viande saignante, le poisson cru, le lait non bouilli et les produits laitiers non pasteurisés : ils exposent à des infections plus graves pendant la grossesse.
- **Eau non traitée** — Bois de l'eau bouillie, filtrée ou traitée. Une diarrhée sévère pendant la grossesse déshydrate vite et fragilise. Lave-toi les mains avant de cuisiner et avant de manger.
- **Toxoplasmose, si tu n'es pas immunisée** — Ta première prise de sang le dira. Si tu ne l'es pas : viande bien cuite, fruits et légumes soigneusement lavés, gants pour jardiner, et confie la litière du chat à quelqu'un d'autre.
- **Gros poissons prédateurs** — Espadon, requin, marlin : à éviter, ils concentrent le mercure. Les petits poissons — sardines, poissons de rivière — restent recommandés deux fois par semaine.
- **Foie, œufs crus, caféine en excès** — Le foie concentre trop de vitamine A. Les œufs crus (mayonnaise maison, mousse au chocolat) exposent à la salmonelle. Pour la caféine, reste sous 200 mg par jour — environ deux cafés.
- **Médicaments et compléments sans avis** — L'ibuprofène et tous les anti-inflammatoires sont formellement contre-indiqués à partir de 24 SA, et déconseillés avant. Le paracétamol reste l'option de première intention, à la dose la plus faible. Demande à ton pharmacien avant toute automédication, plantes comprises.

---

# Signes qui doivent alerter

- **Saignements, surtout avec des douleurs** — Beaucoup de grossesses saignent un peu sans conséquence. Mais des saignements avec une douleur d'un seul côté du bas-ventre, en début de grossesse, peuvent signaler une grossesse extra-utérine : c'est une urgence.
- **Douleur abdominale forte ou continue** — Différente des tiraillements habituels : intense, qui ne passe pas, ou qui réveille la nuit.
- **Fièvre, surtout avec des frissons** — En zone de paludisme, une fièvre chez une femme enceinte se fait tester et traiter le jour même : le paludisme est bien plus grave pendant la grossesse.
- **Vomissements qui empêchent de boire** — Les nausées du début sont normales. Ne plus rien garder pendant 24 h ne l'est pas : ça déshydrate et ça se traite.
- **Maux de tête intenses, vision trouble, gonflement du visage** — Après 20 SA, ce trio peut signaler une pré-éclampsie, liée à la tension. Consultation le jour même.
- **Bébé qui bouge moins** — Au troisième trimestre, une baisse nette des mouvements sur une journée se vérifie à la maternité. On préfère mille fois un dérangement pour rien.
- **Perte de liquide, contractions régulières avant 37 SA** — Liquide clair qui coule sans pouvoir se retenir, ou contractions qui reviennent toutes les dix minutes : direction la maternité.

---

# Quand appeler, quand partir

### Va au CSB tout de suite

_Ne perds pas de temps à téléphoner d'abord. Pars, et fais prévenir en chemin si quelqu'un peut le faire à ta place._

- **Saignement abondant** — L'hémorragie est la première cause de décès maternel, et c'est celle qui va le plus vite. Pars immédiatement, de jour comme de nuit.
- **Douleur forte au ventre, d'un seul côté** — En début de grossesse, avec ou sans saignement, cela peut être une grossesse hors de l'utérus. C'est une urgence chirurgicale.
- **Convulsions, ou maux de tête violents avec vision trouble** — Après 20 SA, ce sont les signes de l'éclampsie et de la pré-éclampsie, liées à la tension. C'est la deuxième grande cause de décès évitable.
- **Fièvre élevée avec frissons** — En zone de paludisme, une fièvre chez une femme enceinte se traite le jour même : le paludisme est plus grave pendant la grossesse.
- **Perte de liquide, ou contractions régulières avant 37 SA** — La poche peut s'être rompue, ou le travail commencer trop tôt. Direction la maternité, même si tu ne souffres pas.
- **Le bébé bouge beaucoup moins que d'habitude** — Au troisième trimestre, une baisse nette des mouvements sur une journée se vérifie sur place. Mieux vaut cent déplacements pour rien.

### Appelle ta sage-femme aujourd'hui

_Ça ne peut pas attendre la prochaine consultation, mais tu as le temps d'un appel pour savoir quoi faire._

- **Vomissements qui empêchent de boire depuis un jour** — Les nausées du début sont normales ; ne plus rien garder du tout déshydrate et se soigne.
- **Grande fatigue, essoufflement, vertiges** — Souvent l'anémie, très fréquente ici. Elle se corrige avec du fer, mais elle se vérifie par une prise de sang.
- **Brûlures en urinant** — Une infection urinaire non traitée peut déclencher un accouchement prématuré. Elle se traite simplement quand on la prend tôt.
- **Gonflement rapide des mains et du visage** — Des chevilles gonflées en fin de journée sont banales ; un gonflement du visage qui apparaît vite se vérifie, à cause de la tension.

### À dire à la prochaine CPN

_Note-le pour ne pas l'oublier le jour J. Ce n'est pas une raison d'appeler entre deux consultations._

- **Nausées du matin, seins tendus, envies fréquentes d'uriner** — Ce sont les signes ordinaires du début de grossesse. Désagréables, pas inquiétants.
- **Constipation, brûlures d'estomac, crampes dans les jambes** — Très fréquents, surtout au 2ᵉ et 3ᵉ trimestre. Il existe des solutions simples — demande-les en consultation.
- **Petites pertes de sang après un rapport** — Souvent sans gravité, le col étant plus fragile. Mais si le saignement devient abondant ou douloureux, la règle du haut de liste s'applique.
- **Questions sur l'alimentation, le travail, les voyages** — Note tes questions au fil des semaines : on oublie toujours la moitié une fois devant la sage-femme.

---

# Où aller

- **CSB-I** — Vaccinations et soins de base, tenus par un personnel paramédical — infirmier, sage-femme ou aide-soignant.
- **CSB-II** — Le niveau de référence pour la grossesse : CPN, accouchement assisté, soins obstétricaux essentiels, dirigé par un médecin. C'est là que se fait l'essentiel du suivi, et les CPN y sont gratuites.
- **CHRD / CHD** — L'hôpital de district, vers lequel le CSB t'oriente si la grossesse présente un risque ou si une césarienne est nécessaire.
- **En urgence, la nuit** — Va directement au CSB-II ou à la maternité la plus proche : ce sont eux qui organisent l'évacuation vers l'hôpital. N'attends pas le matin pour un saignement abondant, une fièvre ou des douleurs fortes.

---

# Facteurs de risque proposés

- **J'ai moins de 18 ans** — Une grossesse avant 18 ans expose davantage à l'hypertension, à l'anémie et à un accouchement difficile. Ce n'est pas une fatalité : c'est une raison de faire toutes les CPN et d'accoucher en structure, pas à la maison.
- **J'ai plus de 35 ans** — Le suivi est un peu plus rapproché, notamment pour la tension et le diabète de grossesse. Signale-le dès la première CPN.
- **C'est ma première grossesse** — La pré-éclampsie est plus fréquente lors d'une première grossesse. La prise de tension à chaque CPN prend deux minutes et c'est ce qui la dépiste.
- **J'ai déjà eu 4 grossesses ou plus** — Le risque d'hémorragie après l'accouchement augmente avec le nombre de grossesses. Accoucher auprès d'un soignant qualifié est d'autant plus important.
- **On m'a dit que j'attends des jumeaux** — Suivi rapproché, besoins en fer plus élevés, et accouchement à prévoir en maternité équipée. L'échographie confirme et oriente.
- **J'ai déjà eu une césarienne** — Le lieu d'accouchement doit être choisi à l'avance, dans une structure capable d'opérer. À décider avec ta sage-femme bien avant 36 SA.
- **J'ai déjà perdu une grossesse ou un bébé** — Ça mérite d'être dit dès la première consultation, pour le suivi comme pour l'accompagnement. Ce n'était pas ta faute, et ça ne prédit pas la suite.
- **J'ai de la tension, ou j'en ai eu enceinte** — C'est le facteur qui demande la surveillance la plus serrée. Tension à chaque CPN, et tout mal de tête violent se signale le jour même.
- **J'ai du diabète** — Le suivi de la glycémie fait partie du suivi de grossesse. À signaler dès la première CPN pour adapter le traitement.
- **On m'a déjà dit que j'étais anémiée** — Le fer quotidien n'est alors pas optionnel, et le déparasitage compte double : les vers entretiennent l'anémie.
- **Je vis avec le VIH** — Le traitement bien suivi rend la transmission au bébé très improbable. Le suivi et les médicaments sont gratuits, et le secret professionnel s'applique.
- **Le centre de santé est loin de chez moi** — C'est un vrai facteur de risque, et le plus sous-estimé. Prépare le trajet à l'avance : qui t'emmène, avec quoi, et où tu dors si le travail commence la nuit.

---

# Options après un test positif (cadre légal malgache)

- **Poursuivre la grossesse** — Le suivi commence par une CPN, le plus tôt possible et avant 12 SA. Les consultations prénatales sont gratuites dans les CSB, tout comme les vaccins et le traitement préventif du paludisme.
- **Ce que dit la loi malgache** — L'interruption volontaire de grossesse est interdite à Madagascar, y compris pour raison médicale : l'article 317 du Code pénal s'applique à la femme comme à toute personne qui l'aiderait. C'est un fait juridique, pas un jugement sur ta situation.
- **Saignements, fièvre, douleurs : va au CSB** — Quelle que soit l'origine du problème, les soins après avortement sont des soins d'urgence et tu y as droit. Les complications d'avortement sont la deuxième cause de décès maternel dans les formations sanitaires malgaches, et c'est presque toujours le retard à consulter qui tue, pas la complication elle-même. N'attends pas.
- **En parler à quelqu'un** — Une sage-femme au CSB, un centre de planification familiale, ou une association de santé de la reproduction. Tu peux demander conseil sans avoir décidé quoi que ce soit, et le secret professionnel s'applique.
- **Prévoir la suite** — Après l'accouchement, la consultation postnatale est le moment pour parler contraception si tu le souhaites — la loi 2017-043 garantit l'accès à la planification familiale, y compris pour les jeunes.

---

# Après la naissance

### Contraception par allaitement (MAMA) — les trois conditions

- **Mon bébé a moins de 6 mois** — Au-delà de 6 mois, la fertilité revient même si les règles ne sont pas réapparues et même si l'allaitement continue. C'est la condition qui tombe la première, et souvent sans qu'on s'en rende compte.
- **J'allaite exclusivement, jour et nuit** — Le sein seul, à la demande : ni eau, ni tisane, ni bouillie, ni biberon. Sans jamais dépasser 4 heures entre deux tétées le jour, ni 6 heures la nuit. C'est la succion fréquente qui bloque l'ovulation — espacer les tétées suffit à la relancer.
- **Mes règles ne sont pas revenues** — Aucun saignement après le 56ᵉ jour suivant l'accouchement. Les pertes des premières semaines (les lochies) ne comptent pas comme des règles.

### Consultations postnatales

- **1ʳᵉ visite — dans les 24 heures** — La plus importante de toutes. On surveille les saignements de la mère, on pèse le bébé, on vérifie la première tétée, et on fait les vaccins de naissance (BCG et polio 0).
- **2ᵉ visite — vers le 3ᵉ jour** — Surveillance de l'infection, de l'ictère du bébé (la peau qui jaunit) et de la mise en route de l'allaitement. C'est le moment où les difficultés de tétée se règlent le plus facilement.
- **3ᵉ visite — entre le 7ᵉ et le 14ᵉ jour** — Contrôle du poids du bébé, du cordon, et de l'état de la mère. On y parle aussi fatigue et moral : le baby blues est fréquent et se dit.
- **4ᵉ visite — à 6 semaines** — Bilan complet de la mère, reprise de la contraception si tu le souhaites, et début des vaccins du bébé (Penta 1). C'est la consultation qui clôt officiellement les suites de couches.

### Vaccins du nourrisson

- **BCG et polio 0 — à la naissance** — Contre la tuberculose et la poliomyélite, dès les premiers jours. Le BCG laisse une petite cicatrice sur le bras : c'est normal.
- **Penta 1, polio et pneumo — à 6 semaines** — Le vaccin combiné protège contre cinq maladies à la fois. Il se donne en même temps que la 4ᵉ consultation postnatale.
- **Penta 2 — à 10 semaines** — Deuxième dose, quatre semaines après la première.
- **Penta 3 — à 14 semaines** — Troisième dose. C'est elle qui donne la protection durable : ne t'arrête pas avant.
- **Rougeole — à 9 mois** — La rougeole reste une cause importante de décès chez le jeune enfant à Madagascar, et les épidémies reviennent régulièrement.

### Conseils à la mère

- **Continue le fer** — Encore au moins trois mois après l'accouchement. L'accouchement fait perdre du sang, et l'anémie post-partum entretient la fatigue qu'on met souvent sur le compte du bébé.
- **Allaitement exclusif jusqu'à 6 mois** — Rien d'autre que le sein, pas même de l'eau — le lait maternel en contient assez, même quand il fait chaud. Ensuite, on continue d'allaiter en ajoutant d'autres aliments, idéalement jusqu'à 2 ans.
- **Mange et bois plus que d'habitude** — Allaiter demande environ un repas de plus par jour. Ce n'est pas le moment de se restreindre : le lait se fabrique avec ce que tu manges et ce que tu bois.
- **Dors sous moustiquaire, avec le bébé** — Le paludisme reste dangereux pour toi et l'est encore plus pour un nourrisson. La moustiquaire imprégnée sert autant après qu'avant.
- **Ce qui n'est pas normal** — Saignement abondant, fièvre, pertes malodorantes, douleur ou rougeur d'un sein avec fièvre, tristesse profonde qui dure. Aucun de ces signes n'est « la fatigue d'une jeune mère » : ils se soignent.
- **Espacer les grossesses** — L'OMS conseille d'attendre environ 24 mois avant une nouvelle grossesse. Des grossesses rapprochées augmentent le risque de prématurité, de petit poids et d'anémie pour la mère.

### Contraceptions compatibles avec l'allaitement

- **Le préservatif** — Disponible tout de suite, sans consultation, et le seul à protéger aussi des IST.
- **La pilule sans œstrogène (microprogestative)** — Compatible avec l'allaitement et utilisable dès 6 semaines, voire plus tôt selon l'avis du soignant. Elle demande une prise très régulière.
- **L'implant et l'injection** — Compatibles avec l'allaitement. L'implant protège plusieurs années, l'injection quelques mois. Tous deux sont disponibles dans les CSB.
- **Le stérilet (DIU)** — Posable juste après l'accouchement ou à partir de 4 à 6 semaines. Il protège plusieurs années et n'a aucun effet sur le lait.
- **À demander en consultation postnatale** — La 4ᵉ visite, à 6 semaines, est prévue pour ça. Tu peux aussi en parler avant : rien n'oblige à attendre.

---

# Réponses du conseiller

Le conseiller répond localement, à partir des données de la personne.
Les réponses ci-dessous sont celles d'une grossesse en cours ; elles
changent selon la situation (cycle, grossesse, après-naissance).

### RETARD — niveau RASSURANT

**Tu n'attends pas de règles**

Ton suivi de grossesse est en cours : les règles s'arrêtent pendant toute la grossesse, c'est d'ailleurs de là que vient le mot « aménorrhée » dans « semaines d'aménorrhée ».

En revanche, un saignement pendant la grossesse n'est jamais à ignorer.

### SAIGNEMENT — niveau URGENT

**Un saignement pendant la grossesse se fait toujours voir**

Une grossesse ne saigne pas normalement. Beaucoup de saignements sont sans gravité, mais ce n'est pas à toi — ni à une application — d'en décider.

Au premier trimestre, un saignement accompagné d'une douleur d'un seul côté du bas-ventre peut signaler une grossesse hors de l'utérus : c'est une urgence chirurgicale.

Si le saignement est abondant, ne téléphone pas d'abord : pars au CSB-II ou à la maternité, de jour comme de nuit, et fais prévenir en chemin.

### DOULEUR — niveau CONSULTER

**Quelle douleur, et où ?**

Des tiraillements dans le bas-ventre, surtout en fin de grossesse, sont ordinaires : l'utérus s'étire.

Ce qui n'est pas ordinaire et se fait voir le jour même : une douleur forte, une douleur qui ne passe pas, une douleur d'un seul côté en début de grossesse, ou des contractions régulières avant 37 semaines.

Un dernier point : l'ibuprofène et tous les anti-inflammatoires sont interdits à partir de 24 SA et déconseillés avant. Le paracétamol est l'option de première intention.

### FIEVRE — niveau URGENT

**Une fièvre se fait tester le jour même**

À Madagascar, toute fièvre chez une femme enceinte ou qui vient d'accoucher doit faire chercher le paludisme sans attendre : il est bien plus grave pendant la grossesse, pour la mère comme pour le bébé.

Une fièvre peut aussi venir d'une infection urinaire, fréquente et capable de déclencher un accouchement prématuré si on la laisse traîner.

Va au CSB. Ne prends pas de médicament au hasard en attendant.

### TEST — niveau RASSURANT

**Ton suivi est déjà ouvert**

Un test positif a été enregistré et le suivi de grossesse est en cours. Si tu as un doute sur le résultat, une prise de sang en laboratoire dose l'hormone précisément.

### FERTILITE — niveau INFO

**Tu es déjà enceinte**

La question du risque de grossesse ne se pose plus pour le moment. Le préservatif reste utile pendant la grossesse : il protège des infections sexuellement transmissibles, qui peuvent atteindre le bébé.

### CONTRACEPTION — niveau INFO

**Ce qui marche vraiment**

Les méthodes les plus efficaces sont celles qu'on ne peut pas oublier : implant et stérilet. La pilule demande une prise très régulière pour tenir ses promesses.

Le préservatif est le seul à protéger aussi des infections sexuellement transmissibles — d'où l'intérêt de le combiner.

Les leçons de l'app détaillent les chiffres d'efficacité en usage réel, pas en usage parfait.

### NAUSEES — niveau INFO

**Désagréable, mais ordinaire**

Nausées, seins tendus, fatigue et envies fréquentes d'uriner sont les signes classiques du début de grossesse. Ils s'atténuent le plus souvent après le premier trimestre.

Ce qui n'est pas ordinaire : ne plus rien garder du tout pendant 24 heures. Ça déshydrate, et ça se traite — appelle ou va au CSB.

### MOUVEMENTS — niveau RASSURANT

**C'est encore tôt pour les sentir**

Les premiers mouvements se perçoivent en général entre 18 et 22 semaines, parfois plus tard pour une première grossesse. Ne pas encore les sentir à 10 SA n'a rien d'anormal.

### RENDEZ_VOUS — niveau RASSURANT

**Tout est coché**

Il n'y a rien en attente dans ton carnet pour l'instant. La prochaine étape apparaîtra quand son moment sera venu.

### ALIMENTATION — niveau INFO

**Le fer avant tout**

L'anémie est la complication la plus fréquente ici, et elle augmente le risque d'hémorragie à l'accouchement. Les comprimés fer + acide folique sont gratuits à la CPN : demande-les si on ne t'en donne pas.

Côté assiette : brèdes, feuilles de manioc, moringa (ananambo), lentilles, haricots, petits poissons entiers, abats. Un fruit acide au repas aide à absorber le fer ; le thé et le café pris en mangeant le bloquent.

À éviter : alcool (aucune dose n'est sans risque), viande et poisson crus, lait non bouilli, eau non traitée.

### ALLAITEMENT — niveau INFO

**Je ne sais pas répondre à celle-là**

Je ne traite que ce qui touche au cycle, à la grossesse et à l'après-naissance, et uniquement à partir de tes données enregistrées ici. Je préfère te le dire plutôt que d'inventer une réponse.

Pour une question générale, l'assistant IA de l'app peut t'aider — mais il s'ouvre dans un service extérieur : n'y écris pas tes données de santé.

### HUMEUR — niveau INFO

**Ce n'est ni dans ta tête, ni un défaut de caractère**

Les hormones du cycle agissent sur le sommeil, l'appétit, la concentration et l'humeur. Se sentir à fleur de peau à certains moments du mois a une cause physique.

Noter ton humeur chaque jour dans le journal finit par montrer un rythme — et voir ce rythme aide souvent plus qu'on ne le croit.

---

# Leçons éducatives

## Mon corps

_Comprendre ce qui se passe, mois après mois_

### 🌙 C'est quoi, un cycle ?  _(3 min)_

> Le compte à rebours qui recommence chaque mois, et ce qu'il fabrique.

**Le jour 1, c'est le premier jour de saignement**

Le cycle menstruel ne commence pas quand les règles finissent, mais le jour où elles commencent. C'est la convention utilisée partout, y compris dans cette app. Le cycle se termine la veille des règles suivantes.

**Combien de temps ça dure**

On dit souvent « 28 jours ». En réalité, un cycle entre 21 et 35 jours est tout à fait normal chez l'adulte, et la durée varie d'un mois à l'autre chez la même personne. Une variation de quelques jours n'a rien d'inquiétant.

**Ce que fait ton corps pendant ce temps**

Chaque mois, l'utérus prépare une muqueuse épaisse au cas où un ovule fécondé viendrait s'y installer. Un ovaire libère un ovule au milieu du cycle. S'il n'y a pas de fécondation, la muqueuse n'est plus utile : elle se détache et s'évacue — ce sont les règles. Puis tout recommence.

**Pourquoi l'humeur et l'énergie bougent**

Deux hormones principales, l'œstrogène et la progestérone, montent et descendent au fil du cycle. Elles n'agissent pas que sur l'utérus : elles touchent le sommeil, l'appétit, la peau, la concentration et l'humeur. Se sentir fatiguée ou à fleur de peau à certains moments du mois n'est ni dans ta tête, ni un défaut de caractère.

### 🔄 Les quatre phases  _(4 min)_

> Règles, folliculaire, ovulation, lutéale — et ce que tu peux en attendre.

**1. Les règles (jours 1 à 5 environ)**

La muqueuse utérine s'évacue. Les hormones sont au plus bas. Beaucoup de femmes se sentent fatiguées, ont froid, ou ont mal au ventre. C'est le moment de lever le pied si tu peux : chaleur, sommeil, aliments riches en fer.

**2. La phase folliculaire (jusqu'à l'ovulation)**

L'œstrogène remonte. L'énergie, l'humeur et la concentration suivent souvent. Beaucoup décrivent cette période comme celle où on a envie de lancer des projets, de bouger, de voir du monde.

**3. L'ovulation (un seul jour)**

Un ovaire libère un ovule. C'est bref : l'ovule ne survit que 12 à 24 heures. Certaines ressentent une petite douleur d'un côté du bas-ventre, ou remarquent des pertes plus claires et élastiques, comme du blanc d'œuf.

**4. La phase lutéale (après l'ovulation)**

La progestérone domine. Cette phase dure assez régulièrement 12 à 14 jours — c'est d'ailleurs pour ça que l'app estime l'ovulation en comptant à rebours depuis les règles suivantes, et non en avant depuis les précédentes. C'est aussi la période du syndrome prémenstruel : seins sensibles, ballonnements, irritabilité, fringales.

**Ce n'est pas une règle universelle**

Ces descriptions sont des tendances, pas un programme. Certaines ne ressentent presque rien, d'autres beaucoup. En notant tes journées dans l'app, tu découvriras ton propre schéma, qui vaut mieux que n'importe quelle moyenne.

### 🥚 L'ovulation et la fenêtre fertile  _(4 min)_

> Pourquoi la période fertile dure six jours alors que l'ovule ne vit qu'un jour.

**L'ovule vit un jour, les spermatozoïdes cinq**

C'est le point-clé, et il surprend souvent. L'ovule ne survit que 12 à 24 heures après avoir été libéré. Mais les spermatozoïdes peuvent survivre jusqu'à 5 jours dans le corps. Un rapport ayant lieu cinq jours AVANT l'ovulation peut donc mener à une grossesse.

**D'où les six jours de fenêtre fertile**

En additionnant : les 5 jours avant l'ovulation, le jour même, et celui qui suit. C'est ce que l'app affiche en vert sur le calendrier. Une grossesse est plus probable pendant ces jours-là.

**Mais cette fenêtre est une estimation, pas une certitude**

L'app calcule à partir de tes cycles passés. Or l'ovulation peut se décaler : stress, maladie, voyage, manque de sommeil, changement de poids. Un décalage de quelques jours suffit à déplacer toute la fenêtre. Personne — aucune app — ne peut te dire avec certitude quand tu ovules.

**Conséquence directe**

Aucun jour du cycle ne peut être considéré comme « sans risque » de grossesse. Si tu ne souhaites pas être enceinte, la protection doit être la même tous les jours du mois. La leçon « Ce qui protège vraiment » détaille les options.

### 〰️ Cycles irréguliers  _(3 min)_

> Très fréquent, surtout les premières années. Quand s'en inquiéter.

**Les premières années, c'est la norme**

Après les premières règles, il faut souvent 2 à 5 ans avant que les cycles se régularisent. Pendant cette période, des cycles de 21 jours puis de 45 jours sont courants, et des mois peuvent être sautés. Ce n'est pas un problème de santé en soi.

**Ce qui peut dérégler un cycle**

Stress, examens, chagrin, voyage et décalage horaire, perte ou prise de poids importante, sport intensif, maladie, manque de sommeil. Le cycle est un bon baromètre de l'état général du corps.

**L'app est moins fiable sur un cycle irrégulier**

Les prévisions reposent sur la moyenne de tes derniers cycles. Si ceux-ci varient beaucoup, l'indicateur « Régularité » affichera « Variable » ou « Irrégulier » : c'est l'app qui te dit honnêtement que ses prévisions sont approximatives. Ne t'appuie surtout pas dessus pour éviter une grossesse.

**Quand en parler à un soignant**

Si tu n'as aucune règle pendant plus de 3 mois sans être enceinte, si les cycles font régulièrement moins de 21 ou plus de 45 jours après plusieurs années de règles, ou si les saignements sont très abondants ou très douloureux.

### 🌸 Les premières règles  _(3 min)_

> À quoi s'attendre, quoi préparer, et ce qui est normal.

**Quand ça arrive**

Le plus souvent entre 10 et 15 ans, généralement deux ans après le début du développement de la poitrine. Il n'y a pas d'âge « correct » : très tôt ou assez tard, les deux existent.

**À quoi ça ressemble**

Souvent peu abondant au début, parfois brun ou rosé plutôt que rouge vif — c'est du sang qui a mis plus de temps à sortir, rien d'anormal. Les premiers cycles sont souvent irréguliers et peuvent être espacés de plusieurs mois.

**Ce qu'il est utile d'avoir**

Des serviettes ou des protections lavables pour commencer (les tampons et la coupe menstruelle demandent un peu plus d'habitude), une trousse discrète dans le sac, et un sous-vêtement de rechange. Marquer le premier jour dans l'app dès maintenant permettra d'y voir clair d'ici quelques mois.

**Ce n'est pas sale**

Les règles sont un processus biologique ordinaire, pas une saleté ni une honte. Tu peux te laver, nager, faire du sport, vivre normalement. Il n'y a aucune raison de se cacher.

### 🤍 Douleurs et flux  _(3 min)_

> Ce qui soulage vraiment, et le seuil au-delà duquel il faut consulter.

**D'où vient la douleur**

L'utérus est un muscle qui se contracte pour évacuer sa muqueuse. Ces contractions provoquent les crampes. Une douleur modérée les premiers jours est fréquente.

**Ce qui aide**

La chaleur sur le ventre ou le bas du dos (bouillotte, douche chaude) est efficace et sans effet secondaire. L'activité physique douce, contre-intuitivement, réduit souvent les crampes. Les anti-inflammatoires comme l'ibuprofène sont plus efficaces que le paracétamol pour ce type de douleur — à prendre selon la notice, et en parler à un pharmacien en cas de doute.

**Un flux abondant, c'est quoi au juste**

Changer de protection toutes les heures ou moins pendant plusieurs heures d'affilée, devoir se lever la nuit pour changer, ou perdre des caillots plus gros qu'une pièce de monnaie. Un flux vraiment abondant peut provoquer une anémie : fatigue, pâleur, essoufflement.

**Une douleur qui t'empêche de vivre n'est pas normale**

Rater les cours ou le travail, vomir de douleur, ne pas être soulagée par les médicaments courants : ce n'est pas « être douillette ». Cela peut signaler une endométriose ou d'autres causes traitables, souvent diagnostiquées avec des années de retard parce qu'on a appris aux filles à endurer. Consulte, et insiste si on te minimise.

### 🩺 Quand consulter  _(2 min)_

> Les signaux qui méritent un avis, sans paniquer pour autant.

**Prends rendez-vous si…**

• Aucune règle à 16 ans, ou aucun signe de puberté à 14 ans
• Plus de 3 mois sans règles sans être enceinte
• Douleurs qui t'empêchent d'aller en cours ou au travail
• Saignements très abondants, ou entre les règles
• Pertes inhabituelles : odeur forte, couleur verte ou grise, démangeaisons
• Douleur pendant les rapports
• Tout doute après un rapport non protégé

**À qui s'adresser**

Médecin généraliste, sage-femme, gynécologue, infirmerie scolaire, centre de planning familial. Dans beaucoup de pays, les centres de planning reçoivent les mineures gratuitement et de façon confidentielle.

**Ce que tu peux apporter**

Tes notes de l'app. Les dates de tes dernières règles, la durée des cycles et tes symptômes récurrents font gagner du temps au soignant et rendent le diagnostic plus précis. C'est l'une des raisons d'utiliser un suivi.

## Se protéger

_Ce qui marche vraiment, chiffres à l'appui_

### 🛡️ Ce qui protège vraiment  _(5 min)_

> Les méthodes classées par efficacité réelle, sans langue de bois.

**Comment lire ces chiffres**

Ils indiquent, sur 100 personnes utilisant la méthode pendant un an, combien tombent enceintes. On donne ici l'« usage réel » : celui où on oublie parfois un comprimé, où le préservatif glisse. C'est le chiffre honnête, souvent très différent de l'usage parfait en laboratoire.

**Très efficaces — moins de 1 grossesse sur 100**

• Implant (bâtonnet sous la peau du bras), 3 ans
• DIU hormonal ou au cuivre (stérilet), 5 à 10 ans

Leur force : une fois posés, il n'y a plus rien à penser. C'est précisément ce qui explique l'écart avec la pilule. Contrairement à une idée reçue, le DIU est possible même sans avoir eu d'enfant.

**Moyennement efficaces — 4 à 9 sur 100**

• Injection trimestrielle : environ 4
• Pilule, patch, anneau : environ 7 à 9

La pilule serait à 0,3 si elle était prise parfaitement chaque jour à la même heure. L'écart entre 0,3 et 9, c'est la vraie vie.

**Moins efficaces — plus de 10 sur 100**

• Préservatif externe : environ 13
• Méthodes calendaires et applications : 12 à 24
• Retrait : environ 20
• Aucune méthode : environ 85

**La méthode du calendrier n'est pas une contraception**

Suivre son cycle dans une app — y compris celle-ci — fait partie des méthodes les moins fiables : jusqu'à 24 personnes sur 100 enceintes en un an. Et c'est encore pire quand les cycles sont irréguliers, ce qui est le cas le plus fréquent chez les adolescentes. Lumea t'aide à connaître ton corps ; elle ne te protège pas d'une grossesse.

**La combinaison la plus solide**

Une méthode très efficace pour la grossesse (implant, DIU ou pilule) ET un préservatif pour les IST. Aucune contraception hormonale ne protège des infections ; le préservatif est le seul à faire les deux à la fois.

### 🧤 Le préservatif, bien utilisé  _(3 min)_

> Le seul moyen qui protège à la fois de la grossesse et des IST.

**Pourquoi il reste indispensable**

C'est la seule méthode qui protège des infections sexuellement transmissibles. Même avec un implant ou un stérilet, le préservatif reste nécessaire tant qu'on n'est pas certain du statut des deux partenaires.

**Les erreurs qui font échouer**

• Le mettre seulement à la fin, après avoir commencé
• Oublier de pincer le réservoir au bout pour chasser l'air
• Le dérouler dans le mauvais sens puis le retourner — il faut en prendre un neuf
• Utiliser une huile ou une crème comme lubrifiant : le latex se perce. Seuls les lubrifiants à base d'eau ou de silicone conviennent
• En mettre deux l'un sur l'autre : le frottement les déchire
• L'ouvrir avec les dents, ou le garder dans une poche serrée au chaud
• Vérifier la date de péremption : un préservatif périmé casse

**S'il craque**

Ça arrive et ce n'est pas une catastrophe si on réagit. Une contraception d'urgence est possible jusqu'à 3 à 5 jours après, et plus elle est prise tôt, plus elle marche. Un dépistage des IST est également conseillé. Voir la leçon suivante.

**Où s'en procurer**

Pharmacies, supermarchés, distributeurs. Les centres de planning familial, les infirmeries scolaires et de nombreuses associations en distribuent gratuitement, sans questions et sans condition d'âge.

### ⏱️ La contraception d'urgence  _(3 min)_

> Que faire après un rapport non protégé — et dans quel délai.

**Chaque heure compte**

La pilule d'urgence (« pilule du lendemain ») est d'autant plus efficace qu'elle est prise tôt. Selon la molécule, le délai maximal est de 3 ou 5 jours, mais l'efficacité chute nettement au fil des heures. Ne pas attendre le lendemain matin par gêne.

**L'option la plus efficace**

La pose d'un DIU au cuivre dans les 5 jours est la contraception d'urgence la plus efficace qui existe — et elle devient ensuite ta contraception durable. Elle nécessite un rendez-vous médical.

**Où l'obtenir**

En pharmacie, sans ordonnance. Dans de nombreux pays elle est délivrée gratuitement et de manière anonyme aux mineures, y compris en pharmacie, en infirmerie scolaire ou en centre de planning familial.

**Ce que ce n'est pas**

Ce n'est pas un avortement : la pilule d'urgence agit en retardant l'ovulation, elle n'interrompt pas une grossesse déjà installée. Ce n'est pas non plus une contraception régulière : elle est moins efficace et moins bien tolérée qu'une méthode continue.

**Et après**

Fais un test de grossesse si les règles ont plus d'une semaine de retard. Pense aussi au dépistage des IST : la contraception d'urgence n'en protège pas.

### ❌ Les idées fausses qui font des bébés  _(3 min)_

> Ce qu'on entend souvent, et pourquoi c'est faux.

**« On ne peut pas tomber enceinte pendant les règles »**

Faux. C'est moins probable, mais possible — surtout avec un cycle court. Les spermatozoïdes survivent jusqu'à 5 jours : un rapport en fin de règles peut rencontrer une ovulation précoce.

**« Pas la première fois »**

Faux. Une grossesse est possible dès le premier rapport, et même avant les toutes premières règles — puisque l'ovulation précède les règles.

**« Le retrait suffit »**

Faux. Environ 20 personnes sur 100 tombent enceintes en un an avec le retrait. Le liquide pré-éjaculatoire peut contenir des spermatozoïdes, et le contrôle du moment est imparfait par nature.

**« Mon app me dit que je ne suis pas fertile aujourd'hui »**

Une app ne mesure rien : elle extrapole à partir de tes cycles passés. L'ovulation se décale avec le stress, la maladie ou la fatigue. C'est pour cette raison que Lumea n'affiche jamais de jour « sans danger ».

**« Une douche ou une position particulière évite la grossesse »**

Faux, et les douches vaginales sont même déconseillées : elles déséquilibrent la flore et augmentent le risque d'infection. Aucune position, aucun rinçage n'a d'effet contraceptif.

## Santé et relations

_IST, consentement, et à qui en parler_

### 🔬 Les IST, sans dramatiser  _(4 min)_

> Souvent sans symptôme, presque toujours traitables, faciles à dépister.

**Le plus important : souvent aucun symptôme**

La chlamydia, l'infection la plus fréquente chez les jeunes, est silencieuse dans la majorité des cas. On peut la transmettre sans le savoir. C'est pourquoi le dépistage ne se décide pas « quand ça fait mal » mais régulièrement, et à chaque changement de partenaire.

**Pourquoi il ne faut pas laisser traîner**

Une chlamydia non traitée peut remonter et abîmer les trompes, ce qui provoque des douleurs chroniques et peut compromettre une grossesse future. Traitée, c'est souvent une simple cure d'antibiotiques.

**Les signes qui doivent alerter**

Pertes inhabituelles ou malodorantes, brûlures en urinant, boutons ou plaies sur les parties génitales, démangeaisons, douleur au bas-ventre, douleur pendant les rapports. Mais encore une fois : leur absence ne prouve rien.

**Le vaccin contre le HPV**

Le papillomavirus se transmet très facilement et cause la quasi-totalité des cancers du col de l'utérus. Le vaccin est recommandé aux filles comme aux garçons, idéalement avant les premiers rapports, et reste utile après. C'est l'un des rares vaccins qui prévient un cancer.

**Se faire dépister**

Un dépistage, c'est en général une prise de sang et/ou un prélèvement urinaire : rapide et indolore. Centres de dépistage gratuits, planning familial, médecin généraliste. Souvent gratuit et anonyme pour les jeunes.

### 💬 Le consentement  _(3 min)_

> Ce que c'est, ce que ça n'est pas, et pourquoi ça se redemande.

**Un oui libre, clair, et réversible**

Le consentement, c'est un accord donné librement, sans pression ni chantage affectif, par une personne en état de décider — donc ni endormie, ni ivre, ni droguée. Et il peut être retiré à tout moment, même après avoir commencé.

**Ce qui n'est pas un consentement**

Le silence. Le fait de ne pas oser dire non. Avoir dit oui la dernière fois. Être en couple ou marié. Avoir accepté un baiser. Porter une certaine tenue. Avoir bu. Céder pour avoir la paix.

**Ça marche dans les deux sens**

Demander n'est pas gênant, c'est la base : « tu as envie ? », « ça va ? », « tu veux qu'on s'arrête ? ». Et personne ne te doit de justification pour dire non.

**Retirer le préservatif en cachette est une agression**

L'enlever pendant le rapport sans que l'autre le sache annule le consentement donné et expose à une grossesse et à des infections. C'est reconnu comme une infraction dans un nombre croissant de pays.

**Si ça s'est mal passé**

Ce n'est jamais ta faute, quelles que soient les circonstances. Tu peux en parler à un adulte de confiance, à un soignant, ou appeler un numéro d'aide. Une contraception d'urgence et un dépistage restent possibles. Un examen médical peut aussi conserver des preuves si tu décides plus tard de porter plainte.

### 🤝 À qui en parler  _(2 min)_

> Des interlocuteurs gratuits et confidentiels, même mineure.

**Le planning familial**

C'est l'endroit le plus adapté : contraception, dépistage, contraception d'urgence, grossesse, violences. Gratuit, confidentiel, et accessible aux mineures sans autorisation parentale dans de nombreux pays.

**L'infirmerie de ton établissement**

Souvent sous-estimée. L'infirmière scolaire est tenue au secret professionnel, peut délivrer une contraception d'urgence et orienter vers les bons services.

**Un pharmacien**

Accessible sans rendez-vous, il peut conseiller sur la contraception d'urgence, les tests de grossesse et les douleurs de règles, et dire quand il faut consulter.

**Un adulte de confiance**

Un parent, une tante, une grande sœur, un professeur. Ce n'est pas toujours possible ni simple, mais ne reste pas seule avec une inquiétude — la plupart des situations se règlent bien quand on en parle tôt.

**Et en cas d'urgence**

Si tu es en danger immédiat, appelle les secours. Beaucoup de pays disposent de lignes d'écoute anonymes et gratuites pour les violences sexuelles et conjugales ; une recherche au nom de ton pays te donnera le numéro local.

