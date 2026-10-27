# ترند المغرب — radar des tendances marocaines (Android)

Application Android native (Kotlin, Jetpack Compose, Material 3), entièrement en arabe et de droite à gauche, avec un design noir, or et rouge. Elle répond à une question : **de quoi parlent les Marocains sur Internet ces dernières 24 heures ?**

**Sans serveur et sans abonnement** : le téléphone interroge lui-même les sources publiques, puis analyse les données localement. Le cache local est effacé au bout de 48 heures.

## Écrans
| Onglet | Contenu |
|---|---|
| **الرئيسية** | « ماذا يحدث في المغرب الآن؟ », compteurs réels (publications analysées, sujets, sources actives), Top 5 avec courbe horaire et progression, « ترند صاعد », barre « اسأل محلل الترند », couverture des sources |
| **الترندات** | Top 20, filtres الكل / سياسة / رياضة / مجتمع / فن / فيديوهات / جدل / اقتصاد / دولي, fenêtres 3 / 6 / 12 / 24 h |
| **آراء المغاربة** | Résumé extractif, fiabilité, « لماذا يتحدث عنه الناس؟ », répartition des opinions (anneau), mots les plus répétés, évolution sur 24 h, avis favorables et opposés, commentaires marquants sans nom d'auteur, sources avec liens |
| **الفيديوهات** | Vidéo en vedette avec « شاهد الفيديو », « الأكثر انتشاراً في المغرب », vidéos similaires, vues par heure et progression réelle depuis la dernière actualisation |
| **المزيد** | Clé YouTube facultative, état de chaque source, méthode |
| **محلل الترند** | Questions en langage naturel (arabe, darija, français), avec des réponses tirées uniquement des sujets collectés |

## Sources
| Source | Accès | Données |
|---|---|---|
| Presse marocaine (هسبريس, اليوم 24, كود, تيل كيل, Hespress FR, Médias24, Yabiladi, TelQuel, H24info, Le Site Info, MAP) | RSS | titres, extraits, dates |
| Google Actualités (AR, FR) | RSS | articles d'autres médias |
| Google Trends Maroc | RSS | recherches en hausse, avec un nombre minimal de recherches |
| Chaînes YouTube marocaines (Hespress, 2M, Medi1 TV, Chouf TV, Al Aoula, Kifache, Télé Maroc) | RSS public | vues et « j'aime » réels |
| Reddit r/Morocco | RSS public | discussions et commentaires |
| YouTube Data API (**facultatif**, clé gratuite) | API | tendances YouTube au Maroc, nombre de commentaires, analyse des commentaires |
| Facebook, Instagram, TikTok | — | ❌ aucun accès légal pour une application : affiché comme « non couvert » |
| X | API payante | ❌ non activé |

Une source en panne n'empêche jamais les autres de fonctionner : elle est signalée dans « حالة المصادر ».

## Principes
- **Aucune donnée inventée.** Chaque chiffre vient d'une source. Une mesure absente s'affiche comme absente.
- **Résumés extractifs** : on reprend un extrait de la source, sans générer de texte.
- **Progression** = nombre de publications des 6 dernières heures comparé aux 6 heures précédentes.
- **Opinions** : classement automatique approximatif (lexique arabe, darija, français et anglais, avec gestion de la négation), appliqué à un échantillon de commentaires publics. La taille de l'échantillon est toujours affichée, et le résultat n'est jamais présenté comme l'avis de tous les Marocains.
- **Polémique (جدل)** : détectée par le vocabulaire des titres (جدل، غضب، انتقادات، polémique…). Cette règle est transparente.
- **Confidentialité** : aucun nom d'auteur de commentaire n'est conservé ni affiché.

## Compilation
Chaque `push` lance GitHub Actions (`.github/workflows/apk.yml`), qui :
1. compile l'APK ;
2. lance les tests du moteur ;
3. publie l'APK dans la release `apk`. Lien de téléchargement direct : `https://github.com/ajimayell-wq/trend-almaghrib/releases/latest/download/trend-almaghrib.apk`.

La clé de signature est fixe (`app/trend-debug.keystore`) : les nouvelles versions s'installent par-dessus l'ancienne. Elle est réservée à un usage personnel et ne convient pas au Play Store.

## Code
- `engine/` : moteur pur Kotlin, testé (`app/src/test`) : analyse des flux, normalisation FR / AR / darija / Arabizi, regroupement TF-IDF, score, rubriques, polémique, sentiment, opinions, assistant.
- `data/` : réseau (OkHttp), cache JSON, dépôt et API YouTube facultative.
- `ui/` : écrans Compose.
